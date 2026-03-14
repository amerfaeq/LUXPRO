#ifndef CORE_ENGINE_HPP
#define CORE_ENGINE_HPP

#include <vector>
#include <cmath>
#include <string>
#include <map>
#include <cstdint>

namespace LuxCore {

    struct Vector2D {
        float x, y;
        Vector2D(float _x = 0, float _y = 0) : x(_x), y(_y) {}
        
        Vector2D operator+(const Vector2D& v) const { return Vector2D(x + v.x, y + v.y); }
        Vector2D operator-(const Vector2D& v) const { return Vector2D(x - v.x, y - v.y); }
        Vector2D operator*(float s) const { return Vector2D(x * s, y * s); }
        
        float length() const { return std::sqrt(x * x + y * y); }
        float dot(const Vector2D& v) const { return x * v.x + y * v.y; }
        Vector2D normalize() const {
            float l = length();
            return l > 0 ? Vector2D(x / l, y / l) : Vector2D(0, 0);
        }
    };

    // Dynamic Path Coloring Configuration
    struct PathColorConfig {
        int directShotColor  = 0xFFFFFF; // Solid White (0 bounces)
        int bank1Color       = 0xFFFF00; // Neon Yellow (1-2 banks)
        int bank2Color       = 0xFF2200; // Intense Red  (3-5 banks)
        int ghostBallColor   = 0x00FFFF; // Pulsating Cyan (stop position)
        float ghostOpacity   = 1.0f; // drives pulsation in rendering layer
    };

    struct RenderParams {
        float lineWidth = 2.0f;
        float opacity = 1.0f;
        int themeColor = 0xFFFFFF;
    };

    struct TrajectoryPath {
        std::vector<Vector2D> points;
        int color;         // Hex color
        float opacity;
        int bankCount = 0; // Number of cushion reflections (banks)
        bool isGhostBall = false; // True if this is the stop-position indicator
        bool pulsate = false;     // True for animated (ghost ball) highlight
        bool isMultiTargetSuccess = false; // True if it hits multiple pockets
    };

    class VisualPredictionEngine {
    public:
        VisualPredictionEngine();
        ~VisualPredictionEngine();

        // Multi-path trajectory calculation
        std::vector<TrajectoryPath> calculatePaths(Vector2D cuePos, Vector2D targetDir, float power);

        // ITPI: Intelligent Target Pocket Identification
        int identifyTargetPocket(Vector2D ballPos, Vector2D velocity);

        // Table Calibration
        void calibrateTable(float width, float height);

        // Super Break: Impulse Force Calculation
        float calculateSuperBreakImpulse(float power, float angle);

        // Alignment Guide Logic
        Vector2D mapScreenToTable(float screenX, float screenY, float screenW, float screenH);

        // Dynamic Path Coloring
        void setColorConfig(int directColor, int bank1Color, int bank2Color, int ghostColor);
        
        // Render Sync
        void setRenderParams(float width, float alpha) { params.lineWidth = width; params.opacity = alpha; }
        RenderParams getRenderParams() { return params; }

    private:
        float tableWidth, tableHeight;
        float frictionCoeff;
        PathColorConfig colorCfg; // Active color configuration
        RenderParams params;
    };

    class MemoryHelper {
    public:
        static MemoryHelper& getInstance() {
            static MemoryHelper instance;
            return instance;
        }

        void setOffset(const std::string& key, uintptr_t addr) { offsets[key] = addr; }
        uintptr_t getOffset(const std::string& key) { return offsets[key]; }
        
        uintptr_t getModuleBase(const char* moduleName);
        bool isProcessable(uintptr_t addr);

    private:
        MemoryHelper() {}
        std::map<std::string, uintptr_t> offsets;
        std::map<std::string, uintptr_t> moduleBases;
    };

    class CoreEngine {
    public:
        static void setTimeSpeed(float scale);
    };

}

#endif // CORE_ENGINE_HPP
