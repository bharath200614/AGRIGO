package com.agrigo.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LaborBookingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ImageView btnBack;
    private Spinner spinnerWorkType;
    private EditText etWorkers, etDuration;
    private TextView tvEstimatedPrice;
    private MaterialButton btnSubmitRequest;
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
    private ListenerRegistration requestListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_labor_booking);

        db = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupMap();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        spinnerWorkType = findViewById(R.id.spinnerWorkType);
        etWorkers = findViewById(R.id.etWorkers);
        etDuration = findViewById(R.id.etDuration);
        tvEstimatedPrice = findViewById(R.id.tvEstimatedPrice);
        btnSubmitRequest = findViewById(R.id.btnSubmitRequest);
        
        bottomSheet = findViewById(R.id.bottomSheet);
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        
        tvLocationAddress = findViewById(R.id.tvLocationAddress);
        pbLocationProgress = findViewById(R.id.pbLocationProgress);

        // Populate Spinner
        String[] jobs = {"Harvesting", "Planting", "Cleaning", "Weeding"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, jobs);
        spinnerWorkType.setAdapter(adapter);

        TextWatcher priceCalculator = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateEstimateAndValidate();
            }
            @Override public void afterTextChanged(Editable s) {}
        };
        etWorkers.addTextChangedListener(priceCalculator);
        etDuration.addTextChangedListener(priceCalculator);
        
        // Fetch wage when work type changes
        spinnerWorkType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                fetchAvgWageForWorkType(jobs[position]);
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private double fetchedAvgWage = 500.0; // Default ₹500/day
    
    private void calculateEstimateAndValidate() {
        String workersStr = etWorkers.getText().toString().trim();
        String durationStr = etDuration.getText().toString().trim();
        
        boolean isValid = !workersStr.isEmpty() && !durationStr.isEmpty() && pickupLatLng != null;
        btnSubmitRequest.setEnabled(isValid);
        
        if (isValid) {
            try {
                int workers = Integer.parseInt(workersStr);
                int duration = Integer.parseInt(durationStr);
                double estimatedTotal = workers * duration * fetchedAvgWage;
                tvEstimatedPrice.setText(String.format(Locale.getDefault(), "₹%.0f", estimatedTotal));
            } catch (NumberFormatException e) {
                tvEstimatedPrice.setText("₹0");
                btnSubmitRequest.setEnabled(false);
            }
        } else {
            tvEstimatedPrice.setText("₹0");
        }
    }
    
    private void fetchAvgWageForWorkType(String workType) {
        db.collection("labor_workers")
                .whereEqualTo("workType", workType.toLowerCase().trim())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    double totalWage = 0;
                    int count = 0;
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        Double wage = doc.getDouble("dailyWage");
                        if (wage != null && wage > 0) {
                            totalWage += wage;
                            count++;
                        }
                    }
                    if (count > 0) {
                        fetchedAvgWage = totalWage / count;
                    } else {
                        fetchedAvgWage = 500.0; // Default if no workers have set wages
                    }
                    calculateEstimateAndValidate();
                });
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

        btnSubmitRequest.setOnClickListener(v -> submitLaborRequest());
    }

    private void submitLaborRequest() {
        String rawWorkType = spinnerWorkType.getSelectedItem().toString();
        String workType = rawWorkType.toLowerCase().trim();
        String workersStr = etWorkers.getText().toString().trim();
        String durationStr = etDuration.getText().toString().trim();
        String estimatedPrice = tvEstimatedPrice.getText().toString();

        if (workersStr.isEmpty() || durationStr.isEmpty() || pickupLatLng == null) {
            ToastUtils.showShort(this, "Please complete the form");
            return;
        }

        int workersRequired = Integer.parseInt(workersStr);
        String farmerId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : preferenceManager.getUserId();

        Map<String, Object> booking = new HashMap<>();
        booking.put("farmerId", farmerId);
        booking.put("farmerName", preferenceManager.getUserName());
        booking.put("farmerLat", pickupLatLng.latitude);
        booking.put("farmerLng", pickupLatLng.longitude);
        booking.put("address", currentResolvedAddress);
        booking.put("workType", workType);
        booking.put("workersRequired", workersRequired);
        booking.put("workersAccepted", 0);
        booking.put("assignedWorkers", new java.util.ArrayList<String>());
        
        android.util.Log.d("LaborBooking", "Broadcasting request for " + workersRequired + " workers of type: " + workType);
        booking.put("duration", durationStr);
        booking.put("estimatedPrice", estimatedPrice);
        booking.put("status", "REQUESTED");
        booking.put("timestamp", System.currentTimeMillis());

        btnSubmitRequest.setEnabled(false);
        db.collection("labor_bookings").add(booking)
                .addOnSuccessListener(documentReference -> {
                    ToastUtils.showShort(this, "Labor Request Broadcasted Successfully!");
                    
                    // Route back to Dashboard where Active Jobs Feed will show it
                    Intent intent = new Intent(this, FarmerDashboardActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmitRequest.setEnabled(true);
                    ToastUtils.showShort(this, "Failed to submit request: " + e.getMessage());
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestListener != null) requestListener.remove();
    }
}
