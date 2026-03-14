package com.luxpro.vip;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.NotificationCompat;

import com.luxpro.vip.auth.AccountVaultManager;

import java.util.Locale;

/**
 * LUX PRO Floating Menu Service – v1.1 Build 3
 * All features wired to native engine. Bridge, Localization, Live VFX params.
 */
public class FloatingMenuService extends Service {

    private static final String TAG = "LuxPRO";
    private NativeEngine nativeEngine;
    private AccountVaultManager vaultManager;

    // --- Native Master Bridge ---
    static {
        try {
            System.loadLibrary("LUXPRO");
            Log.i(TAG, "Native Master Library Loaded (libLUXPRO.so)");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Critical: libLUXPRO.so not found!");
        }
    }

    public native void setFeature(int featureNum, boolean active);
    public native String getLangString(String key);
    public native boolean isBridgeActive();

    private static final String ACTION_LOBBY_UPDATE = "com.luxpro.vip.LOBBY_UPDATE";
    private long lastClickTime = 0;

    // Current language: true = Arabic, false = English
    private boolean isArabic = true;

    // WindowManager & Views
    private WindowManager windowManager;
    private View modMenuView;
    private WindowManager.LayoutParams menuParams;
    private View floatingIcon;
    private WindowManager.LayoutParams iconParams;
    private boolean isMenuOpen = false;

    // State tracking for Bridge
    private boolean bridgeActive = false;

    // Icon size/opacity from settings
    private int iconSize = 130;
    private float iconAlpha = 1.0f;

    private static final String CHANNEL_ID = "LuxProChannel";
    private static final int NOTIF_ID = 1001;

    private final Handler handler = new Handler(Looper.getMainLooper());

    // ─── Broadcast receiver ──────────────────────────────────────────────────
    private final android.content.BroadcastReceiver accountReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.luxpro.vip.UPDATE_ACCOUNT".equals(intent.getAction()) && vaultManager != null) {
                Log.i(TAG, "Account sync triggered");
            } else if (ACTION_LOBBY_UPDATE.equals(intent.getAction())) {
                boolean inLobby = intent.getBooleanExtra("in_lobby", false);
                if (inLobby && NativeEngine.isLibraryLoaded()) {
                    NativeEngine.getInstance().triggerLogSelfDestruct();
                }
            }
        }
    };

    private boolean isClickDebounced() {
        long now = System.currentTimeMillis();
        if (now - lastClickTime < 400) return false;
        lastClickTime = now;
        return true;
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onCreate() {
        super.onCreate();

        Locale locale = getResources().getConfiguration().locale;
        isArabic = locale.getLanguage().startsWith("ar");

        vaultManager = new AccountVaultManager(this);
        NativeEngine.loadNativeLibrary();
        nativeEngine = NativeEngine.getInstance();

        if (NativeEngine.isLibraryLoaded()) {
            nativeEngine.initAdvancedHooks(this);
        }

        Object svc = getSystemService(WINDOW_SERVICE);
        if (svc instanceof WindowManager) windowManager = (WindowManager) svc;

        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());

        android.content.IntentFilter filter = new android.content.IntentFilter();
        filter.addAction("com.luxpro.vip.UPDATE_ACCOUNT");
        filter.addAction(ACTION_LOBBY_UPDATE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(accountReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(accountReceiver, filter);
        }

        setupFloatingIcon();
        setupModMenu();

        // 🛡️ [Build 4] Auto-Activate Bridge & Security tasks
        handler.postDelayed(() -> {
            if (NativeEngine.isLibraryLoaded()) {
                activateBridge();
                scheduleSentinel();
                scheduleSecuritySweep();
                nativeEngine.activateAuraShield();
                nativeEngine.startAuthWatchdog();
                // Enable Bot Automation in Automatic Entry Mode
                nativeEngine.setBotEnabled(true);
            }
        }, 1500);

        Log.i(TAG, "LUX PRO v1.1 Build 4 initialized");
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.service_active_title))
                .setContentText(getString(R.string.service_active_msg))
                .setSmallIcon(android.R.drawable.star_on)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.service_active_title),
                    NotificationManager.IMPORTANCE_LOW);
            ch.setDescription(getString(R.string.service_desc));
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    // ─── Floating Icon ──────────────────────────────────────────────────────
    private void setupFloatingIcon() {
        if (windowManager == null) return;
        try {
            floatingIcon = LayoutInflater.from(this).inflate(R.layout.layout_floating_icon, null);
            iconParams = new WindowManager.LayoutParams(
                    iconSize, iconSize,
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                            : WindowManager.LayoutParams.TYPE_PHONE,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                            | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT);
            iconParams.gravity = Gravity.TOP | Gravity.START;
            iconParams.x = 0;
            iconParams.y = 200;
            windowManager.addView(floatingIcon, iconParams);
            setupIconTouchListener();
        } catch (Exception e) {
            Log.e(TAG, "Icon setup failed: " + e.getMessage());
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupIconTouchListener() {
        if (floatingIcon == null) return;
        final int[] startX = {0}, startY = {0};
        final long[] downTime = {0};

        floatingIcon.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    startX[0] = (int) event.getRawX() - iconParams.x;
                    startY[0] = (int) event.getRawY() - iconParams.y;
                    downTime[0] = System.currentTimeMillis();
                    return true;
                case MotionEvent.ACTION_MOVE:
                    iconParams.x = (int) event.getRawX() - startX[0];
                    iconParams.y = (int) event.getRawY() - startY[0];
                    safeUpdateView(floatingIcon, iconParams);
                    return true;
                case MotionEvent.ACTION_UP:
                    if (System.currentTimeMillis() - downTime[0] < 250 && isClickDebounced()) {
                        toggleMenu();
                    }
                    return true;
            }
            return false;
        });
    }

    // ─── Mod Menu ─────────────────────────────────────────────────────────────
    @SuppressLint("ClickableViewAccessibility")
    private void setupModMenu() {
        if (windowManager == null) return;
        try {
            modMenuView = LayoutInflater.from(this).inflate(R.layout.layout_mod_menu, null);
        } catch (Exception e) {
            Log.e(TAG, "Menu inflate failed: " + e.getMessage());
            return;
        }

        menuParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        menuParams.gravity = Gravity.CENTER;

        // --- Build 4: Tab Switching Logic ---
        final View secVfx = modMenuView.findViewById(R.id.section_vfx);
        final View secAim = modMenuView.findViewById(R.id.section_aim);
        final View secBridge = modMenuView.findViewById(R.id.section_bridge);

        modMenuView.findViewById(R.id.tab_vfx).setOnClickListener(v -> switchTab(secVfx, secAim, secBridge));
        modMenuView.findViewById(R.id.tab_aim).setOnClickListener(v -> switchTab(secAim, secVfx, secBridge));
        modMenuView.findViewById(R.id.tab_bridge).setOnClickListener(v -> switchTab(secBridge, secVfx, secAim));

        // Initial state
        switchTab(secAim, secVfx, secBridge);

        // --- Live PID & Serial ---
        final TextView tvPid = modMenuView.findViewById(R.id.tv_game_pid);
        final TextView tvSerial = modMenuView.findViewById(R.id.tv_user_serial);
        if (tvSerial != null) tvSerial.setText("SERIAL: LUX-" + (1000 + new java.util.Random().nextInt(8999)) + "-VIP");

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (NativeEngine.isLibraryLoaded()) {
                    int pid = nativeEngine.getTargetPid();
                    boolean active = nativeEngine.isBridgeActive();
                    if (tvPid != null) tvPid.setText("PID: " + (pid > 0 ? pid : "---"));
                    
                    // Update Bridge status text (Optional UI Polish)
                    TextView tvStatus = modMenuView.findViewById(R.id.tv_bridge_status);
                    if (tvStatus != null) {
                        tvStatus.setText(active ? "STATUS: ACTIVE" : "STATUS: OFFLINE");
                        tvStatus.setTextColor(active ? 0xFF00FF00 : 0xFFFF0000);
                    }
                }
                handler.postDelayed(this, 2000);
            }
        });

        // --- Header Actions ---
        Button btnLang = modMenuView.findViewById(R.id.btn_menu_lang);
        if (btnLang != null) btnLang.setOnClickListener(v -> toggleLanguage());

        // --- Bind Controls ---
        bindNativeControls();

        try {
            windowManager.addView(modMenuView, menuParams);
            modMenuView.setVisibility(View.GONE);
        } catch (Exception e) {
            Log.e(TAG, "Menu addView failed: " + e.getMessage());
        }
    }

    private void switchTab(View active, View... others) {
        if (active == null) return;
        active.setVisibility(View.VISIBLE);
        for (View v : others) if (v != null) v.setVisibility(View.GONE);
    }

    private void bindNativeControls() {
        if (modMenuView == null || nativeEngine == null) return;

        // --- AIM SECTION ---
        bindSwitch(R.id.sw_prediction, isArabic ? "مسار التوقع الذكي" : "Smart Aim Path",
                nativeEngine::setPredictionEnabled);

        bindSwitch(R.id.sw_autoplay, isArabic ? "اللعب الذاتي [بصمة]" : "Auto Play [Bot]",
                enabled -> setFeature(4, enabled));

        bindSwitch(R.id.sw_win_seq, isArabic ? "تسلسل الفوز المباشر" : "Direct Win Sequence",
                enabled -> setFeature(3, enabled));

        bindSwitch(R.id.sw_break_detector, isArabic ? "الدخول التلقائي" : "Automatic Entry",
                enabled -> setFeature(1, enabled));

        // Line Width
        SeekBar seekWidth = modMenuView.findViewById(R.id.seek_line_width);
        if (seekWidth != null) {
            seekWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar sb, int p, boolean u) {
                    if (NativeEngine.isLibraryLoaded()) nativeEngine.updateRenderParams(p / 5.0f, 0.8f);
                }
                public void onStartTrackingTouch(SeekBar sb) {}
                public void onStopTrackingTouch(SeekBar sb) {}
            });
        }

        // --- VFX SECTION ---
        bindSwitch(R.id.sw_ghost_mode, isArabic ? "اختراق الجدران [VFX]" : "Ghost Trace [VFX]",
                nativeEngine::setGhostModeEnabled);

        SeekBar seekGlow = modMenuView.findViewById(R.id.seek_opacity);
        if (seekGlow != null) {
            seekGlow.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar sb, int p, boolean u) {
                    if (NativeEngine.isLibraryLoaded()) nativeEngine.setVfxParams(p / 100.0f, 0.5f, true, 0,0,0,0);
                }
                public void onStartTrackingTouch(SeekBar sb) {}
                public void onStopTrackingTouch(SeekBar sb) {}
            });
        }

        // Presets
        modMenuView.findViewById(R.id.btn_preset_ghost).setOnClickListener(v -> nativeEngine.setVfxPreset(0));
        modMenuView.findViewById(R.id.btn_preset_dominator).setOnClickListener(v -> nativeEngine.setVfxPreset(1));
        modMenuView.findViewById(R.id.btn_preset_stealth).setOnClickListener(v -> nativeEngine.setVfxPreset(2));

        // --- BRIDGE SECTION ---
        bindSwitch(R.id.sw_secure_bridge, isArabic ? "الجسر الآمن [تلقائي]" : "Secure Bridge [Auto]",
                enabled -> {
                    setFeature(2, enabled);
                    bridgeActive = enabled;
                });

        Button btnBridge = modMenuView.findViewById(R.id.btn_activate_bridge);
        if (btnBridge != null) btnBridge.setOnClickListener(v -> activateBridge());

        // --- FOOTER ---
        SeekBar seekTime = modMenuView.findViewById(R.id.seek_time_scale);
        final TextView tvTime = modMenuView.findViewById(R.id.tv_time_scale);
        if (seekTime != null) {
            seekTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @SuppressLint("SetTextI18n")
                public void onProgressChanged(SeekBar sb, int p, boolean u) {
                    float s = p / 100.0f;
                    if (tvTime != null) tvTime.setText((isArabic ? "سرعة المحاكاة: " : "SIM SPEED: ") + String.format(Locale.US, "%.1fx", s));
                    if (NativeEngine.isLibraryLoaded()) nativeEngine.setTimeScale(s);
                }
                public void onStartTrackingTouch(SeekBar sb) {}
                public void onStopTrackingTouch(SeekBar sb) {}
            });
        }
    }

    // ─── Bridge ─────────────────────────────────────────────────────────────
    private void activateBridge() {
        if (!NativeEngine.isLibraryLoaded()) return;
        try {
            nativeEngine.setSecureBridgeEnabled(true);
            bridgeActive = true;
            Intent intent = new Intent(this, com.luxpro.vip.auth.BridgeWebViewActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            toast(isArabic ? "⚡ جاري إعادة تفعيل الجسر..." : "⚡ Re-activating Bridge...");
        } catch (Exception e) {
            Log.e(TAG, "Bridge Error: " + e.getMessage());
        }
        updateBridgeUI();
    }

    private void updateBridgeUI() {
        if (modMenuView == null) return;
        TextView tv = modMenuView.findViewById(R.id.tv_bridge_status);
        if (tv != null) {
            tv.setText(bridgeActive ? (isArabic ? "نشط ✅" : "ACTIVE ✅") : (isArabic ? "غير نشط ⚠️" : "OFFLINE ⚠️"));
            tv.setTextColor(bridgeActive ? 0xFF00FF88 : 0xFFFF0088);
        }
    }

    // ─── Localization ────────────────────────────────────────────────────────
    private void toggleLanguage() {
        isArabic = !isArabic;
        if (NativeEngine.isLibraryLoaded()) nativeEngine.setNativeLanguage(!isArabic);
        rebuildMenuLabels();
        toast(isArabic ? "تم تحويل اللغة ✅" : "Language switched ✅");
    }

    private void rebuildMenuLabels() {
        if (modMenuView == null) return;
        Button btnLang = modMenuView.findViewById(R.id.btn_menu_lang);
        if (btnLang != null) btnLang.setText(isArabic ? "EN" : "AR");

        bindNativeControls(); // Quick re-apply labels
        updateBridgeUI();
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────
    interface SwitchAction { void run(boolean enabled); }

    private void bindSwitch(int containerId, String label, SwitchAction action) {
        View container = modMenuView.findViewById(containerId);
        if (container == null) return;
        TextView tv = container.findViewById(R.id.item_title);
        if (tv != null) tv.setText(label);
        SwitchCompat sw = container.findViewById(R.id.item_switch);
        if (sw != null) {
            sw.setOnCheckedChangeListener((b, checked) -> {
                if (NativeEngine.isLibraryLoaded()) {
                    action.run(checked);
                    toast((checked ? "✅ " : "⛔ ") + label);
                }
            });
        }
    }

    private void toggleMenu() {
        if (modMenuView == null) return;
        isMenuOpen = !isMenuOpen;
        modMenuView.setVisibility(isMenuOpen ? View.VISIBLE : View.GONE);
    }

    private void hideMenu() {
        isMenuOpen = false;
        if (modMenuView != null) modMenuView.setVisibility(View.GONE);
    }

    private void safeUpdateView(View view, WindowManager.LayoutParams params) {
        try {
            if (windowManager != null && view != null && view.isAttachedToWindow()) {
                windowManager.updateViewLayout(view, params);
            }
        } catch (Exception ignored) {}
    }

    private void safeRemoveView(View view) {
        try {
            if (windowManager != null && view != null && view.isAttachedToWindow()) {
                windowManager.removeView(view);
            }
        } catch (Exception ignored) {}
    }

    private void toast(String msg) {
        handler.post(() -> Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show());
    }

    // ─── Service overrides ────────────────────────────────────────────────────
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(accountReceiver); } catch (Exception ignored) {}
        safeRemoveView(floatingIcon);
        safeRemoveView(modMenuView);
        if (NativeEngine.isLibraryLoaded()) NativeEngine.getInstance().emergencyKill();
        handler.removeCallbacksAndMessages(null);
        Log.i(TAG, "LUX PRO service stopped");
    }

    // ─── Background Tasks ─────────────────────────────────────────────────────
    private void scheduleSentinel() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (NativeEngine.isLibraryLoaded()) NativeEngine.getInstance().runSentinelScan();
                handler.postDelayed(this, 120_000);
            }
        }, 60_000);
    }

    private void scheduleSecuritySweep() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (NativeEngine.isLibraryLoaded()) {
                    boolean ok = NativeEngine.getInstance().checkApkIntegrity()
                            && NativeEngine.getInstance().checkSanitizedEnvironment();
                    if (!ok) {
                        Log.e(TAG, "Security check failed — emergency kill");
                        NativeEngine.getInstance().emergencyKill();
                        stopSelf();
                        return;
                    }
                }
                handler.postDelayed(this, 60_000);
            }
        }, 30_000);
    }
}