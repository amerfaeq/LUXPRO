#include "Menu.h"

namespace Menu {
    bool bIsMenuOpen = true;

    void Draw() {
        if (!bIsMenuOpen) return;

        // تكوين شكل وتصميم نافذة المنيو (LUX OS)
        ImGui::Begin("LUX OS - VIP", &bIsMenuOpen, ImGuiWindowFlags_NoSavedSettings);

        ImGui::Text("🔥 Welcome to the Ultimate Lib OS Injector!");
        ImGui::Separator();

        // ── مميزات وهمية للتجربة (المرحلة الأولى) ──
        static bool bESP = false;
        static bool bAutoPlay = false;
        static bool bInfiniteLines = false;

        ImGui::Checkbox("ESP (Lines & Balls)", &bESP);
        ImGui::Checkbox("Auto-Play (Cheto Level)", &bAutoPlay);
        ImGui::Checkbox("Infinite Lines", &bInfiniteLines);

        ImGui::Separator();

        if (ImGui::Button("Close Menu")) {
            bIsMenuOpen = false;
        }

        ImGui::End();
    }
}
