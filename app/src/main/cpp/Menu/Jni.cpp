#include "Jni.hpp"
#include <list>
#include <vector>
#include <cstring>
#include <string>
#include <pthread.h>
#include <thread>
#include <unistd.h>
#include <fstream>
#include <iostream>
#include <sstream>
#include <dlfcn.h>
#include "Includes/obfuscate.h"
#include "Menu/Jni.hpp"
#include "Includes/Logger.h"

// Macro for safe JNI class lookup
#define FIND_CLASS_SAFE(var, name) \
    jclass var = env->FindClass(name); \
    if (!var) { \
        const char* decodedName = name; \
        LOGE(OBFUSCATE("JNI Error: Failed to find class %s"), decodedName); \
        if (env->ExceptionCheck()) env->ExceptionClear(); \
        return; \
    }

// Macro for safe JNI method lookup (static)
#define GET_STATIC_METHOD_SAFE(var, clazz, name, sig) \
    jmethodID var = env->GetStaticMethodID(clazz, name, sig); \
    if (!var) { \
        const char* decodedName = name; \
        const char* decodedSig = sig; \
        LOGE(OBFUSCATE("JNI Error: Failed to find static method %s with signature %s"), decodedName, decodedSig); \
        if (env->ExceptionCheck()) env->ExceptionClear(); \
        return; \
    }

// Macro for safe JNI method lookup (instance)
#define GET_METHOD_SAFE(var, clazz, name, sig) \
    jmethodID var = env->GetMethodID(clazz, name, sig); \
    if (!var) { \
        const char* decodedName = name; \
        const char* decodedSig = sig; \
        LOGE(OBFUSCATE("JNI Error: Failed to find method %s with signature %s"), decodedName, decodedSig); \
        if (env->ExceptionCheck()) env->ExceptionClear(); \
        return; \
    }

void Dialog(JNIEnv *env, jobject context, const char *title, const char *message, const char *openBtn, const char *closeBtn, int sec, const char *url) {
    FIND_CLASS_SAFE(dialogHelperClass, OBFUSCATE("com/luxpro/max/DialogHelper"));
    GET_STATIC_METHOD_SAFE(showMethod, dialogHelperClass, OBFUSCATE("showDialogWithLink"),
                           OBFUSCATE("(Landroid/content/Context;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;ILjava/lang/String;)V"));

    jstring jTitle = env->NewStringUTF(title);
    jstring jMessage = env->NewStringUTF(message);
    jstring jOpen = env->NewStringUTF(openBtn);
    jstring jClose = env->NewStringUTF(closeBtn);
    jint jSec = sec;
    jstring jUrl = env->NewStringUTF(url);

    env->CallStaticVoidMethod(dialogHelperClass, showMethod, context, jTitle, jMessage, jOpen, jClose, jSec, jUrl);

    env->DeleteLocalRef(jTitle);
    env->DeleteLocalRef(jMessage);
    env->DeleteLocalRef(jOpen);
    env->DeleteLocalRef(jClose);
    env->DeleteLocalRef(jUrl);
}

void Toast(JNIEnv *env, jobject thiz, const char *text, int length) {
    jstring jstr = env->NewStringUTF(text);
    FIND_CLASS_SAFE(toastClass, OBFUSCATE("android/widget/Toast"));
    GET_STATIC_METHOD_SAFE(methodMakeText, toastClass, OBFUSCATE("makeText"), OBFUSCATE("(Landroid/content/Context;Ljava/lang/CharSequence;I)Landroid/widget/Toast;"));
    
    jobject toastobj = env->CallStaticObjectMethod(toastClass, methodMakeText, thiz, jstr, length);
    if (!toastobj) return;

    GET_METHOD_SAFE(methodShow, toastClass, OBFUSCATE("show"), OBFUSCATE("()V"));
    env->CallVoidMethod(toastobj, methodShow);
    
    env->DeleteLocalRef(jstr);
    env->DeleteLocalRef(toastobj);
}

void setText(JNIEnv *env, jobject obj, const char* text){
    if (!obj) return;
    
    jclass html = env->FindClass(OBFUSCATE("android/text/Html"));
    if (!html) {
        if (env->ExceptionCheck()) env->ExceptionClear();
        return;
    }
    jmethodID fromHtml = env->GetStaticMethodID(html, OBFUSCATE("fromHtml"), OBFUSCATE("(Ljava/lang/String;)Landroid/text/Spanned;"));
    if (!fromHtml) {
        if (env->ExceptionCheck()) env->ExceptionClear();
        return;
    }

    jclass textView = env->FindClass(OBFUSCATE("android/widget/TextView"));
    if (!textView) {
        if (env->ExceptionCheck()) env->ExceptionClear();
        return;
    }
    jmethodID setText = env->GetMethodID(textView, OBFUSCATE("setText"), OBFUSCATE("(Ljava/lang/CharSequence;)V"));
    if (!setText) {
        if (env->ExceptionCheck()) env->ExceptionClear();
        return;
    }

    jstring jstr = env->NewStringUTF(text);
    jobject spanned = env->CallStaticObjectMethod(html, fromHtml, jstr);
    if (spanned) {
        env->CallVoidMethod(obj, setText, spanned);
        env->DeleteLocalRef(spanned);
    }
    env->DeleteLocalRef(jstr);
}

void startService(JNIEnv *env, jobject ctx){
    if (!ctx) return;
    jclass native_context = env->GetObjectClass(ctx);
    
    FIND_CLASS_SAFE(intentClass, OBFUSCATE("android/content/Intent"));
    FIND_CLASS_SAFE(launcherClass, OBFUSCATE("com/luxpro/max/Launcher"));
    
    GET_METHOD_SAFE(newIntent, intentClass, OBFUSCATE("<init>"), OBFUSCATE("(Landroid/content/Context;Ljava/lang/Class;)V"));
    jobject intent = env->NewObject(intentClass, newIntent, ctx, launcherClass);
    if (!intent) return;

    GET_METHOD_SAFE(startServiceMethodId, native_context, OBFUSCATE("startService"), OBFUSCATE("(Landroid/content/Intent;)Landroid/content/ComponentName;"));
    env->CallObjectMethod(ctx, startServiceMethodId, intent);
    
    env->DeleteLocalRef(intent);
}

void *exit_thread(void *) {
    sleep(5);
    exit(0);
}

int get_api_sdk(JNIEnv* env) {
    jclass build_version_class = env->FindClass(OBFUSCATE("android/os/Build$VERSION"));
    if (!build_version_class) {
        if (env->ExceptionCheck()) env->ExceptionClear();
        return 0;
    }
    jfieldID sdk_int_field = env->GetStaticFieldID(build_version_class, OBFUSCATE("SDK_INT"), OBFUSCATE("I"));
    if (!sdk_int_field) {
        if (env->ExceptionCheck()) env->ExceptionClear();
        return 0;
    }
    return env->GetStaticIntField(build_version_class, sdk_int_field);
}

void startActivityPermisson(JNIEnv *env, jobject ctx){
    if (!ctx) return;
    jclass native_context = env->GetObjectClass(ctx);
    
    GET_METHOD_SAFE(startActivity, native_context, OBFUSCATE("startActivity"), OBFUSCATE("(Landroid/content/Intent;)V"));
    GET_METHOD_SAFE(pack, native_context, OBFUSCATE("getPackageName"), OBFUSCATE("()Ljava/lang/String;"));
    
    jstring packageName = static_cast<jstring>(env->CallObjectMethod(ctx, pack));
    if (!packageName) return;

    const char *pkg = env->GetStringUTFChars(packageName, 0);
    std::stringstream strpkg;
    strpkg << OBFUSCATE("package:");
    strpkg << pkg;
    std::string pakg = strpkg.str();
    env->ReleaseStringUTFChars(packageName, pkg);

    FIND_CLASS_SAFE(Uri, OBFUSCATE("android/net/Uri"));
    GET_STATIC_METHOD_SAFE(Parce, Uri, OBFUSCATE("parse"), OBFUSCATE("(Ljava/lang/String;)Landroid/net/Uri;"));
    
    jstring jPakg = env->NewStringUTF(pakg.c_str());
    jobject UriMethod = env->CallStaticObjectMethod(Uri, Parce, jPakg);
    env->DeleteLocalRef(jPakg);
    if (!UriMethod) return;

    FIND_CLASS_SAFE(intentclass, OBFUSCATE("android/content/Intent"));
    GET_METHOD_SAFE(newIntent, intentclass, OBFUSCATE("<init>"), OBFUSCATE("(Ljava/lang/String;Landroid/net/Uri;)V"));
    
    jstring jAction = env->NewStringUTF(OBFUSCATE("android.settings.action.MANAGE_OVERLAY_PERMISSION"));
    jobject intent = env->NewObject(intentclass, newIntent, jAction, UriMethod);
    env->DeleteLocalRef(jAction);
    
    if (intent) {
        env->CallVoidMethod(ctx, startActivity, intent);
        env->DeleteLocalRef(intent);
    }
    env->DeleteLocalRef(UriMethod);
    env->DeleteLocalRef(packageName);
}

void CheckOverlayPermission(JNIEnv *env, jclass thiz, jobject ctx){
    if (!ctx) return;
    LOGI(OBFUSCATE("Check overlay permission"));

    int sdkVer = get_api_sdk(env);
    if (sdkVer >= 23) { //Android 6.0
        jclass Settings = env->FindClass(OBFUSCATE("android/provider/Settings"));
        if (Settings) {
            jmethodID canDraw = env->GetStaticMethodID(Settings, OBFUSCATE("canDrawOverlays"), OBFUSCATE("(Landroid/content/Context;)Z"));
            if (canDraw) {
                if (!env->CallStaticBooleanMethod(Settings, canDraw, ctx)){
                    Toast(env, ctx, OBFUSCATE("Overlay permission is required in order to show mod menu."), 1);
                    startActivityPermisson(env, ctx);

                    pthread_t ptid;
                    pthread_create(&ptid, NULL, exit_thread, NULL);
                    return;
                }
            } else if (env->ExceptionCheck()) env->ExceptionClear();
        } else if (env->ExceptionCheck()) env->ExceptionClear();
    }

    LOGI(OBFUSCATE("Start service"));
    startService(env, ctx);
}
