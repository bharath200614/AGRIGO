package com.agrigo.models;

/**
 * Model class for Crop entity
 */
public class Crop {
    private String id;
    private String name;
    private String imageUrl;
    private int iconResId; // For drawable resources
    private double minWeight; // kg
    private double maxWeight; // kg
    private String season;
    private String category;

    public Crop() {
    }

    public Crop(String id, String name, int iconResId) {
        this.id = id;
        this.name = name;
        this.iconResId = iconResId;
    }

    public Crop(String id, String name, int iconResId, double minWeight, double maxWeight) {
        this.id = id;
        this.name = name;
        this.iconResId = iconResId;
        this.minWeight = minWeight;
        this.maxWeight = maxWeight;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public int getIconResId() {
        return iconResId;
    }

    public void setIconResId(int iconResId) {
        this.iconResId = iconResId;
    }

    public double getMinWeight() {
        return minWeight;
    }

    public void setMinWeight(double minWeight) {
        this.minWeight = minWeight;
    }

    public double getMaxWeight() {
        return maxWeight;
    }

    public void setMaxWeight(double maxWeight) {
        this.maxWeight = maxWeight;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
