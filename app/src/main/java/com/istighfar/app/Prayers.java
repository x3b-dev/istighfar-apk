package com.istighfar.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Prayer-time alarms. The page computes the times (offline) and hands a JSON array
 * [{"t":epochMillis,"n":"الفجر"}, ...] to the app; we store it and arm one alarm per entry.
 */
public class Prayers {

    static final String PREFS = "istg_prefs";
    static final String KEY = "prayer_alarms";
    static final int BASE = 1000;
    static final int MAX = 80;

    static void save(Context c, String json) {
        SharedPreferences sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY, json == null ? "[]" : json).apply();
        scheduleAll(c);
    }

    static void cancelAll(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        for (int i = 0; i < MAX; i++) {
            PendingIntent old = pending(c, i, "", PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
            if (old != null) am.cancel(old);   // cancel(null) throws NPE on targetSdk 24+
        }
    }

    static void scheduleAll(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        try { cancelAll(c); } catch (Exception ignored) { }
        String raw = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]");
        long now = System.currentTimeMillis();
        try {
            JSONArray arr = new JSONArray(raw);
            int n = Math.min(arr.length(), MAX);
            for (int i = 0; i < n; i++) {
                JSONObject o = arr.getJSONObject(i);
                long t = o.getLong("t");
                if (t <= now) continue;
                PendingIntent pi = pending(c, i, o.optString("n", ""),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                if (pi != null) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi);
            }
        } catch (Exception ignored) {
        }
    }

    private static PendingIntent pending(Context c, int i, String name, int flags) {
        Intent in = new Intent(c, PrayerReceiver.class);
        in.putExtra("name", name);
        in.putExtra("i", i);
        return PendingIntent.getBroadcast(c, BASE + i, in, flags);
    }
}
