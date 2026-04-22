package com.agrigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.agrigo.R;
import com.agrigo.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
// import com.google.firebase.messaging.FirebaseMessaging; // Disabled: Spark plan, no Cloud Functions
import java.util.HashMap;
import java.util.Map;

public class DriverHomeActivity extends AppCompatActivity {

    private TextView textWelcome;
    private MaterialCardView cardRequests;
    private MaterialCardView cardActiveTracking;
    private MaterialCardView cardEarnings;
    private MaterialCardView cardProfile;
    private MaterialButton buttonLogout;
    private TextView textRequestCount;
    private TextView textEarnings;
    private com.google.firebase.firestore.ListenerRegistration requestsListener;

    private FirebaseAuth mAuth;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_home);

        mAuth = FirebaseAuth.getInstance();
        preferenceManager = new PreferenceManager(this);

        // Guard: if not authenticated, go to WelcomeActivity
        if (mAuth.getCurrentUser() == null) {
            navigateToWelcome();
            return;
        }

        initViews();
        setupListeners();

        // Load user name from preferences
        String name = preferenceManager.getUserName();
        textWelcome.setText("Welcome back, " + (name != null && !name.isEmpty() ? name : "Driver"));

        startListeningForRequests();
        fetchTodayEarnings();
        
        // Start Foreground Service for tracking location & dispatch
        // SAFELY check for permission before starting foreground service on API 34+
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) 
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            
            Intent serviceIntent = new Intent(this, com.agrigo.services.DriverDispatchService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } else {
            Toast.makeText(this, "Location permission needed for automatic dispatch", Toast.LENGTH_LONG).show();
        }
    }

    // FCM setup disabled - Spark plan does not support Cloud Functions for push delivery.
    // Drivers see new requests via real-time Firestore snapshot listener in DriverRequestActivity.
    // Re-enable when upgrading to Blaze plan.
    // private void setupFirebaseMessaging() {
    //     if (mAuth.getCurrentUser() == null) return;
    //     String userId = mAuth.getCurrentUser().getUid();
    //     FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
    //         if (!task.isSuccessful()) return;
    //         String token = task.getResult();
    //         Map<String, Object> tokenData = new HashMap<>();
    //         tokenData.put("fcmToken", token);
    //         FirebaseFirestore.getInstance().collection("users").document(userId)
    //                 .set(tokenData, com.google.firebase.firestore.SetOptions.merge());
    //     });
    //     FirebaseMessaging.getInstance().subscribeToTopic("available_drivers");
    // }

    private void initViews() {
        textWelcome = findViewById(R.id.text_welcome);
        cardRequests = findViewById(R.id.card_requests);
        cardActiveTracking = findViewById(R.id.card_active_tracking);
        cardEarnings = findViewById(R.id.card_earnings);
        cardProfile = findViewById(R.id.card_profile);
        buttonLogout = findViewById(R.id.button_logout);
        textRequestCount = findViewById(R.id.text_request_count);
        textEarnings = findViewById(R.id.text_earnings_amount);
    }

    private void setupListeners() {
        cardRequests.setOnClickListener(v -> {
            startActivity(new Intent(DriverHomeActivity.this, DriverRequestActivity.class));
        });

        cardActiveTracking.setOnClickListener(v -> {
            startActivity(new Intent(DriverHomeActivity.this, TrackingActivity.class));
        });

        cardEarnings.setOnClickListener(v -> {
            Toast.makeText(this, "Earnings Screen not implemented yet", Toast.LENGTH_SHORT).show();
        });

        cardProfile.setOnClickListener(v -> {
            startActivity(new Intent(DriverHomeActivity.this, ProfileActivity.class));
        });

        buttonLogout.setOnClickListener(v -> handleLogout());
    }

    private void handleLogout() {
        if (requestsListener != null) requestsListener.remove();
        
        Intent serviceIntent = new Intent(this, com.agrigo.services.DriverDispatchService.class);
        stopService(serviceIntent);
        
        mAuth.signOut();
        preferenceManager.clearAll();
        navigateToWelcome();
    }

    private void startListeningForRequests() {
        if (requestsListener != null) requestsListener.remove();
        
        requestsListener = FirebaseFirestore.getInstance().collection("transport_requests")
                .whereEqualTo("status", "REQUESTED")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null && textRequestCount != null) {
                        textRequestCount.setText(String.valueOf(value.size()));
                    }
                });
    }
    
    private void fetchTodayEarnings() {
        String driverId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";
        if (driverId.isEmpty()) return;
        
        // Get start of today in millis
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long todayStart = cal.getTimeInMillis();
        
        FirebaseFirestore.getInstance().collection("transport_requests")
                .whereEqualTo("driverId", driverId)
                .whereEqualTo("status", "completed")
                .whereGreaterThanOrEqualTo("completedAt", todayStart)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    double totalEarnings = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        // Try to get fare from different possible fields
                        Double fare = doc.getDouble("fare");
                        if (fare != null) {
                            totalEarnings += fare;
                        } else {
                            String priceStr = doc.getString("estimatedPrice");
                            if (priceStr != null) {
                                try {
                                    totalEarnings += Double.parseDouble(priceStr.replaceAll("[^0-9.]", ""));
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                    if (textEarnings != null) {
                        textEarnings.setText(String.format(java.util.Locale.getDefault(), "₹%.0f", totalEarnings));
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestsListener != null) requestsListener.remove();
    }

    private void navigateToWelcome() {
        Intent intent = new Intent(this, WelcomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
