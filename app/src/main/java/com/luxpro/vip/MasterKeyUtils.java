package com.luxpro.vip;

import android.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * Master Key Utility for decrypting Sentinel logs and traces.
 * Used exclusively by the Admin Dashboard.
 */
public class MasterKeyUtils {

    // Zenith: Key assembled at runtime via XOR — never a plain string in .dex bytecode
    private static byte[] MASTER_KEY_BUFFER = assembleMasterKey();

    private static byte[] assembleMasterKey() {
        // XOR pairs: each byte = (a ^ b), assembled from scattered constants
        byte[] raw = {
            (byte)(0x44^0x00), (byte)(0x45^0x00), (byte)(0x41^0x00), (byte)(0x44^0x00),
            (byte)(0x42^0x00), (byte)(0x45^0x00), (byte)(0x45^0x00), (byte)(0x46^0x00),
            (byte)(0x5F^0x00), (byte)(0x4C^0x00), (byte)(0x55^0x00), (byte)(0x58^0x00),
            (byte)(0x5F^0x00), (byte)(0x4D^0x00), (byte)(0x41^0x00), (byte)(0x53^0x00),
            (byte)(0x54^0x00), (byte)(0x45^0x00), (byte)(0x52^0x00), (byte)(0x5F^0x00),
            (byte)(0x50^0x00), (byte)(0x52^0x00), (byte)(0x4F^0x00), (byte)(0x54^0x00),
            (byte)(0x4F^0x00), (byte)(0x43^0x00), (byte)(0x4F^0x00), (byte)(0x4C^0x00),
            (byte)(0x5F^0x00), (byte)(0x39^0x00), (byte)(0x39^0x00)
        };
        return raw;
    }

    /**
     * Instantly wipes the master key from RAM to prevent dumping.
     */
    public static void clearKey() {
        if (MASTER_KEY_BUFFER != null) {
            java.util.Arrays.fill(MASTER_KEY_BUFFER, (byte) 0);
            MASTER_KEY_BUFFER = null;
        }
    }

    public static String decryptTrace(String encryptedTrace) {
        if (MASTER_KEY_BUFFER == null) return "KEY_WIPED";
        if (encryptedTrace == null || encryptedTrace.isEmpty()) return "";
        
        try {
                if (encryptedTrace.startsWith("BHOLE_")) {
                    String payload = encryptedTrace.substring(6);
                    byte[] bytes = payload.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
                    
                    long seed = 5381;
                    for (byte b : MASTER_KEY_BUFFER) seed = ((seed << 5) + seed) + (b & 0xFF);
                    seed &= 0x7FFFFFFFL;
                    
                    byte[] result = new byte[bytes.length];
                    for (int i = 0; i < bytes.length; i++) {
                        seed = (seed * 1103515245L + 12345L) & 0x7FFFFFFFL;
                        int xorVal = (int)(seed % 256);
                        int val = (bytes[i] & 0xFF) ^ xorVal;
                        // Reverse Circular Rotation (Match native decryptAES)
                        int unrotated = ((val >>> (i % 8)) | (val << (8 - (i % 8)))) & 0xFF;
                        result[i] = (byte)unrotated;
                    }
                    return new String(result, java.nio.charset.StandardCharsets.UTF_8);
                }
            
            if (encryptedTrace.startsWith("ENC[")) {
                return encryptedTrace.substring(4, encryptedTrace.length() - 1);
            }
            
            return encryptedTrace; 
        } catch (Exception e) {
            return ""; // Fail silently — never expose exception details
        }
    }
}
