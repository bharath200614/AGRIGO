package com.agrigo.utils;

import com.agrigo.models.Market;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides market/mandi data with crop-specific pricing.
 * In production, this would come from a live API.
 */
public class MarketDataProvider {

    // Base market data (Indian mandis)
    private static final Market[] ALL_MARKETS = {
        new Market("Guntur Mandi", "Guntur, AP", 0, 16.3067, 80.4365, 0, "up"),
        new Market("Kurnool Market", "Kurnool, AP", 0, 15.8281, 78.0373, 0, "stable"),
        new Market("Raichur APMC", "Raichur, KA", 0, 16.2120, 77.3439, 0, "up"),
        new Market("Hubli Mandi", "Hubli, KA", 0, 15.3647, 75.1240, 0, "down"),
        new Market("Warangal Market", "Warangal, TS", 0, 17.9784, 79.5941, 0, "stable"),
        new Market("Nizamabad APMC", "Nizamabad, TS", 0, 18.6725, 78.0941, 0, "up"),
        new Market("Hyderabad Mandi", "Hyderabad, TS", 0, 17.3850, 78.4867, 0, "up"),
        new Market("Vijayawada Market", "Vijayawada, AP", 0, 16.5062, 80.6480, 0, "stable"),
        new Market("Tirupati APMC", "Tirupati, AP", 0, 13.6288, 79.4192, 0, "down"),
        new Market("Bangalore APMC", "Bangalore, KA", 0, 12.9716, 77.5946, 0, "up"),
        new Market("Chennai Koyambedu", "Chennai, TN", 0, 13.0827, 80.2707, 0, "stable"),
        new Market("Madurai Mandi", "Madurai, TN", 0, 9.9252, 78.1198, 0, "up"),
    };

    // Crop-specific prices per quintal (in INR)
    private static final Map<String, double[]> CROP_PRICES = new HashMap<>();

    static {
        // Each array matches the indices of ALL_MARKETS
        CROP_PRICES.put("Rice",        new double[]{2850, 2700, 2900, 2650, 2800, 2750, 2950, 2800, 2600, 2900, 3000, 2700});
        CROP_PRICES.put("Wheat",       new double[]{2400, 2350, 2500, 2300, 2450, 2380, 2550, 2400, 2250, 2500, 2600, 2350});
        CROP_PRICES.put("Maize",       new double[]{1950, 1900, 2000, 1850, 1980, 1920, 2050, 1950, 1800, 2000, 2100, 1880});
        CROP_PRICES.put("Chilli",      new double[]{18500, 17200, 16800, 15500, 17000, 16500, 19000, 18000, 15000, 17500, 18200, 16000});
        CROP_PRICES.put("Cotton",      new double[]{6800, 6500, 6900, 6200, 6700, 6400, 7000, 6600, 6100, 6800, 7100, 6300});
        CROP_PRICES.put("Sugarcane",   new double[]{3500, 3200, 3400, 3100, 3300, 3250, 3600, 3400, 3000, 3500, 3700, 3150});
        CROP_PRICES.put("Tomato",      new double[]{2200, 1800, 2000, 1600, 1900, 1700, 2400, 2100, 1500, 2300, 2500, 1650});
        CROP_PRICES.put("Onion",       new double[]{2800, 2600, 2700, 2400, 2650, 2500, 3000, 2750, 2300, 2900, 3100, 2450});
        CROP_PRICES.put("Potato",      new double[]{1800, 1650, 1750, 1500, 1700, 1600, 1900, 1800, 1400, 1850, 2000, 1550});
        CROP_PRICES.put("Banana",      new double[]{1500, 1400, 1600, 1300, 1550, 1450, 1700, 1500, 1250, 1600, 1750, 1350});
        CROP_PRICES.put("Groundnut",   new double[]{5800, 5500, 5700, 5200, 5600, 5400, 6000, 5700, 5100, 5900, 6100, 5300});
        CROP_PRICES.put("Soybean",     new double[]{4500, 4200, 4400, 4000, 4350, 4150, 4700, 4400, 3900, 4600, 4800, 4100});
    }

    /**
     * Get markets sorted by distance from farmer's location, with crop-specific pricing.
     */
    public static List<Market> getMarketsForCrop(String cropName, double farmerLat, double farmerLng) {
        List<Market> result = new ArrayList<>();

        double[] prices = CROP_PRICES.get(cropName);

        for (int i = 0; i < ALL_MARKETS.length; i++) {
            Market src = ALL_MARKETS[i];
            double dist = haversineDistance(farmerLat, farmerLng, src.getLat(), src.getLng());
            double price = (prices != null && i < prices.length) ? prices[i] : getDefaultPrice(cropName);

            Market m = new Market(
                src.getName(), src.getDistrict(),
                Math.round(dist * 10.0) / 10.0,
                src.getLat(), src.getLng(),
                price, src.getPriceTrend()
            );
            result.add(m);
        }

        // Sort by distance
        Collections.sort(result, (a, b) -> Double.compare(a.getDistanceKm(), b.getDistanceKm()));

        // Return top 6 nearest
        return result.subList(0, Math.min(6, result.size()));
    }

    /**
     * Filter markets by search query
     */
    public static List<Market> searchMarkets(String query, String cropName, double farmerLat, double farmerLng) {
        List<Market> all = getMarketsForCrop(cropName, farmerLat, farmerLng);
        if (query == null || query.trim().isEmpty()) return all;

        String q = query.toLowerCase().trim();
        List<Market> filtered = new ArrayList<>();
        for (Market m : all) {
            if (m.getName().toLowerCase().contains(q) || m.getDistrict().toLowerCase().contains(q)) {
                filtered.add(m);
            }
        }
        return filtered;
    }

    private static double getDefaultPrice(String cropName) {
        // Fallback average price for crops not in the map
        return 3000;
    }

    /**
     * Haversine formula to compute distance in km
     */
    private static double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth's radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
}
