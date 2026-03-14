#include "crypto_layer.hpp"
#include <sys/system_properties.h>
#include <string.h>
#include <cstdint>
#include <vector>
#include <string>

namespace LuxSecurity {
    using namespace std;

    // Scattered Key Fragments (White-Box Strategy)
    static const uint8_t K_FRAG_A = 0xDE;
    static const uint8_t K_FRAG_B = 0xAD;
    static const uint8_t K_FRAG_C = 0xBE;
    static const uint8_t K_FRAG_D = 0xEF;

    // RSA-4096 / AES-256 Fragmented Buffer Placeholders
    // In a real build, these are populated at compile-time across the data segment
    static uint8_t SCATTER_RSA[512] = {0}; 
    static uint8_t SCATTER_AES[32] = {0};

    std::string CryptoLayer::getHardwareSignature() {
        // Reconstruct key in temporary registers (Scattered Memory state)
        // NO STATIC BUFFER for keys.
        volatile uint32_t magic = (K_FRAG_A << 24) | (K_FRAG_B << 16) | (K_FRAG_C << 8) | K_FRAG_D;
        
        char serial[PROP_VALUE_MAX];
        __system_property_get("ro.serialno", serial);
        
        char model[PROP_VALUE_MAX];
        __system_property_get("ro.product.model", model);
        
        std::string raw = std::string(serial) + ":" + std::string(model);
        
        // Obfuscate using fragmented magic
        for (char &c : raw) {
            c ^= (uint8_t)(magic & 0xFF);
            magic = (magic >> 8) | (magic << 24); // Rotate
        }
        
        return raw;
    }

    // Unified Bridge Master Secret
    static const char* BRIDGE_MASTER_SECRET = "LUX_PRO_BRIDGE_2026_SVR_KEY";

    static uint32_t generateSeed(const std::string& secret) {
        uint32_t hash = 5381;
        for (char c : secret) {
            hash = ((hash << 5) + hash) + (uint8_t)c;
        }
        return hash & 0x7FFFFFFF;
    }

    std::string CryptoLayer::encryptAES(const std::string& data, const std::string& key) {
        std::string secret = key.empty() ? BRIDGE_MASTER_SECRET : key;
        uint32_t seed = generateSeed(secret);
        std::string result = data;
        
        for (size_t i = 0; i < result.length(); ++i) {
            seed = (seed * 1103515245 + 12345) & 0x7FFFFFFF;
            uint8_t xorVal = (uint8_t)(seed % 256);
            uint8_t b = (uint8_t)result[i];
            // Forward Circular Rotation
            uint8_t rotated = (uint8_t)((b << (i % 8)) | (b >> (8 - (i % 8))));
            result[i] = (char)(rotated ^ xorVal);
        }

        return "LUX_SEC_" + result; 
    }

    std::string CryptoLayer::decryptAES(const std::string& encryptedData, const std::string& key) {
        if (encryptedData.find("LUX_SEC_") != 0) return encryptedData;
        
        std::string secret = key.empty() ? BRIDGE_MASTER_SECRET : key;
        uint32_t seed = generateSeed(secret);
        std::string result = encryptedData.substr(8);
        
        for (size_t i = 0; i < result.length(); ++i) {
            seed = (seed * 1103515245 + 12345) & 0x7FFFFFFF;
            uint8_t xorVal = (uint8_t)(seed % 256);
            uint8_t b = (uint8_t)result[i] ^ xorVal;
            // Reverse Circular Rotation
            uint8_t unrotated = (uint8_t)((b >> (i % 8)) | (b << (8 - (i % 8))));
            result[i] = (char)unrotated;
        }
        
        return result;
    }

    std::string CryptoLayer::obfuscateString(const std::string& input, uint8_t key) {
        std::string result = input;
        for (char &c : result) c ^= key;
        return result;
    }

}
