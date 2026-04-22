package com.agrigo.models;

import java.util.List;

public class MachineryBooking {
    private String id;
    private String farmerId;
    private String farmerName;
    private String machineryType;
    private String duration;
    private String status;
    private double farmerLat;
    private double farmerLng;
    private List<String> eligibleMachineIds;
    private String providerId;
    private String address;

    // Transient (not stored in Firestore, computed locally)
    private double distanceKm;

    public MachineryBooking() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFarmerId() { return farmerId; }
    public void setFarmerId(String farmerId) { this.farmerId = farmerId; }
    public String getFarmerName() { return farmerName; }
    public void setFarmerName(String farmerName) { this.farmerName = farmerName; }
    public String getMachineryType() { return machineryType; }
    public void setMachineryType(String machineryType) { this.machineryType = machineryType; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getFarmerLat() { return farmerLat; }
    public void setFarmerLat(double farmerLat) { this.farmerLat = farmerLat; }
    public double getFarmerLng() { return farmerLng; }
    public void setFarmerLng(double farmerLng) { this.farmerLng = farmerLng; }
    public List<String> getEligibleMachineIds() { return eligibleMachineIds; }
    public void setEligibleMachineIds(List<String> eligibleMachineIds) { this.eligibleMachineIds = eligibleMachineIds; }
    public String getProviderId() { return providerId; }
    public void setProviderId(String providerId) { this.providerId = providerId; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
}
