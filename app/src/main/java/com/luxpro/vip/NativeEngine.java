package com.luxpro.vip;

import android.util.Log;

public class NativeEngine {

    private static final String TAG = "LUX_ENGINE_JAVA";

    // Singleton instance
    private static NativeEngine instance;
    private static boolean isLibraryLoaded = false;

    // Load the C++ native library manually to prevent crash-on-boot
    public static synchronized void loadNativeLibrary() {
        if (isLibraryLoaded) return;
        try {
            System.loadLibrary("LUXPRO");
            Log.i(TAG, "Native library 'libLUXPRO.so' manually loaded successfully.");
            isLibraryLoaded = true;
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native library: " + e.getMessage());
        }
    }

    private NativeEngine() {
        // Private constructor
    }

    public static synchronized NativeEngine getInstance() {
        if (instance == null) {
            instance = new NativeEngine();
        }
        return instance;
    }

    /** Returns true if the native .so library was successfully loaded. */
    public static boolean isLibraryLoaded() {
        return isLibraryLoaded;
    }

    // --- Core Linkage: Safe Wrappers ---

    public void setPredictionEnabled(boolean enabled) {
        if (isLibraryLoaded) native_setPredictionEnabled(enabled);
    }

    public void updateRenderParams(float lineWidth, float opacity) {
        if (isLibraryLoaded) native_updateRenderParams(lineWidth, opacity);
    }

    public void setAutoPlayMode(int mode) {
        if (isLibraryLoaded) native_setAutoPlayMode(mode);
    }

    public void setNativeLanguage(boolean isArabic) {
        if (isLibraryLoaded) native_setNativeLanguage(isArabic);
    }

    public void runSentinelScan() {
        if (isLibraryLoaded) native_runSentinelScan();
    }

    public void setSecureBridgeEnabled(boolean enabled) {
        if (isLibraryLoaded) native_setSecureBridgeEnabled(enabled);
    }

    public void setGhostModeEnabled(boolean enabled) {
        if (isLibraryLoaded) native_setGhostModeEnabled(enabled);
    }
    
    public void setBotEnabled(boolean enabled) {
        if (isLibraryLoaded) native_setBotEnabled(enabled);
    }

    public void setTargetTable(int tableId) {
        if (isLibraryLoaded) native_setTargetTable(tableId);
    }

    public void emergencyKill() {
        if (isLibraryLoaded) native_nativeEmergencyKill();
    }

    public void initAdvancedHooks(android.content.Context context) {
        if (isLibraryLoaded) native_initAdvancedHooks(context);
    }

    public void calibrateAlignment(float width, float height) {
        if (isLibraryLoaded) native_calibrateAlignment(width, height);
    }

    public float getSuperBreakPulse(float power, float angle) {
        if (isLibraryLoaded) return native_getSuperBreakPulse(power, angle);
        return 0.0f;
    }

    public void setPathColors(int directColor, int bank1Color, int bank2Color, int ghostColor) {
        if (isLibraryLoaded) native_setPathColors(directColor, bank1Color, bank2Color, ghostColor);
    }

    public void setVfxParams(float glowIntensity, float pulseSpeed, boolean electricArc,
                             int directColor, int bankColor, int pocketColor, int ghostColor) {
        if (isLibraryLoaded) native_setVfxParams(glowIntensity, pulseSpeed, electricArc, directColor, bankColor, pocketColor, ghostColor);
    }

    public void setVfxPreset(int preset) {
        if (isLibraryLoaded) native_setVfxPreset(preset);
    }

    public void setWinSequenceEnabled(boolean enabled) {
        if (isLibraryLoaded) native_setWinSequenceEnabled(enabled);
    }

    public void setTimeScale(float scale) {
        if (isLibraryLoaded) native_setTimeScale(scale);
    }

    public void setVipEnabled(boolean enabled) {
        if (isLibraryLoaded) native_setVipEnabled(enabled);
    }

    public void setPocketMultiplierEnabled(boolean enabled) {
        if (isLibraryLoaded) native_setPocketMultiplierEnabled(enabled);
    }

    public void updateRemoteOffset(long offset) {
        if (isLibraryLoaded) native_nativeUpdateRemoteOffset(offset);
    }

    public void submitAuthToken(String token) {
        if (isLibraryLoaded) {
            String encrypted = com.luxpro.vip.security.AesCipher.encrypt(token);
            if (encrypted != null) {
                native_submitAuthToken(encrypted);
            }
        }
    }

    public void setSpoofedSerial(String serial) {
        if (isLibraryLoaded) native_setSpoofedSerial(serial);
    }

    // --- Build 5 Master Controller ---
    public void setFeature(int featureNum, boolean active) {
        if (isLibraryLoaded) native_setFeature(featureNum, active);
    }

    public String getLangString(String key) {
        if (isLibraryLoaded) return native_getLangString(key);
        return key;
    }

    public boolean isBridgeActive() {
        if (isLibraryLoaded) return native_isBridgeActive();
        return false;
    }

    public boolean isNoBypassActive() {
        if (isLibraryLoaded) return native_isNoBypassActive();
        return false;
    }

    public boolean verifyClassIntegrity(String className, String expectedHash) {
        if (isLibraryLoaded) return native_nativeVerifyClassIntegrity(className, expectedHash);
        return true;
    }

    public void startAuthWatchdog() {
        if (isLibraryLoaded) native_nativeStartAuthWatchdog();
    }

    public void startHeavenlyAuth(String token) {
        if (!isLibraryLoaded || token == null || token.isEmpty()) return;
        try {
            native_nativeSimulateBiometricPulse();
            native_nativeStartHeavenlySession(token);
        } catch (Exception e) {
            logShaderInfo("HEAVENLY_AUTH_ERROR: " + e.getMessage());
        } finally {
            if (isLibraryLoaded) native_nativeShredMemory(token);
        }
    }

    public String getNebulaSessionKey(android.content.Context context) {
        if (!isLibraryLoaded) return "OFFLINE_SESSION";
        String androidId = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        return native_nativeGetSessionKey(androidId);
    }

    public String getEngineStatus() {
        if (isLibraryLoaded) return native_getEngineStatus();
        return "NATIVE_NOT_LOADED";
    }

    public int getTargetPid() {
        if (isLibraryLoaded) return native_getTargetPid();
        return -1;
    }

    public void activateAuraShield() {
        logShaderInfo("AURA_SHIELD: STABILITY_VERIFIED");
    }

    public void triggerNucleusSovereignKill(android.content.Context context) {
        logShaderInfo("SINGULARITY: PURGE_SEQUENCE_START");
        try {
            com.luxpro.vip.auth.AccountVaultManager vault = new com.luxpro.vip.auth.AccountVaultManager(context);
            vault.performDeepPurge(context);
        } catch (Exception e) {
            logShaderInfo("PURGE_ERROR: " + e.getMessage());
        }
        logShaderInfo("SESSION_CLEANED");
    }

    public void logShaderInfo(String msg) {
        android.util.Log.i(TAG, msg);
    }

    public void sendEmoji(int emojiId) {
        if (isLibraryLoaded) native_nativeSendEmoji(emojiId);
    }

    public void fireLoginBridge(String provider) {
        if (isLibraryLoaded) native_fireLoginBridge(provider);
    }

    public int getLobbyBreakPrediction(int tableId) {
        if (isLibraryLoaded) return native_getLobbyBreakPrediction(tableId);
        return -1;
    }

    public byte[] fetchEncryptedSentinelLog() {
        if (isLibraryLoaded) return native_fetchEncryptedSentinelLog();
        return new byte[0];
    }

    public void triggerLogSelfDestruct() {
        if (isLibraryLoaded) native_triggerLogSelfDestruct();
    }

    public boolean checkApkIntegrity() {
        if (isLibraryLoaded) return native_checkApkIntegrity();
        return true;
    }

    public boolean checkSanitizedEnvironment() {
        if (isLibraryLoaded) return native_checkSanitizedEnvironment();
        return true;
    }

    public byte[] encryptVault(String data) {
        if (isLibraryLoaded) return native_nativeEncryptVault(data);
        return new byte[0];
    }

    public String decryptVault(byte[] data) {
        if (isLibraryLoaded) return native_nativeDecryptVault(data);
        return "";
    }

    public void shredMemory(String data) {
        if (isLibraryLoaded) native_nativeShredMemory(data);
    }

    public void triggerImpactFlash() {
        if (isLibraryLoaded) native_nativeTriggerImpactFlash();
    }

    public void setBreakDetectorEnabled(boolean enabled) {
        if (isLibraryLoaded) native_setPredictionEnabled(enabled); // mapped to prediction hook
    }

    public void calibrateTable() {
        if (isLibraryLoaded) native_calibrateAlignment(0f, 0f);
    }

    // --- Private Native Bridge ---
    private native String native_getEngineStatus();
    private native int native_getTargetPid();
    private native void native_setPredictionEnabled(boolean enabled);
    private native void native_updateRenderParams(float lineWidth, float opacity);
    private native void native_setAutoPlayMode(int mode);
    private native void native_setNativeLanguage(boolean isArabic);
    private native void native_runSentinelScan();
    private native void native_setBotEnabled(boolean enabled);
    private native void native_setTargetTable(int tableId);
    private native void native_nativeEmergencyKill();
    private native void native_initAdvancedHooks(android.content.Context context);
    private native void native_calibrateAlignment(float width, float height);
    private native float native_getSuperBreakPulse(float power, float angle);
    private native void native_setPathColors(int directColor, int bank1Color, int bank2Color, int ghostColor);
    private native void native_setVfxParams(float glowIntensity, float pulseSpeed, boolean electricArc,
                                            int directColor, int bankColor, int pocketColor, int ghostColor);
    private native void native_setVfxPreset(int preset);
    private native void native_nativeUpdateRemoteOffset(long offset);
    private native void native_submitAuthToken(String encryptedToken);
    private native void native_setSpoofedSerial(String serial);
    private native boolean native_isBridgeActive();
    private native boolean native_isNoBypassActive();
    private native String native_nativeGetSessionKey(String deviceId);
    private native void native_nativeStartAuthWatchdog();
    private native void native_nativeShredMemory(String data);
    private native void native_nativeSimulateBiometricPulse();
    private native void native_nativeStartHeavenlySession(String token);
    private native boolean native_nativeVerifyClassIntegrity(String className, String expectedHash);
    private native void native_nativeSendEmoji(int emojiId);
    private native int native_getLobbyBreakPrediction(int tableId);
    private native byte[] native_fetchEncryptedSentinelLog();
    private native void native_triggerLogSelfDestruct();
    private native boolean native_checkApkIntegrity();
    private native boolean native_checkSanitizedEnvironment();
    private native byte[] native_nativeEncryptVault(String data);
    private native String native_nativeDecryptVault(byte[] data);
    private native void native_nativeTriggerImpactFlash();
    private native void native_fireLoginBridge(String provider);
    private native void native_setSecureBridgeEnabled(boolean enabled);
    private native void native_setGhostModeEnabled(boolean enabled);
    private native void native_setWinSequenceEnabled(boolean enabled);
    private native void native_setTimeScale(float scale);
    private native void native_setVipEnabled(boolean enabled);
    private native void native_setPocketMultiplierEnabled(boolean enabled);

    // --- Build 5 Master Controller Native ---
    private native void native_setFeature(int featureNum, boolean active);
    private native String native_getLangString(String key);
}
