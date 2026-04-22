package com.agrigo.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.agrigo.R;
import com.agrigo.utils.PreferenceManager;
import com.agrigo.utils.ToastUtils;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class MachineryBookingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ImageView btnBack;
    private Spinner spinnerMachineryType;
    private EditText etDuration, etLandSize;
    private TextView tvEstimatedPrice, tvDispatchPayload;
    private MaterialButton btnFindMachines, btnCancelDispatch;
    private LinearLayout layoutBookingForm, layoutDispatching;
    private View bottomSheet;
    private BottomSheetBehavior<View> bottomSheetBehavior;
    private TextView tvLocationAddress;
    private android.widget.ProgressBar pbLocationProgress;
    private String currentResolvedAddress = "";

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;

    private LatLng pickupLatLng;

    // Search Radius in meters (25 km approx)
    private static final double SEARCH_RADIUS = 25000;
    
    private ListenerRegistration dispatchListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_machinery_booking);

        db = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupMap();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        spinnerMachineryType = findViewById(R.id.spinnerMachineryType);
        etDuration = findViewById(R.id.etDuration);
        etLandSize = findViewById(R.id.etLandSize);
        tvEstimatedPrice = findViewById(R.id.tvEstimatedPrice);
        btnFindMachines = findViewById(R.id.btnFindMachines);
        
        layoutBookingForm = findViewById(R.id.layoutBookingForm);
        layoutDispatching = findViewById(R.id.layoutDispatching);
        tvDispatchPayload = findViewById(R.id.tvDispatchPayload);
        btnCancelDispatch = findViewById(R.id.btnCancelDispatch);
        
        bottomSheet = findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        
        tvLocationAddress = findViewById(R.id.tvLocationAddress);
        pbLocationProgress = findViewById(R.id.pbLocationProgress);

        // Populate Spinner
        String[] machines = {"Harvester", "Sprayer", "Tractor", "Cultivator"};
        int[] icons = {
                R.drawable.ic_harvester,
                R.drawable.ic_sprayer,
                R.drawable.ic_tractor,
                R.drawable.ic_cultivator
        };
        com.agrigo.adapters.MachinerySpinnerAdapter adapter = new com.agrigo.adapters.MachinerySpinnerAdapter(this, machines, icons);
        spinnerMachineryType.setAdapter(adapter);
        
        TextWatcher priceCalculator = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateEstimateAndValidate();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etDuration.addTextChangedListener(priceCalculator);
        etLandSize.addTextChangedListener(priceCalculator);
    }
    
    private void calculateEstimateAndValidate() {
        String durationStr = etDuration.getText().toString().trim();
        String sizeStr = etLandSize.getText().toString().trim();
        
        boolean isValid = !durationStr.isEmpty() && !sizeStr.isEmpty() && pickupLatLng != null;
        btnFindMachines.setEnabled(isValid);
        
        if (isValid) {
            try {
                double duration = Double.parseDouble(durationStr);
                double size = Double.parseDouble(sizeStr);
                double rate = 50.0; // Mock base rate per hour per acre
                double estimatedTotal = duration * size * rate;
                tvEstimatedPrice.setText(String.format(Locale.getDefault(), "₹%.2f", estimatedTotal));
            } catch (NumberFormatException e) {
                tvEstimatedPrice.setText("₹0.00");
                btnFindMachines.setEnabled(false);
            }
        } else {
            tvEstimatedPrice.setText("₹0.00");
        }
    }

    private void setupMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setMyLocationButtonEnabled(false);

        mMap.setOnCameraIdleListener(() -> {
            LatLng center = mMap.getCameraPosition().target;
            setPickupLocation(center);
        });

        detectCurrentLocation();
    }

    private void detectCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 16f));
            } else {
                // Fallback location
                LatLng fallback = new LatLng(17.3850, 78.4867); // Hyderabad
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(fallback, 12f));
            }
        });
    }

    private void setPickupLocation(LatLng latLng) {
        pickupLatLng = latLng;
        calculateEstimateAndValidate();
        
        if (tvLocationAddress != null) {
            tvLocationAddress.setText("Locating...");
            pbLocationProgress.setVisibility(View.VISIBLE);
            
            com.agrigo.utils.GeocodingUtils.getAddressFromLatLng(this, pickupLatLng, address -> {
                currentResolvedAddress = address;
                tvLocationAddress.setText(address);
                pbLocationProgress.setVisibility(View.GONE);
            });
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        findViewById(R.id.fabMyLocation).setOnClickListener(v -> detectCurrentLocation());

        btnCancelDispatch.setOnClickListener(v -> cancelDispatch());

        btnFindMachines.setOnClickListener(v -> {
            String duration = etDuration.getText().toString().trim();
            String landSize = etLandSize.getText().toString().trim();
            if (duration.isEmpty() || landSize.isEmpty() || pickupLatLng == null) {
                ToastUtils.showShort(this, "Please enter all valid details");
                return;
            }
            findEligibleMachines(spinnerMachineryType.getSelectedItem().toString(), duration, landSize);
        });
    }

    private void cancelDispatch() {
        layoutDispatching.setVisibility(View.GONE);
        layoutBookingForm.setVisibility(View.VISIBLE);
        ToastUtils.showShort(this, "Request Cancelled");
        // Typically you'd also delete the firestore document here if it was created
    }

    private void findEligibleMachines(String machineryType, String duration, String landSize) {
        // Normalize machinery type to lowercase for consistent matching
        String normalizedType = machineryType.toLowerCase().trim();
        
        // Prepare UI state
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        layoutBookingForm.setVisibility(View.GONE);
        layoutDispatching.setVisibility(View.VISIBLE);
        tvDispatchPayload.setText(machineryType + " | " + duration + " Hrs | " + tvEstimatedPrice.getText().toString());

        android.util.Log.d("MachineryBooking", "🔍 Searching for machineryType=" + normalizedType);

        // 1. Fetch available machines of this type
        GeoLocation searchCenter = new GeoLocation(pickupLatLng.latitude, pickupLatLng.longitude);
        List<com.firebase.geofire.GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(searchCenter, SEARCH_RADIUS);
        List<com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot>> tasks = new java.util.ArrayList<>();

        for (com.firebase.geofire.GeoQueryBounds b : bounds) {
            com.google.firebase.firestore.Query q = db.collection("machinery_providers")
                    .whereEqualTo("machineryType", normalizedType)
                    .whereEqualTo("status", "FREE")
                    .orderBy("geoHash")
                    .startAt(b.startHash)
                    .endAt(b.endHash);
            tasks.add(q.get());
        }

        com.google.android.gms.tasks.Tasks.whenAllComplete(tasks).addOnCompleteListener(t -> {
            List<com.google.firebase.firestore.DocumentSnapshot> eligibleDocs = new ArrayList<>();
            android.util.Log.d("MachineryBooking", "=== MACHINERY MATCHING DEBUG ===");
            android.util.Log.d("MachineryBooking", "1. Farmer location: " + searchCenter.latitude + ", " + searchCenter.longitude);

            for (com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task : tasks) {
                if (task.isSuccessful() && task.getResult() != null) {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : task.getResult()) {
                        Double lat = doc.getDouble("currentLat");
                        Double lng = doc.getDouble("currentLng");
                        String provType = doc.getString("machineryType");
                        String provStatus = doc.getString("status");
                        
                        if (lat != null && lng != null) {
                            GeoLocation machineLoc = new GeoLocation(lat, lng);
                            double distanceInMeters = GeoFireUtils.getDistanceBetween(searchCenter, machineLoc);
                            
                            android.util.Log.d("MachineryBooking", "2. Provider location: " + lat + ", " + lng + " (" + doc.getId() + ")");
                            android.util.Log.d("MachineryBooking", "3. Distance between them: " + (distanceInMeters/1000) + " km");

                            if (distanceInMeters <= SEARCH_RADIUS) {
                                eligibleDocs.add(doc);
                                android.util.Log.d("MachineryBooking", "  ✅ ELIGIBLE (Within 25km radius) Type: " + provType + " Status: " + provStatus);
                            } else {
                                android.util.Log.d("MachineryBooking", "  ❌ INELIGIBLE (Too far)");
                            }
                        } else {
                            android.util.Log.d("MachineryBooking", "  ❌ INELIGIBLE (Provider location is null)");
                        }
                    }
                }
            }

            android.util.Log.d("MachineryBooking", "4. Number of providers found: " + eligibleDocs.size());
            android.util.Log.d("MachineryBooking", "=================================");

            if (eligibleDocs.isEmpty()) {
                ToastUtils.showShort(this, "No specific machinery found nearby. Creating an open request.");
            }
            
            // Sort by distance
            java.util.Collections.sort(eligibleDocs, (d1, d2) -> {
                double dist1 = GeoFireUtils.getDistanceBetween(searchCenter, new GeoLocation(d1.getDouble("currentLat"), d1.getDouble("currentLng")));
                double dist2 = GeoFireUtils.getDistanceBetween(searchCenter, new GeoLocation(d2.getDouble("currentLat"), d2.getDouble("currentLng")));
                return Double.compare(dist1, dist2);
            });
            
            List<String> eligibleIds = new ArrayList<>();
            for (com.google.firebase.firestore.DocumentSnapshot d : eligibleDocs) {
                eligibleIds.add(d.getId());
            }
            
            createBooking(machineryType, duration, landSize, tvEstimatedPrice.getText().toString(), eligibleIds);
        });
    }

    private void createBooking(String machineryType, String duration, String landSize, String estimatedPrice, List<String> eligibleMachineIds) {
        String farmerId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : preferenceManager.getUserId();

        Map<String, Object> booking = new HashMap<>();
        booking.put("farmerId", farmerId);
        booking.put("farmerName", preferenceManager.getUserName());
        booking.put("farmerLat", pickupLatLng.latitude);
        booking.put("farmerLng", pickupLatLng.longitude);
        booking.put("address", currentResolvedAddress);
        booking.put("machineryType", machineryType.toLowerCase().trim());
        booking.put("landSize", landSize);
        booking.put("duration", duration);
        booking.put("estimatedPrice", estimatedPrice);
        booking.put("status", "REQUESTED");
        booking.put("providerQueue", eligibleMachineIds);
        booking.put("currentProviderIndex", 0);
        booking.put("assignedProviderId", eligibleMachineIds.isEmpty() ? null : eligibleMachineIds.get(0));
        booking.put("timestamp", System.currentTimeMillis());
        
        // Generate 4 digit OTP for secure handshake
        String otp = String.format(Locale.getDefault(), "%04d", new Random().nextInt(10000));
        booking.put("otp", otp);

        db.collection("machinery_bookings").add(booking)
                .addOnSuccessListener(documentReference -> {
                    ToastUtils.showShort(this, "Searching for nearby providers...");
                    
                    // Listen to document to see if a provider accepts
                    dispatchListener = documentReference.addSnapshotListener((snapshot, error) -> {
                         if (error == null && snapshot != null && snapshot.exists()) {
                             String status = snapshot.getString("status");
                             if ("ACCEPTED".equals(status)) {
                                 // Transition to Tracking!
                                 if (dispatchListener != null) dispatchListener.remove();
                                 Intent intent = new Intent(this, MachineryTrackingActivity.class);
                                 intent.putExtra("BOOKING_ID", documentReference.getId());
                                 startActivity(intent);
                                 finish(); // close booking activity
                             } else if ("REJECTED".equalsIgnoreCase(status)) {
                                 List<String> queue = (List<String>) snapshot.get("providerQueue");
                                 Long idxLong = snapshot.getLong("currentProviderIndex");
                                 if (queue != null && idxLong != null) {
                                     int nextIdx = idxLong.intValue() + 1;
                                     if (nextIdx < queue.size()) {
                                         Map<String, Object> bounceUpdates = new HashMap<>();
                                         bounceUpdates.put("currentProviderIndex", nextIdx);
                                         bounceUpdates.put("assignedProviderId", queue.get(nextIdx));
                                         bounceUpdates.put("status", "REQUESTED");
                                         db.collection("machinery_bookings").document(documentReference.getId()).update(bounceUpdates);
                                     } else {
                                         ToastUtils.showShort(this, "All nearby providers busy. Please try again later.");
                                         cancelDispatch();
                                     }
                                 }
                             }
                         }
                    });
                })
                .addOnFailureListener(e -> {
                    ToastUtils.showShort(this, "Error creating booking.");
                    cancelDispatch();
                });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dispatchListener != null) dispatchListener.remove();
    }
}
