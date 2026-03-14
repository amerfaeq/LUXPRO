#ifndef VFX_SHADER_HPP
#define VFX_SHADER_HPP

#include <cstdint>
#include <cmath>

namespace LuxVFX {

    // ── VFX State ──────────────────────────────────────────────────────────────
    struct VfxConfig {
        // Neon Glow
        float glowIntensity   = 0.8f;   // 0.0 → 1.0 (UI 0%-100%)
        float pulseSpeed      = 1.0f;   // Oscillations per second
        bool  electricArcOn   = true;   // Flickering plasma for bank shots

        // Line Colors (packed ARGB int)
        int   directPathColor = 0xFF0088FF;  // Electric Blue
        int   bankPathColor   = 0xFFFF2200;  // Fiery Red
        int   pocketColor     = 0xFF00FF44;  // Neon Lime
        int   ghostBallColor  = 0xFFFFFFFF;  // Phantom White

        // Flash
        bool  impactFlashOn   = true;
        float flashDuration   = 0.15f;   // seconds
    };

    // ── Runtime State ──────────────────────────────────────────────────────────
    struct VfxRuntime {
        float timeAccum = 0.0f;           // Accumulated time in seconds
        float flashTimer = 0.0f;          // Counts down per impact point
        float flickerPhase = 0.0f;        // Random noise seed for electric arc
    };

    class VfxShader {
    public:
        static VfxShader& getInstance() {
            static VfxShader inst;
            return inst;
        }

        void setConfig(const VfxConfig& cfg) { config = cfg; }
        VfxConfig& getConfig() { return config; }

        // Call every frame with delta-time (seconds)
        void tick(float dt);

        // Current glow multiplier (0.0 → 1.0), factoring pulse + intensity
        float getGlowMultiplier() const;

        // Current arc flicker value (0.0 → 1.0), for bank shot instability
        float getArcFlicker() const;

        // Trigger an impact flash (call when ball hits a cushion/stop point)
        void triggerImpactFlash();

        // Returns true while an impact flash is active
        bool isFlashing() const;

        // Returns normalized flash brightness [0.0 → 1.0] for alpha fade
        float getFlashAlpha() const;

        // Shader source accessors (for GL program compilation)
        static const char* getVertexShaderSrc();
        static const char* getFragmentShaderSrc();
        static const char* getFlashShaderSrc();
        void logShaderInfo();

    private:
        VfxShader() = default;
        VfxConfig  config;
        VfxRuntime runtime;
    };

} // namespace LuxVFX

#endif // VFX_SHADER_HPP
