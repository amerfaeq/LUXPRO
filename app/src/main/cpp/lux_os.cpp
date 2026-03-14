#include <jni.h>
#include <EGL/egl.h>
#include <GLES2/gl2.h>
#include <android/log.h>
#include <pthread.h>
#include <unistd.h>
#include <math.h>
#include <mutex>

#include "imgui/imgui.h"
#include "imgui/backends/imgui_impl_android.h"
#include "imgui/backends/imgui_impl_opengl3.h"
#include <shadowhook.h>

#include "Menu.h"
#include "MathUtils.h"

#include "core/sentinel.hpp"
#include "core/native_strings.hpp"

#define LOG_TAG "LUX_OS"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

static std::mutex g_DrawMutex;

// دالة حساب الانعكاس لمسار الطابة (عقل الفيزياء)
void PredictPath(Vector2D startPos, Vector2D velocity, int bounces) {
    Vector2D currentPos = startPos;
    Vector2D currentDir = Normalize(velocity);

    if (currentDir.x == 0 && currentDir.y == 0) return; // الحماية من قسمة الصفر

    ImDrawList* drawList = ImGui::GetForegroundDrawList();
    if (!drawList) return;

    for (int i = 0; i < bounces; i++) {
        // حساب نقطة الاصطدام القادمة مع حدود الطاولة الوهمية
        HitResult hit = RayIntersectsAABB(currentPos, currentDir);
        if (hit.hit) {
            // رسم الخط باستخدام ImGui
            drawList->AddLine(
                ImVec2(currentPos.x, currentPos.y), 
                ImVec2(hit.point.x, hit.point.y), 
                IM_COL32(0, 255, 0, 255), 2.0f
            );

            // تحديث الاتجاه بعد الارتداد
            currentDir = CalculateReflection(currentDir, hit.normal);
            currentPos = hit.point;
            
            // إضافة مسافة بسيطة جداً لكي لا يعلق الشعاع في نفس الجدار
            currentPos = currentPos + currentDir * 1.0f;
        } else {
            // إذا لم يصطدم بشيء، نرسم خطاً طويلاً فقط
            Vector2D endPos = currentPos + currentDir * 2000.0f;
            drawList->AddLine(
                ImVec2(currentPos.x, currentPos.y), 
                ImVec2(endPos.x, endPos.y), 
                IM_COL32(0, 255, 0, 255), 2.0f
            );
            break;
        }
    }
}

// دالة الدخول الحقيقية لـ eglSwapBuffers
EGLBoolean (*old_eglSwapBuffers)(EGLDisplay dpy, EGLSurface surface) = nullptr;

// متغيرات لتهيئة ImGui مرة واحدة فقط
bool g_Initialized = false;

// دالتنا البديلة (Hook) التي تعمل قبل كل إطار يتم رسمه على الشاشة
EGLBoolean new_eglSwapBuffers(EGLDisplay dpy, EGLSurface surface) {
    std::lock_guard<std::mutex> lock(g_DrawMutex);
    
    if (!g_Initialized) {
        // تهيئة سياق (Context) ImGui
        IMGUI_CHECKVERSION();
        ImGui::CreateContext();
        ImGuiIO& io = ImGui::GetIO();
        
        // إعدادات افتراضية
        ImGui::StyleColorsDark();

        // ربط ImGui بمحرك الأندرويد و OpenGL3
        // ملاحظة: استخدمنا nullptr بدل g_Window لأننا نحتاج الوصول للنافذة من Unity
        ImGui_ImplAndroid_Init(nullptr); 
        // ملاحظة: اصدار 300 es أفضل ومناسب أكثر للاندرويد
        ImGui_ImplOpenGL3_Init("#version 300 es");

        g_Initialized = true;
        LOGI("LUX OS: ImGui Initialized successfully in eglSwapBuffers!");
    }

    // ── بدء دورة رسم الإطار (New Frame) ──
    ImGui_ImplOpenGL3_NewFrame();
    ImGui_ImplAndroid_NewFrame();
    ImGui::NewFrame();

    // هنا تضع القائمة العائمة (Menu)
    ImGui::Begin("LUX PRO MOD MENU");
    if (ImGui::Button("تفعيل الخطوط اللانهائية")) { /* كود التفعيل */ }
    if (ImGui::Button("اللعب التلقائي")) { /* كود الاوتو */ }
    ImGui::End();

    // ── استدعاء دالة رسم القائمة العائمة (LUX OS) التي كانت موجودة ──
    Menu::Draw();

    // ── تجربة تفاعلية لـ (عقل الفيزياء) ──
    ImGuiIO& io = ImGui::GetIO();
    ImDrawList* drawList = ImGui::GetForegroundDrawList();
    if (drawList != nullptr) {
        // 1. رسم حدود طاولة البلياردو الوهمية للتجربة باللون الأحمر
        drawList->AddRect(
            ImVec2(TABLE_LEFT, TABLE_TOP), 
            ImVec2(TABLE_RIGHT, TABLE_BOTTOM), 
            IM_COL32(255, 0, 0, 255), 
            0.0f, 0, 2.0f
        );

        // 2. النقطة المحاكية التي تنطلق منها الطابة البيضاء (منتصف الشاشة)
        Vector2D startPos(io.DisplaySize.x / 2.0f, io.DisplaySize.y / 2.0f);
        // 3. اتجاه الماوس أو اللمس (إصبع اللاعب)
        Vector2D mousePos(io.MousePos.x, io.MousePos.y); 
        Vector2D velocity = mousePos - startPos;

        // ── 4. إذا كان اللاعب يلمس الشاشة، ارسم مسار الارتدادات
        if (io.MouseDown[0]) {
            PredictPath(startPos, velocity, 4); // توقع 4 ارتدادات
            
            // رسم الطابة البيضاء (محاكاة)
            drawList->AddCircleFilled(
                ImVec2(startPos.x, startPos.y), 
                15.0f, 
                IM_COL32(255, 255, 255, 255)
            );
        }

        // ── [PHASE 15]: LOBBY ORACLE NATIVE OVERLAY ──
        // (تم إيقاف كود Oracle مؤقتاً لتجنب كراش Sentinel غير المعرف بالكامل)
        // int currentTable = LuxSecurity::Sentinel::getDetectedTableFocus(); 
        // if (currentTable != -1) { ... }
    }
    // ─────────────────────────────────────────

    // ── تقديم الإطار للشاشة (Render) ──
    ImGui::Render();
    // ملاحظة: صححنا GetDrawList إلى GetDrawData لكي يقبل الكومبايلر بناء التطبيق
    ImGui_ImplOpenGL3_RenderDrawData(ImGui::GetDrawData());

    // العودة وإكمال رسم اللعبة الأصلية عبر استدعاء الدالة الأصلية
    if (old_eglSwapBuffers != nullptr) {
        return old_eglSwapBuffers(dpy, surface);
    }
    return EGL_TRUE;
}

// ── نقطة الإقلاع الأساسية للمكتبة (Library Entry Point) ──
void *HackThread(void *arg) {
    LOGI("LUX OS Thread Started! Injecting Hooks via ShadowHook...");

    // تهيئة ShadowHook
    // 1 معناها Mode Unique
    shadowhook_init(SHADOWHOOK_MODE_UNIQUE, false);

    // عمل Hook لدالة eglSwapBuffers في مكتبة libEGL.so
    void* stub = shadowhook_hook_sym_name("libEGL.so", "eglSwapBuffers", (void *)new_eglSwapBuffers, (void **)&old_eglSwapBuffers);
    
    if (stub != nullptr) {
        LOGI("LUX OS: ShadowHook placed on eglSwapBuffers successfully!");
    } else {
        LOGI("LUX OS: ShadowHook failed to hook eglSwapBuffers.");
    }

    return nullptr;
}

// دالة تهيئة ShadowHook يمكن استدعاؤها من الـ Bridge أو Java
extern "C" void InitLuxOSHook() {
    pthread_t thread;
    pthread_create(&thread, nullptr, HackThread, nullptr);
}
