package com.agrigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import android.location.Location;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.agrigo.R;
import com.agrigo.adapters.DriverRequestAdapter;
import com.agrigo.models.DriverRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import java.util.Collections;
import java.util.Comparator;

public class DriverRequestActivity extends AppCompatActivity implements DriverRequestAdapter.OnRequestClickListener {

    private RecyclerView recyclerView;
    private DriverRequestAdapter adapter;
    private List<DriverRequest> requestList;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmptyState;
    private ImageView btnBack;
    private FirebaseFirestore db;
    private ListenerRegistration requestsListener;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentDriverLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_request);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupRecyclerView();
        
        requestLocation();
        
        // Notify dispatch engine that this driver is online and available
        String currentDriverId = FirebaseAuth.getInstance().getCurrentUser() != null 
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() 
                : new com.agrigo.utils.PreferenceManager(this).getUserId();
        
        Map<String, Object> availData = new HashMap<>();
        availData.put("isAvailable", true);
        db.collection("drivers").document(currentDriverId).set(availData, com.google.firebase.firestore.SetOptions.merge());

        listenForRequests();
        
        btnBack.setOnClickListener(v -> finish());

        swipeRefresh.setOnRefreshListener(this::listenForRequests);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (requestsListener != null) {
            requestsListener.remove();
        }
        
        // Take driver offline when activity is destroyed
        String currentDriverId = FirebaseAuth.getInstance().getCurrentUser() != null 
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() 
                : new com.agrigo.utils.PreferenceManager(this).getUserId();
        db.collection("drivers").document(currentDriverId).update("isAvailable", false);
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_requests);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        btnBack = findViewById(R.id.btn_back);
        layoutEmptyState = findViewById(R.id.layout_empty_state);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        requestList = new ArrayList<>();
        adapter = new DriverRequestAdapter(requestList, this);
        recyclerView.setAdapter(adapter);
    }

    private void listenForRequests() {
        Log.d("DriverDiscovery", "listenForRequests called");
        if (swipeRefresh != null) {
            swipeRefresh.setRefreshing(true);
        }
        if (requestsListener != null) {
            requestsListener.remove();
        }

        String currentDriverId = FirebaseAuth.getInstance().getCurrentUser() != null 
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() 
                : new com.agrigo.utils.PreferenceManager(this).getUserId();

        Log.d("DriverDiscovery", "Querying as Driver: " + currentDriverId);

        // Broaden query to see both assigned and unassigned (Open) requests
        requestsListener = db.collection("transport_requests")
                .whereIn("status", java.util.Arrays.asList("REQUESTED", "ACCEPTED"))
                .addSnapshotListener((value, error) -> {
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    if (error != null) {
                        Log.e("DriverDiscovery", "Listen failed.", error);
                        Toast.makeText(this, "Scanning error: " + error.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Log.d("DriverDiscovery", "Received update from Firestore");
                    if (value != null) {
                        Log.d("DriverDiscovery", "Fetched docs: " + value.size());
                        if (value.isEmpty()) {
                            Toast.makeText(this, "No active requests in entire region.", Toast.LENGTH_SHORT).show();
                        }
                        requestList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            DriverRequest request = doc.toObject(DriverRequest.class);
                            if (request != null) {
                                request.setId(doc.getId());
                                
                                String assignedId = doc.getString("assignedDriverId");
                                String legacyDriverId = doc.getString("driverId"); // Fallback
                                
                                // Logic: Only show if it's assigned to me, OR it's unassigned (Open Market)
                                boolean isAssignedToMe = (assignedId != null && assignedId.equals(currentDriverId)) 
                                        || (legacyDriverId != null && legacyDriverId.equals(currentDriverId));
                                boolean isOpenMarket = (assignedId == null || assignedId.isEmpty()) 
                                        && (legacyDriverId == null || legacyDriverId.isEmpty());

                                if (!isAssignedToMe && !isOpenMarket) {
                                    continue; // Skip jobs explicitly assigned to someone else
                                }

                                // For open market requests, filter by vehicleType
                                if (isOpenMarket) {
                                    String driverVehicleType = new com.agrigo.utils.PreferenceManager(DriverRequestActivity.this).getVehicleType();
                                    String myVehicleType = (driverVehicleType != null) ? driverVehicleType.toLowerCase().trim() : "";
                                    String reqVehicleType = request.getVehicleType();
                                    
                                    if (reqVehicleType == null || !reqVehicleType.toLowerCase().trim().equals(myVehicleType)) {
                                        continue; // Skip jobs that don't match the driver's vehicle type
                                    }
                                }

                                // Calculate distance locally for proximity sorting
                                if (currentDriverLocation != null && request.getSourceLat() != 0) {
                                    float[] results = new float[1];
                                    Location.distanceBetween(
                                        currentDriverLocation.getLatitude(), currentDriverLocation.getLongitude(),
                                        request.getSourceLat(), request.getSourceLng(),
                                        results
                                    );
                                    double distKm = results[0] / 1000.0;
                                    request.setDistance(distKm);
                                    
                                    // Filter by broad distance for testing (1000km)
                                    if (distKm > 1000.0) continue; 
                                } else {
                                    request.setDistance(9999.0); // Unknown distance
                                }
                                
                                requestList.add(request);
                            }
                        }
                        
                        // Sort by distance (nearest first)
                        Collections.sort(requestList, (a, b) -> Double.compare(a.getDistance(), b.getDistance()));
                        
                        adapter.notifyDataSetChanged();
                        
                        // Diagnostic feedback
                        if (requestList.isEmpty() && !value.isEmpty()) {
                             Toast.makeText(this, "Jobs found but matched to others or too far.", Toast.LENGTH_SHORT).show();
                        } else if (!requestList.isEmpty()) {
                             Toast.makeText(this, "Discovered " + requestList.size() + " job opportunities!", Toast.LENGTH_SHORT).show();
                        }
                        
                        // Toggle empty state
                        if (layoutEmptyState != null) {
                            layoutEmptyState.setVisibility(requestList.isEmpty() ? View.VISIBLE : View.GONE);
                        }
                    }
                });
        
        Toast.makeText(this, "Scanning for job requests...", Toast.LENGTH_SHORT).show();
    }

    private void requestLocation() {
        Log.d("DriverDiscovery", "requestLocation called");
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    Log.d("DriverDiscovery", "Location acquired: " + location.getLatitude() + ", " + location.getLongitude());
                    currentDriverLocation = location;
                    
                    // FORCE SYNC: Use .set(..., merge()) to ensure the driver doc exists in discovery
                    String currentDriverId = FirebaseAuth.getInstance().getCurrentUser() != null 
                            ? FirebaseAuth.getInstance().getCurrentUser().getUid() 
                            : new com.agrigo.utils.PreferenceManager(this).getUserId();
                    
                    Map<String, Object> locUpdates = new HashMap<>();
                    locUpdates.put("driverId", currentDriverId);
                    locUpdates.put("currentLat", location.getLatitude());
                    locUpdates.put("currentLng", location.getLongitude());
                    locUpdates.put("isAvailable", true);
                    locUpdates.put("lastUpdated", System.currentTimeMillis());
                    
                    // Simple Geohash for GeoFire compatibility
                    String hash = com.firebase.geofire.GeoFireUtils.getGeoHashForLocation(
                            new com.firebase.geofire.GeoLocation(location.getLatitude(), location.getLongitude()));
                    locUpdates.put("geoHash", hash);
                    
                    db.collection("drivers").document(currentDriverId)
                            .set(locUpdates, com.google.firebase.firestore.SetOptions.merge());
                    
                    listenForRequests(); // Refresh with location info
                } else {
                    Log.w("DriverDiscovery", "LastLocation is null");
                    // Fallback: search anyway without distance sorting
                    listenForRequests();
                }
            }).addOnFailureListener(e -> {
                Log.e("DriverDiscovery", "Failed to get location", e);
                listenForRequests();
            });
        } else {
            Log.w("DriverDiscovery", "Location permission NOT granted");
            listenForRequests();
        }
    }

    @Override
    public void onAcceptClick(DriverRequest request) {
        if (request == null || request.getId() == null) return;
        
        String driverId = FirebaseAuth.getInstance().getCurrentUser() != null 
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() 
                : new com.agrigo.utils.PreferenceManager(this).getUserId();

        swipeRefresh.setRefreshing(true);

        com.google.firebase.firestore.DocumentReference docRef = db.collection("transport_requests").document(request.getId());
        
        db.runTransaction((com.google.firebase.firestore.Transaction.Function<Void>) transaction -> {
            DocumentSnapshot snapshot = transaction.get(docRef);
            String status = snapshot.getString("status");
            String assignedDriverId = snapshot.getString("assignedDriverId");
            String legacyDriverId = snapshot.getString("driverId");

            boolean isAssignedToMe = driverId.equals(assignedDriverId) || driverId.equals(legacyDriverId);
            boolean isOpenMarket = (assignedDriverId == null || assignedDriverId.isEmpty())
                    && (legacyDriverId == null || legacyDriverId.isEmpty());
            
            if ("REQUESTED".equals(status) && (isAssignedToMe || isOpenMarket)) {
                // Claim the job atomically
                transaction.update(docRef, "status", "ACCEPTED");
                transaction.update(docRef, "assignedDriverId", driverId);
                transaction.update(docRef, "driverId", driverId);
                
                // Set driver as BUSY
                com.google.firebase.firestore.DocumentReference driverRef = db.collection("drivers").document(driverId);
                try {
                    transaction.update(driverRef, "isAvailable", false);
                    transaction.update(driverRef, "status", "BUSY");
                } catch (Exception e) {
                    Log.w("DriverAccept", "Driver doc not found in 'drivers' collection, skipping status update");
                }
                return null;
            } else {
                throw new com.google.firebase.firestore.FirebaseFirestoreException(
                    "Job already taken or status changed", 
                    com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
            }
        }).addOnSuccessListener(aVoid -> {
            swipeRefresh.setRefreshing(false);
            Toast.makeText(this, "Accepted request from " + request.getFarmerName(), Toast.LENGTH_SHORT).show();
            
            // Navigate to TrackingActivity for live navigation
            Intent intent = new Intent(this, TrackingActivity.class);
            intent.putExtra("REQUEST_ID", request.getId());
            startActivity(intent);
            finish();
        }).addOnFailureListener(e -> {
            swipeRefresh.setRefreshing(false);
            Toast.makeText(this, "Failed to accept, it may have been taken.", Toast.LENGTH_LONG).show();
        });
    }

    @Override
    public void onDeclineClick(DriverRequest request) {
        Toast.makeText(this, "Declined request from " + request.getFarmerName(), Toast.LENGTH_SHORT).show();
        
        // Mark status as REJECTED so TransportBookingActivity tries next driver
        if (request.getId() != null) {
            db.collection("transport_requests").document(request.getId()).update("status", "REJECTED");
        }
        
        requestList.remove(request);
        adapter.notifyDataSetChanged();
    }
}
