#ifndef AUTO_PLAY_HPP
#define AUTO_PLAY_HPP

#include "core_engine.hpp"

namespace LuxCore {

    struct HumanMotionParams {
        float aimingSpeedBase = 1.0f;
        float thinkingDelayMin = 1.5f;
        float thinkingDelayMax = 3.5f;
        float powerAdjustmentOvershoot = 0.05f; // 5% overshoot
        float microAdjustmentChance = 0.7f;
    };

    class AutoPlay {
    public:
        static Vector2D generateBezierPath(Vector2D start, Vector2D end, float t);
        static float calculateSuperBreakImpulse(int ballCount);
        
        void executeShot(Vector2D targetPos, float power, bool fastMode);
        void simulateThinkingDelay();
        void performMicroAdjustments(float targetAngle);
        void smoothGlideAim(float startAngle, float targetAngle);
        void simulateNaturalPowerPull(float targetPower);

    private:
        HumanMotionParams params;
    };

}

#endif // AUTO_PLAY_HPP
