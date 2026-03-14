package com.luxpro.vip;

import android.util.Log;
import android.content.Context;
import androidx.annotation.Keep;

@Keep
public class NativeEngine {
    private static NativeEngine instance;
    private static boolean isLibraryLoaded = false;

    static {
        try {
            System.loadLibrary("luxpro_native");
            isLibraryLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            try {
                System.loadLibrary("LUXPRO");
                isLibraryLoaded = true;
            } catch (UnsatisfiedLinkError e2) {
                Log.e("LUX_PRO", "Native library failure");
            }
        }
    }

    public static synchronized NativeEngine getInstance() {
        if (instance == null) instance = new NativeEngine();
        return instance;
    }

    public static boolean isLibraryLoaded() { return isLibraryLoaded; }

    // --- هذي الأوامر اللي مسببة اللون الأحمر بصورك (رجعتها كلها) ---
    public boolean isBridgeActive() { return isLibraryLoaded && native_isBridgeActive(); }
    public void fireLoginBridge(String p) { if(isLibraryLoaded) native_fireLoginBridge(p); }
    public String getEngineStatus() { return isLibraryLoaded ? native_getEngineStatus() : "OFFLINE"; }
    public int getTargetPid() { return isLibraryLoaded ? native_getTargetPid() : -1; }
    public void initAdvancedHooks(Context c) { if(isLibraryLoaded) native_initAdvancedHooks(c); }
    public void startAuthWatchdog() { if(isLibraryLoaded) native_nativeStartAuthWatchdog(); }
    public boolean checkApkIntegrity() { return isLibraryLoaded ? native_checkApkIntegrity() : true; }
    public boolean checkSanitizedEnvironment() { return isLibraryLoaded ? native_checkSanitizedEnvironment() : true; }

    // --- سطر الأوتوماتيك (Automatic Entry) ---
    public static native void setAutomaticEntry(boolean active);

    // --- Core Linkage: Wrappers ---
    public void setPredictionEnabled(boolean enabled) { if (isLibraryLoaded) native_setPredictionEnabled(enabled); }
    public void updateRenderParams(float lineWidth, float opacity) { if (isLibraryLoaded) native_updateRenderParams(lineWidth, opacity); }
    public void setGhostModeEnabled(boolean enabled) { if (isLibraryLoaded) native_setGhostModeEnabled(enabled); }
    public void setVfxParams(float intensity, float speed, boolean arc, int c1, int c2, int c3, int c4) {
        if (isLibraryLoaded) native_setVfxParams(intensity, speed, arc, c1, c2, c3, c4);
    }
    public void setVfxPreset(int preset) { if (isLibraryLoaded) native_setVfxPreset(preset); }
    public void setTimeScale(float scale) { if (isLibraryLoaded) native_setTimeScale(scale); }
    public void setNativeLanguage(boolean isArabic) { if (isLibraryLoaded) native_setNativeLanguage(isArabic); }
    public void runSentinelScan() { if (isLibraryLoaded) native_runSentinelScan(); }
    public void emergencyKill() { if (isLibraryLoaded) native_nativeEmergencyKill(); }
    public void triggerLogSelfDestruct() { if (isLibraryLoaded) native_triggerLogSelfDestruct(); }
    public void activateAuraShield() { Log.i("LUX_PRO", "Aura Shield Active"); }
    public void setSecureBridgeEnabled(boolean enabled) { if (isLibraryLoaded) native_setSecureBridgeEnabled(enabled); }

    // --- Native Methods ---
    private native boolean native_isBridgeActive();
    private native void native_fireLoginBridge(String p);
    private native String native_getEngineStatus();
    private native int native_getTargetPid();
    private native void native_initAdvancedHooks(Context c);
    private native void native_nativeStartAuthWatchdog();
    private native boolean native_checkApkIntegrity();
    private native boolean native_checkSanitizedEnvironment();

    private native void native_setPredictionEnabled(boolean e);
    private native void native_updateRenderParams(float l, float o);
    private native void native_setGhostModeEnabled(boolean e);
    private native void native_setVfxParams(float i, float s, boolean a, int c1, int c2, int c3, int c4);
    private native void native_setVfxPreset(int p);
    private native void native_setTimeScale(float s);
    private native void native_setNativeLanguage(boolean a);
    private native void native_runSentinelScan();
    private native void native_nativeEmergencyKill();
    private native void native_triggerLogSelfDestruct();
    private native void native_setSecureBridgeEnabled(boolean e);
}