package com.agrigo.utils;

import android.util.Patterns;

/**
 * Utility class for input validation
 */
public class ValidationUtils {

    /**
     * Validate phone number (10 digits for India)
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isEmpty()) {
            return false;
        }
        // Remove any spaces or hyphens
        String cleanPhone = phone.replaceAll("[\\s\\-()]", "");
        // Check if it's 10 digits
        return cleanPhone.length() == 10 && cleanPhone.matches("\\d+");
    }

    /**
     * Validate email address
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /**
     * Validate password (minimum 6 characters)
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        return password.length() >= 6;
    }

    /**
     * Validate password strength
     * @return 0: Weak, 1: Fair, 2: Good, 3: Strong
     */
    public static int getPasswordStrength(String password) {
        if (password == null || password.length() < 6) {
            return 0;
        }

        int strength = 0;

        // Check for uppercase letters
        if (password.matches(".*[A-Z].*")) {
            strength++;
        }

        // Check for lowercase letters
        if (password.matches(".*[a-z].*")) {
            strength++;
        }

        // Check for digits
        if (password.matches(".*\\d.*")) {
            strength++;
        }

        // Check for special characters
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            strength++;
        }

        // Check length (8 characters or more for strong)
        if (password.length() >= 8) {
            strength++;
        }

        return Math.min(strength, 3); // Return max 3 (strong)
    }

    /**
     * Validate full name
     */
    public static boolean isValidName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return false;
        }
        return name.trim().length() >= 2 && name.matches("[a-zA-Z\\s]+");
    }

    /**
     * Get password strength label
     */
    public static String getPasswordStrengthLabel(int strength) {
        switch (strength) {
            case 0:
                return "Weak";
            case 1:
                return "Fair";
            case 2:
                return "Good";
            case 3:
                return "Strong";
            default:
                return "Unknown";
        }
    }

    /**
     * Validate all login fields
     */
    public static boolean isValidLoginInput(String email, String password) {
        return isValidEmail(email) && isValidPassword(password);
    }

    /**
     * Validate all registration fields
     */
    public static boolean isValidRegistrationInput(String name, String email, String password) {
        return isValidName(name) && isValidEmail(email) && isValidPassword(password);
    }
}
