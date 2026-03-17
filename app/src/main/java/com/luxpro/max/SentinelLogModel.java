package com.luxpro.max;

public class SentinelLogModel {
    public String time;
    public String hwid;
    public String gameVersion;
    public String scanStatus;
    public String encryptedTrace;

    public SentinelLogModel() {
        // Required for Firebase
    }

    public SentinelLogModel(String time, String hwid, String gameVersion, String scanStatus, String encryptedTrace) {
        this.time = time;
        this.hwid = hwid;
        this.gameVersion = gameVersion;
        this.scanStatus = scanStatus;
        this.encryptedTrace = encryptedTrace;
    }
}
