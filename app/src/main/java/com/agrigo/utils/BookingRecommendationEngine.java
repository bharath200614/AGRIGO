package com.agrigo.utils;

import com.agrigo.models.VehicleSuggestion;
import java.util.ArrayList;
import java.util.List;

public class BookingRecommendationEngine {

    /**
     * Recommends machinery based on crop, service type and land size.
     */
    public static List<VehicleSuggestion> getMachineryRecommendations(String cropType, String serviceType, double landSize) {
        List<VehicleSuggestion> recommendations = new ArrayList<>();
        
        // Logic for Machinery based on Service Type
        if (serviceType.equalsIgnoreCase("Ploughing")) {
            recommendations.add(new VehicleSuggestion("tractor", "Tractor with Plough", 0));
            recommendations.get(0).setEstimatedCost(landSize * 1200);
        } else if (serviceType.equalsIgnoreCase("Sowing")) {
            recommendations.add(new VehicleSuggestion("rotavator", "Rotavator & Seeder", 0));
            recommendations.get(0).setEstimatedCost(landSize * 1500);
        } else if (serviceType.equalsIgnoreCase("Spraying")) {
            recommendations.add(new VehicleSuggestion("sprayer", "Power Sprayer", 0));
            recommendations.get(0).setEstimatedCost(landSize * 800);
        } else if (serviceType.equalsIgnoreCase("Harvesting")) {
            // Recommendation can vary by crop
            String machineName = "Harvester";
            double rate = 2500;
            if (cropType.equalsIgnoreCase("Sugarcane")) {
                machineName = "Sugarcane Harvester";
                rate = 5000;
            } else if (cropType.equalsIgnoreCase("Paddy")) {
                machineName = "Combined Harvester (Track type)";
                rate = 2800;
            }
            recommendations.add(new VehicleSuggestion("harvester", machineName, 0));
            recommendations.get(0).setEstimatedCost(landSize * rate);
        } else if (serviceType.equalsIgnoreCase("Excavation")) {
            recommendations.add(new VehicleSuggestion("jcb", "JCB Earthmover", 0));
            recommendations.get(0).setEstimatedCost(landSize * 3000);
        } else if (serviceType.equalsIgnoreCase("Leveling")) {
            recommendations.add(new VehicleSuggestion("bulldozer", "Bulldozer Leveler", 0));
            recommendations.get(0).setEstimatedCost(landSize * 4000);
        }
        
        return recommendations;
    }

    /**
     * Recommends transport vehicle based on crop weight.
     */
    public static List<VehicleSuggestion> getTransportRecommendations(String cropType, double weight) {
        List<VehicleSuggestion> recommendations = new ArrayList<>();
        
        // Base recommendation by weight
        if (weight <= 500) {
            recommendations.add(new VehicleSuggestion("auto", "Eco Transport (Auto)", 500));
            recommendations.get(0).setEstimatedCost(500 + (weight * 2.5));
        } else if (weight <= 2000) {
            recommendations.add(new VehicleSuggestion("mini_truck", "Mini Truck (Tempo)", 2000));
            recommendations.get(0).setEstimatedCost(1200 + (weight * 2.0));
        } else if (weight <= 5000) {
            recommendations.add(new VehicleSuggestion("truck", "Standard Truck", 5000));
            recommendations.get(0).setEstimatedCost(2500 + (weight * 1.8));
        } else {
            recommendations.add(new VehicleSuggestion("lorry", "Heavy Duty Lorry", 15000));
            recommendations.get(0).setEstimatedCost(4000 + (weight * 1.5));
        }
        
        return recommendations;
    }

    /**
     * Calculates labour cost based on worker count and work type.
     */
    public static double calculateLabourCost(String cropType, int workerCount, String workType) {
        double baseRate = 400; // default daily wage
        
        if (workType.equalsIgnoreCase("Harvesting")) {
            baseRate = 550;
        } else if (workType.equalsIgnoreCase("Planting")) {
            baseRate = 450;
        } else if (workType.equalsIgnoreCase("Cleaning")) {
            baseRate = 400;
        }
        
        // Sugarcane harvesting is harder
        if (cropType.equalsIgnoreCase("Sugarcane") && workType.equalsIgnoreCase("Harvesting")) {
            baseRate += 150;
        }

        return workerCount * baseRate;
    }
}
