package pw.thedrhax.mosmetro.preferences;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;

import pw.thedrhax.mosmetro.R;
import pw.thedrhax.util.CredentialStore;

/** Login / password pair stored in {@link CredentialStore}, edited in a dialog. */
public class LoginFormPreference extends Preference {
    private CredentialStore creds;

    public LoginFormPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public LoginFormPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public LoginFormPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public LoginFormPreference(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        creds = new CredentialStore(context);
        setPersistent(false);
    }

    @Override
    protected void onClick() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.loginform_preference, null);

        EditText text_login = v.findViewById(R.id.text_login);
        text_login.setText(creds.getLogin(getKey()));

        EditText text_password = v.findViewById(R.id.text_password);
        text_password.setText(creds.getPassword(getKey()));

        new AlertDialog.Builder(getContext())
                .setTitle(getTitle())
                .setView(v)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> creds.put(getKey(),
                        text_login.getText().toString(),
                        text_password.getText().toString()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
