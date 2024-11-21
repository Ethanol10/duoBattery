package com.microsoft.surface.battery;

import android.app.Service;
import android.app.StatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import android.util.StatsEvent;
import com.android.internal.util.ConcurrentUtils;
import java.text.ParseException;
import java.util.List;

/* loaded from: classes.dex */
public class BatteryService extends Service {
    protected static Context mContext;
    private static SharedPreferences mPrefs;
    private static SharedPreferences.Editor mPrefsEditor;
    private StatsManager mStatsManager;
    private StatsPullAtomCallbackImpl pullAtomCallback;
    private static final String TAG = BatteryService.class.getName();
    private static final ChargingReceiver chargingReceiver = new ChargingReceiver();
    private static boolean isFirstCall = true;
    private static boolean prevPlugInState = false;
    private static Integer prevDocActiveState = 0;
    private static boolean prevDOCEnabledByUser = false;
    private static long DOCOptOutTotalEventsCount = 0;

    @Override // android.app.Service
    public IBinder onBind(Intent intent) {
        return null;
    }

    private class StatsPullAtomCallbackImpl implements StatsManager.StatsPullAtomCallback {
        private StatsPullAtomCallbackImpl() {
        }

        public int onPullAtom(int i, List<StatsEvent> list) {
            Log.d(BatteryService.TAG, "Pull message callback function get called for tagId " + i);
            if (i != 150200) {
                throw new UnsupportedOperationException("Unknown tagId = " + i);
            }
            Integer num = 0;
            try {
                Boolean valueOf = Boolean.valueOf(ChargingReceiver.getIsPluggedIn());
                Boolean valueOf2 = Boolean.valueOf(DOCObserver.getIsDOCEnabledByUser());
                Boolean IsDOCTimeRange = AlarmObserver.IsDOCTimeRange();
                String earliestAlarm = AlarmObserver.getEarliestAlarm();
                if (valueOf.booleanValue() && valueOf2.booleanValue() && IsDOCTimeRange.booleanValue()) {
                    num = 1;
                }
                list.add(StatsEvent.newBuilder().setAtomId(i).writeInt(num.intValue()).writeBoolean(valueOf.booleanValue()).writeString(earliestAlarm).writeBoolean(valueOf2.booleanValue()).writeLong(BatteryService.DOCOptOutTotalEventsCount).build());
                Log.e(BatteryService.TAG, "Added data to list & returning from callback");
                return 0;
            } catch (Exception e) {
                e.printStackTrace();
                return 1;
            }
        }
    }

    @Override // android.app.Service
    public int onStartCommand(Intent intent, int i, int i2) {
        Log.d(TAG, "BatteryService started");
        Context applicationContext = getApplicationContext();
        mContext = applicationContext;
        registerReceivers(applicationContext);
        this.mStatsManager = (StatsManager) mContext.getSystemService("stats");
        registerBatteryServiceStats();
        return super.onStartCommand(intent, i, i2);
    }

    static void registerReceivers(Context context) {
        context.registerReceiver(chargingReceiver, new IntentFilter("android.intent.action.BATTERY_CHANGED"));
        context.getContentResolver().registerContentObserver(Settings.Global.getUriFor("app_smart_charging_enabled"), true, new DOCObserver(new Handler(), context));
        context.getContentResolver().registerContentObserver(Settings.System.getUriFor("next_alarm_formatted"), true, new AlarmObserver(new Handler(), context));
    }

    static boolean isInActiveDOC(Context context) throws ParseException {
        Integer num = 0;
        Boolean valueOf = Boolean.valueOf(ChargingReceiver.getIsPluggedIn());
        Boolean valueOf2 = Boolean.valueOf(DOCObserver.getIsDOCEnabledByUser());
        Boolean IsDOCTimeRange = AlarmObserver.IsDOCTimeRange();
        String str = TAG;
        Log.d(str, "DOC by user = " + valueOf2);
        Log.d(str, "Device pluggedin = " + valueOf);
        Log.d(str, "In DOC time range = " + IsDOCTimeRange);
        Boolean valueOf3 = Boolean.valueOf(isDocEnable(context));
        boolean z = true;
        if (valueOf.booleanValue() && valueOf2.booleanValue() && IsDOCTimeRange.booleanValue() && valueOf3.booleanValue()) {
            num = 1;
        }
        if (isFirstCall || prevDocActiveState != num) {
            boolean dOCProperty = setDOCProperty(num.toString());
            if (dOCProperty) {
                Log.d(str, "successfully set doc_en sysfs node as " + num);
                z = dOCProperty;
            } else {
                Log.d(str, "failed to set doc_en sysfs node as " + num);
                return dOCProperty;
            }
        }
        if (isFirstCall || prevPlugInState != valueOf.booleanValue() || prevDocActiveState != num || prevDOCEnabledByUser != valueOf2.booleanValue()) {
            Log.d(str, "prevPlugInState = " + prevPlugInState + " isPluggedIn = " + valueOf);
            Log.d(str, "prevDocActiveState = " + prevDocActiveState + " isInActiveDOC = " + num);
            Log.d(str, "prevDOCEnabledByUser = " + prevDOCEnabledByUser + " isDOCEnabledByUser = " + valueOf2);
            if (prevDOCEnabledByUser != valueOf2.booleanValue() && !valueOf2.booleanValue()) {
                DOCOptOutTotalEventsCount++;
                Log.d(str, "DOCOptOutTotalEventsCount = " + DOCOptOutTotalEventsCount);
                SharedPreferences sharedPreferences = mPrefs;
                if (sharedPreferences != null) {
                    SharedPreferences.Editor edit = sharedPreferences.edit();
                    mPrefsEditor = edit;
                    edit.putLong("doc_opt_out_total_event_count", DOCOptOutTotalEventsCount);
                    mPrefsEditor.apply();
                }
            }
            prevPlugInState = valueOf.booleanValue();
            prevDocActiveState = num;
            prevDOCEnabledByUser = valueOf2.booleanValue();
            isFirstCall = false;
            SurfaceBatteryServiceStatsLog.write(105400, num.intValue(), valueOf.booleanValue(), AlarmObserver.getEarliestAlarm(), valueOf2.booleanValue(), DOCOptOutTotalEventsCount);
        }
        return z;
    }

    private static boolean setDOCProperty(String str) {
        try {
            SystemProperties.set("persist.vendor.battery.doc_en", str);
            return true;
        } catch (Exception unused) {
            return false;
        }
    }

    private void registerBatteryServiceStats() {
        String str = TAG;
        Log.d(str, "registering callback for statsd");
        StatsPullAtomCallbackImpl statsPullAtomCallbackImpl = new StatsPullAtomCallbackImpl();
        this.pullAtomCallback = statsPullAtomCallbackImpl;
        this.mStatsManager.setPullAtomCallback(150200, (StatsManager.PullAtomMetadata) null, ConcurrentUtils.DIRECT_EXECUTOR, statsPullAtomCallbackImpl);
        Log.d(str, "Called back registed for pull atom \n150200");
    }

    static boolean isDocEnable(Context context) {
        Resources system = Resources.getSystem();
        String string = system.getString(system.getIdentifier("config_smart_charging_available", "bool", "android"));
        String str = TAG;
        Log.d(str, "config_smart_charging_available is " + string);
        boolean booleanValue = Boolean.valueOf(string).booleanValue();
        boolean z = Settings.Secure.getInt(context.getContentResolver(), context.getResources().getString(R.string.smartcharging_setting), 0) == 1;
        Log.d(str, "optimizedChargingSecureSettingsEnabled is " + z);
        return booleanValue || z;
    }

    static void sharedPreferencesInit(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("BatteryServicePrefs", 0);
        mPrefs = sharedPreferences;
        DOCOptOutTotalEventsCount = sharedPreferences.getLong("doc_opt_out_total_event_count", 0L);
    }
}