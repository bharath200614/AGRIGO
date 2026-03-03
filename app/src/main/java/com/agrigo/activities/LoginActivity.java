package com.agrigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.text.method.HideReturnsTransformationMethod;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.agrigo.R;
import com.agrigo.utils.PreferenceManager;
import com.agrigo.utils.ToastUtils;
import com.agrigo.utils.ValidationUtils;

/**
 * Login Activity - User authentication screen
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etPhone;
    private EditText etPassword;
    private Button btnLogin;
    private Button btnRegister;
    private TextView tvForgotPassword;
    private TextView tvSwitchRole;
    private ImageButton btnBack;
    private ImageButton btnTogglePassword;
    private TextView tvErrorMessage;
    private TextView tvPhoneError;
    private TextView tvPasswordError;

    private PreferenceManager preferenceManager;
    private boolean isPasswordVisible = false;
    private String currentRole = "farmer"; // Default role

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        preferenceManager = new PreferenceManager(this);

        // Initialize views
        initializeViews();

        // Set up listeners
        setupListeners();

        // Apply entrance animations
        applyEntranceAnimations();

        // Get role from intent if passed
        if (getIntent().hasExtra("role")) {
            currentRole = getIntent().getStringExtra("role");
            updateRoleDisplay();
        }
    }

    private void initializeViews() {
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        tvSwitchRole = findViewById(R.id.tv_switch_role);
        btnBack = findViewById(R.id.btn_back);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        tvErrorMessage = findViewById(R.id.tv_error_message);
        tvPhoneError = findViewById(R.id.tv_phone_error);
        tvPasswordError = findViewById(R.id.tv_password_error);
    }

    private void setupListeners() {
        // Login button
        btnLogin.setOnClickListener(v -> handleLogin());

        // Register button
        btnRegister.setOnClickListener(v -> navigateToRegister());

        // Back button
        btnBack.setOnClickListener(v -> onBackPressed());

        // Forgot password
        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());

        // Switch role
        tvSwitchRole.setOnClickListener(v -> switchRole());

        // Toggle password visibility
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        // Clear errors on input
        etPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                tvPhoneError.setVisibility(View.GONE);
            }
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                tvPasswordError.setVisibility(View.GONE);
            }
        });
    }

    private void handleLogin() {
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Reset errors
        tvPhoneError.setVisibility(View.GONE);
        tvPasswordError.setVisibility(View.GONE);
        tvErrorMessage.setVisibility(View.GONE);

        // Validate phone
        if (!ValidationUtils.isValidPhone(phone)) {
            tvPhoneError.setText(R.string.error_invalid_phone);
            tvPhoneError.setVisibility(View.VISIBLE);
            etPhone.requestFocus();
            return;
        }

        // Validate password
        if (!ValidationUtils.isValidPassword(password)) {
            tvPasswordError.setText(R.string.error_invalid_password);
            tvPasswordError.setVisibility(View.VISIBLE);
            etPassword.requestFocus();
            return;
        }

        // Perform login (Firebase integration ready)
        performLogin(phone, password);
    }

    private void performLogin(String phone, String password) {
        // Show loading state
        btnLogin.setEnabled(false);
        btnLogin.setText(R.string.loading);

        // TODO: Integrate Firebase Authentication
        // FirebaseAuth.getInstance().signInWithPhoneNumber(phone, callbacks...)
        
        // For now, simulate successful login
        simulateLogin(phone, password);
    }

    private void simulateLogin(String phone, String password) {
        // Simulate network delay
        new android.os.Handler().postDelayed(() -> {
            btnLogin.setEnabled(true);
            btnLogin.setText(R.string.login_button);

            // Store user data in preferences
            preferenceManager.setUserPhone(phone);
            preferenceManager.setUserRole(currentRole);
            preferenceManager.setIsLoggedIn(true);

            // Navigate to appropriate dashboard
            navigateToDashboard();
        }, 1500);
    }

    private void navigateToDashboard() {
        Intent intent;
        if ("farmer".equalsIgnoreCase(currentRole)) {
            intent = new Intent(this, FarmerDashboardActivity.class);
        } else {
            // TODO: Create DriverDashboardActivity
            intent = new Intent(this, FarmerDashboardActivity.class); // Placeholder until DriverDashboard is ready
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        intent.putExtra("role", currentRole);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, android.R.anim.fade_out);
    }

    private void handleForgotPassword() {
        // TODO: Implement password reset flow
        ToastUtils.showShort(this, "Password reset coming soon!");
    }

    private void switchRole() {
        currentRole = "farmer".equalsIgnoreCase(currentRole) ? "driver" : "farmer";
        updateRoleDisplay();

        // Animate role switch
        tvSwitchRole.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(100)
                .withEndAction(() -> {
                    tvSwitchRole.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }

    private void updateRoleDisplay() {
        tvSwitchRole.setText("farmer".equalsIgnoreCase(currentRole) ? R.string.role_driver : R.string.role_farmer);
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            // Hide password
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_eye_hidden);
        } else {
            // Show password
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_eye_visible);
        }
        isPasswordVisible = !isPasswordVisible;
        // Move cursor to end
        etPassword.setSelection(etPassword.getText().length());
    }

    private void applyEntranceAnimations() {
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        slideIn.setDuration(600);

        etPhone.startAnimation(slideIn);
        etPassword.startAnimation(slideIn);

        slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        slideIn.setDuration(600);
        slideIn.setStartOffset(200);
        btnLogin.startAnimation(slideIn);
    }

    @Override
    public void onBackPressed() {
        // Navigate back to welcome screen
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right);
    }
}
