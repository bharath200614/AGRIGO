package com.agrigo.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility class for managing SharedPreferences
 */
public class PreferenceManager {

    private static final String PREFS_NAME = "AgriGo_Prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_TOKEN = "user_token";

    private SharedPreferences sharedPreferences;

    public PreferenceManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    // User ID
    public void setUserId(String userId) {
        sharedPreferences.edit().putString(KEY_USER_ID, userId).apply();
    }

    public String getUserId() {
        return sharedPreferences.getString(KEY_USER_ID, "");
    }

    // User Name
    public void setUserName(String userName) {
        sharedPreferences.edit().putString(KEY_USER_NAME, userName).apply();
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, "");
    }

    // User Phone
    public void setUserPhone(String userPhone) {
        sharedPreferences.edit().putString(KEY_USER_PHONE, userPhone).apply();
    }

    public String getUserPhone() {
        return sharedPreferences.getString(KEY_USER_PHONE, "");
    }

    // User Role
    public void setUserRole(String role) {
        sharedPreferences.edit().putString(KEY_USER_ROLE, role).apply();
    }

    public String getUserRole() {
        return sharedPreferences.getString(KEY_USER_ROLE, "");
    }

    // Login Status
    public void setIsLoggedIn(boolean isLoggedIn) {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, isLoggedIn).apply();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // User Token
    public void setUserToken(String token) {
        sharedPreferences.edit().putString(KEY_USER_TOKEN, token).apply();
    }

    public String getUserToken() {
        return sharedPreferences.getString(KEY_USER_TOKEN, "");
    }

    // Clear all preferences
    public void clearAll() {
        sharedPreferences.edit().clear().apply();
    }
}
