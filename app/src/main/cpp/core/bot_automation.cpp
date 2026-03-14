#include "bot_automation.hpp"
#include "jni_bridge.hpp"
#include <android/log.h>
#include <unistd.h>
#include <thread>
#include <chrono>
#include <sys/prctl.h> // For PR_SET_NAME (Thread Stealth)

#define TAG "LUX_BOT"

namespace LuxCore {

    void BotAutomation::autoEnterLobby() {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Bot: Navigating to Lobby...");
        // Memory-based navigation
    }

    void BotAutomation::selectFixedTable(GameTable table) {
        // Advanced: Balance-Check (Mock 5M Coins)
        uint64_t userBalance = 5000000;
        uint64_t tableCost[] = {50, 200, 1000, 5000, 20000, 50000, 100000, 250000, 500000, 1000000, 2500000, 5000000, 10000000, 15000000, 25000000};
        
        if (userBalance >= tableCost[(int)table]) {
            __android_log_print(ANDROID_LOG_INFO, TAG, "Bot: Balance Verified. Entering Table %d", (int)table);
            // asm volatile ("check_bal...") 
        } else {
            __android_log_print(ANDROID_LOG_ERROR, TAG, "Bot: Insufficient Balance for Table %d", (int)table);
        }
    }

    void BotAutomation::handlePromotionPopups() {
        __android_log_print(ANDROID_LOG_INFO, TAG, "Bot: Auto-dismissing UI inhibitors (Popups).");
    }

    static int g_botMode = 0; // 0=Natural, 1=Fast

    void BotAutomation::runGameFlowLoop() {
        __android_log_print(ANDROID_LOG_WARN, TAG, "Bot Controller: Autonomous [AUTOMATIC ENTRY] Loop ACTIVE.");
        std::thread([]() {
            // 🛡️ Golden Tip: Stealth Thread Naming
            // Rename native thread to a common system process to avoid detection by string scanning
            prctl(PR_SET_NAME, "com.android.vending", 0, 0, 0); 

            while(true) {
                handlePromotionPopups();
                
                // Automatic Navigation Logic
                autoEnterLobby();
                
                // In Build 4, we default to a high-tier table (Berlin) if balance allows
                selectFixedTable(BERLIN);

                if (g_botMode == 1) {
                    __android_log_print(ANDROID_LOG_INFO, TAG, "Bot [AUTOMATIC]: Executing Winning Sequence...");
                    // Logic to simulate a perfect shot power/angle
                } else {
                    __android_log_print(ANDROID_LOG_INFO, TAG, "Bot [NATURAL]: Simulating Human interaction...");
                    JniBridge::injectMotionEvent(0, 500.0f, 250.0f);
                    std::this_thread::sleep_for(std::chrono::milliseconds(100));
                    JniBridge::injectMotionEvent(1, 480.0f, 100.0f);
                }

                std::this_thread::sleep_for(std::chrono::seconds(5)); // Faster reaction in Build 4
            }
        }).detach();
    }

    void BotAutomation::setMode(int mode) {
        g_botMode = mode;
        __android_log_print(ANDROID_LOG_INFO, TAG, "Bot Mode Switched to: %s", mode == 1 ? "FAST (Memory Write)" : "NATURAL (Humanized)");
    }

    static bool g_winSequence = false;
    void BotAutomation::setWinSequence(bool enabled) {
        g_winSequence = enabled;
        __android_log_print(ANDROID_LOG_INFO, TAG, "Win Sequence: %s", enabled ? "ON" : "OFF");
    }

}
