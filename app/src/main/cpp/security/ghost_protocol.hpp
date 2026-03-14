#ifndef GHOST_PROTOCOL_HPP
#define GHOST_PROTOCOL_HPP

#include <sys/mman.h>
#include <sys/syscall.h>
#include <unistd.h>
#include <fcntl.h>
#include <string>

namespace LuxSecurity {

    class GhostProtocol {
    public:
        static void enableAntiDebug();
        static void setPhantomState(bool enabled);
        static bool checkAntiKernel();
        static bool verifyRuntimeIntegrity();
        static bool checkApkIntegrity();
        static bool checkSanitizedEnvironment();
        static bool verifyClassIntegrity(const char* className, const char* expectedHash);
        static void auraShieldPulse();
        static std::string generateHardwareBoundEntropy(const char* salt);
        static void antiDumpPulse();
        static void performVRAMVoid();
        static bool validateQuantumTiming();
        static bool detectKernelProbes();
        static int rawSyscallOpen(const char* path, int flags);
        static std::string generateSessionKey(const char* deviceId);
        static void rotateNebulaSalt();
        static std::string generateQuantumSignature(const char* data);
        static void shredSensitiveMemory(void* ptr, size_t size);
        static int createFilelessBuffer(const void* data, size_t size, const char* name);
        static void zeroTraceCleanup(int fd, size_t size);
        static void triggerDestructiveKill();
        static void wipeMemory(void* ptr, size_t size);
        
        // Sentinel Silent Reporting Support
        static int getActiveLogFD();
        static void setActiveLogFD(int fd);
    };

}

#endif // GHOST_PROTOCOL_HPP
