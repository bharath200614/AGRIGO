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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.agrigo.R;
import com.agrigo.utils.PreferenceManager;
import com.agrigo.utils.ToastUtils;
import com.agrigo.utils.ValidationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.FirebaseNetworkException;

// removed timber import
/**
 * Login Activity - User authentication screen
 */
public class LoginActivity extends AppCompatActivity {

    private EditText etEmail;
    private EditText etPassword;
    private Button btnLogin;
    private TextView btnRegister;
    private TextView tvForgotPassword;
    private ImageView ivLoginLogo;
    private TextView tvErrorMessage;
    private ProgressBar progressBar;

    private TextView tvSwitchRole;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
 
        try {
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            preferenceManager = new PreferenceManager(this);
        } catch (Exception e) {
            android.util.Log.e("LoginActivity", "Init failed", e);
            ToastUtils.showShort(this, "Initialization error. Attempting to continue...");
        }

        // Initialize views first, always
        initializeViews();

        // Cross-check: if SharedPrefs says logged in but Firebase has no session, clear stale prefs
        if (preferenceManager.isLoggedIn() && mAuth.getCurrentUser() == null) {
            // Stale login preference found, but FirebaseAuth has no session. Clearing.
            preferenceManager.clearAll();
        }

        if (preferenceManager.isLoggedIn() && mAuth.getCurrentUser() != null) {
            // User already logged in, navigating to dashboard.
            navigateToDashboard();
            return;
        }

        // Set up listeners
        setupListeners();

        // Apply entrance animations
        applyEntranceAnimations();
    }

    private void initializeViews() {
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.tv_register_link);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        ivLoginLogo = findViewById(R.id.iv_login_logo);
        tvErrorMessage = findViewById(R.id.tv_error_message);
        progressBar = findViewById(R.id.progress_bar);
        tvSwitchRole = findViewById(R.id.tv_switch_role);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> handleLogin());
        btnRegister.setOnClickListener(v -> navigateToRegister());
        ivLoginLogo.setOnClickListener(v -> {
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        tvForgotPassword.setOnClickListener(v -> handleForgotPassword());
        if (tvSwitchRole != null) {
            tvSwitchRole.setOnClickListener(v -> {
                Intent intent = new Intent(this, WelcomeActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            });
        }
    }

    private void handleLogin() {
        android.util.Log.d("LoginActivity", "Handle Login clicked");
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        tvErrorMessage.setVisibility(View.GONE);

        if (!ValidationUtils.isValidEmail(email)) {
            etEmail.setError("Invalid email format");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        performLogin(email, password);
    }

    private void performLogin(String email, String password) {
        showLoading(true);

        // Starting login

        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        // Firebase Auth login successful
                        fetchUserProfile(user.getUid());
                    } else {
                        // FirebaseUser is null after successful login
                        showLoading(false);
                    }
                } else {
                    showLoading(false);
                    Exception exception = task.getException();
                    // Login failed

                    tvErrorMessage.setVisibility(View.VISIBLE);
                    if (exception instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                        tvErrorMessage.setText("User not found. Please register.");
                    } else if (exception instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                        tvErrorMessage.setText("Incorrect email or password.");
                    } else if (exception instanceof FirebaseNetworkException) {
                        tvErrorMessage.setText("Network error. Check your internet connection.");
                    } else {
                        tvErrorMessage.setText("Authentication failed: " + (exception != null ? exception.getMessage() : "Unknown error"));
                    }
                }
            });
    }

    private void fetchUserProfile(String userId) {
        // Fetching user profile
        db.collection("users").document(userId).get()
            .addOnCompleteListener(task -> {
                showLoading(false);
                if (task.isSuccessful() && task.getResult() != null) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        String name = document.getString("name");
                        String role = document.getString("role");
                        String phone = document.getString("phone");
                        // Profile retrieved

                        // Save to preferences
                        preferenceManager.setUserId(userId);
                        preferenceManager.setUserName(name);
                        preferenceManager.setUserPhone(phone);
                        preferenceManager.setUserRole(role);
                        preferenceManager.setIsLoggedIn(true);

                        navigateToDashboard();
                    } else {
                        // User profile document does not exist
                        ToastUtils.showShort(this, "User profile not found");
                        mAuth.signOut(); // Ensure they don't stay in broken state
                    }
                } else {
                    // Error fetching profile
                    String errMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                    
                    // Show error in persistent UI instead of transient Toast
                    tvErrorMessage.setVisibility(View.VISIBLE);
                    if (errMsg.contains("PERMISSION_DENIED")) {
                        tvErrorMessage.setText("Permission Denied: Update Firestore Rules in Firebase Console.");
                    } else {
                        tvErrorMessage.setText("Error fetching profile: " + errMsg);
                    }
                    
                    // Note: If this says 'PERMISSION_DENIED', the Firestore security rules need to be updated
                    // in the Firebase Console to allow: read, write: if request.auth != null;
                }
            });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (btnLogin != null) {
            btnLogin.setEnabled(!show);
            btnLogin.setText(show ? "" : getString(R.string.login_button));
        }
    }

    private void navigateToDashboard() {
        String role = preferenceManager.getUserRole();
        // Navigating to dashboard
        Intent intent;
        if ("farmer".equalsIgnoreCase(role)) {
            ToastUtils.showShort(this, "Welcome Farmer: " + (preferenceManager.getUserName() != null ? preferenceManager.getUserName() : ""));
            intent = new Intent(this, FarmerDashboardActivity.class);
        } else if ("driver".equalsIgnoreCase(role)) {
            ToastUtils.showShort(this, "Welcome Driver: " + (preferenceManager.getUserName() != null ? preferenceManager.getUserName() : ""));
            intent = new Intent(this, DriverHomeActivity.class); 
        } else if ("labor".equalsIgnoreCase(role)) {
            ToastUtils.showShort(this, "Welcome Labour: " + (preferenceManager.getUserName() != null ? preferenceManager.getUserName() : ""));
            intent = new Intent(this, LaborHomeActivity.class);
        } else if ("machinery_provider".equalsIgnoreCase(role)) {
            ToastUtils.showShort(this, "Welcome Machinery Provider");
            intent = new Intent(this, MachineryProviderActivity.class);
        } else {
            // Fallback
            ToastUtils.showShort(this, "Role: " + role + " - Defaulting to Farmer");
            intent = new Intent(this, FarmerDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void navigateToRegister() {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    private void handleForgotPassword() {
        String email = etEmail.getText().toString().trim();
        if (email.isEmpty() || !ValidationUtils.isValidEmail(email)) {
            ToastUtils.showShort(this, "Enter a valid email address first");
            return;
        }
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    ToastUtils.showShort(this, "Password reset link sent to your email");
                } else {
                    ToastUtils.showShort(this, "Error: " + task.getException().getMessage());
                }
            });
    }

    private void applyEntranceAnimations() {
        // Animations temporarily disabled to ensure button interactivity
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
