package com.luxpro.vip;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * LUX PRO — BaseActivity
 * جميع Activities ترث منها لضمان تطبيق اللغة الصحيحة في كل شاشة.
 */
public class BaseActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();
        checkTimeTamper();
        startSubscriptionMonitor();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopSubscriptionMonitor();
    }

    // Guard: prevents multiple security dialogs from stacking every 30s
    private boolean isSecurityDialogShowing = false;

    private void checkTimeTamper() {
        if (isSecurityDialogShowing) return; // Already showing, skip
        if (TimeSyncManager.isSynced() && TimeSyncManager.getMonotonicDrift() > 120000) {
            showTimeTamperDialog();
        }
    }

    private android.os.Handler monitorHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable monitorRunnable = new Runnable() {
        @Override
        public void run() {
            checkSubscriptionStatus();
            checkTimeTamper(); // Periodic check even if app is in foreground
            monitorHandler.postDelayed(this, 30000); // Check every 30 seconds
        }
    };

    private void startSubscriptionMonitor() {
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null) {
            monitorHandler.post(monitorRunnable);
        }
    }

    private void stopSubscriptionMonitor() {
        monitorHandler.removeCallbacks(monitorRunnable);
    }

    private void checkSubscriptionStatus() {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        com.google.firebase.database.FirebaseDatabase.getInstance().getReference("Users")
                .child(uid).child("expiry_date").addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Long val = snapshot.getValue(Long.class);
                    if (val != null) {
                        long expiryTime = val;
                        if (TimeSyncManager.getServerTime() >= expiryTime) {
                            showSessionExpiredDialog();
                        }
                    }
                }
            }
            @Override public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {}
        });
    }

    private void showSessionExpiredDialog() {
        if (isFinishing() || isSecurityDialogShowing) return; // Guard against duplicate dialogs
        isSecurityDialogShowing = true;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.security_session_expired_title)
                .setMessage(R.string.security_session_expired_msg)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    isSecurityDialogShowing = false;
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                    finishAffinity();
                    System.exit(0);
                })
                .show();
    }

    private void showTimeTamperDialog() {
        if (isFinishing() || isSecurityDialogShowing) return; // Guard against duplicate dialogs
        isSecurityDialogShowing = true;
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.security_time_tamper_title)
                .setMessage(R.string.security_time_tamper_msg)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    isSecurityDialogShowing = false;
                    finishAffinity();
                    System.exit(0);
                })
                .show();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }
}
