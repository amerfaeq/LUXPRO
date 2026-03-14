#ifndef TOOLS_H
#define TOOLS_H

#include <jni.h>
#include <string.h>
#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <unistd.h>
#include <sys/mman.h>
#include <android/log.h>

#define LOG_TAG "LUX_PRO_MASTER"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)

// دالة جلب عنوان المكتبة لعملية خارجية (Base Address)
inline uintptr_t GetBaseAddress(int pid, const char* name) {
    uintptr_t base = 0;
    char line[512];
    char mapsPath[128];
    if (pid <= 0) snprintf(mapsPath, sizeof(mapsPath), "/proc/self/maps");
    else snprintf(mapsPath, sizeof(mapsPath), "/proc/%d/maps", pid);

    FILE* f = fopen(mapsPath, "r");
    if (f) {
        while (fgets(line, sizeof(line), f)) {
            if (strstr(line, name)) {
                base = (uintptr_t)strtoull(line, NULL, 16);
                break;
            }
        }
        fclose(f);
    }
    return base;
}

// زيادة تحميل للدعم القديم (العملية المحلية)
inline uintptr_t GetBaseAddress(const char* name) {
    return GetBaseAddress(0, name);
}

// دالة تعديل البايتات (Memory Patch)
inline void PatchMemory(uintptr_t address, const char* hex) {
    if (address == 0 || hex == NULL) return;
    
    // حساب طول البايتات (كل بايت يمثله حرفين وفراغ، أو حرفين ملتصقين)
    // الطريقة المبسطة هنا تفترض وجود فراغات أو لا، لكن الكود الأصلي استخدم i*3
    // سأقوم بتحسينها لتكون أكثر مرونة
    
    size_t hexLen = strlen(hex);
    size_t byteCount = 0;
    for(size_t i = 0; i < hexLen; i++) {
        if (hex[i] != ' ') byteCount++;
    }
    byteCount /= 2;

    uint8_t* bytes = (uint8_t*)malloc(byteCount);
    const char* pos = hex;
    for (size_t i = 0; i < byteCount; i++) {
        while (*pos == ' ') pos++;
        sscanf(pos, "%2hhx", &bytes[i]);
        pos += 2;
    }

    size_t pageSize = sysconf(_SC_PAGESIZE);
    uintptr_t start = address & ~(pageSize - 1);
    
    // تأكد من تغطية كل المساحة المطلوبة إذا كانت تتجاوز الصفحة الواحدة
    size_t protectLen = ((address + byteCount - 1) & ~(pageSize - 1)) - start + pageSize;

    mprotect((void *)start, protectLen, PROT_READ | PROT_WRITE | PROT_EXEC);
    memcpy((void *)address, bytes, byteCount);
    mprotect((void *)start, protectLen, PROT_READ | PROT_EXEC);
    
    free(bytes);
}

#endif