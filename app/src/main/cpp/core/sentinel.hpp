#ifndef SENTINEL_HPP
#define SENTINEL_HPP

#include <vector>
#include <string>
#include <cstdint>

namespace LuxSecurity {

    class Sentinel {
    public:
        enum BreakPrediction {
            ANALYZING = 0,
            YOUR_BREAK = 1,
            OPPONENT_BREAK = 2
        };

        // Automated Offset Finder (Pattern Scanning)
        static uintptr_t findPattern(uintptr_t start, uintptr_t end, const char* pattern, const char* mask);
        static uintptr_t resolveSovereignOffset(uintptr_t start, uintptr_t end, const std::vector<std::pair<const char*, const char*>>& signatures);
        
        // Dynamic RAM signature tracking
        static void trackMemorySignatures();

        // Silent Reporting System
        static void ExecuteSecretLog(const std::string& status);
        
        // Silent Background Operation
        static void runSentinelSweep();
        static void startAuthWatchdog();

        // Lobby Oracle Logic
        static BreakPrediction getLobbyBreakPrediction(int tableId);
        static void applyRemoteOffset(uintptr_t newOffset);
        static int getDetectedTableFocus();
        static int currentFocusTable;

    private:
        static bool compareBytes(const uint8_t* data, const uint8_t* pattern, const char* mask);
    };

}

#endif // SENTINEL_HPP
