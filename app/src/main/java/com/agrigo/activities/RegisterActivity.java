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
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.agrigo.R;
import com.agrigo.utils.PreferenceManager;
import com.agrigo.utils.ToastUtils;
import com.agrigo.utils.ValidationUtils;

/**
 * Registration Activity - User account creation screen
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName;
    private EditText etPhone;
    private EditText etPassword;
    private Button btnRegisterAccount;
    private TextView tvLoginLink;
    private ImageButton btnBack;
    private ImageButton btnTogglePassword;
    private FrameLayout roleFarmerContainer;
    private FrameLayout roleDriverContainer;
    private TextView tvNameError;
    private TextView tvPhoneError;
    private TextView tvPasswordError;
    private TextView tvErrorMessage;

    private PreferenceManager preferenceManager;
    private boolean isPasswordVisible = false;
    private String selectedRole = "farmer"; // Default role

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        preferenceManager = new PreferenceManager(this);

        // Initialize views
        initializeViews();

        // Set up listeners
        setupListeners();

        // Apply entrance animations
        applyEntranceAnimations();

        // Get role from intent if passed
        if (getIntent().hasExtra("role")) {
            selectedRole = getIntent().getStringExtra("role");
            updateRoleSelection();
        }
    }

    private void initializeViews() {
        etFullName = findViewById(R.id.et_full_name);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        btnRegisterAccount = findViewById(R.id.btn_register_account);
        tvLoginLink = findViewById(R.id.tv_login_link);
        btnBack = findViewById(R.id.btn_back);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        roleFarmerContainer = findViewById(R.id.role_farmer_container);
        roleDriverContainer = findViewById(R.id.role_driver_container);
        tvNameError = findViewById(R.id.tv_name_error);
        tvPhoneError = findViewById(R.id.tv_phone_error);
        tvPasswordError = findViewById(R.id.tv_password_error);
        tvErrorMessage = findViewById(R.id.tv_error_message);
    }

    private void setupListeners() {
        // Register button
        btnRegisterAccount.setOnClickListener(v -> handleRegistration());

        // Login link
        tvLoginLink.setOnClickListener(v -> navigateToLogin());

        // Back button
        btnBack.setOnClickListener(v -> onBackPressed());

        // Toggle password visibility
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());

        // Role selection
        roleFarmerContainer.setOnClickListener(v -> selectRole("farmer"));
        roleDriverContainer.setOnClickListener(v -> selectRole("driver"));

        // Clear errors on input
        etFullName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tvNameError.setVisibility(View.GONE);
        });

        etPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tvPhoneError.setVisibility(View.GONE);
        });

        etPassword.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) tvPasswordError.setVisibility(View.GONE);
        });
    }

    private void handleRegistration() {
        String name = etFullName.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString();

        // Reset errors
        tvNameError.setVisibility(View.GONE);
        tvPhoneError.setVisibility(View.GONE);
        tvPasswordError.setVisibility(View.GONE);
        tvErrorMessage.setVisibility(View.GONE);

        // Validate name
        if (!ValidationUtils.isValidName(name)) {
            tvNameError.setText(R.string.error_invalid_name);
            tvNameError.setVisibility(View.VISIBLE);
            etFullName.requestFocus();
            return;
        }

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

        // Perform registration
        performRegistration(name, phone, password, selectedRole);
    }

    private void performRegistration(String name, String phone, String password, String role) {
        // Show loading state
        btnRegisterAccount.setEnabled(false);
        btnRegisterAccount.setText(R.string.loading);

        // TODO: Integrate Firebase Authentication
        // FirebaseAuth.getInstance().createUserWithPhoneNumber(phone, callbacks...)
        
        // For now, simulate successful registration
        simulateRegistration(name, phone, password, role);
    }

    private void simulateRegistration(String name, String phone, String password, String role) {
        // Simulate network delay
        new android.os.Handler().postDelayed(() -> {
            btnRegisterAccount.setEnabled(true);
            btnRegisterAccount.setText(R.string.register_account);

            // Store user data in preferences
            preferenceManager.setUserName(name);
            preferenceManager.setUserPhone(phone);
            preferenceManager.setUserRole(role);
            preferenceManager.setIsLoggedIn(true);

            ToastUtils.showShort(this, "Registration successful!");

            // Navigate to appropriate dashboard
            navigateToDashboard(role);
        }, 1500);
    }

    private void navigateToDashboard(String role) {
        Intent intent;
        if ("farmer".equalsIgnoreCase(role)) {
            intent = new Intent(this, FarmerDashboardActivity.class);
        } else {
            // TODO: Create DriverDashboardActivity
            intent = new Intent(this, FarmerDashboardActivity.class); // Placeholder until DriverDashboard is ready
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("role", selectedRole);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right);
        finish();
    }

    private void selectRole(String role) {
        selectedRole = role;
        updateRoleSelection();

        // Animate selection
        if ("farmer".equals(role)) {
            roleFarmerContainer.animate().scaleY(1.05f).scaleX(1.05f).setDuration(200).start();
        } else {
            roleDriverContainer.animate().scaleY(1.05f).scaleX(1.05f).setDuration(200).start();
        }
    }

    private void updateRoleSelection() {
        if ("farmer".equalsIgnoreCase(selectedRole)) {
            roleFarmerContainer.setBackground(getDrawable(R.drawable.bg_role_card_selected));
            roleDriverContainer.setBackground(getDrawable(R.drawable.bg_role_card_unselected));
        } else {
            roleFarmerContainer.setBackground(getDrawable(R.drawable.bg_role_card_unselected));
            roleDriverContainer.setBackground(getDrawable(R.drawable.bg_role_card_selected));
        }
    }

    private void togglePasswordVisibility() {
        if (isPasswordVisible) {
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_eye_hidden);
        } else {
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            btnTogglePassword.setImageResource(R.drawable.ic_eye_visible);
        }
        isPasswordVisible = !isPasswordVisible;
        etPassword.setSelection(etPassword.getText().length());
    }

    private void applyEntranceAnimations() {
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        slideIn.setDuration(600);

        etFullName.startAnimation(slideIn);

        slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        slideIn.setDuration(600);
        slideIn.setStartOffset(150);
        etPhone.startAnimation(slideIn);

        slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        slideIn.setDuration(600);
        slideIn.setStartOffset(300);
        etPassword.startAnimation(slideIn);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.slide_out_right);
    }
}
