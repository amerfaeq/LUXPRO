package com.luxpro.max.security;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class AesCipher {
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    // Master Key for the Auth Tunnel - In production, this would be derived from Native code
    private static final String BRIDGE_MASTER_SECRET = "LUX_PRO_BRIDGE_2026_SVR_KEY";

    public static String encrypt(String data) {
        try {
            byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
            long seed = generateSeed(BRIDGE_MASTER_SECRET);
            
            for (int i = 0; i < bytes.length; i++) {
                seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
                int xor = (int)(seed % 256);
                int b = (bytes[i] & 0xFF);
                // XOR + Circular Rotation
                int rotated = ((b << (i % 8)) | (b >>> (8 - (i % 8)))) & 0xFF;
                bytes[i] = (byte)(rotated ^ xor);
            }
            return "LUX_SEC_" + Base64.encodeToString(bytes, Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }

    public static String decrypt(String encryptedData) {
        try {
            String data = encryptedData;
            if (data.startsWith("LUX_SEC_")) data = data.substring(8);
            byte[] bytes = Base64.decode(data, Base64.NO_WRAP);
            long seed = generateSeed(BRIDGE_MASTER_SECRET);
            
            for (int i = 0; i < bytes.length; i++) {
                seed = (seed * 1103515245L + 12345L) & 0x7fffffffL;
                int xor = (int)(seed % 256);
                int val = (bytes[i] & 0xFF) ^ xor;
                // Reverse Circular Rotation
                int unrotated = ((val >>> (i % 8)) | (val << (8 - (i % 8)))) & 0xFF;
                bytes[i] = (byte)unrotated;
            }
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }

    private static long generateSeed(String secret) {
        long hash = 5381;
        for (char c : secret.toCharArray()) {
            hash = ((hash << 5) + hash) + c;
        }
        return hash & 0x7fffffffL;
    }

    private static byte[] generateKey(String secret) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
    }
}
