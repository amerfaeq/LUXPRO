#pragma once
#include "imgui.h"

// الـ Namespace الخاص بالمنيو لترتيب الأكواد
namespace Menu {
    // تعريف حالة المنيو (مفتوح / مغلق)
    extern bool bIsMenuOpen;

    // دالة رسم المنيو (تُستدعى في كل إطار Frame)
    void Draw();
}
