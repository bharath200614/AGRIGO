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

public class WelcomeActivity extends AppCompatActivity {

    private ImageView ivAppLogo;
    private TextView tvAppName;
    private TextView tvSubtitle;
    private ImageView ivFarmerTruck;
    private Button btnGetStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        // Initialize views
        initializeViews();

        // Apply animations
        applyAnimations();

        // Set up button click listener
        setupButtonListeners();
    }

    private void initializeViews() {
        ivAppLogo = findViewById(R.id.iv_app_logo);
        tvAppName = findViewById(R.id.tv_app_name);
        tvSubtitle = findViewById(R.id.tv_subtitle);
        ivFarmerTruck = findViewById(R.id.iv_farmer_truck);
        btnGetStarted = findViewById(R.id.btn_get_started);
    }

    private void applyAnimations() {
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

        // Button slide-up fade-in
        Animation slideUpButton = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade);
        slideUpButton.setDuration(800);
        slideUpButton.setStartOffset(1000);
        btnGetStarted.startAnimation(slideUpButton);
    }

    private void setupButtonListeners() {
        btnGetStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate to Login Activity
                navigateToLogin();
            }
        });

        btnGetStarted.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                // Button scale animation feedback
                v.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            v.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .start();
                        }
                    })
                    .start();
                return false;
            }
        });
    }

    private void navigateToLogin() {
        // Fade out animation before transition
        Animation fadeOut = AnimationUtils.loadAnimation(this, android.R.anim.fade_out);
        fadeOut.setDuration(300);
        
        fadeOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // Navigate to LoginActivity
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });

        findViewById(R.id.welcome_root).startAnimation(fadeOut);
    }

    @Override
    public void onBackPressed() {
        // Prevent back button on welcome screen
        super.onBackPressed();
    }
}
