package com.agrigo.network;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Generic API response wrapper for all API calls
 */
public class ApiResponse<T> {
    
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("data")
    private T data;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("error")
    private String error;
    
    @SerializedName("timestamp")
    private String timestamp;

    public ApiResponse() {
    }

    public ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    // Getters and Setters
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
