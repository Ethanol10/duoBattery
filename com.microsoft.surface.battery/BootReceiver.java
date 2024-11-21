package com.microsoft.surface.battery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/* loaded from: classes.dex */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BootReceiver.class.getName();

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        Boolean bool = Boolean.FALSE;
        try {
            bool = Boolean.valueOf(BatteryService.isDocEnable(context));
            Log.d(TAG, "isDocEnabled is " + bool);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (bool.booleanValue()) {
            if ("android.intent.action.LOCKED_BOOT_COMPLETED".equals(intent.getAction())) {
                Log.d(TAG, "Boot completed intent Received");
                context.startService(new Intent(context, (Class<?>) BatteryService.class));
            }
            if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
                BatteryService.sharedPreferencesInit(context);
                return;
            }
            return;
        }
        Log.d(TAG, "BatteryService start condition not meet");
    }
}