package com.agrigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.agrigo.R;
import com.agrigo.utils.PreferenceManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class FarmerDashboardActivity extends BaseActivity {

    private TextView tvFarmerName;
    private ImageView btnNotifications;
    private com.google.android.material.imageview.ShapeableImageView btnProfileAvatar;


    private com.google.android.material.card.MaterialCardView cardTransport, cardMachinery, cardLabor;
    private com.google.android.material.card.MaterialCardView btnQuickBookings, btnQuickTrack, btnQuickProfile;
    
    private BottomNavigationView bottomNavigationView;

    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_dashboard);

        preferenceManager = new PreferenceManager(this);
        
        initializeViews();
        setupListeners();
        loadUserData();
    }

    private void initializeViews() {
        tvFarmerName = findViewById(R.id.tvFarmerName);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnProfileAvatar = findViewById(R.id.btnProfileAvatar);


        cardTransport = findViewById(R.id.cardTransport);
        cardMachinery = findViewById(R.id.cardMachinery);
        cardLabor = findViewById(R.id.cardLabor);

        btnQuickBookings = findViewById(R.id.btnQuickBookings);
        btnQuickTrack = findViewById(R.id.btnQuickTrack);
        btnQuickProfile = findViewById(R.id.btnQuickProfile);

        bottomNavigationView = findViewById(R.id.bottomNavigation);
    }

    private void setupListeners() {
        // Main Cards
        cardTransport.setOnClickListener(v -> startActivity(new Intent(this, TransportBookingActivity.class)));
        cardMachinery.setOnClickListener(v -> startActivity(new Intent(this, MachineryBookingActivity.class)));
        cardLabor.setOnClickListener(v -> startActivity(new Intent(this, LaborBookingActivity.class)));

        // Quick Actions
        btnQuickBookings.setOnClickListener(v -> startActivity(new Intent(this, MyBookingsActivity.class)));
        btnQuickTrack.setOnClickListener(v -> {
            // Might need a tracking hub or simply open bookings
            startActivity(new Intent(this, MyBookingsActivity.class));
        });
        btnQuickProfile.setOnClickListener(v -> startActivity(new Intent(this, ProfileActivity.class)));
        // Bottom Navigation
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_bookings) {
                startActivity(new Intent(this, MyBookingsActivity.class));
                return true;
            } else if (itemId == R.id.nav_track) {
                startActivity(new Intent(this, MyBookingsActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    private void loadUserData() {
        String name = preferenceManager.getUserName();
        if (name != null && !name.isEmpty()) {
            tvFarmerName.setText(name);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }
}
