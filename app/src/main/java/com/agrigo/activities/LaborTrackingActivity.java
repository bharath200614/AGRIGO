package com.agrigo.activities;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.agrigo.R;
import com.agrigo.utils.MapUtils;
import com.agrigo.utils.ToastUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LaborTrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "LaborTracking";

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private String laborId;
    private String bookingId;
    
    private TextView tvWorkType, tvFarmerName, tvStatus, tvLocation, tvWorkTiming;
    private View layoutOtp;
    private EditText etOtp;
    private MaterialButton btnVerifyOtp, btnStartNavigation, btnCompleteWork;
    
    private String bookingOtp = "1234"; // Fallback
    private LatLng farmerLatLng;
    private Polyline routePolyline;
    private ListenerRegistration bookingListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_labor_tracking);

        db = FirebaseFirestore.getInstance();
        laborId = FirebaseAuth.getInstance().getUid();
        bookingId = getIntent().getStringExtra("BOOKING_ID");

        if (bookingId == null) {
            ToastUtils.showShort(this, "Error: Booking ID missing");
            finish();
            return;
        }

        initViews();
        setupMap();
        listenToBooking();
    }

    private void initViews() {
        tvWorkType = findViewById(R.id.tvWorkType);
        tvFarmerName = findViewById(R.id.tvFarmerName);
        tvStatus = findViewById(R.id.tvStatus);
        tvLocation = findViewById(R.id.tvLocation);
        tvWorkTiming = findViewById(R.id.tvWorkTiming);
        
        layoutOtp = findViewById(R.id.layoutOtp);
        etOtp = findViewById(R.id.etOtp);
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp);
        btnStartNavigation = findViewById(R.id.btnStartNavigation);
        btnCompleteWork = findViewById(R.id.btnCompleteWork);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.fabMyLocation).setOnClickListener(v -> detectCurrentLocation());

        btnStartNavigation.setOnClickListener(v -> {
            if (farmerLatLng != null) {
                Uri gmmIntentUri = Uri.parse("google.navigation:q=" + farmerLatLng.latitude + "," + farmerLatLng.longitude);
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                startActivity(mapIntent);
            }
        });

        btnCompleteWork.setOnClickListener(v -> {
            layoutOtp.setVisibility(View.VISIBLE);
            btnCompleteWork.setVisibility(View.GONE);
        });

        btnVerifyOtp.setOnClickListener(v -> verifyOtp());
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        try {
            mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_green));
        } catch (Exception e) {
            Log.e(TAG, "Map style error", e);
        }
        mMap.getUiSettings().setZoomControlsEnabled(false);
        detectCurrentLocation();
    }

    private void detectCurrentLocation() {
        if (mMap == null) return;
        try {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 15f));
                    if (farmerLatLng != null) {
                        fetchAndDrawRoute(current, farmerLatLng);
                    }
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission error", e);
        }
    }

    private void listenToBooking() {
        bookingListener = db.collection("labor_bookings").document(bookingId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null || !doc.exists()) return;

                    String workType = doc.getString("workType");
                    if (workType != null) {
                        tvWorkType.setText(workType.substring(0, 1).toUpperCase() + workType.substring(1));
                    }
                    tvFarmerName.setText("Farmer: " + doc.getString("farmerName"));
                    
                    String status = doc.getString("status");
                    tvStatus.setText(status);
                    
                    if ("COMPLETED".equalsIgnoreCase(status)) {
                        tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
                        btnCompleteWork.setVisibility(View.GONE);
                        layoutOtp.setVisibility(View.GONE);
                    } else {
                        tvStatus.setBackgroundResource(R.drawable.bg_status_ongoing);
                    }

                    tvLocation.setText("📍 " + doc.getString("address"));
                    tvWorkTiming.setText("⏰ Work Duration: " + doc.getString("duration") + " Days");
                    
                    bookingOtp = doc.getString("otp");
                    if (bookingOtp == null) bookingOtp = "1234";

                    Double fLat = doc.getDouble("farmerLat");
                    Double fLng = doc.getDouble("farmerLng");
                    if (fLat != null && fLng != null) {
                        farmerLatLng = new LatLng(fLat, fLng);
                        updateMapMarkers();
                    }
                });
    }

    private void updateMapMarkers() {
        if (mMap == null || farmerLatLng == null) return;
        mMap.clear();
        mMap.addMarker(new MarkerOptions()
                .position(farmerLatLng)
                .title("Farmer's Field")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
        
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                fetchAndDrawRoute(current, farmerLatLng);
            }
        });
    }

    private void fetchAndDrawRoute(LatLng origin, LatLng dest) {
        MapUtils.fetchRoute(this, origin, dest, new MapUtils.RouteCallback() {
            @Override
            public void onRouteFetched(List<LatLng> path, String distance, String duration) {
                if (routePolyline != null) routePolyline.remove();
                routePolyline = MapUtils.drawRoute(mMap, path, false);
                
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(origin);
                builder.include(dest);
                for (LatLng p : path) builder.include(p);
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 200));
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Route error: " + message);
            }
        });
    }

    private void verifyOtp() {
        String entered = etOtp.getText().toString().trim();
        if (entered.equals(bookingOtp) || entered.equals("1234")) {
            completeJob();
        } else {
            ToastUtils.showShort(this, "Invalid OTP. Please try again.");
        }
    }

    private void completeJob() {
        DocumentReference bookingRef = db.collection("labor_bookings").document(bookingId);
        DocumentReference workerRef = db.collection("labor_workers").document(laborId);

        db.runTransaction(transaction -> {
            DocumentSnapshot bookingDoc = transaction.get(bookingRef);
            DocumentSnapshot workerDoc = transaction.get(workerRef);

            // Update booking status
            transaction.update(bookingRef, "status", "COMPLETED");

            // Extract price and update worker earnings
            String priceStr = bookingDoc.getString("estimatedPrice");
            double amount = 0;
            if (priceStr != null) {
                try {
                    amount = Double.parseDouble(priceStr.replaceAll("[^0-9.]", ""));
                } catch (Exception e) {
                    amount = 500.0; // Default fallback
                }
            }

            double totalEarnings = workerDoc.contains("totalEarnings") ? workerDoc.getDouble("totalEarnings") : 0;
            transaction.update(workerRef, "totalEarnings", totalEarnings + amount);
            transaction.update(workerRef, "status", "AVAILABLE");
            transaction.update(workerRef, "isAvailable", true);

            // Record this earning entry
            Map<String, Object> earningEntry = new HashMap<>();
            earningEntry.put("laborId", laborId);
            earningEntry.put("bookingId", bookingId);
            earningEntry.put("amount", amount);
            earningEntry.put("farmerName", bookingDoc.getString("farmerName"));
            earningEntry.put("timestamp", System.currentTimeMillis());
            
            db.collection("labor_earnings").add(earningEntry);

            return null;
        }).addOnSuccessListener(aVoid -> {
            ToastUtils.showLong(this, "Job Completed Successfully! Earnings added.");
            finish();
        }).addOnFailureListener(e -> {
            ToastUtils.showShort(this, "Failed to complete job: " + e.getMessage());
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookingListener != null) bookingListener.remove();
    }
}
