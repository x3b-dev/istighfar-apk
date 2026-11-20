package com.istighfar.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReminderReceiver extends BroadcastReceiver {

    static final String CHANNEL = "istighfar_reminders";

    @Override
    public void onReceive(Context c, Intent in) {
        int idx = in.getIntExtra("idx", 0);
        if (idx < 0 || idx >= Reminders.HOURS.length) idx = 0;
        if (!Reminders.isEnabled(c)) return;
        Reminders.schedule(c, idx);   // arm tomorrow's reminder first
        show(c, idx);
    }

    static void show(Context c, int idx) {
        NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        NotificationChannel ch = new NotificationChannel(
                CHANNEL, "تذكير الاستغفار", NotificationManager.IMPORTANCE_DEFAULT);
        ch.setDescription("تذكير يومي بالاستغفار");
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        nm.createNotificationChannel(ch);

        Intent open = new Intent(c, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String text = Reminders.messageFor(idx);
        Notification n = new Notification.Builder(c, CHANNEL)
                .setSmallIcon(R.drawable.ic_notif)
                .setColor(0xFFCE8E27)
                .setContentTitle("استغفار")
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_REMINDER)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .build();
        nm.notify(200 + idx, n);
    }
}
