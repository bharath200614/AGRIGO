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

    // Crop types constants
    public static final String CROP_PADDY = "paddy";
    public static final String CROP_TOMATO = "tomato";
    public static final String CROP_BANANA = "banana";
    public static final String CROP_SUGARCANE = "sugarcane";

    /**
     * Get all available crops
     */
    public static List<Crop> getAllCrops(Context context) {
        List<Crop> crops = new ArrayList<>();
        
        crops.add(new Crop(CROP_PADDY, context.getString(R.string.crop_paddy), 
                R.drawable.ic_crop_paddy, 100, 5000));
        crops.add(new Crop(CROP_TOMATO, context.getString(R.string.crop_tomato), 
                R.drawable.ic_crop_tomato, 50, 2000));
        crops.add(new Crop(CROP_BANANA, context.getString(R.string.crop_banana), 
                R.drawable.ic_crop_banana, 200, 3000));
        crops.add(new Crop(CROP_SUGARCANE, context.getString(R.string.crop_sugarcane), 
                R.drawable.ic_crop_sugarcane, 500, 8000));
        
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
            return "auto";           // Auto/Tuk-tuk
        } else if (weight <= 1500) {
            return "mini_truck";     // Mini truck
        } else if (weight <= 3000) {
            return "truck";          // Standard truck
        } else {
            return "lorry";          // Heavy lorry
        }
    }

    /**
     * Check if weight requires commercial vehicle
     */
    public static boolean requiresCommercialVehicle(double weight) {
        return weight > 500;
    }
}
