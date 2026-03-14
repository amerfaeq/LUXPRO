#ifndef LUX_NATIVE_STRINGS_HPP
#define LUX_NATIVE_STRINGS_HPP

namespace LuxCore {

    enum class NativeStringId {
        MSG_EMERGENCY_KILL,
        MSG_TABLE_CALIBRATED,
        MSG_PATH_COLORS_UPDATED,
        MSG_VFX_PRESET_GHOST,
        MSG_VFX_PRESET_DOMINATOR,
        MSG_VFX_PRESET_STEALTH,
        MSG_SPOOF_MOUNT,
        MSG_SPOOF_PROPS,
        MSG_SEC_RUNTIME_TAMPER,
        MSG_SEC_ENV_TAMPER,
        MSG_SEC_TRACE_TAMPER,
        MSG_VFX_INIT_SUCCESS,
        MSG_VFX_INIT_FAIL
    };

    class NativeStrings {
    public:
        static bool isArabic;

    public:
        // Called via JNI when the user toggles language in the Android UI
        static void setLanguage(bool arabic) {
            isArabic = arabic;
        }

        static const char* get(NativeStringId id) {
            if (isArabic) {
                switch (id) {
                    case NativeStringId::MSG_EMERGENCY_KILL:      return "تفعيل بروتوكول الإبادة: جاري مسح الذاكرة!";
                    case NativeStringId::MSG_TABLE_CALIBRATED:    return "تمت معايرة الطاولة بنجاح";
                    case NativeStringId::MSG_PATH_COLORS_UPDATED: return "تم تحديث ألوان المسارات";
                    case NativeStringId::MSG_VFX_PRESET_GHOST:    return "تفعيل نمط التدخل: شبح 👻";
                    case NativeStringId::MSG_VFX_PRESET_DOMINATOR:return "تفعيل نمط التدخل: جلاد 💀";
                    case NativeStringId::MSG_VFX_PRESET_STEALTH:  return "تفعيل نمط التدخل: تخفي 🕶️";
                    case NativeStringId::MSG_SPOOF_MOUNT:         return "تم تزييف تصاريح القرص (Mounts)";
                    case NativeStringId::MSG_SPOOF_PROPS:         return "تم تزييف خصائص النظام (Properties)";
                    case NativeStringId::MSG_SEC_RUNTIME_TAMPER:  return "تحذير أمني: تلاعب بالذاكرة الحية!";
                    case NativeStringId::MSG_SEC_ENV_TAMPER:      return "تحذير أمني: تلاعب ببيئة التشغيل!";
                    case NativeStringId::MSG_SEC_TRACE_TAMPER:    return "تحذير أمني: تتبع عمليات محظور!";
                    case NativeStringId::MSG_VFX_INIT_SUCCESS:    return "تم تشغيل محرك VFX بنجاح";
                    case NativeStringId::MSG_VFX_INIT_FAIL:       return "فشل تشغيل محرك VFX!";
                    default: return "رسالة نظام";
                }
            } else {
                switch (id) {
                    case NativeStringId::MSG_EMERGENCY_KILL:      return "EMERGENCY KILL-SWITCH: TOTAL MEMORY WIPE!";
                    case NativeStringId::MSG_TABLE_CALIBRATED:    return "Table Coordinates Synchronized";
                    case NativeStringId::MSG_PATH_COLORS_UPDATED: return "Trajectory Colors Updated";
                    case NativeStringId::MSG_VFX_PRESET_GHOST:    return "VFX Mode: 👻 GHOST";
                    case NativeStringId::MSG_VFX_PRESET_DOMINATOR:return "VFX Mode: 💀 DOMINATOR";
                    case NativeStringId::MSG_VFX_PRESET_STEALTH:  return "VFX Mode: 🕶️ STEALTH";
                    case NativeStringId::MSG_SPOOF_MOUNT:         return "Mount Points Spoofed Successfully";
                    case NativeStringId::MSG_SPOOF_PROPS:         return "System Properties Spoofed Successfully";
                    case NativeStringId::MSG_SEC_RUNTIME_TAMPER:  return "SECURITY ALERT: Runtime Tampering Detected!";
                    case NativeStringId::MSG_SEC_ENV_TAMPER:      return "SECURITY ALERT: Environment Tampering Detected!";
                    case NativeStringId::MSG_SEC_TRACE_TAMPER:    return "SECURITY ALERT: Tracer Execution Detected!";
                    case NativeStringId::MSG_VFX_INIT_SUCCESS:    return "VFX Renderer initialized";
                    case NativeStringId::MSG_VFX_INIT_FAIL:       return "VFX Renderer failed to initialize";
                    default: return "System Message";
                }
            }
        }
    };

    // Initialize to default Arabic assuming Arab users
    inline bool NativeStrings::isArabic = true;

} // namespace LuxCore

#endif // LUX_NATIVE_STRINGS_HPP
