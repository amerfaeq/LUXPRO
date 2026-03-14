package com.luxpro.vip;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Locale;
import com.luxpro.vip.BuildConfig;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class SplashActivity extends BaseActivity {

    private ProgressBar loadingBar;
    private TextView statusText;
    private int progressStatus = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        loadingBar = findViewById(R.id.progressBar);
        statusText = findViewById(R.id.tvStatus);

        // TTS
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale currentLocale = Locale.getDefault();
                if (currentLocale.getLanguage().equals("en")) {
                    tts.setLanguage(Locale.ENGLISH);
                    tts.setSpeechRate(0.8f);
                    tts.speak(getString(R.string.welcome_msg), TextToSpeech.QUEUE_FLUSH, null, null);
                } else {
                    tts.setLanguage(new Locale("ar"));
                    tts.setSpeechRate(0.8f);
                    tts.speak(getString(R.string.welcome_msg), TextToSpeech.QUEUE_FLUSH, null, null);
                }
            }
        });

        // تفعيل Crashlytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);

        // ── فحص التحديث الإجباري أولاً ──
        checkForceUpdate();
    }

    /** يفحص Firebase إذا في تحديث إجباري */
    private void checkForceUpdate() {
        FirebaseDatabase.getInstance().getReference("AppUpdate")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        if (snap.exists()) {
                            Long remoteVersion = snap.child("version").getValue(Long.class);
                            String apkUrl = snap.child("apk_url").getValue(String.class);
                            int localVersion = BuildConfig.VERSION_CODE;

                            if (remoteVersion != null && remoteVersion > localVersion && apkUrl != null) {
                                // تحديث إجباري - أظهر Dialog بدون زر إغلاق
                                showForceUpdateDialog(apkUrl);
                                return;
                            }
                        }
                        // لا يوجد تحديث - أكمل التحميل العادي
                        startLoadingAnimation();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError e) {
                        startLoadingAnimation();
                    }
                });
    }

    /** Dialog إجباري لا يمكن إغلاقه - يحمل التحديث ويثبته داخلياً */
    private void showForceUpdateDialog(String apkUrl) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.force_update_title))
                .setMessage(getString(R.string.force_update_msg))
                .setPositiveButton(R.string.update_now_btn, (d, w) -> {
                    if (apkUrl != null && !apkUrl.isEmpty()) {
                        Intent serviceIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl));
                        startActivity(serviceIntent);
                    }
                })
                .setCancelable(false)
                .create();

        dialog.show();
        dialog.setCanceledOnTouchOutside(false);
    }

    private void startLoadingAnimation() {
        new Thread(() -> {
            while (progressStatus < 100) {
                progressStatus += 1;
                handler.post(() -> {
                    loadingBar.setProgress(progressStatus);
                    statusText.setText(getString(R.string.initializing_msg, progressStatus));
                });
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interrupt flag
                    return; // Exit thread cleanly — Activity may be destroyed
                }
            }
            handler.post(() -> {
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            });
        }).start();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null); // Cancel pending posts before destruction
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}