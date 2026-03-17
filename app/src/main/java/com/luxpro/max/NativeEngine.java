package com.luxpro.max;

import androidx.annotation.Keep;

@Keep
public class NativeEngine {
    static {
        System.loadLibrary("InternalSystem");
    }

    private static NativeEngine instance = new NativeEngine();
    public static NativeEngine getInstance() { return instance; }

    // الأوامر الجديدة المرتبطة بالـ C++
    public native void initAdvancedHooks(Object context);
    public native void toggleLongLines(boolean active);
    public native void toggleAntiBan(boolean active);
    public native void toggleGhostBall(boolean active);
    public native void toggleAntiShake(boolean active);
    public native void toggleInvisibleTrace(boolean active);

    // توافق مع الجافا القديم (Legacy Compatibility)
    public native String getEngineStatus();
    public native int getTargetPid();
    public static native void setAutomaticEntry(boolean active);
    public native void submitAuthToken(String token);
    public native void setSpoofedSerial(String serial);
    public native boolean isBridgeActive();
    public native void triggerLogSelfDestruct();
    public native void activateAuraShield();
    public native void startAuthWatchdog();
    public native void setBotEnabled(boolean enabled);
    public native void updateRenderParams(float width, float brightness);
    public native void setGhostModeEnabled(boolean enabled);
    public native void setVfxParams(float p1, float p2, boolean p3, int p4, int p5, int p6, int p7);
    public native void setVfxPreset(int preset);
    public native void setSecureBridgeEnabled(boolean enabled);
    public native void setNativeLanguage(boolean isEnglish);
    public native void setTimeScale(float scale);
    public native void runSentinelScan();
    public native boolean checkApkIntegrity();
    public native boolean checkSanitizedEnvironment();
    public native void emergencyKill();
    public native void setSessionIP(String ip);
    public native int getBreakStatus();
    public native void toggleSnookerSolver(boolean active);
    public native void togglePocketClearance(boolean active);
    public native void toggleCueLanding(boolean active);
    public native void togglePowerGuide(boolean active);
    public native void toggleDeadEndAlert(boolean active);
    public native void toggleOpponentTracer(boolean active);
    public native void toggleHumanizeDelay(boolean active);
    public native void toggleIntegrityCheck(boolean active);

    // Vault & Bridge & AI Features
    public native byte[] encryptVault(String data);
    public native String decryptVault(byte[] data);
    public native void shredMemory(String data);
    public native void startHeavenlyAuth(String data);
    public native void fireLoginBridge(String provider);
    public native void toggleAIPath(boolean active);
    public native void setPocketFocus(boolean active);

    // Helper to check if library is loaded to prevent crashes
    private static boolean libraryLoaded = false;
    public static void loadNativeLibrary() {
        if (libraryLoaded) return;
        try {
            System.loadLibrary("InternalSystem");
            libraryLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            android.util.Log.e("LUX_NATIVE", "Failed to load InternalSystem: " + e.getMessage());
        }
    }
    static {
        loadNativeLibrary();
    }
    public static boolean isLibraryLoaded() { return libraryLoaded; }
}