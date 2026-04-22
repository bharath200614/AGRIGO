package com.agrigo.models;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class TransportRequest {

    @DocumentId
    private String id;
    
    private String farmerId;
    private String crop;
    private double weight;
    
    private LocationData pickupLocation;
    private LocationData dropLocation;
    
    private String predictedVehicle;
    private String driverId; // null initially
    private String status; // "pending", "accepted", "completed"
    
    @ServerTimestamp
    private Date createdAt;

    public TransportRequest() {
        // Required empty constructor for Firestore serialization
    }

    public TransportRequest(String farmerId, String crop, double weight, LocationData pickupLocation, LocationData dropLocation, String predictedVehicle) {
        this.farmerId = farmerId;
        this.crop = crop;
        this.weight = weight;
        this.pickupLocation = pickupLocation;
        this.dropLocation = dropLocation;
        this.predictedVehicle = predictedVehicle;
        this.driverId = null;
        this.status = "pending";
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

    public String getCrop() {
        return crop;
    }

    public void setCrop(String crop) {
        this.crop = crop;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public LocationData getPickupLocation() {
        return pickupLocation;
    }

    public void setPickupLocation(LocationData pickupLocation) {
        this.pickupLocation = pickupLocation;
    }

    public LocationData getDropLocation() {
        return dropLocation;
    }

    public void setDropLocation(LocationData dropLocation) {
        this.dropLocation = dropLocation;
    }

    public String getPredictedVehicle() {
        return predictedVehicle;
    }

    public void setPredictedVehicle(String predictedVehicle) {
        this.predictedVehicle = predictedVehicle;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public static class LocationData {
        private double lat;
        private double lng;

        public LocationData() {}

        public LocationData(double lat, double lng) {
            this.lat = lat;
            this.lng = lng;
        }

        public double getLat() {
            return lat;
        }

        public void setLat(double lat) {
            this.lat = lat;
        }

        public double getLng() {
            return lng;
        }

        public void setLng(double lng) {
            this.lng = lng;
        }
    }
}
