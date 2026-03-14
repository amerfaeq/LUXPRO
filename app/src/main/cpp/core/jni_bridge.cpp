#include <jni.h>
#include <string>
#include <unistd.h>
#include <dirent.h>
#include <android/log.h>
#include <cstdio>
#include <cstdlib>
#include <mutex>
#include <thread>
#include "jni_bridge.hpp"
#include "KittyMemory/KittyMemory.h"
#include "KittyMemory/MemoryPatch.h"
#include "../Tools.h"

#define TAG "LUX_MASTER"

static int g_targetPid = -1;
static bool g_bridgeActive = false;
static std::mutex g_stateMutex;
static std::string g_spoofedSerial = "LUX-SPOOF-001";

static JavaVM* g_vm = nullptr;
static jobject g_context = nullptr;

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
    g_vm = vm;
    JNIEnv *env;
    if (vm->GetEnv((void **)&env, JNI_VERSION_1_6) != JNI_OK) return JNI_ERR;

    g_targetPid = find_game_pid();
    if (g_targetPid != -1) {
        g_bridgeActive = true;
        __android_log_print(ANDROID_LOG_INFO, TAG, "LUX PRO Build 5: Bridge ACTIVE (PID: %d)", g_targetPid);
    }

    return JNI_VERSION_1_6;
}

extern "C" {

    JNIEXPORT void JNICALL
    Java_com_luxpro_vip_NativeEngine_setFeature(JNIEnv *env, jobject thiz, jint featureNum, jboolean active) {
        const char* libName = "libgame.so"; 
        switch (featureNum) {
            case 1: if (active) KittyMemory::patchFromHex(libName, 0x123456, "01 00 A0 E3 1E FF 2F E1"); break;
            case 2: if (active) KittyMemory::patchFromHex(libName, 0x234567, "01 00 A0 E3 1E FF 2F E1"); break;
            case 3: if (active) KittyMemory::patchFromHex(libName, 0x345678, "00 F0 20 E3"); break;
            case 4: if (active) KittyMemory::patchFromHex(libName, 0x456789, "0A 00 A0 E3 1E FF 2F E1"); break;
        }
    }

    JNIEXPORT jstring JNICALL
    Java_com_luxpro_vip_NativeEngine_getLangString(JNIEnv *env, jobject thiz, jstring key) {
        return key; 
    }

    JNIEXPORT jboolean JNICALL Java_com_luxpro_vip_NativeEngine_native_1isBridgeActive(JNIEnv* env, jobject thiz) {
        return (jboolean)(g_targetPid != -1);
    }

    JNIEXPORT jint JNICALL Java_com_luxpro_vip_NativeEngine_native_1getTargetPid(JNIEnv* env, jobject thiz) {
        if (g_targetPid == -1) g_targetPid = find_game_pid();
        return (jint)g_targetPid;
    }

    // Restore missing JNI methods used by other parts of the app
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_native_1initAdvancedHooks(JNIEnv* env, jobject thiz, jobject context) {
        if (g_context) env->DeleteGlobalRef(g_context);
        g_context = env->NewGlobalRef(context);
    }

    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_native_1setPredictionEnabled(JNIEnv* env, jobject thiz, jboolean enabled) {}
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_native_1updateRenderParams(JNIEnv* env, jobject thiz, jfloat w, jfloat o) {}
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_native_1setBotEnabled(JNIEnv* env, jobject thiz, jboolean e) {}
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_native_1setWinSequenceEnabled(JNIEnv* env, jobject thiz, jboolean e) {}
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_native_1setGhostModeEnabled(JNIEnv* env, jobject thiz, jboolean e) {}
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_native_1setSecureBridgeEnabled(JNIEnv* env, jobject thiz, jboolean e) {}

} // extern "C"

namespace JniBridge {
    void setGlobalContext(JNIEnv* env, jobject context) {
        if (g_context) env->DeleteGlobalRef(g_context);
        g_context = env->NewGlobalRef(context);
    }

    void fireLoginBridge(const char* provider) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Login Bridge Fired: %s", provider);
    }

    std::string getActiveSpoofedSerial() {
        return g_spoofedSerial;
    }

    void callNativeSendEmoji(int emojiId) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Emoji Sent: %d", emojiId);
    }

    void injectMotionEvent(int action, float x, float y) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Motion Injected: A=%d, X=%.1f, Y=%.1f", action, x, y);
    }
}
