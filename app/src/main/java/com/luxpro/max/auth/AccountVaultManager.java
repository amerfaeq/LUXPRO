package com.luxpro.max.auth;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AccountVaultManager {
    private static final String PREF_NAME = "LUX_VAULT";
    private static final String KEY_ACCOUNTS = "accounts_history";
    private final SharedPreferences prefs;
    private final Gson gson;

    public static class AccountInfo {
        public String name;
        public String provider;
        // NOTE: lastToken intentionally removed — tokens live in RAM-only Heavenly Vault
        public boolean isActive;

        public AccountInfo(String name, String provider) {
            this.name = name;
            this.provider = provider;
            this.isActive = true;
        }
    }

    public AccountVaultManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public List<AccountInfo> getAccountHistory() {
        String json = prefs.getString(KEY_ACCOUNTS, null);
        if (json == null) return new ArrayList<>();
        
        // Shadow Armor: Decrypt Vault using NDK
        String decrypted = null; // Declare decrypted outside try for finally block
        try {
            byte[] encrypted = android.util.Base64.decode(json, android.util.Base64.DEFAULT);
            decrypted = com.luxpro.max.NativeEngine.getInstance().decryptVault(encrypted);
            List<AccountInfo> accounts = gson.fromJson(decrypted, new TypeToken<List<AccountInfo>>(){}.getType());
            return accounts;
        } catch (Exception e) {
            return new ArrayList<>();
        } finally {
            // Nebula: Vaporize sensitive JSON from memory after use
            if (decrypted != null && !decrypted.isEmpty()) {
                com.luxpro.max.NativeEngine.getInstance().shredMemory(decrypted);
            }
        }
    }

    public void saveAccount(Context context, String name, String provider) {
        List<AccountInfo> history = getAccountHistory();
        
        // Mark others as inactive
        for (AccountInfo acc : history) acc.isActive = false;

        // Check if exists
        AccountInfo existing = null;
        for (AccountInfo acc : history) {
            if (acc.name.equals(name) && acc.provider.equals(provider)) {
                existing = acc;
                break;
            }
        }

        if (existing != null) {
            history.remove(existing);
            existing.isActive = true;
            history.add(0, existing);
        } else {
            history.add(0, new AccountInfo(name, provider));
        }

        // Keep last 3
        if (history.size() > 3) history = history.subList(0, 3);
        
        // Shadow Armor: Encrypt Vault using NDK
        String rawJson = gson.toJson(history);
        byte[] encrypted = com.luxpro.max.NativeEngine.getInstance().encryptVault(rawJson);
        String base64Encrypted = android.util.Base64.encodeToString(encrypted, android.util.Base64.DEFAULT);
        prefs.edit().putString(KEY_ACCOUNTS, base64Encrypted).apply();

        // Heavenly: Transcend Session to RAM-Only Space BEFORE shredding
        com.luxpro.max.NativeEngine.getInstance().startHeavenlyAuth(name + "|" + provider);
        
        // Now shred the raw JSON from Java heap
        com.luxpro.max.NativeEngine.getInstance().shredMemory(rawJson);
    }

    /**
     * Deep cleansing of ONLY cookie/cache data to prevent detection.
     * Does NOT touch the Vault shared_prefs to avoid self-destruction.
     */
    public void performDeepPurge(Context context) {
        try {
            File root = context.getFilesDir().getParentFile();
            if (root != null && root.exists()) {
                // Only wipe user-facing browser/cache data, NOT the vault prefs
                String[] folders = {"files", "cache", "app_webview", "app_textures"};
                for (String folder : folders) {
                    deleteRecursive(new File(root, folder));
                }
                // Selectively clean shared_prefs but preserve LUX_VAULT
                File sharedPrefs = new File(root, "shared_prefs");
                if (sharedPrefs.isDirectory()) {
                    File[] files = sharedPrefs.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            if (!f.getName().startsWith("LUX_VAULT")) {
                                f.delete();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Fail silently for stealth
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory == null || !fileOrDirectory.exists()) return;
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) { // null-guard: listFiles() returns null on unreadable dirs
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }
}
