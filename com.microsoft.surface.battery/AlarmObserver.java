package com.microsoft.surface.battery;

import android.app.AlarmManager;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.util.Log;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/* loaded from: classes.dex */
public class AlarmObserver extends ContentObserver {
    private static Context observerContext;
    private static final String TAG = AlarmObserver.class.getName();
    private static Integer docTimeRange = 10;
    private static Integer docExitRange = 2;
    private static long startDocTimeInMillis = -1;
    private static long endDocTimeInMillis = -1;
    private static boolean DOCActiveState = false;

    private static void setStartDOCTime(Calendar calendar) {
        startDocTimeInMillis = calendar.getTimeInMillis() - (((docTimeRange.intValue() * 60) * 60) * 1000);
    }

    private static void setEndDOCTime(Calendar calendar) {
        endDocTimeInMillis = calendar.getTimeInMillis() - (((docExitRange.intValue() * 60) * 60) * 1000);
    }

    AlarmObserver(Handler handler, Context context) {
        super(handler);
        observerContext = context;
        Log.d(TAG, "Alarm Observer constructs");
    }

    @Override // android.database.ContentObserver
    public void onChange(boolean z) {
        super.onChange(z);
        try {
            BatteryService.isInActiveDOC(observerContext);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private static boolean checkValidAlarm(Calendar calendar) {
        int i = calendar.get(11);
        int i2 = calendar.get(12);
        if (5 <= i) {
            if (i < 10) {
                return true;
            }
            if (i == 10 && i2 == 0) {
                return true;
            }
        }
        Log.d(TAG, "Next alarm is not set within WakeUpHour");
        return false;
    }

    private static void handleChange(Calendar calendar) throws ParseException {
        AlarmManager alarmManager = (AlarmManager) observerContext.getSystemService("alarm");
        if (alarmManager != null) {
            AlarmManager.AlarmClockInfo nextAlarmClock = alarmManager.getNextAlarmClock();
            if (nextAlarmClock != null) {
                long triggerTime = nextAlarmClock.getTriggerTime();
                Calendar calendar2 = Calendar.getInstance();
                calendar2.setTimeInMillis(triggerTime);
                String format = String.format("%02d:%02d", Integer.valueOf(calendar2.get(11)), Integer.valueOf(calendar2.get(12)));
                Log.d(TAG, "Next alarm is " + calendar2.getDisplayName(7, 1, Locale.getDefault()) + " " + format);
                if (checkValidAlarm(calendar2)) {
                    setStartDOCTime(calendar2);
                    setEndDOCTime(calendar2);
                    DOCActiveState = true;
                    return;
                }
                DOCActiveState = false;
                return;
            }
            Log.d(TAG, "Error at getting getNextAlarmClock. DOC will not be executed");
            DOCActiveState = false;
            return;
        }
        Log.d(TAG, "Error at getting ALARM_SERVICE. DOC will not be executed");
        DOCActiveState = false;
    }

    static Boolean IsDOCTimeRange() throws ParseException {
        Calendar calendar = Calendar.getInstance();
        long timeInMillis = calendar.getTimeInMillis();
        String format = String.format("%02d:%02d", Integer.valueOf(calendar.get(11)), Integer.valueOf(calendar.get(12)));
        String str = TAG;
        Log.d(str, "Current local time: " + calendar.getDisplayName(7, 1, Locale.getDefault()) + " " + format);
        handleChange(calendar);
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTimeInMillis(startDocTimeInMillis);
        String format2 = String.format("%02d:%02d", Integer.valueOf(calendar2.get(11)), Integer.valueOf(calendar2.get(12)));
        Calendar calendar3 = Calendar.getInstance();
        calendar3.setTimeInMillis(endDocTimeInMillis);
        Log.d(str, "DOC start time: " + calendar2.getDisplayName(7, 1, Locale.getDefault()) + " " + format2 + " DOC end time: " + calendar3.getDisplayName(7, 1, Locale.getDefault()) + " " + String.format("%02d:%02d", Integer.valueOf(calendar3.get(11)), Integer.valueOf(calendar3.get(12))));
        if (DOCActiveState && startDocTimeInMillis <= timeInMillis && timeInMillis <= endDocTimeInMillis) {
            Log.d(str, "Current local time is within DOC time range");
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    static String getEarliestAlarm() throws ParseException {
        AlarmManager alarmManager = (AlarmManager) observerContext.getSystemService("alarm");
        if (alarmManager != null) {
            AlarmManager.AlarmClockInfo nextAlarmClock = alarmManager.getNextAlarmClock();
            if (nextAlarmClock != null) {
                long triggerTime = nextAlarmClock.getTriggerTime();
                Date date = new Date();
                date.setTime(triggerTime);
                Log.d(TAG, "Next alarm time is: " + date.toString());
                return date.toString();
            }
            Log.d(TAG, "No alarm is set");
            return "No alarm is set";
        }
        Log.d(TAG, "Error at getting Alarm Service");
        return "No alarm is set";
    }
}