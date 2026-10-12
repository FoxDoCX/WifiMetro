/**
 * Wi-Fi в метро (pw.thedrhax.mosmetro, Moscow Wi-Fi autologin)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package pw.thedrhax.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Persistent log of authentication attempts (newest first in {@link #read}).
 */
public final class ConnectionHistory {
    private static final String PREF_KEY = "connection_history";
    private static final int MAX_ENTRIES = 100;

    public static final class Entry {
        public final long timestamp;
        public final String ssid;
        public final String provider;
        public final String result;
        public final long duration;

        Entry(long timestamp, String ssid, String provider, String result, long duration) {
            this.timestamp = timestamp;
            this.ssid = ssid;
            this.provider = provider;
            this.result = result;
            this.duration = duration;
        }

        public boolean isSuccess() {
            return "CONNECTED".equals(result) || "ALREADY_CONNECTED".equals(result);
        }
    }

    private final SharedPreferences settings;

    public ConnectionHistory(@NonNull Context context) {
        settings = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @SuppressWarnings("unchecked")
    public synchronized void add(String ssid, String provider, String result, long duration) {
        JSONArray array = load();

        JSONObject item = new JSONObject();
        item.put("ts", System.currentTimeMillis());
        item.put("ssid", ssid);
        item.put("provider", provider);
        item.put("result", result);
        item.put("duration", duration);
        array.add(item);

        while (array.size() > MAX_ENTRIES) {
            array.remove(0);
        }

        settings.edit().putString(PREF_KEY, array.toJSONString()).apply();
    }

    @NonNull
    public synchronized List<Entry> read() {
        List<Entry> result = new ArrayList<>();
        for (Object o : load()) {
            if (!(o instanceof JSONObject)) continue;
            JSONObject item = (JSONObject) o;
            result.add(new Entry(
                    asLong(item.get("ts")),
                    String.valueOf(item.get("ssid")),
                    String.valueOf(item.get("provider")),
                    String.valueOf(item.get("result")),
                    asLong(item.get("duration"))
            ));
        }
        Collections.reverse(result);
        return result;
    }

    @Nullable
    public Entry last() {
        List<Entry> all = read();
        return all.isEmpty() ? null : all.get(0);
    }

    public synchronized void clear() {
        settings.edit().remove(PREF_KEY).apply();
    }

    @NonNull
    public String toCsv() {
        StringBuilder sb = new StringBuilder("timestamp,ssid,provider,result,duration_ms\n");
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.ROOT);
        for (Entry e : read()) {
            sb.append(fmt.format(new java.util.Date(e.timestamp))).append(',')
                    .append(csv(e.ssid)).append(',')
                    .append(csv(e.provider)).append(',')
                    .append(e.result).append(',')
                    .append(e.duration).append('\n');
        }
        return sb.toString();
    }

    private static String csv(String value) {
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static long asLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0;
    }

    @NonNull
    private JSONArray load() {
        String raw = settings.getString(PREF_KEY, null);
        if (raw == null) return new JSONArray();
        try {
            Object parsed = new JSONParser().parse(raw);
            if (parsed instanceof JSONArray) return (JSONArray) parsed;
        } catch (ParseException ignored) {}
        return new JSONArray();
    }
}
