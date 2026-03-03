package com.agrigo.models;

/**
 * Model class for Driver Location entity
 */
public class DriverLocation {
    private String driverId;
    private double latitude;
    private double longitude;
    private long timestamp;
    private float accuracy;

    public DriverLocation() {
    }

    public DriverLocation(String driverId, double latitude, double longitude) {
        this.driverId = driverId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public float getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }
}
