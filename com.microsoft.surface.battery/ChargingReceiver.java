package com.microsoft.surface.battery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import java.text.ParseException;

/* loaded from: classes.dex */
public class ChargingReceiver extends BroadcastReceiver {
    private static boolean isPluggedIn = false;
    private final String TAG = ChargingReceiver.class.getName();

    @Override // android.content.BroadcastReceiver
    public void onReceive(Context context, Intent intent) {
        if (intent.getIntExtra("plugged", -1) == 0) {
            Log.d(this.TAG, "Device is not charging");
            isPluggedIn = false;
        } else {
            Log.d(this.TAG, "Device is charging");
            isPluggedIn = true;
        }
        try {
            BatteryService.isInActiveDOC(context);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    static boolean getIsPluggedIn() {
        return isPluggedIn;
    }
}