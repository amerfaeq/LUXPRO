package com.luxpro.vip;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import androidx.annotation.NonNull;

public class TimeSyncManager {
    // Volatile: written by Firebase background thread, read by UI thread
    private static volatile long serverTimeOffset = 0;
    private static volatile boolean isSynced = false;
    private static volatile long initialMonotonicRatio = 0;

    public static void init() {
        FirebaseDatabase.getInstance().getReference(".info/serverTimeOffset")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long offset = snapshot.getValue(Long.class);
                        if (offset != null) {
                            serverTimeOffset = offset;
                            isSynced = true;
                            // Capture the reference ratio at the first sync
                            if (initialMonotonicRatio == 0) {
                                initialMonotonicRatio = getServerTime() - android.os.SystemClock.elapsedRealtime();
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    public static long getServerTime() {
        return System.currentTimeMillis() + serverTimeOffset;
    }

    public static boolean isSynced() {
        return isSynced;
    }

    public static long getOffset() {
        return Math.abs(serverTimeOffset);
    }

    public static long getMonotonicDrift() {
        if (!isSynced || initialMonotonicRatio == 0) return 0;
        long currentRatio = getServerTime() - android.os.SystemClock.elapsedRealtime();
        return Math.abs(currentRatio - initialMonotonicRatio);
    }
}
