package com.agrigo.models;

/**
 * Model for a suggested market / mandi
 */
public class Market {
    private String name;
    private String district;
    private double distanceKm;
    private double lat;
    private double lng;

    // Crop-specific pricing (price per quintal)
    private double pricePerQuintal;
    private String priceTrend; // "up", "down", "stable"

    public Market() {}

    public Market(String name, String district, double distanceKm, double lat, double lng, double pricePerQuintal, String priceTrend) {
        this.name = name;
        this.district = district;
        this.distanceKm = distanceKm;
        this.lat = lat;
        this.lng = lng;
        this.pricePerQuintal = pricePerQuintal;
        this.priceTrend = priceTrend;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public double getDistanceKm() { return distanceKm; }
    public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public double getPricePerQuintal() { return pricePerQuintal; }
    public void setPricePerQuintal(double pricePerQuintal) { this.pricePerQuintal = pricePerQuintal; }

    public String getPriceTrend() { return priceTrend; }
    public void setPriceTrend(String priceTrend) { this.priceTrend = priceTrend; }
}
