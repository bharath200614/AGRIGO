package com.agrigo.network;

import retrofit2.Call;
import retrofit2.http.POST;
import retrofit2.http.Body;

/**
 * Retrofit service interface for Render ML prediction API
 */
public interface MLPredictionService {

    /**
     * Sends cropType and weight to the ML model and gets back a vehicleType prediction
     */
    @POST("predict")
    Call<MLPredictionResponse> predictVehicle(@Body MLPredictionRequest request);
}
