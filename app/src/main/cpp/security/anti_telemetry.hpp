#ifndef ANTI_TELEMETRY_HPP
#define ANTI_TELEMETRY_HPP

#include <string>

namespace LuxSecurity {

    class AntiTelemetry {
    public:
        // Hooks networking and logging functions to block reports
        static void isolateProcess();
        
        // Purges internal game logs before they reach the server
        static void purgeLocalLogs();

    private:
        static void hookLogBuffer();
    };

}

#endif // ANTI_TELEMETRY_HPP
