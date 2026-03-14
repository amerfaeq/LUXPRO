#ifndef VFX_BLOOM_GLSL_HPP
#define VFX_BLOOM_GLSL_HPP

// ══════════════════════════════════════════════════════
// LUX PRO — Neon Bloom GLSL Shaders (OpenGL ES 3.0)
// Optimized for Android mobile GPUs (Adreno / Mali)
// ══════════════════════════════════════════════════════

namespace LuxVFX {

// ── Vertex Shader: Trajectory Line ───────────────────
// Passes position + a_glowFactor to fragment shader
static constexpr const char* BLOOM_VERTEX_SRC = R"GLSL(
    #version 300 es
    precision mediump float;

    in  vec2  a_position;  // Screen-space point on trajectory
    in  float a_glowFactor; // From CPU: glow multiplier [0.0..1.0]

    uniform mat4 u_mvp;

    out float v_glow;

    void main() {
        gl_Position = u_mvp * vec4(a_position, 0.0, 1.0);
        v_glow      = a_glowFactor;
    }
)GLSL";

// ── Fragment Shader: Neon Bloom + Arc Flicker ────────
// Outputs base color + additive glow bloom around line
static constexpr const char* BLOOM_FRAGMENT_SRC = R"GLSL(
    #version 300 es
    precision mediump float;

    in  float v_glow;

    uniform vec4  u_lineColor;    // Base RGBA line color
    uniform vec4  u_glowColor;    // Bloom RGBA (usually same hue, higher brightness)
    uniform float u_arcFlicker;   // 0.4..1.0: plasma arc instability value
    uniform int   u_isGhostBall;  // 1 = pulsating ghost-ball indicator

    out vec4 fragColor;

    void main() {
        // Core line color
        vec4 base = u_lineColor;

        // Additive neon glow
        vec4 bloom = u_glowColor * v_glow;

        // Electric arc: modulate alpha with flicker for bank shots
        float flicker = mix(1.0, u_arcFlicker, float(v_glow > 0.0));

        // Ghost ball: extra pulsating ring
        if (u_isGhostBall == 1) {
            float ring = abs(sin(gl_FragCoord.x * 0.05 + gl_FragCoord.y * 0.05));
            bloom += vec4(1.0, 1.0, 1.0, ring * v_glow * 0.4);
        }

        fragColor = (base + bloom) * flicker;
    }
)GLSL";

// ── Fragment Shader: Impact Flash ────────────────────
// Full-screen tinted flash at cushion impact point
static constexpr const char* FLASH_FRAGMENT_SRC = R"GLSL(
    #version 300 es
    precision mediump float;

    uniform vec4  u_flashColor;  // e.g. white or bright line color
    uniform float u_flashAlpha;  // Fades from 1.0 → 0.0 over flashDuration

    out vec4 fragColor;

    void main() {
        fragColor = u_flashColor * u_flashAlpha;
    }
)GLSL";

} // namespace LuxVFX

#endif // VFX_BLOOM_GLSL_HPP
