package com.agrigo.models;

/**
 * Model class for Vehicle Suggestion response from API
 */
public class VehicleSuggestion {
    private String vehicleId;
    private String vehicleType; // "auto", "truck", "lorry", "tempo"
    private String vehicleName;
    private int capacity; // kg
    private double estimatedCost;
    private int availableCount;
    private String description;
    private int estimatedTime; // minutes

    public VehicleSuggestion() {
    }

    public VehicleSuggestion(String vehicleType, String vehicleName, int capacity) {
        this.vehicleType = vehicleType;
        this.vehicleName = vehicleName;
        this.capacity = capacity;
    }

    // Getters and Setters
    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getVehicleName() {
        return vehicleName;
    }

    public void setVehicleName(String vehicleName) {
        this.vehicleName = vehicleName;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public double getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(double estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public int getAvailableCount() {
        return availableCount;
    }

    public void setAvailableCount(int availableCount) {
        this.availableCount = availableCount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getEstimatedTime() {
        return estimatedTime;
    }

    public void setEstimatedTime(int estimatedTime) {
        this.estimatedTime = estimatedTime;
    }
}
