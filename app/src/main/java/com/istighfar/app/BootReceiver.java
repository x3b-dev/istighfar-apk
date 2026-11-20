package com.istighfar.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Re-arms reminders and prayer alarms after a reboot or an app update. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent in) {
        if (Reminders.isEnabled(c)) Reminders.scheduleAll(c);
        Prayers.scheduleAll(c);
    }
}
