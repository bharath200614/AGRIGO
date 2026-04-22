package com.agrigo.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agrigo.R;
import com.agrigo.adapters.MachineryRequestAdapter;
import com.agrigo.models.MachineryBooking;
import com.agrigo.utils.GeocodingUtils;
import com.agrigo.utils.PreferenceManager;
import com.agrigo.utils.ToastUtils;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Transaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MachineryProviderActivity extends AppCompatActivity implements MachineryRequestAdapter.OnRequestClickListener {

    private static final String TAG = "MachineryProviderAct";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private ImageView btnBack;
    private RecyclerView recyclerViewRequests;
    private TextView tvEmptyState;
    private SwitchMaterial switchOnlineStatus;
    private TextView tvStatusText;
    private MaterialButton btnProfile;

    private MachineryRequestAdapter adapter;
    private List<MachineryBooking> requestList;

    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;
    private ListenerRegistration requestsListener;
    private ListenerRegistration openRequestsListener;
    private String providerId;
    private String providerMachineryType = "";

    // ── Location ──
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private boolean isReceivingLocationUpdates = false;
    private Location lastKnownLocation;
    private String locationMode = "AUTO"; // AUTO or MANUAL

    private TextView tvLocationMode;
    private TextView tvCurrentAddress;
    private MaterialButton btnChangeLocation;
    
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
                    updateLocationUI();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_machinery_provider);

        db = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(this);
        providerId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : preferenceManager.getUserId();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        initLocationCallback();

        initViews();
        fetchProviderMachineryType();
        fetchCurrentStatus();
    }

    // ────────────────── Views ──────────────────

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> onBackPressed());

        recyclerViewRequests = findViewById(R.id.recyclerViewRequests);
        tvEmptyState = findViewById(R.id.tvEmptyState);
        switchOnlineStatus = findViewById(R.id.switchOnlineStatus);
        tvStatusText = findViewById(R.id.tvStatusText);

        // Profile button — if it exists in the layout
        btnProfile = findViewById(R.id.btnProfile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v ->
                startActivity(new Intent(this, MachineryProviderProfileActivity.class))
            );
        }

        requestList = new ArrayList<>();
        adapter = new MachineryRequestAdapter(requestList, this);
        recyclerViewRequests.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewRequests.setAdapter(adapter);

        // Show waiting state
        tvEmptyState.setVisibility(View.VISIBLE);
        tvEmptyState.setText("Waiting for requests...");
        recyclerViewRequests.setVisibility(View.GONE);

        switchOnlineStatus.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateStatus(isChecked);
        });

        tvLocationMode = findViewById(R.id.tvLocationMode);
        tvCurrentAddress = findViewById(R.id.tvCurrentAddress);
        btnChangeLocation = findViewById(R.id.btnChangeLocation);

        if (btnChangeLocation != null) {
            btnChangeLocation.setOnClickListener(v -> {
                if (!switchOnlineStatus.isChecked()) {
                    ToastUtils.showShort(this, "Please go Online first to update location.");
                    return;
                }
                Intent intent = new Intent(this, MapPickerActivity.class);
                if (lastKnownLocation != null) {
                    intent.putExtra("initLat", lastKnownLocation.getLatitude());
                    intent.putExtra("initLng", lastKnownLocation.getLongitude());
                }
                mapPickerLauncher.launch(intent);
            });
        }
    }

    // ────────────────── Provider Type Fetch ──────────────────

    private void fetchProviderMachineryType() {
        db.collection("machinery_providers").document(providerId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String type = doc.getString("machineryType");
                        if (type != null) {
                            providerMachineryType = type.toLowerCase().trim();
                        }
                        
                        String mode = doc.getString("locationMode");
                        if (mode != null && !mode.isEmpty()) {
                            locationMode = mode;
                        }
                        updateLocationUI();
                        
                        Log.d(TAG, "Provider machineryType = " + providerMachineryType + " | Mode = " + locationMode);
                    }
                    // Start listening for requests after we know our type
                    listenForIncomingRequests();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch machinery type", e);
                    listenForIncomingRequests();
                });
    }

    // ────────────────── Status ──────────────────

    private void fetchCurrentStatus() {
        db.collection("users").document(providerId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                Boolean isOnline = documentSnapshot.getBoolean("isOnline");
                if (isOnline != null && isOnline) {
                    switchOnlineStatus.setChecked(true);
                    tvStatusText.setText("Online");
                    tvStatusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                    startLocationUpdates();
                } else {
                    switchOnlineStatus.setChecked(false);
                    tvStatusText.setText("Offline");
                    tvStatusText.setTextColor(android.graphics.Color.parseColor("#E53935"));
                }
            }
        });
    }

    private void updateStatus(boolean isOnline) {
        db.collection("machinery_providers").document(providerId).update(
                "isOnline", isOnline,
                "status", isOnline ? "FREE" : "OFFLINE",
                "isAvailable", isOnline
        ).addOnSuccessListener(aVoid -> {
            db.collection("users").document(providerId).update("isOnline", isOnline);

            Intent serviceIntent = new Intent(this, com.agrigo.services.DriverDispatchService.class);
            if (isOnline) {
                tvStatusText.setText("Online");
                tvStatusText.setTextColor(android.graphics.Color.parseColor("#4CAF50"));

                // SAFELY check for permission before starting foreground service on API 34+
                if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) 
                        == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent);
                    } else {
                        startService(serviceIntent);
                    }
                } else {
                    ToastUtils.showShort(this, "Location permission needed for automatic dispatch");
                }
                
                startLocationUpdates();
                ToastUtils.showShort(this, "You are now Online");
            } else {
                tvStatusText.setText("Offline");
                tvStatusText.setTextColor(android.graphics.Color.parseColor("#E53935"));
                stopService(serviceIntent);
                stopLocationUpdates();
                ToastUtils.showShort(this, "You are now Offline");
            }
        }).addOnFailureListener(e -> {
            switchOnlineStatus.setChecked(!isOnline);
            ToastUtils.showShort(this, "Failed to update status");
        });
    }

    // ────────────────── Location ──────────────────

    private void initLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                // If manual mode is active, do not override location with GPS data.
                if ("MANUAL".equalsIgnoreCase(locationMode)) {
                    Log.d(TAG, "Skipping GPS update: locationMode is MANUAL");
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();

                    if (lat == 0.0 && lng == 0.0) {
                        Log.w(TAG, "Skipping (0,0) location update");
                        continue;
                    }
                    if (location.hasAccuracy() && location.getAccuracy() > 100) {
                        Log.w(TAG, "Skipping low-accuracy location: " + location.getAccuracy() + "m");
                        continue;
                    }

                    lastKnownLocation = location;
                    updateLocationInFirestore(lat, lng);
                    updateLocationUI();
                }
            }
        };
    }

    private void startLocationUpdates() {
        if (isReceivingLocationUpdates) return;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
                .setMinUpdateDistanceMeters(10f)
                .setWaitForAccurateLocation(true)
                .build();

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        isReceivingLocationUpdates = true;

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null && !"MANUAL".equalsIgnoreCase(locationMode)) {
                double lat = location.getLatitude();
                double lng = location.getLongitude();
                if (lat != 0.0 || lng != 0.0) {
                    lastKnownLocation = location;
                    updateLocationInFirestore(lat, lng);
                    updateLocationUI();
                }
            }
        });

        Log.d(TAG, "Location updates STARTED");
    }

    private void stopLocationUpdates() {
        if (!isReceivingLocationUpdates) return;
        fusedLocationClient.removeLocationUpdates(locationCallback);
        isReceivingLocationUpdates = false;
        Log.d(TAG, "Location updates STOPPED");
    }

    private void updateLocationInFirestore(double lat, double lng) {
        if (providerId == null || providerId.isEmpty()) return;

        String geoHash = GeoFireUtils.getGeoHashForLocation(new GeoLocation(lat, lng));

        Map<String, Object> updates = new HashMap<>();
        updates.put("currentLat", lat);
        updates.put("currentLng", lng);
        updates.put("geoHash", geoHash);
        updates.put("locationMode", locationMode);
        updates.put("lastUpdated", System.currentTimeMillis());

        db.collection("machinery_providers").document(providerId).update(updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Location → " + lat + ", " + lng + " | hash=" + geoHash))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update location", e));
    }
    
    private void updateLocationUI() {
        if (tvLocationMode == null || tvCurrentAddress == null) return;
        
        if ("MANUAL".equalsIgnoreCase(locationMode)) {
            tvLocationMode.setText("Custom Location \uD83D\uDCCD");
            tvLocationMode.setTextColor(android.graphics.Color.parseColor("#16A34A")); // Green
        } else {
            tvLocationMode.setText("Using GPS \uD83D\uDCE1");
            tvLocationMode.setTextColor(android.graphics.Color.parseColor("#16A34A")); // Green
        }
        
        if (lastKnownLocation != null) {
            com.google.android.gms.maps.model.LatLng latLng = new com.google.android.gms.maps.model.LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            GeocodingUtils.getAddressFromLatLng(this, latLng, address -> tvCurrentAddress.setText(address));
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                ToastUtils.showShort(this, "Location permission is required to go online");
                switchOnlineStatus.setChecked(false);
            }
        }
    }

    // ────────────────── Incoming Requests ──────────────────

    private void listenForIncomingRequests() {
        // 1. Listen for directly assigned requests
        requestsListener = db.collection("machinery_bookings")
                .whereEqualTo("assignedProviderId", providerId)
                .whereIn("status", Arrays.asList("REQUESTED", "ACCEPTED", "ARRIVED", "WORK_STARTED"))
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) return;
                    updateRequestList(value.getDocuments(), false);
                });

        // 2. Fallback: Listen for open requests (unassigned) matching our machineryType
        if (!providerMachineryType.isEmpty()) {
            openRequestsListener = db.collection("machinery_bookings")
                    .whereEqualTo("status", "REQUESTED")
                    .whereEqualTo("machineryType", providerMachineryType)
                    .addSnapshotListener((value, error) -> {
                        if (error != null || value == null) return;

                        // Filter: only show requests that have no assigned provider
                        List<DocumentSnapshot> openDocs = new ArrayList<>();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String assignedId = doc.getString("assignedProviderId");
                            if (assignedId == null || assignedId.isEmpty()) {
                                // Calculate distance if we have location
                                if (lastKnownLocation != null) {
                                    Double fLat = doc.getDouble("farmerLat");
                                    Double fLng = doc.getDouble("farmerLng");
                                    if (fLat != null && fLng != null) {
                                        double dist = GeoFireUtils.getDistanceBetween(
                                                new GeoLocation(fLat, fLng),
                                                new GeoLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude())
                                        );
                                        if (dist <= 25000) { // 25km radius
                                            openDocs.add(doc);
                                        }
                                    }
                                } else {
                                    // No location yet, show all unassigned requests
                                    openDocs.add(doc);
                                }
                            }
                        }
                        updateRequestList(openDocs, true);
                    });
        }
    }

    private final List<MachineryBooking> assignedRequests = new ArrayList<>();
    private final List<MachineryBooking> openRequests = new ArrayList<>();

    private void updateRequestList(List<DocumentSnapshot> docs, boolean isOpenRequests) {
        List<MachineryBooking> targetList = isOpenRequests ? openRequests : assignedRequests;
        targetList.clear();

        for (DocumentSnapshot doc : docs) {
            MachineryBooking booking = doc.toObject(MachineryBooking.class);
            if (booking != null) {
                booking.setId(doc.getId());

                // Calculate distance for display
                if (lastKnownLocation != null) {
                    Double fLat = doc.getDouble("farmerLat");
                    Double fLng = doc.getDouble("farmerLng");
                    if (fLat != null && fLng != null) {
                        double dist = GeoFireUtils.getDistanceBetween(
                                new GeoLocation(fLat, fLng),
                                new GeoLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude())
                        );
                        booking.setDistanceKm(dist / 1000.0);
                    }
                }

                // Get address from doc
                String address = doc.getString("address");
                if (address != null) {
                    booking.setAddress(address);
                }

                targetList.add(booking);
            }
        }

        // Merge assigned + open into the display list
        requestList.clear();
        requestList.addAll(assignedRequests);
        requestList.addAll(openRequests);

        adapter.notifyDataSetChanged();

        if (requestList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            tvEmptyState.setText("Waiting for requests...");
            recyclerViewRequests.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            recyclerViewRequests.setVisibility(View.VISIBLE);
        }
    }

    // ────────────────── Accept Job ──────────────────

    @Override
    public void onAcceptClick(MachineryBooking booking) {
        DocumentReference docRef = db.collection("machinery_bookings").document(booking.getId());

        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(docRef);
            String status = snapshot.getString("status");
            String assignedId = snapshot.getString("assignedProviderId");

            // Accept if: assigned to us, or unassigned (open request)
            boolean isAssignedToUs = providerId.equals(assignedId);
            boolean isOpenRequest = (assignedId == null || assignedId.isEmpty()) && "REQUESTED".equals(status);

            if ("REQUESTED".equals(status) && (isAssignedToUs || isOpenRequest)) {
                transaction.update(docRef, "status", "ACCEPTED");
                transaction.update(docRef, "providerId", providerId);
                transaction.update(docRef, "assignedProviderId", providerId);

                DocumentReference provRef = db.collection("machinery_providers").document(providerId);
                transaction.update(provRef, "isAvailable", false);
                transaction.update(provRef, "status", "BUSY");

                return null;
            } else {
                throw new FirebaseFirestoreException("Job already taken or changed status",
                        FirebaseFirestoreException.Code.ABORTED);
            }
        }).addOnSuccessListener(aVoid -> {
            ToastUtils.showShort(this, "Job Accepted!");
            Intent intent = new Intent(this, MachineryTrackingActivity.class);
            intent.putExtra("BOOKING_ID", booking.getId());
            startActivity(intent);
        }).addOnFailureListener(e -> {
            ToastUtils.showShort(this, "Failed to accept, it may have been taken.");
        });
    }

    // ────────────────── Lifecycle ──────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestsListener != null) requestsListener.remove();
        if (openRequestsListener != null) openRequestsListener.remove();
        stopLocationUpdates();
    }
}
