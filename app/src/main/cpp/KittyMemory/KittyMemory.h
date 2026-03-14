#ifndef KITTYMEMORY_H
#define KITTYMEMORY_H

#include <string>
#include <vector>
#include <stdint.h>
#include "../Tools.h"

namespace KittyMemory {
    inline void patchFromHex(const char* libName, uintptr_t offset, const char* hex) {
        int pid = 0; // In Build 5, we use the detected game PID from JNI_OnLoad
        // For simplicity in this shim, we'll use a global PID if set, otherwise 0 for local
        uintptr_t base = GetBaseAddress(libName);
        if (base) {
            PatchMemory(base + offset, hex);
        }
    }
}

#endif
