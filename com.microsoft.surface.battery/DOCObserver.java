package com.microsoft.surface.battery;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import java.text.ParseException;

/* loaded from: classes.dex */
public class DOCObserver extends ContentObserver {
    private static boolean isDOCEnabledByUser = false;
    private final String TAG;
    Context observerContext;

    DOCObserver(Handler handler, Context context) {
        super(handler);
        this.TAG = DOCObserver.class.getName();
        this.observerContext = context;
        try {
            handleChange();
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override // android.database.ContentObserver
    public void onChange(boolean z) {
        super.onChange(z);
        try {
            handleChange();
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        try {
            BatteryService.isInActiveDOC(this.observerContext);
        } catch (ParseException e2) {
            e2.printStackTrace();
        }
    }

    static boolean getIsDOCEnabledByUser() {
        return isDOCEnabledByUser;
    }

    private void handleChange() throws Settings.SettingNotFoundException {
        Integer valueOf = Integer.valueOf(Settings.Global.getInt(this.observerContext.getContentResolver(), "app_smart_charging_enabled", 1));
        Log.d(this.TAG, "DOC user settings=" + valueOf);
        isDOCEnabledByUser = valueOf.intValue() == 1;
    }
}