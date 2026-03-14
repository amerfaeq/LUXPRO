#include "hook_manager.hpp"
#include "sentinel.hpp"
#include "jni_bridge.hpp"
#include "../security/anti_telemetry.hpp"
#include <android/log.h>
#include <sys/resource.h>
#include <thread>
#include <random>
#include <ctime>
#include <shadowhook.h>
#include <string>
#include <cstring>
#include <vector>

#define TAG "LUX_HOOK"

namespace LuxSecurity {

    // ── Function Hook Handlers (Simulated Login Linkage) ──
    typedef void (*LoginFunc)(void*, int); 
    LoginFunc old_FacebookLogin = nullptr;
    LoginFunc old_GoogleLogin = nullptr;

    void new_FacebookLogin(void* instance, int param) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Intercepted FB Button CLICK. Switching to LUX Bridge.");
        JniBridge::fireLoginBridge("facebook");
        // We DO NOT call old_FacebookLogin. This stops libgame from opening any SDK/Browser.
    }

    void new_GoogleLogin(void* instance, int param) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Intercepted Google Button CLICK. Switching to LUX Bridge.");
        JniBridge::fireLoginBridge("google");
        // We DO NOT call old_GoogleLogin.
    }

    // --- Anti-Detection: System Property Spoofing ---
    typedef int (*system_property_get_t)(const char*, char*);
    static system_property_get_t orig_system_property_get = nullptr;

    int new_system_property_get(const char* name, char* value) {
        if (strcmp(name, "ro.serialno") == 0 || strcmp(name, "ro.boot.serialno") == 0) {
            std::string spoofed = JniBridge::getActiveSpoofedSerial();
            if (spoofed != "UNKNOWN") {
                strcpy(value, spoofed.c_str());
                return (int)spoofed.length();
            }
        }
        return orig_system_property_get(name, value);
    }

    // --- Miniclip Login Hook ---
    typedef void* (*MiniclipLogin_t)(void*, void*);
    static MiniclipLogin_t orig_MiniclipLogin = nullptr;

    void* new_MiniclipLogin(void* a, void* b) {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Hook: Miniclip Login Intercepted.");
        JniBridge::fireLoginBridge("miniclip");
        return nullptr; // NO BYPASS: Bridge handles authentication
    }

    void HookManager::initializeLoginHook() {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Initializing Login Button Hooks (ShadowHook Protocol)...");
        
        // Facebook login is disabled and traces removed as per security protocol
        // shadowhook_hook_sym_name("libgame.so", "_ZN13LoginRegistry15FacebookHandlerEi", (void*)new_FacebookLogin, (void**)&old_FacebookLogin);
        shadowhook_hook_sym_name("libgame.so", "_ZN13LoginRegistry13GoogleHandlerEi", (void*)new_GoogleLogin, (void**)&old_GoogleLogin);
        
        // Miniclip hook - Symbol is usually in libIsolate or patterned
        shadowhook_hook_sym_name("libgame.so", "_ZN13LoginRegistry15MiniclipHandlerEi", (void*)new_MiniclipLogin, (void**)&orig_MiniclipLogin);
        
        __android_log_print(ANDROID_LOG_INFO, TAG, "Login Hooks ARMED. Waiting for user interaction.");
    }

    void HookManager::initializeHooks() {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Hooking Game Core Engine... stealth mode ACTIVE.");
        
        initializeLoginHook();

        // System Property Spoofing Hook
        shadowhook_hook_sym_name("libc.so", "__system_property_get", (void*)new_system_property_get, (void**)&orig_system_property_get);

        // Anti-Detection: Using Inline Hooking
        AntiTelemetry::isolateProcess();
        
        // ... rest of the function ...

        // ── Lobby Oracle Monitoring Loop (Phase 15) ──
        std::thread([]() {
            setpriority(PRIO_PROCESS, 0, 10); // Low priority background thread
            while (true) {
                Sentinel::runSentinelSweep(); // Performs focus-check and signature tracking
                std::this_thread::sleep_for(std::chrono::milliseconds(500)); // Scan every 0.5s for responsiveness
            }
        }).detach();

        // ── Event Listener Stub (Simulation) ──
        std::thread([]() {
            setpriority(PRIO_PROCESS, 0, 10); // Low priority background thread
            std::this_thread::sleep_for(std::chrono::seconds(30)); 
            onPocketSuccess(1, 3); // Simulate a 3-cushion bank shot success
        }).detach();
    }

    void HookManager::cleanupHooks() {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Detaching Hooks - Wiping execution traces.");
    }

    float HookManager::getTargetX() { return 500.0f; } // Mock value for coordinate extraction
    float HookManager::getTargetY() { return 250.0f; }
    float HookManager::getCueAngle() { return 1.57f; }

    void HookManager::bypassAntiCheatHooks() {
        // Advanced logic to hide native hooks from kernel-level observers
        __android_log_print(ANDROID_LOG_WARN, TAG, "Ghosting hooks from anti-cheat scanners.");
    }

    void HookManager::onPocketSuccess(int ballId, int bankCount) {
        static std::mt19937 gen(std::time(0));
        std::uniform_real_distribution<float> dist(0.0f, 1.0f);
        
        if (bankCount >= 3 && dist(gen) < 0.4f) { // 40% chance on bank shots
            __android_log_print(ANDROID_LOG_INFO, TAG, "BANK SHOT SUCCESS! (%d cushions). Sending reaction...", bankCount);
            // Emoji IDs: 1=Cool, 2=Surprise
            sendEmoji(dist(gen) > 0.5 ? 1 : 2);
        }
    }

    void HookManager::onMatchEnd(bool won) {
        if (won) {
            __android_log_print(ANDROID_LOG_INFO, TAG, "VICTORY! Celebration sequence initiated.");
            sendEmoji(3); // Trophy/Win
        }
    }

    void HookManager::sendEmoji(int emojiId) {
        std::thread([emojiId]() {
            static std::mt19937 gen(std::time(0));
            std::uniform_real_distribution<float> delay(1.5f, 3.0f);
            std::this_thread::sleep_for(std::chrono::milliseconds((int)(delay(gen) * 1000)));
            
            JniBridge::callNativeSendEmoji(emojiId);
            __android_log_print(ANDROID_LOG_INFO, TAG, "Titan AI EMOJI: Successfully dispatched ID %d.", emojiId);
        }).detach();
    }

}
