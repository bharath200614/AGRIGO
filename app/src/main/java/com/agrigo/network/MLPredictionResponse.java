package com.agrigo.network;

import com.google.gson.annotations.SerializedName;

/**
 * Response body from ML prediction endpoint
 */
public class MLPredictionResponse {

    @SerializedName("vehicle")
    private String vehicleType;

    public MLPredictionResponse() {
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }
}
