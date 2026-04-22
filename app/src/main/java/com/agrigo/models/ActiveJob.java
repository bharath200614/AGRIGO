package com.agrigo.models;

public class ActiveJob implements Comparable<ActiveJob> {
    private String id;
    private String serviceType; // Transport, Machinery, Labor
    private String title;
    private String subtitle;
    private String status;
    private long timestamp;
    
    private String providerName;
    private String progressText;
    private int progressPercentage;

    public ActiveJob(String id, String serviceType, String title, String subtitle, String status, long timestamp) {
        this.id = id;
        this.serviceType = serviceType;
        this.title = title;
        this.subtitle = subtitle;
        this.status = status;
        this.timestamp = timestamp;
        this.providerName = "Assigning...";
        this.progressText = "0%";
        this.progressPercentage = 0;
    }

    public String getId() { return id; }
    public String getServiceType() { return serviceType; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getStatus() { return status; }
    public long getTimestamp() { return timestamp; }
    
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    
    public String getProgressText() { return progressText; }
    public void setProgressText(String progressText) { this.progressText = progressText; }
    
    public int getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }

    @Override
    public int compareTo(ActiveJob other) {
        // Sort descending by timestamp
        return Long.compare(other.timestamp, this.timestamp);
    }
}
