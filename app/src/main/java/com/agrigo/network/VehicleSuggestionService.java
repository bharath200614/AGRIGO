package com.agrigo.network;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Body;
import retrofit2.http.Header;
import com.agrigo.models.VehicleSuggestion;
import java.util.List;

/**
 * Retrofit service for vehicle suggestion API endpoint
 */
public interface VehicleSuggestionService {

    /**
     * Get vehicle suggestions based on crop type and weight
     */
    @POST("api/v1/vehicles/suggest")
    Call<ApiResponse<List<VehicleSuggestion>>> getSuggestedVehicles(
            @Body VehicleSuggestionRequest request,
            @Header("Authorization") String token
    );
}

/**
 * Request model for vehicle suggestion API
 */
class VehicleSuggestionRequest {
    public String cropType;
    public int weight;
    public String location;
    public String pickupLocation;
    public String deliveryLocation;

    public VehicleSuggestionRequest(String cropType, int weight, String location) {
        this.cropType = cropType;
        this.weight = weight;
        this.location = location;
    }
}
