package com.istighfar.app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

/** Daily istighfar reminders at fixed local times. */
final class Reminders {

    /** Local hours (24h): 6 AM, 10 AM, 2 PM, 6 PM, 10 PM */
    static final int[] HOURS = {6, 10, 14, 18, 22};

    static final String[] MESSAGES = {
            "أستغفر الله العظيم وأتوب إليه… خفِّف حِملك بدقيقة استغفار",
            "حان وقت الاستغفار، فكلُّ استغفارةٍ تُسقِط عنك ذنبًا",
            "خذ نَفَسًا وقل: أستغفر الله، ثم أكمل يومك خفيفًا",
            "وَاسْتَغْفِرُوا اللَّهَ إِنَّ اللَّهَ غَفُورٌ رَحِيمٌ",
            "رَبِّ اغْفِرْ لِي وَتُبْ عَلَيَّ، إِنَّكَ أَنْتَ التَّوَّابُ الرَّحِيمُ",
            "مئةُ استغفارٍ تفرغ بها وعاء الذنوب… ابدأ الآن",
            "لا تدع يومك يمضي بلا استغفار، فالوعاء ينتظرك"
    };

    private static final String PREFS = "istg_prefs";
    private static final String KEY = "notif";

    private Reminders() {}

    static boolean isEnabled(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, false);
    }

    static void setEnabled(Context c, boolean on) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, on).apply();
        if (on) scheduleAll(c);
        else cancelAll(c);
    }

    static void scheduleAll(Context c) {
        for (int i = 0; i < HOURS.length; i++) schedule(c, i);
    }

    static void cancelAll(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        for (int i = 0; i < HOURS.length; i++) am.cancel(pending(c, i));
    }

    /** Schedules the next occurrence (today or tomorrow) of reminder [idx]. */
    static void schedule(Context c, int idx) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        Calendar t = Calendar.getInstance();
        t.set(Calendar.HOUR_OF_DAY, HOURS[idx]);
        t.set(Calendar.MINUTE, 0);
        t.set(Calendar.SECOND, 0);
        t.set(Calendar.MILLISECOND, 0);
        if (t.getTimeInMillis() <= System.currentTimeMillis() + 1000) {
            t.add(Calendar.DAY_OF_YEAR, 1);
        }
        // Works while the phone is locked / in Doze (may be delayed by a few minutes by the system).
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t.getTimeInMillis(), pending(c, idx));
    }

    static String messageFor(int idx) {
        int day = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
        return MESSAGES[(day * HOURS.length + idx) % MESSAGES.length];
    }

    private static PendingIntent pending(Context c, int idx) {
        Intent i = new Intent(c, ReminderReceiver.class);
        i.putExtra("idx", idx);
        return PendingIntent.getBroadcast(c, 100 + idx, i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
