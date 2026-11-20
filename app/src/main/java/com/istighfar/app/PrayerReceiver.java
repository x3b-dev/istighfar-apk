package com.istighfar.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Shows the "it is time for prayer X" notification with the normal notification sound. */
public class PrayerReceiver extends BroadcastReceiver {

    static final String CHANNEL = "istighfar_prayers";

    @Override
    public void onReceive(Context c, Intent in) {
        String name = in.getStringExtra("name");
        if (name == null || name.length() == 0) return;
        int i = in.getIntExtra("i", 0);

        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        NotificationChannel ch = new NotificationChannel(
                CHANNEL, "مواقيت الصلاة", NotificationManager.IMPORTANCE_HIGH);
        ch.setDescription("تنبيه عند دخول وقت الصلاة");
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);

        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 1, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String text = "حان الآن موعد أذان " + name;
        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_notif)
                .setColor(0xFFCE8E27)
                .setContentTitle(name)
                .setContentText(text)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build();
        nm.notify(400 + (i % 80), n);
    }
}
