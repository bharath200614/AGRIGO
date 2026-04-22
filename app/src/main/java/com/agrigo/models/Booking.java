package com.agrigo.models;

/**
 * Model class for Booking entity
 */
public class Booking {
    private String id;
    private String farmerId;
    private String driverId;
    private String cropType;
    private double weight;
    private String vehicleType;
    private String fromLocation;
    private String toLocation;
    private String status; // "requested", "accepted", "navigating_to_pickup", "at_pickup", "on_trip", "completed", "cancelled"
    private long timestamp;
    private long updatedAt;
    private double cost;

    // Navigation fields
    private String otp;
    private double sourceLat;
    private double sourceLng;
    private double destLat;
    private double destLng;
    private String sourceAddress;
    private String destAddress;
    private String farmerName;
    private String farmerPhone;

    public Booking() {
    }

    public Booking(String farmerId, String cropType, double weight) {
        this.farmerId = farmerId;
        this.cropType = cropType;
        this.weight = weight;
        this.status = "requested";
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFarmerId() {
        return farmerId;
    }

    public void setFarmerId(String farmerId) {
        this.farmerId = farmerId;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getCropType() {
        return cropType;
    }

    public void setCropType(String cropType) {
        this.cropType = cropType;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getFromLocation() {
        return fromLocation;
    }

    public void setFromLocation(String fromLocation) {
        this.fromLocation = fromLocation;
    }

    public String getToLocation() {
        return toLocation;
    }

    public void setToLocation(String toLocation) {
        this.toLocation = toLocation;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public double getCost() {
        return cost;
    }

    public void setCost(double cost) {
        this.cost = cost;
    }

    // Navigation field getters and setters
    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public double getSourceLat() {
        return sourceLat;
    }

    public void setSourceLat(double sourceLat) {
        this.sourceLat = sourceLat;
    }

    public double getSourceLng() {
        return sourceLng;
    }

    public void setSourceLng(double sourceLng) {
        this.sourceLng = sourceLng;
    }

    public double getDestLat() {
        return destLat;
    }

    public void setDestLat(double destLat) {
        this.destLat = destLat;
    }

    public double getDestLng() {
        return destLng;
    }

    public void setDestLng(double destLng) {
        this.destLng = destLng;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public String getDestAddress() {
        return destAddress;
    }

    public void setDestAddress(String destAddress) {
        this.destAddress = destAddress;
    }

    public String getFarmerName() {
        return farmerName;
    }

    public void setFarmerName(String farmerName) {
        this.farmerName = farmerName;
    }

    public String getFarmerPhone() {
        return farmerPhone;
    }

    public void setFarmerPhone(String farmerPhone) {
        this.farmerPhone = farmerPhone;
    }

    // Status helpers
    public boolean isOngoing() {
        return "ongoing".equalsIgnoreCase(status) || "on_trip".equalsIgnoreCase(status);
    }

    public boolean isCompleted() {
        return "completed".equalsIgnoreCase(status);
    }

    public boolean isRequested() {
        return "requested".equalsIgnoreCase(status);
    }

    public boolean isNavigating() {
        return "navigating_to_pickup".equalsIgnoreCase(status) || "on_trip".equalsIgnoreCase(status);
    }
}
