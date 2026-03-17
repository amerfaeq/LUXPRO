package com.luxpro.max;

import java.util.HashMap;
import java.util.Map;

public class OffsetsManager {
    // Absolute Zero: Private map — only accessible via synchronized methods
    private static final Map<String, String> featurePatches = new HashMap<>();

    public static synchronized void addPatch(String address, String hexCode) {
        if (address != null && hexCode != null) featurePatches.put(address, hexCode);
    }

    public static synchronized Map<String, String> getPatches() {
        return new HashMap<>(featurePatches); // Defensive copy — never expose internal ref
    }

    public static synchronized void clearPatches() {
        featurePatches.clear();
    }

    public static long hexToLong(String hex) {
        if (hex == null || hex.isEmpty()) return 0L;
        try {
            return Long.decode(hex);
        } catch (NumberFormatException e) {
            return 0L; // Fail safe — never crash on bad offset data
        }
    }
}