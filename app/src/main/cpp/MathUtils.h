#ifndef MATH_UTILS_H
#define MATH_UTILS_H

#include <cmath>

struct Vector2D {
    float x;
    float y;

    Vector2D() : x(0), y(0) {}
    Vector2D(float x, float y) : x(x), y(y) {}

    Vector2D operator+(const Vector2D& other) const { return Vector2D(x + other.x, y + other.y); }
    Vector2D operator-(const Vector2D& other) const { return Vector2D(x - other.x, y - other.y); }
    Vector2D operator*(float scalar) const { return Vector2D(x * scalar, y * scalar); }
    Vector2D operator/(float scalar) const { return Vector2D(x / scalar, y / scalar); }
};

inline float DotProduct(const Vector2D& a, const Vector2D& b) {
    return a.x * b.x + a.y * b.y;
}

inline float Magnitude(const Vector2D& v) {
    return std::sqrt(v.x * v.x + v.y * v.y);
}

inline Vector2D Normalize(const Vector2D& v) {
    float mag = Magnitude(v);
    if (mag == 0) return Vector2D(0, 0);
    return v / mag;
}

inline float Distance(const Vector2D& a, const Vector2D& b) {
    return Magnitude(a - b);
}

// Reflection formula: V_ref = V - 2 * (V dot N) * N
inline Vector2D CalculateReflection(const Vector2D& velocity, const Vector2D& normal) {
    float dot = DotProduct(velocity, normal);
    return velocity - normal * (2.0f * dot);
}

// =========================================================
// TABLE BOUNDARY COLLISION
// =========================================================

// Temporary table boundaries (Screen Coordinates)
// You must adjust these values to perfectly match the pool table in the game
const float TABLE_LEFT = 200.0f;
const float TABLE_RIGHT = 1800.0f;
const float TABLE_TOP = 200.0f;           // Top of the screen is Y=0
const float TABLE_BOTTOM = 800.0f;

struct HitResult {
    bool hit;
    Vector2D point;
    Vector2D normal;
    float distance;
};

// Check intersection of a Ray (startPos + direction) against Table Walls
inline HitResult RayIntersectsAABB(const Vector2D& origin, const Vector2D& dir) {
    HitResult result;
    result.hit = false;
    result.distance = 999999.0f;

    // Check Left Wall (x = TABLE_LEFT)
    if (dir.x < 0) { 
        float t = (TABLE_LEFT - origin.x) / dir.x;
        if (t > 0) {
            float yHit = origin.y + dir.y * t;
            if (yHit >= TABLE_TOP && yHit <= TABLE_BOTTOM) {
                if (t < result.distance) {
                    result.hit = true;
                    result.distance = t;
                    result.point = Vector2D(TABLE_LEFT, yHit);
                    result.normal = Vector2D(1, 0); // Normal points right
                }
            }
        }
    }
    // Check Right Wall (x = TABLE_RIGHT)
    else if (dir.x > 0) { 
        float t = (TABLE_RIGHT - origin.x) / dir.x;
        if (t > 0) {
            float yHit = origin.y + dir.y * t;
            if (yHit >= TABLE_TOP && yHit <= TABLE_BOTTOM) {
                if (t < result.distance) {
                    result.hit = true;
                    result.distance = t;
                    result.point = Vector2D(TABLE_RIGHT, yHit);
                    result.normal = Vector2D(-1, 0); // Normal points left
                }
            }
        }
    }

    // Check Top Wall (y = TABLE_TOP)
    if (dir.y < 0) { 
        float t = (TABLE_TOP - origin.y) / dir.y;
        if (t > 0) {
            float xHit = origin.x + dir.x * t;
            if (xHit >= TABLE_LEFT && xHit <= TABLE_RIGHT) {
                if (t < result.distance) {
                    result.hit = true;
                    result.distance = t;
                    result.point = Vector2D(xHit, TABLE_TOP);
                    result.normal = Vector2D(0, 1); // Normal points down
                }
            }
        }
    }
    // Check Bottom Wall (y = TABLE_BOTTOM)
    else if (dir.y > 0) { 
        float t = (TABLE_BOTTOM - origin.y) / dir.y;
        if (t > 0) {
            float xHit = origin.x + dir.x * t;
            if (xHit >= TABLE_LEFT && xHit <= TABLE_RIGHT) {
                if (t < result.distance) {
                    result.hit = true;
                    result.distance = t;
                    result.point = Vector2D(xHit, TABLE_BOTTOM);
                    result.normal = Vector2D(0, -1); // Normal points up
                }
            }
        }
    }

    return result;
}

#endif // MATH_UTILS_H
