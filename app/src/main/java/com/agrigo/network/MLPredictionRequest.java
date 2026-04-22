package com.agrigo.network;

import com.google.gson.annotations.SerializedName;

/**
 * Request body for ML prediction endpoint
 */
public class MLPredictionRequest {

    @SerializedName("crop")
    private String cropType;

    @SerializedName("weight")
    private double weight;

    public MLPredictionRequest(String cropType, double weight) {
        this.cropType = cropType;
        this.weight = weight;
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
}
