/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 */

package pw.thedrhax.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import android.util.Base64;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Stores login/password with AES-GCM via AndroidKeystore.
 * Falls back to plaintext SharedPreferences if Keystore is unavailable.
 * Migrates old plaintext values on first read.
 */
public final class CredentialStore {
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "WifiMetroCreds";
    private static final String PREFS = "wifimetro_secure_creds";
    private static final String PREFIX = "enc:";

    private final SharedPreferences secure;
    private final SharedPreferences legacy;

    public CredentialStore(@NonNull Context context) {
        Context app = context.getApplicationContext();
        this.secure = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        this.legacy = PreferenceManager.getDefaultSharedPreferences(app);
        ensureKey();
    }

    public String getLogin(String key) {
        return get(key + "_login");
    }

    public String getPassword(String key) {
        return get(key + "_password");
    }

    public void put(String key, String login, String password) {
        putValue(key + "_login", login != null ? login : "");
        putValue(key + "_password", password != null ? password : "");
        // Remove plaintext leftovers
        legacy.edit()
                .remove(key + "_login")
                .remove(key + "_password")
                .apply();
    }

    private String get(String fullKey) {
        String stored = secure.getString(fullKey, null);
        if (stored != null) {
            return decrypt(stored);
        }

        String legacyValue = legacy.getString(fullKey, "");
        if (legacyValue != null && !legacyValue.isEmpty()) {
            putValue(fullKey, legacyValue);
            legacy.edit().remove(fullKey).apply();
        }
        return legacyValue != null ? legacyValue : "";
    }

    private void putValue(String fullKey, String value) {
        secure.edit().putString(fullKey, encrypt(value)).apply();
    }

    private void ensureKey() {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            if (ks.containsAlias(KEY_ALIAS)) return;

            KeyGenerator kg = KeyGenerator.getInstance("AES", ANDROID_KEYSTORE);
            android.security.keystore.KeyGenParameterSpec spec =
                    new android.security.keystore.KeyGenParameterSpec.Builder(
                            KEY_ALIAS,
                            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT
                                    | android.security.keystore.KeyProperties.PURPOSE_DECRYPT)
                            .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setKeySize(256)
                            .build();
            kg.init(spec);
            kg.generateKey();
        } catch (Exception ignored) {
            // Fallback: plaintext in secure prefs file (still excluded from backup)
        }
    }

    private SecretKey getKey() throws GeneralSecurityException {
        try {
            KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
            ks.load(null);
            return (SecretKey) ks.getKey(KEY_ALIAS, null);
        } catch (Exception e) {
            throw new GeneralSecurityException(e);
        }
    }

    private String encrypt(String plain) {
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getKey(), new GCMParameterSpec(128, iv));
            byte[] enc = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + enc.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(enc, 0, out, iv.length, enc.length);
            return PREFIX + Base64.encodeToString(out, Base64.NO_WRAP);
        } catch (Exception e) {
            return plain;
        }
    }

    private String decrypt(String stored) {
        if (stored == null) return "";
        if (!stored.startsWith(PREFIX)) return stored;
        try {
            byte[] all = Base64.decode(stored.substring(PREFIX.length()), Base64.NO_WRAP);
            byte[] iv = new byte[12];
            byte[] enc = new byte[all.length - 12];
            System.arraycopy(all, 0, iv, 0, 12);
            System.arraycopy(all, 12, enc, 0, enc.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getKey(), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(enc), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
}
