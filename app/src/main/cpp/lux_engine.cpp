#include <jni.h>
#include <string>
#include <android/log.h>
#include <unistd.h>
#include <dirent.h>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#define LOG_TAG "LUX_ENGINE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// --- المرحلة الأولى: أوامر الـ JNI واختبار الاتصال ---

extern "C" JNIEXPORT jstring JNICALL
Java_com_luxpro_vip_NativeEngine_getEngineStatus(JNIEnv* env, jobject thiz) {
    LOGI("getEngineStatus called from Java");
    std::string status = "✅ LUX Native Engine is ACTIVE and connected via JNI!";
    return env->NewStringUTF(status.c_str());
}

// دالة مبدئية لجلب Process ID الخاص بلعبة 8 Ball Pool
int get_process_id(const char *process_name) {
    int pid = -1;
    DIR *dir = opendir("/proc");
    if (dir == nullptr) return -1;

    struct dirent *entry;
    while ((entry = readdir(dir)) != nullptr) {
        int current_pid = atoi(entry->d_name);
        if (current_pid != 0) {
            char cmdline_path[256];
            snprintf(cmdline_path, sizeof(cmdline_path), "/proc/%d/cmdline", current_pid);

            FILE *cmdline_file = fopen(cmdline_path, "r");
            if (cmdline_file != nullptr) {
                char current_process_name[256];
                if (fgets(current_process_name, sizeof(current_process_name), cmdline_file) != nullptr) {
                    if (strcmp(current_process_name, process_name) == 0) {
                        pid = current_pid;
                        fclose(cmdline_file);
                        break;
                    }
                }
                fclose(cmdline_file);
            }
        }
    }
    closedir(dir);
    return pid;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_luxpro_vip_NativeEngine_getTargetPid(JNIEnv* env, jobject thiz) {
    int pid = get_process_id("com.miniclip.eightballpool");
    if (pid > 0) {
        LOGI("Target process found! PID: %d", pid);
    } else {
        LOGE("Target process NOT found.");
    }
    return pid;
}
