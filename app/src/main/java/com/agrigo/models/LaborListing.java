package com.agrigo.models;

public class LaborListing {
    private String listingId;
    private String laborId;
    private String laborName;
    private String cropType;
    private double price;
    private String location;
    private boolean isOnline;
    private long timestamp;

    public LaborListing() {
        // Required empty constructor for Firestore
    }

    public LaborListing(String listingId, String laborId, String laborName, String cropType, double price, String location, boolean isOnline, long timestamp) {
        this.listingId = listingId;
        this.laborId = laborId;
        this.laborName = laborName;
        this.cropType = cropType;
        this.price = price;
        this.location = location;
        this.isOnline = isOnline;
        this.timestamp = timestamp;
    }

    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }
    
    public String getLaborId() { return laborId; }
    public void setLaborId(String laborId) { this.laborId = laborId; }
    
    public String getLaborName() { return laborName; }
    public void setLaborName(String laborName) { this.laborName = laborName; }
    
    public String getCropType() { return cropType; }
    public void setCropType(String cropType) { this.cropType = cropType; }
    
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean isOnline) { this.isOnline = isOnline; }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
