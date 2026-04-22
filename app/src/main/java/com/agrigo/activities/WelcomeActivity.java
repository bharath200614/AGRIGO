package com.agrigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.agrigo.R;
import com.agrigo.utils.PreferenceManager;
import com.agrigo.utils.ToastUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import timber.log.Timber;
import com.agrigo.utils.DatasetSeeder;

public class WelcomeActivity extends AppCompatActivity {

    private ImageView ivAppLogo;
    private TextView tvAppName;
    private TextView tvSubtitle;
    private ImageView ivFarmerTruck;
    private Button btnLogin;
    private View btnRegister;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Initialize views
        initializeViews();

        // Initialize Firebase and Preferences securely
        try {
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            preferenceManager = new PreferenceManager(this);
        } catch (Exception e) {
            Timber.e(e, "Error initializing Firebase or PreferenceManager");
            ToastUtils.showShort(this, "Application initialization failed. Please restart.");
            return;
        }

        // Check if user is already logged in
        checkUserSession();

        // Apply animations
        applyAnimations();

        // Set up button click listener
        setupButtonListeners();
    }

    private void initializeViews() {
//        ivAppLogo = findViewById(R.id.iv_app_logo);
//        tvAppName = findViewById(R.id.tv_app_name);
//        tvSubtitle = findViewById(R.id.tv_subtitle);
//        ivFarmerTruck = findViewById(R.id.iv_farmer_truck);
        btnLogin = findViewById(R.id.btn_login);
        btnRegister = findViewById(R.id.btn_register);
    }

    private void checkUserSession() {
        if (mAuth == null) {
            Timber.e("FirebaseAuth is null in WelcomeActivity");
            return;
        }
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String uid = currentUser.getUid();
            Timber.d("User session found in WelcomeActivity. Fetching specific role for UID: %s", uid);

            // Fetch role from Firestore directly per requirements
            db.collection("users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String role = document.getString("role");
                            String name = document.getString("name");
                            Timber.d("Welcome session recovered. Name: %s, Role: %s", name, role);

                            // Restore session context
                            preferenceManager.setUserId(uid);
                            preferenceManager.setUserName(name);
                            preferenceManager.setUserRole(role);
                            preferenceManager.setIsLoggedIn(true);

                            navigateToDashboard(role);
                        } else {
                            Timber.w("No user document found during Welcome session check for UID: %s", uid);
                            mAuth.signOut();
                        }
                    } else {
                        Timber.e(task.getException(), "Session error reading Firestore in Welcome session check.");
                        ToastUtils.showShort(this, "Session error. Please login.");
                    }
                });
        } else {
            Timber.d("No active Firebase session in WelcomeActivity.");
        }
    }

    private void navigateToDashboard(String role) {
        Timber.d("Navigating to dashboard with role: %s", role);
        Intent intent;
        if ("farmer".equalsIgnoreCase(role)) {
            intent = new Intent(this, FarmerDashboardActivity.class);
        } else if ("driver".equalsIgnoreCase(role)) {
            intent = new Intent(this, DriverHomeActivity.class);
        } else if ("labor".equalsIgnoreCase(role)) {
            intent = new Intent(this, LaborHomeActivity.class);
        } else {
            intent = new Intent(this, FarmerDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void applyAnimations() {
/*
        // Logo fade-in
        Animation fadeInLogo = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeInLogo.setDuration(600);
        ivAppLogo.startAnimation(fadeInLogo);

        // App name fade-in with delay
        Animation fadeInName = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeInName.setDuration(600);
        fadeInName.setStartOffset(200);
        tvAppName.startAnimation(fadeInName);

        // Subtitle fade-in with delay
        Animation fadeInSubtitle = AnimationUtils.loadAnimation(this, R.anim.fade_in);
        fadeInSubtitle.setDuration(600);
        fadeInSubtitle.setStartOffset(400);
        tvSubtitle.startAnimation(fadeInSubtitle);

        // Illustration slide-up fade-in
        Animation slideUpIllustration = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade);
        slideUpIllustration.setDuration(800);
        slideUpIllustration.setStartOffset(600);
        ivFarmerTruck.startAnimation(slideUpIllustration);
*/

        // Button slide-up fade-in
        Animation slideUpButton = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade);
        slideUpButton.setDuration(800);
        slideUpButton.setStartOffset(1000);
        btnLogin.startAnimation(slideUpButton);
        btnRegister.startAnimation(slideUpButton);
    }

    private void setupButtonListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToActivity(LoginActivity.class);
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigateToActivity(RegisterActivity.class);
            }
        });
    }

    private void navigateToActivity(Class<?> targetActivity) {
        Intent intent = new Intent(WelcomeActivity.this, targetActivity);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onBackPressed() {
        // Prevent back button on welcome screen
        super.onBackPressed();
    }
}
