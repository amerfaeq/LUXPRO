#include "Includes/obfuscate.h"
#include "Menu/Menu.hpp"
#include "Utils.hpp"

#define CHECK_JNI(clazz, name) \
    if (!clazz) { \
        LOGE(OBFUSCATE("JNI Initialization Error: Class not found: %s"), name); \
        if (env->ExceptionCheck()) env->ExceptionClear(); \
        return JNI_ERR; \
    }

int RegisterMenu(JNIEnv *env) {
    JNINativeMethod methods[] = {
            {OBFUSCATE("Icon"),            OBFUSCATE(
                                                   "()Ljava/lang/String;"),                                                           reinterpret_cast<void *>(Icon)},
            {OBFUSCATE("IconWebViewData"), OBFUSCATE(
                                                   "()Ljava/lang/String;"),                                                           reinterpret_cast<void *>(IconWebViewData)},
            {OBFUSCATE("IsGameLibLoaded"), OBFUSCATE(
                                                   "()Z"),                                                                            reinterpret_cast<void *>(isGameLibLoaded)},
            {OBFUSCATE("Init"),            OBFUSCATE(
                                                   "(Landroid/content/Context;Landroid/widget/TextView;Landroid/widget/TextView;)V"), reinterpret_cast<void *>(Init)},
            {OBFUSCATE("SettingsList"),    OBFUSCATE(
                                                   "()[Ljava/lang/String;"),                                                          reinterpret_cast<void *>(SettingsList)},
            {OBFUSCATE("GetFeatureList"),  OBFUSCATE(
                                                   "()[Ljava/lang/String;"),                                                          reinterpret_cast<void *>(GetFeatureList)},
    };

    jclass clazz = env->FindClass(OBFUSCATE("com/luxpro/max/Menu"));
    CHECK_JNI(clazz, "com/luxpro/max/Menu");

    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0) {
        LOGE(OBFUSCATE("JNI Initialization Error: Failed to register natives for com/luxpro/max/Menu"));
        return JNI_ERR;
    }
    return JNI_OK;
}

int RegisterPreferences(JNIEnv *env) {
    JNINativeMethod methods[] = {
            {OBFUSCATE("Changes"), OBFUSCATE("(Landroid/content/Context;ILjava/lang/String;IJZLjava/lang/String;)V"), reinterpret_cast<void *>(Changes)},
    };
    jclass clazz = env->FindClass(OBFUSCATE("com/luxpro/max/Preferences"));
    CHECK_JNI(clazz, "com/luxpro/max/Preferences");

    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0) {
        LOGE(OBFUSCATE("JNI Initialization Error: Failed to register natives for com/luxpro/max/Preferences"));
        return JNI_ERR;
    }
    return JNI_OK;
}

int RegisterMain(JNIEnv *env) {
    JNINativeMethod methods[] = {
            {OBFUSCATE("CheckOverlayPermission"), OBFUSCATE("(Landroid/content/Context;)V"),
             reinterpret_cast<void *>(CheckOverlayPermission)},
    };
    jclass clazz = env->FindClass(OBFUSCATE("com/luxpro/max/Main"));
    CHECK_JNI(clazz, "com/luxpro/max/Main");

    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0) {
        LOGE(OBFUSCATE("JNI Initialization Error: Failed to register natives for com/luxpro/max/Main"));
        return JNI_ERR;
    }

    return JNI_OK;
}

extern "C"
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    if (vm->GetEnv((void **) &env, JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    if (RegisterMenu(env) != JNI_OK) return JNI_ERR;
    if (RegisterPreferences(env) != JNI_OK) return JNI_ERR;
    if (RegisterMain(env) != JNI_OK) return JNI_ERR;

    LOGI(OBFUSCATE("NativeCore JNI Loaded Successfully."));
    return JNI_VERSION_1_6;
}