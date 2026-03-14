#ifndef VFX_RENDERER_HPP
#define VFX_RENDERER_HPP

// ══════════════════════════════════════════════════════════════
//  LUX PRO — VFX OpenGL ES 3.0 Render Pipeline Helper
//  Handles shader compilation, uniform binding, and draw calls
//  Optimized for Adreno 6xx / Mali-G7x (60–120 FPS target)
// ══════════════════════════════════════════════════════════════

#include <GLES3/gl3.h>
#include <cstdint>
#include <android/log.h>
#include "vfx_shader.hpp"
#include "vfx_bloom_glsl.hpp"

#define TAG_R "LUX_VFX_RENDERER"

namespace LuxVFX {

    // ── GL Utilities ────────────────────────────────────────────────
    inline GLuint compileShader(GLenum type, const char* src) {
        GLuint shader = glCreateShader(type);
        glShaderSource(shader, 1, &src, nullptr);
        glCompileShader(shader);

        GLint ok = 0;
        glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
        if (!ok) {
            char log[512];
            glGetShaderInfoLog(shader, 512, nullptr, log);
            __android_log_print(ANDROID_LOG_ERROR, TAG_R, "Shader compile error: %s", log);
            glDeleteShader(shader);
            return 0;
        }
        return shader;
    }

    inline GLuint linkProgram(GLuint vert, GLuint frag) {
        GLuint prog = glCreateProgram();
        glAttachShader(prog, vert);
        glAttachShader(prog, frag);
        glLinkProgram(prog);

        GLint ok = 0;
        glGetProgramiv(prog, GL_LINK_STATUS, &ok);
        if (!ok) {
            char log[512];
            glGetProgramInfoLog(prog, 512, nullptr, log);
            __android_log_print(ANDROID_LOG_ERROR, TAG_R, "Program link error: %s", log);
            glDeleteProgram(prog);
            return 0;
        }
        glDeleteShader(vert);
        glDeleteShader(frag);
        return prog;
    }

    // ── VFX Render Pipeline ─────────────────────────────────────────
    class VfxRenderer {
    public:
        static VfxRenderer& getInstance() {
            static VfxRenderer inst;
            return inst;
        }

        // Call once from GL thread after EGL context is ready
        bool init() {
            GLuint vert = compileShader(GL_VERTEX_SHADER,   BLOOM_VERTEX_SRC);
            GLuint frag = compileShader(GL_FRAGMENT_SHADER, BLOOM_FRAGMENT_SRC);
            if (!vert || !frag) return false;
            bloomProgram = linkProgram(vert, frag);

            GLuint fvert = compileShader(GL_VERTEX_SHADER,   BLOOM_VERTEX_SRC);
            GLuint ffrag = compileShader(GL_FRAGMENT_SHADER, FLASH_FRAGMENT_SRC);
            if (!fvert || !ffrag) return false;
            flashProgram = linkProgram(fvert, ffrag);

            // Cache uniform locations
            uMvp         = glGetUniformLocation(bloomProgram, "u_mvp");
            uLineColor   = glGetUniformLocation(bloomProgram, "u_lineColor");
            uGlowColor   = glGetUniformLocation(bloomProgram, "u_glowColor");
            uArcFlicker  = glGetUniformLocation(bloomProgram, "u_arcFlicker");
            uIsGhostBall = glGetUniformLocation(bloomProgram, "u_isGhostBall");
            uFlashColor  = glGetUniformLocation(flashProgram, "u_flashColor");
            uFlashAlpha  = glGetUniformLocation(flashProgram, "u_flashAlpha");

            __android_log_print(ANDROID_LOG_INFO, TAG_R,
                "VFX Renderer: Programs compiled. Bloom=%u Flash=%u", bloomProgram, flashProgram);
            initialized = true;
            return true;
        }

        // Call before drawing each trajectory segment
        void bindBloomUniforms(float r, float g, float b, bool isGhostBall) {
            if (!initialized) return;
            VfxShader& vfx = VfxShader::getInstance();
            float glow     = vfx.getGlowMultiplier();
            float flicker  = vfx.getArcFlicker();

            glUseProgram(bloomProgram);
            glUniform4f(uLineColor, r, g, b, 1.0f);
            glUniform4f(uGlowColor, r * 1.6f, g * 1.6f, b * 1.6f, glow); // Brighter bloom
            glUniform1f(uArcFlicker, flicker);
            glUniform1i(uIsGhostBall, isGhostBall ? 1 : 0);
        }

        // Call at cushion impact point
        void drawImpactFlash(float r, float g, float b) {
            if (!initialized) return;
            VfxShader& vfx = VfxShader::getInstance();
            if (!vfx.isFlashing()) return;
            float alpha = vfx.getFlashAlpha(); // fades over flashDuration
            glUseProgram(flashProgram);
            glUniform4f(uFlashColor, r, g, b, 1.0f);
            glUniform1f(uFlashAlpha, alpha);
        }

        GLuint getBloomProgram() const { return bloomProgram; }
        GLuint getFlashProgram() const { return flashProgram; }
        bool   isReady()        const { return initialized; }

    private:
        VfxRenderer() = default;
        GLuint bloomProgram = 0, flashProgram = 0;
        GLint  uMvp = -1, uLineColor = -1, uGlowColor = -1;
        GLint  uArcFlicker = -1, uIsGhostBall = -1;
        GLint  uFlashColor = -1, uFlashAlpha = -1;
        bool   initialized = false;
    };

} // namespace LuxVFX

#endif // VFX_RENDERER_HPP
