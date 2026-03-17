package com.luxpro.max;

import com.google.firebase.database.PropertyName;

public class AnnouncementModel {
    private String id;
    private String title;
    private String content;
    private String linkUrl;
    private boolean isPinned;
    private long timestamp;

    public AnnouncementModel() {
        // Required for Firebase
    }

    public AnnouncementModel(String id, String title, String content, String linkUrl, boolean isPinned, long timestamp) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.linkUrl = linkUrl;
        this.isPinned = isPinned;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }

    @PropertyName("isPinned")
    public boolean isPinned() { return isPinned; }
    
    @PropertyName("isPinned")
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
