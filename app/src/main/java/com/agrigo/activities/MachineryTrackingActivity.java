package com.agrigo.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.agrigo.R;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.auth.FirebaseAuth;

public class MachineryTrackingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private ImageView btnBack;
    private TextView tvFarmerName, tvJobDetails;
    private MaterialButton btnUpdateStatus, btnNavigateMaps;

    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore db;
    private ListenerRegistration trackingListener;

    private String bookingId;
    private String currentStatus = "ACCEPTED";
    private LatLng farmerLocation;
    private LatLng providerLocation;

    private Marker farmerMarker, providerMarker;
    
    private com.agrigo.utils.PreferenceManager preferenceManager;
    private boolean isProvider;
    private String currentUserId;
    private String assignedProviderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_machinery_tracking);

        bookingId = getIntent().getStringExtra("BOOKING_ID");

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        preferenceManager = new com.agrigo.utils.PreferenceManager(this);
        isProvider = "machinery_provider".equalsIgnoreCase(preferenceManager.getUserRole());
        currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : preferenceManager.getUserId();

        initViews();
        setupMap();
        
        if (bookingId != null) {
            listenToBooking();
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        tvFarmerName = findViewById(R.id.tvFarmerName);
        tvJobDetails = findViewById(R.id.tvJobDetails);
        btnUpdateStatus = findViewById(R.id.btnUpdateStatus);
        btnNavigateMaps = findViewById(R.id.btnNavigateMaps);

        btnUpdateStatus.setOnClickListener(v -> handleStatusUpdate());
        btnNavigateMaps.setOnClickListener(v -> {
            if (farmerLocation != null) {
                android.net.Uri gmmIntentUri = android.net.Uri.parse("google.navigation:q=" + farmerLocation.latitude + "," + farmerLocation.longitude);
                android.content.Intent mapIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                if (mapIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(mapIntent);
                } else {
                    ToastUtils.showShort(this, "Google Maps is not installed");
                }
            } else {
                ToastUtils.showShort(this, "Destination location not loaded yet");
            }
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
        updateLocation();
    }

    private void listenToBooking() {
        DocumentReference docRef = db.collection("machinery_bookings").document(bookingId);
        trackingListener = docRef.addSnapshotListener((snapshot, error) -> {
            if (error != null || snapshot == null || !snapshot.exists()) return;

            tvFarmerName.setText(snapshot.getString("farmerName"));
            tvJobDetails.setText(snapshot.getString("machineryType") + " • " + snapshot.getString("duration") + " Hours");
            
            Double fLat = snapshot.getDouble("farmerLat");
            Double fLng = snapshot.getDouble("farmerLng");
            if (fLat != null && fLng != null && farmerLocation == null) {
                farmerLocation = new LatLng(fLat, fLng);
                drawMarkers();
            }

            assignedProviderId = snapshot.getString("assignedProviderId");
            currentStatus = snapshot.getString("status");
            updateButtonState();
        });
    }

    private void updateButtonState() {
        if (!isProvider) {
            btnUpdateStatus.setVisibility(android.view.View.GONE);
            return;
        }

        switch (currentStatus) {
            case "ACCEPTED":
                btnUpdateStatus.setText("I Have Arrived");
                btnUpdateStatus.setBackgroundColor(Color.parseColor("#2E7D32"));
                btnUpdateStatus.setEnabled(true);
                break;
            case "ARRIVED":
                btnUpdateStatus.setText("Start Work");
                btnUpdateStatus.setBackgroundColor(Color.parseColor("#16A34A"));
                btnUpdateStatus.setEnabled(true);
                break;
            case "WORK_STARTED":
                btnUpdateStatus.setText("Complete Work");
                btnUpdateStatus.setBackgroundColor(Color.parseColor("#16A34A"));
                btnUpdateStatus.setEnabled(true);
                break;
            case "WORK_COMPLETED":
                btnUpdateStatus.setText("Job Completed");
                btnUpdateStatus.setBackgroundColor(Color.parseColor("#9E9E9E"));
                btnUpdateStatus.setEnabled(false);
                ToastUtils.showShort(this, "Job Finished Successfully!");
                break;
        }
    }

    private void handleStatusUpdate() {
        if ("ARRIVED".equals(currentStatus)) {
            // Need OTP to start work
            final android.widget.EditText input = new android.widget.EditText(this);
            input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            input.setHint("Enter 4-digit OTP provided by Farmer");

            new android.app.AlertDialog.Builder(this)
                .setTitle("Verify OTP")
                .setMessage("Please enter the OTP to start the job.")
                .setView(input)
                .setPositiveButton("Verify", (dialog, which) -> {
                    String enteredOtp = input.getText().toString().trim();
                    if(enteredOtp.isEmpty()) {
                        ToastUtils.showShort(this, "OTP cannot be empty");
                        return;
                    }
                    verifyOtpAndStartJob(enteredOtp);
                })
                .setNegativeButton("Cancel", null)
                .show();
        } else {
            String nextStatus = "ACCEPTED";
            if ("ACCEPTED".equals(currentStatus)) nextStatus = "ARRIVED";
            // WORK_STARTED is handled by OTP verify
            else if ("WORK_STARTED".equals(currentStatus)) nextStatus = "WORK_COMPLETED";

            final String finalNextStatus = nextStatus;

            btnUpdateStatus.setEnabled(false);
            db.collection("machinery_bookings").document(bookingId)
                    .update("status", finalNextStatus)
                    .addOnSuccessListener(aVoid -> {
                        if ("WORK_COMPLETED".equals(finalNextStatus) && isProvider && assignedProviderId != null) {
                            // Free up the provider
                            java.util.Map<String, Object> provUpdates = new java.util.HashMap<>();
                            provUpdates.put("status", "FREE");
                            provUpdates.put("isAvailable", true);
                            db.collection("machinery_providers").document(assignedProviderId).update(provUpdates);
                        }
                    })
                    .addOnFailureListener(e -> {
                        btnUpdateStatus.setEnabled(true);
                        ToastUtils.showShort(this, "Failed to update status");
                    });
        }
    }

    private void verifyOtpAndStartJob(String enteredOtp) {
        btnUpdateStatus.setEnabled(false);
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(db.collection("machinery_bookings").document(bookingId));
            String validOtp = snapshot.getString("otp");
            if (validOtp != null && validOtp.equals(enteredOtp)) {
                transaction.update(db.collection("machinery_bookings").document(bookingId), "status", "WORK_STARTED");
                return null;
            } else {
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                    "Invalid OTP", com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED
                );
            }
        }).addOnSuccessListener(result -> {
            ToastUtils.showShort(this, "OTP Verified. Work Started!");
        }).addOnFailureListener(e -> {
            btnUpdateStatus.setEnabled(true);
            ToastUtils.showShort(this, "Verification failed: " + e.getMessage());
        });
    }

    private void updateLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                providerLocation = new LatLng(location.getLatitude(), location.getLongitude());
                drawMarkers();
            }
        });
    }

    private com.google.android.gms.maps.model.Polyline routePolyline;

    private void drawMarkers() {
        if (mMap == null) return;
        
        mMap.clear();
        routePolyline = null;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        if (farmerLocation != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(farmerLocation)
                    .title("Farmer Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            builder.include(farmerLocation);
        }

        if (providerLocation != null) {
            mMap.addMarker(new MarkerOptions()
                    .position(providerLocation)
                    .title("My Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            builder.include(providerLocation);
        }

        // Draw road-following route between provider and farmer
        if (farmerLocation != null && providerLocation != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
            
            com.agrigo.utils.MapUtils.fetchRoute(this, providerLocation, farmerLocation, new com.agrigo.utils.MapUtils.RouteCallback() {
                @Override
                public void onRouteFetched(java.util.List<LatLng> path, String distance, String duration) {
                    if (mMap == null) return;
                    if (routePolyline != null) routePolyline.remove();
                    routePolyline = com.agrigo.utils.MapUtils.drawRoute(mMap, path, false);
                    
                    // Show distance and ETA in job details
                    if (tvJobDetails != null) {
                        String currentText = tvJobDetails.getText().toString();
                        tvJobDetails.setText(currentText + " • " + distance + " • ETA: " + duration);
                    }
                }

                @Override
                public void onError(String message) {
                    Log.w("MachineryTracking", "Route fetch error: " + message);
                    // Fallback: draw straight line
                    if (mMap != null) {
                        routePolyline = mMap.addPolyline(new com.google.android.gms.maps.model.PolylineOptions()
                                .add(providerLocation, farmerLocation)
                                .width(12)
                                .color(android.graphics.Color.parseColor("#16A34A")));
                    }
                }
            });
        } else if (providerLocation != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(providerLocation, 15f));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (trackingListener != null) trackingListener.remove();
    }
}
