package com.microsoft.surface.battery;

import android.util.StatsEvent;
import android.util.StatsLog;

/* loaded from: classes.dex */
public class SurfaceBatteryServiceStatsLog {
    public static void write(int i, int i2, boolean z, String str, boolean z2, long j) {
        StatsEvent.Builder newBuilder = StatsEvent.newBuilder();
        newBuilder.setAtomId(i);
        newBuilder.writeInt(i2);
        newBuilder.writeBoolean(z);
        newBuilder.writeString(str);
        newBuilder.writeBoolean(z2);
        newBuilder.writeLong(j);
        newBuilder.usePooledBuffer();
        StatsLog.write(newBuilder.build());
    }
}