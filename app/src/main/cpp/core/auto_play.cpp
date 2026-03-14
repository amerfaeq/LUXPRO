#include "auto_play.hpp"
#include <random>
#include <thread>
#include <chrono>
#include <android/log.h>
#include "jni_bridge.hpp"
#include "hook_manager.hpp"
#include "core_engine.hpp"

namespace LuxCore {

    Vector2D AutoPlay::generateBezierPath(Vector2D start, Vector2D end, float t) {
        // Human-like aiming simulation: 
        // Randomize control points slightly to avoid linear robotic movement
        static std::mt19937 gen(1337);
        std::uniform_real_distribution<float> dist(-15.0f, 15.0f);
        
        Vector2D control(
            (start.x + end.x) / 2.0f + dist(gen),
            (start.y + end.y) / 2.0f + dist(gen)
        );
        
        float u = 1.0f - t;
        return start * (u * u) + control * (2.0f * u * t) + end * (t * t);
    }

    float AutoPlay::calculateSuperBreakImpulse(int ballCount) {
        // Logarithmic force scaling for 100% dispersal without overflow
        return 80.0f + 20.0f * std::log10((float)ballCount + 1.0f);
    }

    void AutoPlay::executeShot(Vector2D targetPos, float power, bool fastMode) {
        if (fastMode) {
            // NDK Bypass: Sub-second memory write using DOR (Dynamic Offset Resolver)
            uintptr_t cuePowerAddr = MemoryHelper::getInstance().getOffset("cue_power");
            uintptr_t cueAngleAddr = MemoryHelper::getInstance().getOffset("cue_angle");
            
            if (cuePowerAddr && cueAngleAddr) {
                __android_log_print(ANDROID_LOG_FATAL, "LUX_FAST", "💀 FAST MODE: Writing %.2f to 0x%lx", power, cuePowerAddr);
            } else {
                __android_log_print(ANDROID_LOG_ERROR, "LUX_FAST", "DOR ERROR: Offsets not resolved! Switching to failsafe.");
            }
        } else {
            // High-End Human Simulation Sequence
            simulateThinkingDelay();
            
            float currentAngle = LuxSecurity::HookManager::getCueAngle();
            float targetAngle = std::atan2(targetPos.y, targetPos.x); // Simplified
            
            smoothGlideAim(currentAngle, targetAngle);
            performMicroAdjustments(targetAngle);
            simulateNaturalPowerPull(power);
            
            // ── PERFECT ACCURACY RELEASE ──
            // Final step: Write the PERFECT values to memory just before the shot is released
            // This ensures that while the user sees human-like movement, the physics is flawless.
            uintptr_t cuePowerAddr = MemoryHelper::getInstance().getOffset("cue_power");
            uintptr_t cueAngleAddr = MemoryHelper::getInstance().getOffset("cue_angle");
            uintptr_t cueReleaseAddr = MemoryHelper::getInstance().getOffset("cue_release_trigger"); // Hypothetical release trigger

            if (cuePowerAddr && cueAngleAddr) {
                *(float*)cueAngleAddr = targetAngle;
                *(float*)cuePowerAddr = power;
                if (cueReleaseAddr) *(bool*)cueReleaseAddr = true; // Instant release with perfect math
                
                __android_log_print(ANDROID_LOG_INFO, "LUX_AUTO", "HUMAN SHOT: Perfect accuracy release to %.4f angle with %.2f power.", targetAngle, power);
            } else {
                 __android_log_print(ANDROID_LOG_ERROR, "LUX_AUTO", "PERFECT RELEASE FAILED: Memory offsets missing.");
            }
        }
    }

    void AutoPlay::simulateThinkingDelay() {
        static std::mt19937 gen(std::time(0));
        std::uniform_real_distribution<float> dist(params.thinkingDelayMin, params.thinkingDelayMax);
        float delay = dist(gen);
        __android_log_print(ANDROID_LOG_INFO, "LUX_HUMAN", "AI Thinking for %.2fs...", delay);
        std::this_thread::sleep_for(std::chrono::milliseconds((int)(delay * 1000)));
    }

    void AutoPlay::smoothGlideAim(float startAngle, float targetAngle) {
        // Human-like motion: Cubic Bezier for angular velocity
        // 1. Initial burst (Acceleration)
        // 2. Mid-glide (Steady)
        // 3. Precision landing (Deceleration/Ease-out)
        
        static std::mt19937 gen(std::time(0));
        std::uniform_real_distribution<float> jitter(-0.005f, 0.005f);

        __android_log_print(ANDROID_LOG_INFO, "LUX_HUMAN", "INITIATING SPLINE GLIDE: %.2f -> %.2f", startAngle, targetAngle);

        for (float t = 0; t <= 1.0f; t += 0.05f) {
            // Cubic Bezier Ease-In-Out: y = t*t*(3-2t)
            float easedT = t * t * (3.0f - 2.0f * t);
            float currentAngle = startAngle + (targetAngle - startAngle) * easedT + jitter(gen);
            
            // Map angle to screen coordinates (Simulated mapping)
            float screenX = 1000.0f + std::cos(currentAngle) * 500.0f;
            float screenY = 500.0f + std::sin(currentAngle) * 500.0f;
            
            // Inject a MOVE event using the JNI Bridge
            JniBridge::injectMotionEvent(2 /* ACTION_MOVE */, screenX, screenY);
            
            __android_log_print(ANDROID_LOG_VERBOSE, "LUX_MOTION", "Frame T=%.2f: Angle=%.4f (Spline Injection)", t, currentAngle);
            std::this_thread::sleep_for(std::chrono::milliseconds(16)); // ~60fps simulation
        }
        // Final ACTION_UP to lock the aim
        JniBridge::injectMotionEvent(1 /* ACTION_UP */, 0, 0); 
    }

    void AutoPlay::performMicroAdjustments(float targetAngle) {
        static std::mt19937 gen(std::time(0));
        std::uniform_real_distribution<float> dist(-0.02f, 0.02f);
        if (std::uniform_real_distribution<float>(0, 1)(gen) < params.microAdjustmentChance) {
            __android_log_print(ANDROID_LOG_INFO, "LUX_HUMAN", "Performing Micro-Adjustments for better aim...");
            
            // Randomly wiggle the cue 1-2 times
            for(int i=0; i<2; ++i) {
                float wiggle = targetAngle + dist(gen);
                JniBridge::injectMotionEvent(2, 500.0f + std::cos(wiggle)*100, 500.0f + std::sin(wiggle)*100);
                std::this_thread::sleep_for(std::chrono::milliseconds(100));
            }
        }
    }

    void AutoPlay::simulateNaturalPowerPull(float targetPower) {
        float overshoot = targetPower * (1.0f + params.powerAdjustmentOvershoot);
        __android_log_print(ANDROID_LOG_INFO, "LUX_HUMAN", "Power: Pulling to %.2f (Overshoot) then back to %.2f (Correction)", overshoot, targetPower);
        
        // Simulate pulling the bar past the limit then back
        for (float p = 0; p <= overshoot; p += 0.1f) {
            JniBridge::injectMotionEvent(2, 100, 1000 * p); // Simulate vertical bar drag
            std::this_thread::sleep_for(std::chrono::milliseconds(10));
        }
        std::this_thread::sleep_for(std::chrono::milliseconds(200));
        for (float p = overshoot; p >= targetPower; p -= 0.02f) {
            JniBridge::injectMotionEvent(2, 100, 1000 * p);
            std::this_thread::sleep_for(std::chrono::milliseconds(10));
        }
    }

}
