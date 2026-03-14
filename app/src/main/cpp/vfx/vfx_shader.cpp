#include "vfx_shader.hpp"
#include "vfx_bloom_glsl.hpp"
#include <android/log.h>
#include <cstdlib>
#include <cmath>

#define TAG "LUX_VFX"

namespace LuxVFX {

    void VfxShader::tick(float dt) {
        runtime.timeAccum    += dt;
        runtime.flickerPhase += dt * 17.3f; // Fast offset for arc noise
        if (runtime.flashTimer > 0.0f) {
            runtime.flashTimer -= dt;
        }
    }

    float VfxShader::getGlowMultiplier() const {
        if (config.glowIntensity <= 0.0f) return 0.0f;

        // Breathing / pulsation: sine wave between 0.5 and 1.0
        float pulse = 0.5f + 0.5f * std::sin(runtime.timeAccum * config.pulseSpeed * 6.2831853f);

        return config.glowIntensity * pulse; // Range: 0.0 → glowIntensity
    }

    float VfxShader::getArcFlicker() const {
        if (!config.electricArcOn) return 1.0f;

        // Pseudo-random flicker: fract(sin(x) * k)
        float x = runtime.flickerPhase;
        float v = x - std::floor(x); // fract
        v = std::sin(v * 43758.5453f);
        v = v - std::floor(v);

        // Map to [0.4, 1.0] so the line never disappears completely
        return 0.4f + 0.6f * v;
    }

    void VfxShader::triggerImpactFlash() {
        if (config.impactFlashOn) {
            runtime.flashTimer = config.flashDuration;
            __android_log_print(ANDROID_LOG_DEBUG, TAG, "Impact Flash triggered!");
        }
    }

    bool VfxShader::isFlashing() const {
        return runtime.flashTimer > 0.0f;
    }

    float VfxShader::getFlashAlpha() const {
        if (config.flashDuration <= 0.0f) return 0.0f;
        // Normalized countdown: starts at 1.0, falls to 0.0 as timer expires
        float alpha = runtime.flashTimer / config.flashDuration;
        return alpha < 0.0f ? 0.0f : (alpha > 1.0f ? 1.0f : alpha);
    }

    // ── Shader Build Log (Debug) ─────────────────────────────────
    // Returns a human-readable summary of shader constants for validation
    const char* VfxShader::getVertexShaderSrc()   { return BLOOM_VERTEX_SRC; }
    const char* VfxShader::getFragmentShaderSrc()  { return BLOOM_FRAGMENT_SRC; }
    const char* VfxShader::getFlashShaderSrc()     { return FLASH_FRAGMENT_SRC; }

    void VfxShader::logShaderInfo() {
        __android_log_print(ANDROID_LOG_INFO, TAG, "VFX Shaders Ready. Sources: V=%p F=%p Flash=%p",
            (void*)BLOOM_VERTEX_SRC, (void*)BLOOM_FRAGMENT_SRC, (void*)FLASH_FRAGMENT_SRC);
        __android_log_print(ANDROID_LOG_INFO, TAG, "Active Config → Glow=%.2f Pulse=%.2f Arc=%d Flash=%d",
            config.glowIntensity, config.pulseSpeed, config.electricArcOn, config.impactFlashOn);
    }

} // namespace LuxVFX
