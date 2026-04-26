package com.agrigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.agrigo.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

public class IncomingRequestActivity extends BaseActivity implements OnMapReadyCallback {

    private static final String TAG = "IncomingRequestActivity";

    private TextView tvTitle, tvTimer, tvDistance, tvPickup, tvDetails;
    private MaterialButton btnAccept, btnReject;

    private FirebaseFirestore db;
    private String requestId;
    private String serviceType; // "TRANSPORT" or "MACHINERY"
    private String uid;
    
    private CountDownTimer countDownTimer;
    private GoogleMap mMap;
    private LatLng targetLocation;
    
    private boolean isFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Wake up screen and show over lock screen
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.activity_incoming_request);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        if (getIntent() != null) {
            requestId = getIntent().getStringExtra("REQUEST_ID");
            serviceType = getIntent().getStringExtra("SERVICE_TYPE");
        }

        if (requestId == null || serviceType == null) {
            finish();
            return;
        }

        initViews();
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        loadRequestDetails();
        startTimer();
    }

    private void initViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvTimer = findViewById(R.id.tvTimer);
        tvDistance = findViewById(R.id.tvDistance);
        tvPickup = findViewById(R.id.tvPickup);
        tvDetails = findViewById(R.id.tvDetails);
        btnAccept = findViewById(R.id.btnAccept);
        btnReject = findViewById(R.id.btnReject);

        if ("TRANSPORT".equals(serviceType)) {
            tvTitle.setText("New Transport Request");
        } else {
            tvTitle.setText("New Machinery Request");
        }

        btnAccept.setOnClickListener(v -> acceptRequest());
        btnReject.setOnClickListener(v -> rejectRequest("Driver Rejected"));
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (tvTimer != null) {
                    tvTimer.setText(String.valueOf(millisUntilFinished / 1000));
                }
            }

            @Override
            public void onFinish() {
                if (!isFinished) {
                    rejectRequest("Timeout");
                }
            }
        }.start();
    }

    private void loadRequestDetails() {
        String collection = "TRANSPORT".equals(serviceType) ? "transport_requests" : "machinery_bookings";
        
        db.collection(collection).document(requestId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String farmerName = doc.getString("farmerName");
                Double lat, lng;
                
                if ("TRANSPORT".equals(serviceType)) {
                    String sourceObj = doc.getString("sourceAddress");
                    Double weight = doc.getDouble("weight");
                    String crop = doc.getString("cropType");
                    String toL = doc.getString("destAddress");
                    Double estimatedPrice = doc.getDouble("estimatedPrice");
                    
                    tvPickup.setText("Pickup: " + (sourceObj != null ? sourceObj : "Location"));
                    
                    StringBuilder details = new StringBuilder();
                    details.append("Drop: ").append(toL != null ? toL : "N/A");
                    details.append("\nCrop: ").append(crop != null ? crop : "N/A");
                    details.append(" | ").append(weight != null ? String.format("%.0f", weight) : "0").append("kg");
                    if (estimatedPrice != null && estimatedPrice > 0) {
                        details.append("\n\uD83D\uDCB0 Estimated Earnings: ₹").append(String.format("%.0f", estimatedPrice));
                    }
                    tvDetails.setText(details.toString());
                    
                    lat = doc.getDouble("sourceLat");
                    lng = doc.getDouble("sourceLng");
                } else {
                    String machType = doc.getString("machineryType");
                    String landSize = doc.getString("landSize");
                    tvPickup.setText("Farmer: " + farmerName);
                    tvDetails.setText("Type: " + machType + " | Size: " + landSize);
                    
                    lat = doc.getDouble("farmerLat");
                    lng = doc.getDouble("farmerLng");
                }
                
                if (lat != null && lng != null) {
                    targetLocation = new LatLng(lat, lng);
                    updateMap();
                    
                    // Calculate real-time distance from driver's current location
                    calculateLiveDistance(lat, lng);
                }
                
                // Get pre-calculated distance parameter
                Double dist = doc.getDouble("distance");
                if (dist != null) {
                     tvDistance.setText(String.format("%.1f km away", dist));
                }
            } else {
                rejectRequest("Invalid Request");
            }
        });
    }
    
    private void calculateLiveDistance(double farmerLat, double farmerLng) {
        // Try to get driver's current location from Firestore for accurate distance
        if (uid != null && !uid.isEmpty()) {
            String provCol = "TRANSPORT".equals(serviceType) ? "drivers" : "machinery_providers";
            db.collection(provCol).document(uid).get().addOnSuccessListener(driverDoc -> {
                if (driverDoc.exists()) {
                    Double dLat = driverDoc.getDouble("currentLat");
                    Double dLng = driverDoc.getDouble("currentLng");
                    if (dLat != null && dLng != null) {
                        float[] results = new float[1];
                        android.location.Location.distanceBetween(dLat, dLng, farmerLat, farmerLng, results);
                        float distKm = results[0] / 1000f;
                        tvDistance.setText(String.format("%.1f km away", distKm));
                    }
                }
            });
        }
    }

    private void acceptRequest() {
        if (isFinished) return;
        isFinished = true;
        if (countDownTimer != null) countDownTimer.cancel();

        btnAccept.setEnabled(false);
        btnReject.setEnabled(false);
        btnAccept.setText("Accepting...");

        String collection = "TRANSPORT".equals(serviceType) ? "transport_requests" : "machinery_bookings";
        String assignedField = "TRANSPORT".equals(serviceType) ? "assignedDriverId" : "assignedProviderId";
        DocumentReference docRef = db.collection(collection).document(requestId);

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(docRef);
            String status = snapshot.getString("status");
            String assignedId = snapshot.getString(assignedField);

            // Ensure no one else took it and we are the assigned one, or it's an open request
            if ("REQUESTED".equals(status) && (uid.equals(assignedId) || assignedId == null)) {
                transaction.update(docRef, "status", "ACCEPTED");
                
                if ("TRANSPORT".equals(serviceType)) {
                    transaction.update(docRef, "driverId", uid);
                    transaction.update(docRef, "assignedDriverId", uid);
                } else {
                    transaction.update(docRef, "providerId", uid);
                    transaction.update(docRef, "assignedProviderId", uid);
                }
                
                // Also update the provider to BUSY
                String provCol = "TRANSPORT".equals(serviceType) ? "drivers" : "machinery_providers";
                DocumentReference provRef = db.collection(provCol).document(uid);
                transaction.update(provRef, "isAvailable", false);
                transaction.update(provRef, "status", "BUSY");
                
                return null;
            } else {
                throw new FirebaseFirestoreException("Request already handled or changed",
                        FirebaseFirestoreException.Code.ABORTED);
            }
        }).addOnSuccessListener(aVoid -> {
            Toast.makeText(this, "Accepted successfully!", Toast.LENGTH_SHORT).show();
            
            // Go to Tracking
            Intent intent;
            if ("TRANSPORT".equals(serviceType)) {
                intent = new Intent(this, TrackingActivity.class);
            } else {
                intent = new Intent(this, MachineryTrackingActivity.class);
            }
            intent.putExtra("REQUEST_ID", requestId); // Unified or just pass what they expect
            intent.putExtra("BOOKING_ID", requestId); // Machinery expects BOOKING_ID
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed: Request took by another or expired.", Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void rejectRequest(String reason) {
        if (isFinished) return;
        isFinished = true;
        if (countDownTimer != null) countDownTimer.cancel();

        Log.d(TAG, "Rejecting request. Reason: " + reason);
        btnAccept.setEnabled(false);
        btnReject.setEnabled(false);

        String collection = "TRANSPORT".equals(serviceType) ? "transport_requests" : "machinery_bookings";
        
        // We do a simple update to set status to REJECTED. The farmer's listener will pick it up and assign the next driver in the queue.
        db.collection(collection).document(requestId).update("status", "REJECTED")
            .addOnCompleteListener(task -> {
                finish();
            });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setAllGesturesEnabled(false);
        updateMap();
    }

    private void updateMap() {
        if (mMap != null && targetLocation != null) {
            mMap.addMarker(new MarkerOptions().position(targetLocation).title("Target"));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(targetLocation, 14f));
        }
    }
    
    @Override
    public void onBackPressed() {
        // Prevent back button from escaping without an explicit accept/reject
        // Or treat it as reject
        rejectRequest("User pressed back");
        super.onBackPressed();
    }
}
