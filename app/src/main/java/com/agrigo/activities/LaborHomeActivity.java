package com.agrigo.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agrigo.R;
import com.agrigo.adapters.LaborJobAdapter;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.agrigo.utils.GeocodingUtils;
import com.agrigo.utils.PreferenceManager;
import com.agrigo.utils.MapUtils;
import com.agrigo.utils.ToastUtils;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LaborHomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "LaborHomeAct";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;

    private SwitchMaterial switchOnline;
    private TextView tvStatusDescription;
    private RecyclerView recyclerViewJobs;
    private TextView textWelcome;
    private View layoutEmptyJobs;
    private View btnProfile;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;

    private LaborJobAdapter jobAdapter;
    private ListenerRegistration jobsListener;
    private String laborId;
    private String workerWorkType = "";

    // Location Tracking
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isReceivingLocationUpdates = false;
    private Location lastKnownLocation;
    private String locationMode = "AUTO"; // AUTO or MANUAL
    private GoogleMap mMap;
    private List<Marker> jobMarkers = new ArrayList<>();
    private Polyline routePolyline;


    
    private final ActivityResultLauncher<Intent> mapPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    double lat = result.getData().getDoubleExtra("lat", 0);
                    double lng = result.getData().getDoubleExtra("lng", 0);
                    
                    locationMode = "MANUAL";
                    Location newLoc = new Location("");
                    newLoc.setLatitude(lat);
                    newLoc.setLongitude(lng);
                    lastKnownLocation = newLoc;
                    
                    updateLocationInFirestore(lat, lng);
                    listenForJobs(); // Refresh jobs with new location
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_labor_home);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(this);
        
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        laborId = mAuth.getCurrentUser().getUid();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initializeViews();
        setupRecyclerView();
        initLocationCallback();
        
        fetchWorkerDetails();
        fetchCurrentStatus();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void initializeViews() {
        switchOnline = findViewById(R.id.switch_online);
        tvStatusDescription = findViewById(R.id.tv_status_description);
        recyclerViewJobs = findViewById(R.id.recyclerViewJobs);
        textWelcome = findViewById(R.id.text_welcome);
        layoutEmptyJobs = findViewById(R.id.layoutEmptyJobs);

        textWelcome.setText(preferenceManager.getUserName() != null ? preferenceManager.getUserName() : "Labour Partner");

        findViewById(R.id.btn_logout).setOnClickListener(v -> logout());

        // Profile button
        btnProfile = findViewById(R.id.btnProfile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                startActivity(new Intent(this, LaborWorkerProfileActivity.class));
            });
        }
        
        switchOnline.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateStatusText(isChecked);
            if (isChecked) {
                startLocationUpdates();
                if (findViewById(R.id.cardMap) != null) findViewById(R.id.cardMap).setVisibility(View.VISIBLE);
            } else {
                stopLocationUpdates();
                stopListeningForJobs();
                if (jobAdapter != null) jobAdapter.updateData(new ArrayList<>(), null);
                setEmptyState(false);
                if (findViewById(R.id.cardMap) != null) findViewById(R.id.cardMap).setVisibility(View.GONE);
                if (routePolyline != null) routePolyline.remove();
            }
        });


    }

    private void fetchWorkerDetails() {
        db.collection("labor_workers").document(laborId).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.getString("workType") != null && !doc.getString("workType").isEmpty()) {
                workerWorkType = doc.getString("workType").toLowerCase().trim();
                
                String mode = doc.getString("locationMode");
                if (mode != null && !mode.isEmpty()) {
                    locationMode = mode;
                }
                
                Log.d(TAG, "Worker type fetched: " + workerWorkType + " | Mode: " + locationMode);
                if (switchOnline.isChecked()) {
                    listenForJobs(); 
                }
            } else {
                Log.e(TAG, "Worker type missing! Cannot fetch jobs.");
                ToastUtils.showLong(this, "Please update your Work Type in Profile first.");
            }
        }).addOnFailureListener(e -> Log.e(TAG, "Failed to fetch worker details", e));
    }

    private void fetchCurrentStatus() {
        db.collection("users").document(laborId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Boolean isOnline = doc.getBoolean("isOnline");
                if (Boolean.TRUE.equals(isOnline)) {
                    switchOnline.setChecked(true);
                }
            }
        });
    }

    private void updateStatusText(boolean isOnline) {
        if (isOnline) {
            tvStatusDescription.setText("Working Status: ONLINE");
            tvStatusDescription.setTextColor(getResources().getColor(R.color.primary_green));
            setEmptyState(true);
            if (findViewById(R.id.cardMap) != null) findViewById(R.id.cardMap).setVisibility(View.VISIBLE);
        } else {
            tvStatusDescription.setText("Working Status: OFFLINE");
            tvStatusDescription.setTextColor(getResources().getColor(R.color.text_secondary));
            setEmptyState(false);
            if (findViewById(R.id.cardMap) != null) findViewById(R.id.cardMap).setVisibility(View.GONE);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("isOnline", isOnline);
        data.put("status", isOnline ? "ONLINE" : "OFFLINE");
        data.put("isAvailable", isOnline);

        db.collection("labor_workers").document(laborId)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnFailureListener(e -> Log.e(TAG, "Failed to update labor_workers status: " + e.getMessage()));

        db.collection("users").document(laborId).update("isOnline", isOnline)
            .addOnFailureListener(e -> Log.e(TAG, "Failed to update users status: " + e.getMessage()));
    }

    // LOCATION STUFF

    private void initLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if ("MANUAL".equalsIgnoreCase(locationMode)) {
                    Log.d(TAG, "Skipping GPS update: locationMode is MANUAL");
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    if (lat == 0.0 && lng == 0.0) continue;

                    lastKnownLocation = location;
                    updateLocationInFirestore(lat, lng);
                    
                    if (switchOnline.isChecked() && jobAdapter != null && jobAdapter.getItemCount() > 0) {
                        jobAdapter.updateCurrentLocation(lastKnownLocation);
                    }
                }
            }
        };
    }

    private void startLocationUpdates() {
        if (isReceivingLocationUpdates) return;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateDistanceMeters(10f)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        isReceivingLocationUpdates = true;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && (location.getLatitude() != 0.0 || location.getLongitude() != 0.0)) {
                if (!"MANUAL".equalsIgnoreCase(locationMode)) {
                    lastKnownLocation = location;
                    updateLocationInFirestore(location.getLatitude(), location.getLongitude());
                }
            }
            listenForJobs();
        });
    }

    private void stopLocationUpdates() {
        if (!isReceivingLocationUpdates) return;
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isReceivingLocationUpdates = false;
    }

    private void updateLocationInFirestore(double lat, double lng) {
        String geoHash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(lat, lng));
        Map<String, Object> updates = new HashMap<>();
        updates.put("currentLat", lat);
        updates.put("currentLng", lng);
        updates.put("geoHash", geoHash);
        updates.put("locationMode", locationMode);

        db.collection("labor_workers").document(laborId).update(updates)
            .addOnFailureListener(e -> Log.e(TAG, "Failed to update location", e));
    }
    

    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                ToastUtils.showShort(this, "Location permission required to see nearby jobs");
                switchOnline.setChecked(false);
            }
        }
    }

    // JOB LISTENING & ACCEPTING

    private void setupRecyclerView() {
        recyclerViewJobs.setLayoutManager(new LinearLayoutManager(this));
        jobAdapter = new LaborJobAdapter(new ArrayList<>(), this::acceptJobConcurrent, lastKnownLocation);
        recyclerViewJobs.setAdapter(jobAdapter);
        recyclerViewJobs.setVisibility(View.GONE);
    }

    private void setEmptyState(boolean isOnline) {
        if (!isOnline) {
            recyclerViewJobs.setVisibility(View.GONE);
            if(layoutEmptyJobs != null) layoutEmptyJobs.setVisibility(View.GONE);
            return;
        }

        if (jobAdapter == null || jobAdapter.getItemCount() == 0) {
            recyclerViewJobs.setVisibility(View.GONE);
            if(layoutEmptyJobs != null) {
                layoutEmptyJobs.setVisibility(View.VISIBLE);
            }
        } else {
            recyclerViewJobs.setVisibility(View.VISIBLE);
            if(layoutEmptyJobs != null) layoutEmptyJobs.setVisibility(View.GONE);
        }
    }

    private void listenForJobs() {
        if (!switchOnline.isChecked() || workerWorkType.isEmpty()) return;

        stopListeningForJobs();

        jobsListener = db.collection("labor_bookings")
            .whereEqualTo("workType", workerWorkType)
            .whereEqualTo("status", "REQUESTED")
            .addSnapshotListener((snapshots, e) -> {
                if (e != null) {
                    Toast.makeText(this, "Failed to load jobs", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                List<DocumentSnapshot> validJobs = new ArrayList<>();
                if (snapshots != null) {
                    for (DocumentSnapshot doc : snapshots.getDocuments()) {
                        // Check if job is fully booked
                        Long req = doc.getLong("workersRequired");
                        Long acc = doc.getLong("workersAccepted");
                        long required = req != null ? req : 1;
                        long accepted = acc != null ? acc : 0;
                        
                        // Check if we are already in the assignedWorkers array
                        List<String> assigned = (List<String>) doc.get("assignedWorkers");
                        if (assigned != null && assigned.contains(laborId)) {
                            // Already accepted, don't show it as available here. Maybe we show it in another tab later.
                            continue;
                        }

                        if (accepted < required) {
                            // Filter by distance (Large radius for testing: 25,000km)
                            if (lastKnownLocation != null) {
                                Double fLat = doc.getDouble("farmerLat");
                                Double fLng = doc.getDouble("farmerLng");
                                if (fLat != null && fLng != null) {
                                    double dist = GeoFireUtils.getDistanceBetween(
                                            new GeoLocation(fLat, fLng),
                                            new GeoLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude())
                                    );
                                    if (dist <= 25000000) {
                                        validJobs.add(doc);
                                    }
                                }
                            } else {
                                // If no location yet, add it anyway.
                                validJobs.add(doc);
                            }
                        }
                    }
                    Log.d(TAG, "listenForJobs: valid jobs found = " + validJobs.size());
                    jobAdapter.updateData(validJobs, lastKnownLocation);
                    updateJobMarkers(validJobs);
                    setEmptyState(true);
                }
            });
    }

    private void updateJobMarkers(List<DocumentSnapshot> jobs) {
        if (mMap == null) return;
        
        // Clear old markers
        for (Marker m : jobMarkers) m.remove();
        jobMarkers.clear();

        if (jobs.isEmpty()) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        boolean hasPoints = false;

        for (DocumentSnapshot doc : jobs) {
            Double lat = doc.getDouble("farmerLat");
            Double lng = doc.getDouble("farmerLng");
            if (lat != null && lng != null) {
                LatLng pos = new LatLng(lat, lng);
                Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(pos)
                    .title(doc.getString("workType") != null ? doc.getString("workType") : "Farm Work")
                    .snippet("Farmer: " + doc.getString("farmerName"))
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                
                jobMarkers.add(marker);
                builder.include(pos);
                hasPoints = true;
            }
        }

        if (lastKnownLocation != null) {
            builder.include(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
            hasPoints = true;
        }

        if (hasPoints && !jobs.isEmpty()) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
        
        if (lastKnownLocation != null) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()), 13f));
        }
    }

    private void stopListeningForJobs() {
        if (jobsListener != null) {
            jobsListener.remove();
            jobsListener = null;
        }
    }

    private void acceptJobConcurrent(DocumentSnapshot jobDoc) {
        String bookingId = jobDoc.getId();
        DocumentReference bookingRef = db.collection("labor_bookings").document(bookingId);
        DocumentReference workerRef = db.collection("labor_workers").document(laborId);
        
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(bookingRef);
            
            Long req = snapshot.getLong("workersRequired");
            Long acc = snapshot.getLong("workersAccepted");
            long required = req != null ? req : 1;
            long accepted = acc != null ? acc : 0;
            
            if (accepted >= required) {
                throw new FirebaseFirestoreException(
                    "Job is already full!", FirebaseFirestoreException.Code.ABORTED
                );
            }

            List<String> assigned = (List<String>) snapshot.get("assignedWorkers");
            if (assigned == null) assigned = new ArrayList<>();
            if (assigned.contains(laborId)) {
                throw new FirebaseFirestoreException(
                    "You have already accepted this job.", FirebaseFirestoreException.Code.ABORTED
                );
            }
            
            // Safe increment and array union
            long newAcceptedCount = accepted + 1;
            assigned.add(laborId);
            
            transaction.update(bookingRef, "workersAccepted", newAcceptedCount);
            transaction.update(bookingRef, "assignedWorkers", assigned);

            // If job is now full, mark it ACCEPTED
            if (newAcceptedCount >= required) {
                transaction.update(bookingRef, "status", "ACCEPTED");
            }

            // Mark this worker as BUSY and Unavailable
            transaction.update(workerRef, "status", "BUSY");
            transaction.update(workerRef, "isAvailable", false);
            
            return null;
        }).addOnSuccessListener(result -> {
            Toast.makeText(this, "Job successfully accepted!", Toast.LENGTH_SHORT).show();
            
            Double fLat = jobDoc.getDouble("farmerLat");
            Double fLng = jobDoc.getDouble("farmerLng");
            if (fLat != null && fLng != null) {
                showRouteToFarmer(new LatLng(fLat, fLng));
            }
        }).addOnFailureListener(e -> {
            if (e instanceof FirebaseFirestoreException && ((FirebaseFirestoreException) e).getCode() == FirebaseFirestoreException.Code.ABORTED) {
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Failed to accept job: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logout() {
        stopListeningForJobs();
        stopLocationUpdates();
        mAuth.signOut();
        preferenceManager.clearAll();
        
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void showRouteToFarmer(LatLng farmerPos) {
        if (mMap == null || lastKnownLocation == null) return;

        // Clear other markers
        for (Marker m : jobMarkers) m.remove();
        jobMarkers.clear();

        // Add Farmer marker
        mMap.addMarker(new MarkerOptions()
            .position(farmerPos)
            .title("Farmer Location")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));

        LatLng currentPos = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());

        MapUtils.fetchRoute(this, currentPos, farmerPos, new MapUtils.RouteCallback() {
            @Override
            public void onRouteFetched(List<LatLng> path, String distance, String duration) {
                if (routePolyline != null) routePolyline.remove();
                routePolyline = MapUtils.drawRoute(mMap, path, false);
                
                // Focus on the route
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (LatLng point : path) builder.include(point);
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 200));
                
                ToastUtils.showLong(LaborHomeActivity.this, "Route found: " + distance + " away (" + duration + ")");
            }

            @Override
            public void onError(String message) {
                Toast.makeText(LaborHomeActivity.this, "Routing error: " + message, Toast.LENGTH_SHORT).show();
                // Simple straight line fallback if API fails
                if (routePolyline != null) routePolyline.remove();
                routePolyline = mMap.addPolyline(new com.google.android.gms.maps.model.PolylineOptions()
                    .add(currentPos, farmerPos)
                    .width(12).color(android.graphics.Color.parseColor("#16A34A")));
                
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                    new LatLngBounds.Builder().include(currentPos).include(farmerPos).build(), 200));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopListeningForJobs();
        stopLocationUpdates();
    }
}
