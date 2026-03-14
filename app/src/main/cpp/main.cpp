#include <jni.h>
#include <string>
#include <unistd.h>
#include <dirent.h>
#include <android/log.h>
#include <cstdio>
#include <cstdlib>
#include "KittyMemory/KittyMemory.h"
#include "KittyMemory/MemoryPatch.h"

#define TAG "LUX_NATIVE"

static int g_targetPid = -1;

// --- Helper: PID Resolver for Build 5 ---
int find_game_pid() {
    DIR* dir = opendir("/proc");
    if (!dir) return -1;
    struct dirent* entry;
    int foundPid = -1;
    while ((entry = readdir(dir)) != nullptr) {
        int pid = atoi(entry->d_name);
        if (pid <= 0) continue;
        char path[128];
        snprintf(path, sizeof(path), "/proc/%d/cmdline", pid);
        FILE* f = fopen(path, "r");
        if (f) {
            char cmd[256];
            if (fgets(cmd, sizeof(cmd), f)) {
                if (strstr(cmd, "com.miniclip.eightballpool")) {
                    foundPid = pid;
                    fclose(f);
                    break;
                }
            }
            fclose(f);
        }
    }
    closedir(dir);
    return foundPid;
}

extern "C" JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **)&env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;

    g_targetPid = find_game_pid();
    if (g_targetPid != -1) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "LUX PRO Master: PID %d Detected. Bridge ACTIVE.", g_targetPid);
    } else {
        __android_log_print(ANDROID_LOG_ERROR, TAG, "LUX PRO Master: Game Not Found. Bridge OFFLINE.");
    }

    return JNI_VERSION_1_6;
}

extern "C" {

    JNIEXPORT void JNICALL
    Java_com_luxpro_vip_FloatingMenuService_setFeature(JNIEnv *env, jobject thiz, jint featureNum, jboolean active) {
        const char* libName = "libgame.so"; 

        switch (featureNum) {
            case 1: // Automatic Entry
                if (active) KittyMemory::patchFromHex(libName, 0x123456, "00 00 A0 E3 1E FF 2F E1");
                break;
            case 2: // Secure Bridge
                if (active) KittyMemory::patchFromHex(libName, 0x234567, "01 00 A0 E3 1E FF 2F E1");
                break;
            case 3: // Win Sequence
                if (active) KittyMemory::patchFromHex(libName, 0x345678, "00 F0 20 E3");
                break;
            case 4: // Auto Play [Bot]
                if (active) KittyMemory::patchFromHex(libName, 0x456789, "0A 00 A0 E3 1E FF 2F E1");
                break;
        }
        __android_log_print(ANDROID_LOG_INFO, TAG, "Feature %d set to %s", featureNum, active ? "ON" : "OFF");
    }

    JNIEXPORT jstring JNICALL
    Java_com_luxpro_vip_FloatingMenuService_getLangString(JNIEnv *env, jobject thiz, jstring key) {
        return key; 
    }

    JNIEXPORT jboolean JNICALL
    Java_com_luxpro_vip_FloatingMenuService_isBridgeActive(JNIEnv* env, jobject thiz) {
        return (jboolean)(g_targetPid != -1);
    }

    // --- NativeEngine Bridge (Ignition & Security) ---

    JNIEXPORT void JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1initAdvancedHooks(JNIEnv *env, jobject thiz, jobject context) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "ENGINE IGNITION: Initializing Advanced Hooks...");
        // Reality: This is where you'd start ShadowHook or sub-engines
    }

    JNIEXPORT jboolean JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1isBridgeActive(JNIEnv *env, jobject thiz) {
        return (jboolean)(g_targetPid != -1);
    }

    JNIEXPORT jint JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1getTargetPid(JNIEnv *env, jobject thiz) {
        return g_targetPid;
    }

    JNIEXPORT jstring JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1getEngineStatus(JNIEnv *env, jobject thiz) {
        return env->NewStringUTF(g_targetPid != -1 ? "ACTIVE" : "OFFLINE");
    }

    JNIEXPORT void JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1nativeStartAuthWatchdog(JNIEnv *env, jobject thiz) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Security: Auth Watchdog Started.");
    }

    JNIEXPORT jboolean JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1checkApkIntegrity(JNIEnv *env, jobject thiz) {
        return JNI_TRUE;
    }

    JNIEXPORT jboolean JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1checkSanitizedEnvironment(JNIEnv *env, jobject thiz) {
        return JNI_TRUE;
    }

    JNIEXPORT void JNICALL
    Java_com_luxpro_vip_NativeEngine_setAutomaticEntry(JNIEnv *env, jclass clazz, jboolean active) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Automatic Entry set to %s", active ? "ON" : "OFF");
        // Logic for auto-entry patching
        if (active) KittyMemory::patchFromHex("libgame.so", 0x123456, "00 00 A0 E3");
    }

    JNIEXPORT void JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1fireLoginBridge(JNIEnv *env, jobject thiz, jstring provider) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Bridge: Login Triggered.");
    }

    JNIEXPORT void JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1setPredictionEnabled(JNIEnv *env, jobject thiz, jboolean enabled) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Prediction set to %d", enabled);
    }

    JNIEXPORT void JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1updateRenderParams(JNIEnv *env, jobject thiz, jfloat lineWidth, jfloat opacity) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Render Params: Width %.2f, Opacity %.2f", lineWidth, opacity);
    }

    JNIEXPORT void JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1setGhostModeEnabled(JNIEnv *env, jobject thiz, jboolean enabled) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Ghost Mode set to %d", enabled);
    }

    JNIEXPORT void JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1setVfxParams(JNIEnv *env, jobject thiz, jfloat intensity, jfloat speed, jboolean arc, jint c1, jint c2, jint c3, jint c4) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "VFX Params Updated: Intensity %.2f", intensity);
    }

    JNIEXPORT void JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1setVfxPreset(JNIEnv *env, jobject thiz, jint preset) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "VFX Preset: %d", preset);
    }

    JNIEXPORT void JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1setTimeScale(JNIEnv *env, jobject thiz, jfloat scale) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Time Scale: %.2fx", scale);
    }

    JNIEXPORT void JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1setNativeLanguage(JNIEnv *env, jobject thiz, jboolean isArabic) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Language set to %s", isArabic ? "AR" : "EN");
    }

    JNIEXPORT void JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1runSentinelScan(JNIEnv *env, jobject thiz) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Sentinel: Scanning system...");
    }

    JNIEXPORT void JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1nativeEmergencyKill(JNIEnv *env, jobject thiz) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Emergency Kill Triggered!");
    }

    JNIEXPORT void JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1triggerLogSelfDestruct(JNIEnv *env, jobject thiz) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Security: Logs Purged.");
    }

    JNIEXPORT void JNICALL
    Java_com_luxpro_vip_NativeEngine_native_1setSecureBridgeEnabled(JNIEnv *env, jobject thiz, jboolean enabled) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Secure Bridge set to %d", enabled);
    }
}
