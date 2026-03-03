package com.agrigo.models;

/**
 * Model class for User entity
 */
public class User {
    private String id;
    private String name;
    private String phone;
    private String email;
    private String role; // "farmer" or "driver"
    private String profileImageUrl;
    private long createdAt;
    private long updatedAt;

    public User() {
    }

    public User(String id, String name, String phone, String role) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.role = role;
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isFarmer() {
        return "farmer".equalsIgnoreCase(role);
    }

    public boolean isDriver() {
        return "driver".equalsIgnoreCase(role);
    }
}
