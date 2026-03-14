#ifndef CRYPTO_LAYER_HPP
#define CRYPTO_LAYER_HPP

#include <string>
#include <cstdint>

namespace LuxSecurity {

    class CryptoLayer {
    public:
        static std::string encryptAES(const std::string& data, const std::string& key);
        static std::string decryptAES(const std::string& encryptedData, const std::string& key);
        
        static std::string getHardwareSignature();
        static std::string obfuscateString(const std::string& input, uint8_t key);
    };

}

#endif // CRYPTO_LAYER_HPP
