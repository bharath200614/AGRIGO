package com.agrigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agrigo.R;
import com.agrigo.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class DriverHomeActivity extends BaseActivity {

    private TextView textWelcome;
    private MaterialCardView cardRequests;
    private MaterialCardView cardActiveTracking;
    private MaterialCardView cardEarnings;
    private MaterialCardView cardProfileNav;
    private MaterialButton cardProfile; // top-right avatar button
    private MaterialButton buttonLogout;
    private TextView textRequestCount;
    private TextView textEarnings;
    private TextView tvDriverStatus;
    private androidx.appcompat.widget.SwitchCompat switchAvailability;
    private RecyclerView recyclerRequests;
    private LinearLayout layoutEmptyState;
    private com.agrigo.adapters.DriverRequestAdapter requestAdapter;
    private List<com.agrigo.models.DriverRequest> requestList = new ArrayList<>();

    private com.google.firebase.firestore.ListenerRegistration requestsListener;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
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

    private void initViews() {
        textWelcome = findViewById(R.id.text_welcome);
        cardRequests = findViewById(R.id.card_requests);
        cardActiveTracking = findViewById(R.id.card_active_tracking);
        cardEarnings = findViewById(R.id.card_earnings);
        cardProfileNav = findViewById(R.id.card_profile_nav);
        cardProfile = findViewById(R.id.card_profile);
        buttonLogout = findViewById(R.id.button_logout);
        textRequestCount = findViewById(R.id.text_request_count);
        textEarnings = findViewById(R.id.text_earnings_amount);
        tvDriverStatus = findViewById(R.id.tv_driver_status);
        switchAvailability = findViewById(R.id.switch_availability);
        recyclerRequests = findViewById(R.id.recycler_requests);
        layoutEmptyState = findViewById(R.id.layout_empty_state);

        if (recyclerRequests != null) {
            recyclerRequests.setLayoutManager(new LinearLayoutManager(this));
            requestAdapter = new com.agrigo.adapters.DriverRequestAdapter(requestList, new com.agrigo.adapters.DriverRequestAdapter.OnRequestClickListener() {
                @Override
                public void onAcceptClick(com.agrigo.models.DriverRequest request) {
                    acceptTransportRequest(request);
                }

                @Override
                public void onDeclineClick(com.agrigo.models.DriverRequest request) {
                    requestList.remove(request);
                    requestAdapter.notifyDataSetChanged();
                }
            });
            recyclerRequests.setAdapter(requestAdapter);
        }
    }

    private void setupListeners() {
        cardRequests.setOnClickListener(v -> {
            startActivity(new Intent(DriverHomeActivity.this, DriverRequestActivity.class));
        });

        cardActiveTracking.setOnClickListener(v -> {
            String driverId = mAuth.getCurrentUser().getUid();
            FirebaseFirestore.getInstance().collection("transport_requests")
                .whereEqualTo("driverId", driverId)
                .whereIn("status", java.util.Arrays.asList("ACCEPTED", "ON_TRIP", "ARRIVED"))
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String activeId = querySnapshot.getDocuments().get(0).getId();
                        Intent intent = new Intent(DriverHomeActivity.this, TrackingActivity.class);
                        intent.putExtra("REQUEST_ID", activeId);
                        startActivity(intent);
                    } else {
                        Toast.makeText(this, "No active trip to track", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error finding active trip", Toast.LENGTH_SHORT).show());
        });

        cardEarnings.setOnClickListener(v -> {
            startActivity(new Intent(this, DriverEarningsActivity.class));
        });

        if (cardProfileNav != null) {
            cardProfileNav.setOnClickListener(v -> {
                startActivity(new Intent(DriverHomeActivity.this, ProfileActivity.class));
            });
        }

        cardProfile.setOnClickListener(v -> {
            startActivity(new Intent(DriverHomeActivity.this, ProfileActivity.class));
        });

        buttonLogout.setOnClickListener(v -> handleLogout());

        if (switchAvailability != null) {
            switchAvailability.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateAvailability(isChecked);
            });
            // Set initial state
            db.collection("drivers").document(mAuth.getUid()).get().addOnSuccessListener(doc -> {
                if (doc.exists() && doc.contains("isAvailable")) {
                    switchAvailability.setChecked(doc.getBoolean("isAvailable"));
                }
            });
        }
    }

    private void updateAvailability(boolean available) {
        String uid = mAuth.getUid();
        db.collection("drivers").document(uid).update("isAvailable", available, "status", available ? "AVAILABLE" : "OFFLINE")
            .addOnSuccessListener(aVoid -> {
                tvDriverStatus.setText(available ? "Online — Accepting Loads" : "Offline — No Requests");
            });
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

        String driverVehicleType = preferenceManager.getVehicleType();
        String myVehicleType = (driverVehicleType != null) ? driverVehicleType.toLowerCase().trim() : "";

        requestsListener = FirebaseFirestore.getInstance().collection("transport_requests")
                .whereEqualTo("status", "REQUESTED")
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        requestList.clear();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : value.getDocuments()) {
                            // Only count requests matching the driver's vehicle type
                            com.agrigo.models.DriverRequest request = doc.toObject(com.agrigo.models.DriverRequest.class);
                            if (request != null) {
                                request.setId(doc.getId());
                                String reqVehicleType = request.getVehicleType();
                                if (reqVehicleType != null && reqVehicleType.toLowerCase().trim().equals(myVehicleType)) {
                                    requestList.add(request);
                                }
                            }
                        }

                        // Sort by newest first
                        java.util.Collections.sort(requestList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                        int count = requestList.size();
                        if (textRequestCount != null) {
                            textRequestCount.setText(String.valueOf(count));
                        }
                        // Show/hide empty state
                        if (layoutEmptyState != null) {
                            layoutEmptyState.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
                        }
                        if (recyclerRequests != null) {
                            recyclerRequests.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                            requestAdapter.notifyDataSetChanged();
                        }
                    }
                });
    }

    private void acceptTransportRequest(com.agrigo.models.DriverRequest request) {
        String uid = mAuth.getUid();
        com.google.firebase.firestore.DocumentReference docRef = db.collection("transport_requests").document(request.getId());
        
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(docRef);
            String status = snapshot.getString("status");
            if ("REQUESTED".equals(status)) {
                transaction.update(docRef, "status", "ACCEPTED", "driverId", uid, "assignedDriverId", uid);
                transaction.update(db.collection("drivers").document(uid), "isAvailable", false, "status", "BUSY");
                return null;
            } else {
                throw new com.google.firebase.firestore.FirebaseFirestoreException("Job taken", com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
            }
        }).addOnSuccessListener(aVoid -> {
            Intent intent = new Intent(this, TrackingActivity.class);
            intent.putExtra("REQUEST_ID", request.getId());
            startActivity(intent);
        }).addOnFailureListener(e -> Toast.makeText(this, "Failed to accept job", Toast.LENGTH_SHORT).show());
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
                    for (QueryDocumentSnapshot doc : querySnapshot) {
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
