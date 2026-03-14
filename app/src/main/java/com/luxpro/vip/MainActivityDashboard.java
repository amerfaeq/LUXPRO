package com.luxpro.vip;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
// المكتبة اللي كانت ناقصة ومسببة اللون الأحمر
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class MainActivityDashboard extends BaseActivity {

    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    public boolean isEngineReady = false;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_dashboard);

        // 1️⃣ تفعيل رادار كشف الانهيارات أولاً
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);

        bottomNav = findViewById(R.id.bottomNav);

        // ── زر تبديل اللغة (ImageButton في الـ XML) ──
        ImageButton btnLang = findViewById(R.id.btnLanguageToggle);
        if (btnLang != null) {
            btnLang.setOnClickListener(v -> {
                String newLang = LocaleHelper.toggleLanguage(this);
                Toast.makeText(this,
                        newLang.equals(LocaleHelper.LANG_AR) ? "تم التحويل للعربية ✅" : "Switched to English ✅",
                        Toast.LENGTH_SHORT).show();
                Intent restart = getPackageManager().getLaunchIntentForPackage(getPackageName());
                if (restart != null) {
                    restart.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    finishAffinity();
                    startActivity(restart);
                }
            });
        }

        // ── تهيئة المحرك ──
        initializeEternalEngine();

        // ── الصفحة الافتراضية ──
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new HomeFragment()).commit();

        // ── التنقل بين الصفحات ──
        if (bottomNav != null) {
            bottomNav.setOnItemSelectedListener(item -> {
                Fragment selected = null;
                int id = item.getItemId();
                if (id == R.id.nav_home)
                    selected = new HomeFragment();
                else if (id == R.id.nav_rewards)
                    selected = new RewardsFragment();
                else if (id == R.id.nav_settings)
                    selected = new SettingsFragment();
                else if (id == R.id.nav_admin)
                    selected = new AdminFragment();
                if (selected != null)
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.container, selected).commit();
                return true;
            });
        }

        // ── فحص Admin ──
        checkIfAdmin();
    }

    private void checkIfAdmin() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null)
            return;
        FirebaseDatabase.getInstance().getReference("Admins").child(uid)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        if (snap.exists() && Boolean.TRUE.equals(snap.getValue(Boolean.class))) {
                            if (bottomNav != null) {
                                bottomNav.setVisibility(View.GONE);
                            }
                            getSupportFragmentManager().beginTransaction()
                                    .replace(R.id.container, new AdminFragment()).commit();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError e) {
                    }
                });
    }

    public void initializeEternalEngine() {
        new Thread(() -> {
            // محاولة جلب حالة المحرك
            try {
                String status = NativeEngine.getInstance().getEngineStatus();
                int pid = NativeEngine.getInstance().getTargetPid();

                runOnUiThread(() -> {
                    Toast.makeText(this, status + "\n8BP PID: " + pid, Toast.LENGTH_LONG).show();
                    isEngineReady = true;
                });
            } catch (Exception e) {
                // إذا فشل المحرك، الرادار سيسجل السبب هنا
                FirebaseCrashlytics.getInstance().recordException(e);
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacksAndMessages(null);
    }
}