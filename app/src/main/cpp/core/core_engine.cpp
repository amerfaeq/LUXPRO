#include "hook_manager.hpp"
#include "core_engine.hpp"
#include <algorithm>
#include <cstdio>
#include <cstring>
#include <dirent.h>
#include <android/log.h>
#include <sys/mman.h>
#include <unistd.h>

namespace LuxCore {

    VisualPredictionEngine::VisualPredictionEngine() : tableWidth(1000.0f), tableHeight(500.0f), frictionCoeff(0.01f) {}

    VisualPredictionEngine::~VisualPredictionEngine() {}

    void VisualPredictionEngine::calibrateTable(float width, float height) {
        // Absolute Zero: Secure Calculation Buffer (Non-Swappable)
        static void* secureBuffer = nullptr;
        static bool isBufferReady = false;
        static size_t bufferSize = 4096;

        if (!isBufferReady && secureBuffer == nullptr) {
            void* buf = mmap(NULL, bufferSize, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
            if (buf != MAP_FAILED) {
                if (mlock(buf, bufferSize) == 0) {
                    secureBuffer = buf;
                    isBufferReady = true;
                    __android_log_print(ANDROID_LOG_INFO, "LUX_ENGINE", "Absolute Zero: Secure Paging ARMED.");
                } else {
                    // mlock failed — buffer valid but un-pinned, still use it
                    secureBuffer = buf;
                    isBufferReady = true;
                }
            }
        }

        // Apply ephemeral encryption to the buffer only when ready
        if (isBufferReady && secureBuffer != nullptr) {
            uint8_t* data = (uint8_t*)secureBuffer;
            for (size_t i = 0; i < bufferSize; i++) data[i] ^= 0x7F; // Scramble
        }

        tableWidth = width;
        tableHeight = height;
    }

    // Dynamic Path Coloring: Update color config from UI in real-time
    void VisualPredictionEngine::setColorConfig(int directColor, int bank1Color, int bank2Color, int ghostColor) {
        colorCfg.directShotColor = directColor;
        colorCfg.bank1Color      = bank1Color;
        colorCfg.bank2Color      = bank2Color;
        colorCfg.ghostBallColor  = ghostColor;
    }

    // Resolve color based on bank count
    static int resolveColor(const PathColorConfig& cfg, int bankCount) {
        if (bankCount == 0)        return cfg.directShotColor; // White: Direct Shot
        if (bankCount <= 2)        return cfg.bank1Color;      // Neon Yellow: 1-2 Banks
        return cfg.bank2Color;                                 // Intense Red: 3-5+ Banks
    }

    std::vector<TrajectoryPath> VisualPredictionEngine::calculatePaths(Vector2D cuePos, Vector2D targetDir, float power) {
        std::vector<TrajectoryPath> paths;
        
        TrajectoryPath primary;
        primary.opacity = params.opacity;
        primary.bankCount = 0;
        
        Vector2D currentPos = cuePos;
        Vector2D velocity = targetDir.normalize() * power;
        
        primary.points.push_back(currentPos);
        
        int pocketsHit = 0;

        // Physics simulation with bank counting and pocket interception
        for (int i = 0; i < 60; ++i) {
            currentPos = currentPos + velocity;
            
            // Wall reflection (Cushion/Bank Detection)
            bool hitWall = false;
            if (currentPos.x <= 0 || currentPos.x >= tableWidth) {
                velocity.x *= -1;
                currentPos.x = std::max(0.0f, std::min(currentPos.x, tableWidth));
                hitWall = true;
            }
            if (currentPos.y <= 0 || currentPos.y >= tableHeight) {
                velocity.y *= -1;
                currentPos.y = std::max(0.0f, std::min(currentPos.y, tableHeight));
                hitWall = true;
            }
            
            if (hitWall) primary.bankCount++;
            
            primary.points.push_back(currentPos);

            // Multi-Target Refraction: Check if this step enters ANY pocket
            int pId = identifyTargetPocket(currentPos, velocity);
            if (pId != -1) {
                pocketsHit++;
                if (pocketsHit >= 2) primary.isMultiTargetSuccess = true;
            }

            velocity = velocity * (1.0f - frictionCoeff);
            if (velocity.length() < 0.1f) break;
        }
        
        // ── Dynamic Color Assignment based on bank count ──
        primary.color = resolveColor(colorCfg, primary.bankCount);
        
        // If multi-target success, override with Royal Purple
        if (primary.isMultiTargetSuccess) primary.color = 0xAA00FF; 

        paths.push_back(primary);

        // ── Ghost Ball (Stop Position Indicator) ──
        TrajectoryPath ghostBall;
        ghostBall.isGhostBall = true;
        ghostBall.pulsate     = true;
        ghostBall.color       = colorCfg.ghostBallColor;
        ghostBall.opacity     = params.opacity * 0.9f;
        ghostBall.points.push_back(currentPos); // Final rest position
        paths.push_back(ghostBall);

        return paths;
    }

    float VisualPredictionEngine::calculateSuperBreakImpulse(float power, float angle) {
        return power * 1.5f * std::cos(angle * 0.5f);
    }

    Vector2D VisualPredictionEngine::mapScreenToTable(float screenX, float screenY, float screenW, float screenH) {
        float normX = (screenX / screenW) * tableWidth;
        float normY = (screenY / screenH) * tableHeight;
        return Vector2D(normX, normY);
    }

    int VisualPredictionEngine::identifyTargetPocket(Vector2D ballPos, Vector2D velocity) {
        std::vector<Vector2D> pockets = {
            {0, 0}, {500, 0}, {1000, 0},
            {0, 500}, {500, 500}, {1000, 500}
        };

        int bestPocket = -1;
        float maxDot = -2.0f;
        Vector2D dir = velocity.normalize();

        for (int i = 0; i < (int)pockets.size(); ++i) {
            Vector2D toPocket = (pockets[i] - ballPos).normalize();
            float dot = dir.x * toPocket.x + dir.y * toPocket.y;
            if (dot > maxDot) {
                maxDot = dot;
                bestPocket = i;
            }
        }
        
        return bestPocket;
    }

    uintptr_t MemoryHelper::getModuleBase(const char* moduleName) {
        if (moduleBases.count(moduleName)) return moduleBases[moduleName];

        FILE* fp = fopen("/proc/self/maps", "rt");
        if (!fp) return 0;

        uintptr_t addr = 0;
        char line[512];
        while (fgets(line, sizeof(line), fp)) {
            if (strstr(line, moduleName)) {
                addr = (uintptr_t)strtoull(line, NULL, 16);
                break;
            }
        }
        fclose(fp);
        moduleBases[moduleName] = addr;
        return addr;
    }

    bool MemoryHelper::isProcessable(uintptr_t addr) {
        if (addr == 0) return false;
        // Zenith Pulse: Basic page-alignment and validation check
        // We use msync with MS_ASYNC to verify if the memory address is mapped
        void* ptr = (void*)(addr & ~0xFFF); // Align to 4KB page
        return (msync(ptr, 1, MS_ASYNC) == 0);
    }

    void CoreEngine::setTimeSpeed(float scale) {
        // Advanced: Inject TimeScale into Game Engine (Mock/Placeholder)
        __android_log_print(ANDROID_LOG_INFO, "LUX_ENGINE", "Game Time Scale set to %.1fx", scale);
    }

}

