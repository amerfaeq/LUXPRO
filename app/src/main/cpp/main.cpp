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
}
