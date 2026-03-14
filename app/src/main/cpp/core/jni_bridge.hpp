#ifndef JNI_BRIDGE_HPP
#define JNI_BRIDGE_HPP

#include <jni.h>
#include <string>

extern "C" {
    JNIEXPORT jstring JNICALL Java_com_luxpro_vip_NativeEngine_getEngineStatus(JNIEnv* env, jobject thiz);
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_native_1fireLoginBridge(JNIEnv* env, jobject thiz, jstring jprovider);
    JNIEXPORT jint JNICALL Java_com_luxpro_vip_NativeEngine_getTargetPid(JNIEnv* env, jobject thiz);
    
    // Core Linkage
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_setPredictionEnabled(JNIEnv* env, jobject thiz, jboolean enabled);
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_updateRenderParams(JNIEnv* env, jobject thiz, jfloat lineWidth, jfloat opacity);
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_setAutoPlayMode(JNIEnv* env, jobject thiz, jint mode);

    // Sentinel & Bot
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_runSentinelScan(JNIEnv* env, jobject thiz);
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_setBotEnabled(JNIEnv* env, jobject thiz, jboolean enabled);
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_setTargetTable(JNIEnv* env, jobject thiz, jint tableId);

    // Deep Core
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_nativeEmergencyKill(JNIEnv* env, jobject thiz);
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_initAdvancedHooks(JNIEnv* env, jobject thiz, jobject context);

    // Final Blueprint
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_calibrateAlignment(JNIEnv* env, jobject thiz, jfloat width, jfloat height);
    JNIEXPORT jfloat JNICALL Java_com_luxpro_vip_NativeEngine_getSuperBreakPulse(JNIEnv* env, jobject thiz, jfloat power, jfloat angle);
    void setGlobalContext(JNIEnv* env, jobject context);
    JNIEXPORT jint JNICALL Java_com_luxpro_vip_NativeEngine_getLobbyBreakPrediction(JNIEnv* env, jobject thiz, jint tableId);

    // Auth Bridge & Anti-Detection
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_native_1submitAuthToken(JNIEnv* env, jobject thiz, jstring encryptedToken);
    JNIEXPORT void JNICALL Java_com_luxpro_vip_NativeEngine_native_1setSpoofedSerial(JNIEnv* env, jobject thiz, jstring serial);
    JNIEXPORT jboolean JNICALL Java_com_luxpro_vip_NativeEngine_native_1isBridgeActive(JNIEnv* env, jobject thiz);
}

// Internal Native Utility for Core Engine consumption
namespace JniBridge {
    void injectMotionEvent(int action, float x, float y);
    void callNativeSendEmoji(int emojiId);
    void fireLobbyUpdate(int tableId);
    void fireLoginBridge(const char* provider);
    std::string getActiveSpoofedSerial();
}

#endif // JNI_BRIDGE_HPP
