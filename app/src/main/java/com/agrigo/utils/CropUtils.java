package com.agrigo.utils;

import android.content.Context;
import com.agrigo.R;
import com.agrigo.models.Crop;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for crop-related operations
 */
public class CropUtils {

    /**
     * Get all available crops (51 crops for ML integration)
     * Each crop has a unique emoji icon matching user specification
     */
    public static List<Crop> getAllCrops(Context context) {
        List<Crop> crops = new ArrayList<>();
        
        // ═══════════════════════════════════════
        // Cereals (10)
        // ═══════════════════════════════════════
        crops.add(new Crop("rice", "Rice", "🌾", R.drawable.ic_crop_cereal, 50, 10000));
        crops.add(new Crop("wheat", "Wheat", "🌾", R.drawable.ic_crop_cereal, 50, 10000));
        crops.add(new Crop("maize", "Maize", "🌽", R.drawable.ic_crop_cereal, 50, 10000));
        crops.add(new Crop("barley", "Barley", "🌾", R.drawable.ic_crop_cereal, 50, 8000));
        crops.add(new Crop("jowar", "Jowar", "🌾", R.drawable.ic_crop_cereal, 50, 8000));
        crops.add(new Crop("bajra", "Bajra", "🌾", R.drawable.ic_crop_cereal, 50, 8000));
        crops.add(new Crop("ragi", "Ragi", "🍘", R.drawable.ic_crop_cereal, 50, 6000));
        crops.add(new Crop("foxtail_millet", "Foxtail Millet", "🌿", R.drawable.ic_crop_cereal, 50, 5000));
        crops.add(new Crop("little_millet", "Little Millet", "🌱", R.drawable.ic_crop_cereal, 50, 5000));
        crops.add(new Crop("kodo_millet", "Kodo Millet", "🍙", R.drawable.ic_crop_cereal, 50, 5000));
        
        // ═══════════════════════════════════════
        // Pulses (8)
        // ═══════════════════════════════════════
        crops.add(new Crop("chickpea", "Chickpea", "🫘", R.drawable.ic_crop_pulse, 50, 6000));
        crops.add(new Crop("pigeon_pea", "Pigeon Pea", "🌰", R.drawable.ic_crop_pulse, 50, 6000));
        crops.add(new Crop("green_gram", "Green Gram", "🟢", R.drawable.ic_crop_pulse, 50, 5000));
        crops.add(new Crop("black_gram", "Black Gram", "⚫", R.drawable.ic_crop_pulse, 50, 5000));
        crops.add(new Crop("lentil", "Lentil", "🟠", R.drawable.ic_crop_pulse, 50, 5000));
        crops.add(new Crop("horse_gram", "Horse Gram", "🟤", R.drawable.ic_crop_pulse, 50, 4000));
        crops.add(new Crop("cowpea", "Cowpea", "🫛", R.drawable.ic_crop_pulse, 50, 4000));
        crops.add(new Crop("field_pea", "Field Pea", "🟡", R.drawable.ic_crop_pulse, 50, 4000));
        
        // ═══════════════════════════════════════
        // Oilseeds (8)
        // ═══════════════════════════════════════
        crops.add(new Crop("groundnut", "Groundnut", "🥜", R.drawable.ic_crop_oilseed, 50, 6000));
        crops.add(new Crop("sunflower", "Sunflower", "🌻", R.drawable.ic_crop_oilseed, 50, 5000));
        crops.add(new Crop("mustard", "Mustard", "🌼", R.drawable.ic_crop_oilseed, 50, 5000));
        crops.add(new Crop("soybean", "Soybean", "🫘", R.drawable.ic_crop_oilseed, 50, 6000));
        crops.add(new Crop("sesame", "Sesame", "⚪", R.drawable.ic_crop_oilseed, 50, 4000));
        crops.add(new Crop("castor", "Castor", "🏵️", R.drawable.ic_crop_oilseed, 50, 5000));
        crops.add(new Crop("linseed", "Linseed", "💧", R.drawable.ic_crop_oilseed, 50, 4000));
        crops.add(new Crop("safflower", "Safflower", "🌸", R.drawable.ic_crop_oilseed, 50, 4000));
        
        // ═══════════════════════════════════════
        // Commercial (7)
        // ═══════════════════════════════════════
        crops.add(new Crop("cotton", "Cotton", "☁️", R.drawable.ic_crop_commercial, 50, 8000));
        crops.add(new Crop("sugarcane", "Sugarcane", "🎋", R.drawable.ic_crop_commercial, 100, 15000));
        crops.add(new Crop("jute", "Jute", "🧵", R.drawable.ic_crop_commercial, 50, 6000));
        crops.add(new Crop("tobacco", "Tobacco", "🍃", R.drawable.ic_crop_commercial, 50, 5000));
        crops.add(new Crop("tea", "Tea", "🍵", R.drawable.ic_crop_commercial, 50, 4000));
        crops.add(new Crop("coffee", "Coffee", "☕", R.drawable.ic_crop_commercial, 50, 5000));
        crops.add(new Crop("rubber", "Rubber", "🌳", R.drawable.ic_crop_commercial, 50, 6000));
        
        // ═══════════════════════════════════════
        // Fruits (8)
        // ═══════════════════════════════════════
        crops.add(new Crop("mango", "Mango", "🥭", R.drawable.ic_crop_fruit, 50, 8000));
        crops.add(new Crop("banana", "Banana", "🍌", R.drawable.ic_crop_fruit, 100, 10000));
        crops.add(new Crop("apple", "Apple", "🍎", R.drawable.ic_crop_fruit, 50, 6000));
        crops.add(new Crop("grapes", "Grapes", "🍇", R.drawable.ic_crop_fruit, 50, 5000));
        crops.add(new Crop("orange", "Orange", "🍊", R.drawable.ic_crop_fruit, 50, 6000));
        crops.add(new Crop("papaya", "Papaya", "🍈", R.drawable.ic_crop_fruit, 50, 5000));
        crops.add(new Crop("guava", "Guava", "🍐", R.drawable.ic_crop_fruit, 50, 5000));
        crops.add(new Crop("pomegranate", "Pomegranate", "🔴", R.drawable.ic_crop_fruit, 50, 5000));
        
        // ═══════════════════════════════════════
        // Vegetables (9)
        // ═══════════════════════════════════════
        crops.add(new Crop("tomato", "Tomato", "🍅", R.drawable.ic_crop_vegetable, 50, 5000));
        crops.add(new Crop("potato", "Potato", "🥔", R.drawable.ic_crop_vegetable, 50, 8000));
        crops.add(new Crop("onion", "Onion", "🧅", R.drawable.ic_crop_vegetable, 50, 8000));
        crops.add(new Crop("brinjal", "Brinjal", "🍆", R.drawable.ic_crop_vegetable, 50, 4000));
        crops.add(new Crop("cabbage", "Cabbage", "🥬", R.drawable.ic_crop_vegetable, 50, 5000));
        crops.add(new Crop("cauliflower", "Cauliflower", "🥦", R.drawable.ic_crop_vegetable, 50, 5000));
        crops.add(new Crop("carrot", "Carrot", "🥕", R.drawable.ic_crop_vegetable, 50, 4000));
        crops.add(new Crop("okra", "Okra", "🫑", R.drawable.ic_crop_vegetable, 50, 3000));
        crops.add(new Crop("chilli", "Chilli", "🌶️", R.drawable.ic_crop_vegetable, 50, 3000));
        
        return crops;
    }

    /**
     * Get crop by ID
     */
    public static Crop getCropById(String cropId, Context context) {
        List<Crop> crops = getAllCrops(context);
        for (Crop crop : crops) {
            if (crop.getId().equalsIgnoreCase(cropId)) {
                return crop;
            }
        }
        return null;
    }

    /**
     * Validate weight for crop
     */
    public static boolean isValidWeightForCrop(double weight, String cropId, Context context) {
        Crop crop = getCropById(cropId, context);
        if (crop == null) return false;
        return weight >= crop.getMinWeight() && weight <= crop.getMaxWeight();
    }

    /**
     * Get weight error message
     */
    public static String getWeightErrorMessage(double weight, String cropId, Context context) {
        Crop crop = getCropById(cropId, context);
        if (crop == null) return "Invalid crop";
        
        if (weight < crop.getMinWeight()) {
            return "Minimum weight for " + crop.getName() + " is " + crop.getMinWeight() + " kg";
        } else if (weight > crop.getMaxWeight()) {
            return "Maximum weight for " + crop.getName() + " is " + crop.getMaxWeight() + " kg";
        }
        return "Invalid weight";
    }

    /**
     * Get vehicle type for weight (suggestion logic)
     */
    public static String getSuggestedVehicleType(double weight) {
        if (weight <= 500) {
            return "auto";
        } else if (weight <= 1500) {
            return "mini_truck";
        } else if (weight <= 3000) {
            return "truck";
        } else {
            return "lorry";
        }
    }

    /**
     * Check if weight requires commercial vehicle
     */
    public static boolean requiresCommercialVehicle(double weight) {
        return weight > 500;
    }

    /**
     * Get icon resource for a crop type
     */
    public static int getCropIcon(Context context, String cropType) {
        if (cropType == null) return R.drawable.ic_leaf;
        
        switch (cropType.toLowerCase()) {
            case "rice":
            case "paddy":
                return R.drawable.ic_crop_paddy;
            case "tomato":
                return R.drawable.ic_crop_tomato;
            case "banana":
                return R.drawable.ic_crop_banana;
            case "sugarcane":
                return R.drawable.ic_crop_sugarcane;
            default:
                return R.drawable.ic_leaf;
        }
    }
}
