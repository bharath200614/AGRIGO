package com.agrigo.models;

public class DriverRequest {
    private String id;
    private String farmerName;
    private String fromLocation;
    private String toLocation;
    private long timestamp;
    private String vehicleType;
    private double price;
    private String cropType;
    private String farmerId;

    // Map-based booking fields
    private double sourceLat;
    private double sourceLng;
    private double destLat;
    private double destLng;
    private double distance;
    private String sourceAddress;
    private String destAddress;

    public DriverRequest() {
        // Empty constructor needed for Firestore
    }

    public DriverRequest(String id, String farmerName, String fromLocation, String toLocation, long timestamp, String vehicleType, double price, String cropType, String farmerId) {
        this.id = id;
        this.farmerName = farmerName;
        this.fromLocation = fromLocation;
        this.toLocation = toLocation;
        this.timestamp = timestamp;
        this.vehicleType = vehicleType;
        this.price = price;
        this.cropType = cropType;
        this.farmerId = farmerId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getFarmerName() { return farmerName; }
    public String getFromLocation() { return fromLocation; }
    public void setFromLocation(String fromLocation) { this.fromLocation = fromLocation; }
    public String getToLocation() { return toLocation; }
    public void setToLocation(String toLocation) { this.toLocation = toLocation; }
    public long getTimestamp() { return timestamp; }
    public String getVehicleType() { return vehicleType; }
    public double getPrice() { return price; }
    public String getCropType() { return cropType; }
    public void setCropType(String cropType) { this.cropType = cropType; }
    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }

    // Map-based booking getters and setters
    public double getSourceLat() { return sourceLat; }
    public void setSourceLat(double sourceLat) { this.sourceLat = sourceLat; }
    public double getSourceLng() { return sourceLng; }
    public void setSourceLng(double sourceLng) { this.sourceLng = sourceLng; }
    public double getDestLat() { return destLat; }
    public void setDestLat(double destLat) { this.destLat = destLat; }
    public double getDestLng() { return destLng; }
    public void setDestLng(double destLng) { this.destLng = destLng; }
    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }
    public String getSourceAddress() { return sourceAddress; }
    public void setSourceAddress(String sourceAddress) { this.sourceAddress = sourceAddress; }
    public String getDestAddress() { return destAddress; }
    public void setDestAddress(String destAddress) { this.destAddress = destAddress; }
}
