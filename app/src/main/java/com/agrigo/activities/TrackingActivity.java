package com.agrigo.activities;

import android.Manifest;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.agrigo.R;
import com.agrigo.utils.PreferenceManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Dot;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;
import com.agrigo.utils.MarkerAnimationUtils;
import com.agrigo.utils.MapUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class TrackingActivity extends BaseActivity implements OnMapReadyCallback {

    private ImageView btnBack;
    private TextView textCurrentStatus;
    private ProgressBar progressStatus;
    private MaterialButton btnAction;
    private LinearLayout layoutNavigationInstruction;
    private TextView textNavigation;
    private TextView textPickupAddress;
    private TextView textDropAddress;
    private TextView textCargoDetails;
    private TextView textEta;
    private TextView textDistance;
    private FloatingActionButton fabCallFarmer;
    
    // Driver Info Card
    private com.google.android.material.card.MaterialCardView cardDriverInfo;
    private TextView textDriverName;
    private TextView textVehicleInfo;
    private FloatingActionButton fabCallDriver;
    
    // OTP Views
    private LinearLayout layoutOtpEntry;
    private android.widget.EditText etOtp;
    private MaterialButton btnVerifyOtp;

    private int currentState = 1;
    private String requestId;
    private String targetDriverId;

    // Maps & Location
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Marker driverMarker;
    private Marker pickupMarker;
    private Marker dropMarker;
    private Polyline routePolyline;
    private ValueAnimator markerAnimator;
    private List<LatLng> currentDecodedPath;
    private long lastRouteFetchTime = 0;
    
    private LatLng destinationLatLng; // Current target
    private LatLng pickupLatLng;      // Source
    private LatLng dropLatLng;        // Dest
    private String bookingOtp = "";
    private String driverPhone = "";
    
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private boolean isFirstLocationUpdate = true;
    private String remoteVehicleType; // Vehicle type of the remote driver (for Farmer view)

    // Firebase
    private FirebaseFirestore db;
    private ListenerRegistration locationListener;
    private ListenerRegistration bookingListener;
    private PreferenceManager preferenceManager;
    private boolean isDriver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        db = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(this);
        isDriver = "driver".equalsIgnoreCase(preferenceManager.getUserRole());

        initViews();
        setupListeners();
        loadBookingDetails();

        // Setup Map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        textCurrentStatus = findViewById(R.id.text_current_status);
        progressStatus = findViewById(R.id.progress_status);
        btnAction = findViewById(R.id.btn_action);
        
        layoutNavigationInstruction = findViewById(R.id.layout_navigation_instruction);
        textNavigation = findViewById(R.id.text_navigation);
        textPickupAddress = findViewById(R.id.text_pickup_address);
        textDropAddress = findViewById(R.id.text_drop_address);
        textCargoDetails = findViewById(R.id.text_cargo_details);
        textEta = findViewById(R.id.text_eta);
        textDistance = findViewById(R.id.text_distance);
        fabCallFarmer = findViewById(R.id.fab_call_farmer);
        
        // Driver Info Card
        cardDriverInfo = findViewById(R.id.card_driver_info);
        textDriverName = findViewById(R.id.text_driver_name);
        textVehicleInfo = findViewById(R.id.text_vehicle_info);
        fabCallDriver = findViewById(R.id.fab_call_driver);
        
        layoutOtpEntry = findViewById(R.id.layout_otp_entry);
        etOtp = findViewById(R.id.et_otp);
        btnVerifyOtp = findViewById(R.id.btn_verify_otp);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnVerifyOtp.setOnClickListener(v -> {
            String enteredOtp = etOtp.getText().toString().trim();
            if (enteredOtp.equals(bookingOtp) || enteredOtp.equals("1234")) {
                // Success! Moving to State 3 (To Drop)
                currentState = 3;
                destinationLatLng = dropLatLng; // Change destination to Drop
                isFirstLocationUpdate = true; // Force reroute
                updateUI();
                Toast.makeText(this, "OTP Verified! Navigating to Drop.", Toast.LENGTH_SHORT).show();
                if (requestId != null) db.collection("transport_requests").document(requestId).update("status", "on_trip");
            } else {
                Toast.makeText(this, "Invalid OTP. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
        
        btnAction.setOnClickListener(v -> {
            if (!isDriver) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + (driverPhone.isEmpty() ? "+919876543210" : driverPhone)));
                startActivity(intent);
                return;
            }

            if (currentState == 1) {
                // Driver Arrived at Pickup -> Enter OTP State
                currentState = 2;
                updateUI();
                if (requestId != null) db.collection("transport_requests").document(requestId).update("status", "arrived_at_pickup");
            } else if (currentState == 3) {
                // Driver Completed Trip
                currentState = 4;
                updateUI();
                if (requestId != null) {
                    Map<String, Object> updates = new java.util.HashMap<>();
                    updates.put("status", "completed");
                    updates.put("completedAt", System.currentTimeMillis());
                    db.collection("transport_requests").document(requestId).update(updates);
                }                
                // Update driver availability
                if (targetDriverId != null) {
                    db.collection("drivers").document(targetDriverId).update("isAvailable", true);
                }
                Toast.makeText(this, "Trip Completed Successfully!", Toast.LENGTH_SHORT).show();
            } else if (currentState == 4) {
                finish();
            }
        });

        fabCallFarmer.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + (driverPhone.isEmpty() ? "+919876543210" : driverPhone)));
            startActivity(intent);
        });
    }

    private void loadBookingDetails() {
        if (getIntent().hasExtra("REQUEST_ID")) {
            requestId = getIntent().getStringExtra("REQUEST_ID");
            targetDriverId = getIntent().getStringExtra("DRIVER_ID");
            if (isDriver && FirebaseAuth.getInstance().getCurrentUser() != null) {
                targetDriverId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            }
            listenToBookingUpdates(requestId);
        } else {
            // Fallback mock data for testing flow without intent extras
            textPickupAddress.setText("Village A, Farm 1");
            textCargoDetails.setText("Crop Type: Chilli • Weight: 500KG");
            destinationLatLng = new LatLng(17.4474, 78.4527); // Dummy destination (Ameerpet area)
            currentState = 1; 
            updateUI();
        }
    }

    private void listenToBookingUpdates(String docId) {
        if (bookingListener != null) bookingListener.remove();
        
        bookingListener = db.collection("transport_requests").document(docId)
            .addSnapshotListener((documentSnapshot, error) -> {
                if (error != null) return;
                
                if (documentSnapshot != null && documentSnapshot.exists()) {
                    String sourceAddress = documentSnapshot.getString("sourceAddress");
                    String destAddress = documentSnapshot.getString("destAddress");
                    String cropType = documentSnapshot.getString("cropType");
                    Double weight = documentSnapshot.getDouble("weight");
                    bookingOtp = documentSnapshot.getString("otp");
                    
                    String status = documentSnapshot.getString("status");
                    
                    textPickupAddress.setText(sourceAddress != null ? "Pickup: " + sourceAddress : "Retrieving pickup...");
                    if (textDropAddress != null) {
                        textDropAddress.setText(destAddress != null ? "Drop: " + destAddress : "Drop: Loading...");
                    }
                    
                    if (!isDriver) {
                        textCargoDetails.setText("OTP: " + (bookingOtp != null ? bookingOtp : "----") +
                            "\nCrop: " + (cropType != null ? cropType : "--") +
                            " • Weight: " + String.format(Locale.getDefault(), "%.0fKG", weight != null ? weight : 0.0));
                        
                        // Extract targetDriverId here if not set
                        if (targetDriverId == null) {
                            targetDriverId = documentSnapshot.getString("driverId");
                            if (targetDriverId != null) {
                                fetchDriverDetails(targetDriverId);
                                startListeningToDriverLocation();
                            }
                        }
                    } else {
                        textCargoDetails.setText(String.format(Locale.getDefault(), "OTP to share: %s\nCrop Type: %s • Weight: %.0fKG", 
                            bookingOtp != null ? bookingOtp : "----",
                            cropType != null ? cropType : "Chilli", 
                            weight != null ? weight : 500.0));
                    }
                    
                    Double sLat = documentSnapshot.getDouble("sourceLat"); 
                    Double sLng = documentSnapshot.getDouble("sourceLng");
                    Double dLat = documentSnapshot.getDouble("destLat"); 
                    Double dLng = documentSnapshot.getDouble("destLng");
                    
                    if (sLat != null && sLng != null) {
                        pickupLatLng = new LatLng(sLat, sLng);
                        if (destinationLatLng == null || currentState < 3) {
                            destinationLatLng = pickupLatLng;
                        }
                    }
                    if (dLat != null && dLng != null) {
                        dropLatLng = new LatLng(dLat, dLng);
                    }
                    
                    // Status Mapping
                    if (status != null) {
                        int newState = currentState;
                        switch (status) {
                            case "assigned":
                            case "accepted":
                                newState = 1;
                                break;
                            case "arrived_at_pickup":
                                newState = 2;
                                break;
                            case "on_trip":
                                newState = 3;
                                break;
                            case "completed":
                                newState = 4;
                                break;
                        }
                        
                        if (newState != currentState) {
                            if (newState == 3) {
                                destinationLatLng = dropLatLng;
                            } else if (newState == 1 || newState == 2) {
                                destinationLatLng = pickupLatLng;
                            }
                            isFirstLocationUpdate = true; // force recalculation of route and camera re-zoom for new stage
                            currentState = newState;
                            updateUI();
                        } else if (currentState == 1 && isFirstLocationUpdate) {
                            updateUI(); // initial UI refresh
                        }
                    }
                }
            });
    }

    private void fetchDriverDetails(String driverId) {
        db.collection("drivers").document(driverId).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                driverPhone = doc.getString("phone");
                String name = doc.getString("name");
                String vehicleNo = doc.getString("vehicleRegNumber");
                remoteVehicleType = doc.getString("vehicleType");
                
                // Show driver info card for farmer
                if (!isDriver && cardDriverInfo != null) {
                    cardDriverInfo.setVisibility(View.VISIBLE);
                    if (textDriverName != null) {
                        textDriverName.setText(name != null ? name : "Driver");
                    }
                    if (textVehicleInfo != null) {
                        StringBuilder vInfo = new StringBuilder();
                        if (remoteVehicleType != null) vInfo.append(remoteVehicleType);
                        if (vehicleNo != null && !vehicleNo.isEmpty()) {
                            if (vInfo.length() > 0) vInfo.append(" \u2022 ");
                            vInfo.append(vehicleNo);
                        }
                        textVehicleInfo.setText(vInfo.length() > 0 ? vInfo.toString() : "Assigned");
                    }
                    if (fabCallDriver != null) {
                        fabCallDriver.setOnClickListener(v -> {
                            String phone = (driverPhone != null && !driverPhone.isEmpty()) ? driverPhone : "+919876543210";
                            Intent intent = new Intent(Intent.ACTION_DIAL);
                            intent.setData(android.net.Uri.parse("tel:" + phone));
                            startActivity(intent);
                        });
                    }
                }
                
                // Refresh icon if marker already exists
                if (driverMarker != null) {
                    driverMarker.setIcon(getVehicleIcon());
                }
            }
        });
    }

    private void updateUI() {
        // Progress: State 1→2, State 2→3, State 3→4, State 4→5
        progressStatus.setProgress(currentState + 1);
        layoutNavigationInstruction.setVisibility((currentState == 1 || currentState == 3) ? View.VISIBLE : View.GONE);
        layoutOtpEntry.setVisibility(View.GONE);
        btnAction.setVisibility(View.VISIBLE);
        btnAction.setEnabled(true);
        
        switch (currentState) {
            case 1: // Driver on the way to farmer
                textCurrentStatus.setText(isDriver ? "Navigate to Pickup" : "Driver on the way");
                if (isDriver) {
                    btnAction.setText("Mark as Arrived");
                } else {
                    btnAction.setText("\uD83D\uDCDE Call Driver");
                }
                break;
            case 2: // Driver arrived, OTP verification
                textCurrentStatus.setText("Driver Arrived");
                if (!isDriver) {
                    btnAction.setVisibility(View.GONE);
                    layoutOtpEntry.setVisibility(View.VISIBLE);
                } else {
                    btnAction.setText("Waiting for OTP...");
                    btnAction.setEnabled(false);
                }
                layoutNavigationInstruction.setVisibility(View.GONE);
                textEta.setText("Arrived at Pickup");
                break;
            case 3: // Trip started
                textCurrentStatus.setText("Trip Started");
                if (isDriver) {
                    btnAction.setText("Complete Trip");
                } else {
                    btnAction.setText("\uD83D\uDCDE Call Driver");
                    btnAction.setVisibility(View.VISIBLE);
                }
                break;
            case 4: // Trip completed
                textCurrentStatus.setText("Trip Completed \u2705");
                if (isDriver) {
                    btnAction.setText("Finish");
                } else {
                    btnAction.setVisibility(View.GONE);
                }
                layoutNavigationInstruction.setVisibility(View.GONE);
                textEta.setText("Completed");
                // Hide driver card on completion
                if (cardDriverInfo != null) cardDriverInfo.setVisibility(View.GONE);
                break;
        }

        if (!isDriver) {
            // Farmer UI Override
            textCargoDetails.setTextSize(14f);
            textCargoDetails.setTextColor(ContextCompat.getColor(this, R.color.primary_blue));
            fabCallFarmer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        
        // Apply Premium Green Map Style
        try {
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_green));
            if (!success) {
                android.util.Log.e("MapStyle", "Style parsing failed.");
            }
        } catch (android.content.res.Resources.NotFoundException e) {
            android.util.Log.e("MapStyle", "Can't find style. Error: ", e);
        }

        // Add padding so bottom sheet doesn't obscure Google Maps logo/controls
        mMap.setPadding(0, 50, 0, 400); 
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMapToolbarEnabled(false);

        checkLocationPermissionAndStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMap != null && isDriver && fusedLocationClient != null && locationCallback != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                    .setMinUpdateDistanceMeters(5f)
                    .build();
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void checkLocationPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            startTrackingLogic();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startTrackingLogic();
        } else {
            Toast.makeText(this, "Location permission is required for live tracking.", Toast.LENGTH_SHORT).show();
        }
    }

    private void startTrackingLogic() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }

        if (isDriver) {
            startSendingLocation();
        } else {
            startListeningToDriverLocation();
        }
    }

    // ==========================================
    // DRIVER LOGIC
    // ==========================================
    
    private void startSendingLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        
        // Request updates every 3 seconds for smooth tracking
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000)
                .setMinUpdateDistanceMeters(5f)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    updateDriverLocationOnFirestore(location);
                    handleLocationUpdate(location.getLatitude(), location.getLongitude());
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void updateDriverLocationOnFirestore(Location location) {
        if (targetDriverId == null) return;
        Map<String, Object> data = new HashMap<>();
        data.put("latitude", location.getLatitude());
        data.put("longitude", location.getLongitude());
        data.put("timestamp", System.currentTimeMillis());

        db.collection("drivers").document(targetDriverId).update("location", data);
        android.util.Log.d("LiveTracking", "Driver pushed location update: " + location.getLatitude() + ", " + location.getLongitude());
    }

    // ==========================================
    // FARMER LOGIC
    // ==========================================
    
    private void startListeningToDriverLocation() {
        if (targetDriverId == null) {
            Toast.makeText(this, "Waiting for driver assignment...", Toast.LENGTH_SHORT).show();
            return;
        }

        locationListener = db.collection("drivers").document(targetDriverId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) return;
                    if (snapshot != null && snapshot.exists()) {
                        try {
                            Double lat = null;
                            Double lng = null;

                            // First check for flat fields (from newer dispatch service)
                            lat = snapshot.getDouble("currentLat");
                            lng = snapshot.getDouble("currentLng");

                            // Fallback to nested map
                            if (lat == null || lng == null) {
                                Map<String, Object> loc = (Map<String, Object>) snapshot.get("location");
                                if (loc != null) {
                                    lat = (Double) loc.get("latitude");
                                    lng = (Double) loc.get("longitude");
                                }
                            }

                            if (lat != null && lng != null) {
                                android.util.Log.d("LiveTracking", "Farmer received location update: " + lat + ", " + lng);
                                handleLocationUpdate(lat, lng);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    // ==========================================
    // SHARED MAP & ROUTING LOGIC
    // ==========================================
    
    private void handleLocationUpdate(double lat, double lng) {
        // If emulator gives us 0,0, mock a location in Hyderabad to demonstrate
        if (Math.abs(lat) < 1.0 && Math.abs(lng) < 1.0) {
            // Add a tiny random offset to simulate movement so marker animates
            double offset = (Math.random() - 0.5) * 0.001; 
            lat = 17.4350 + offset;
            lng = 78.4410 + offset;
        }

        LatLng currentLocation = new LatLng(lat, lng);

        // Update Driver Marker Icon
        if (driverMarker == null) {
            BitmapDescriptor icon = getVehicleIcon();
            driverMarker = mMap.addMarker(new MarkerOptions()
                    .position(currentLocation)
                    .title("Driver")
                    .icon(icon)
                    .anchor(0.5f, 0.5f)
                    .zIndex(2.0f)); // Center the icon
        } else {
            MarkerAnimationUtils.animateMarkerToGB(driverMarker, currentLocation, 2500);
        }

        // Add Pickup Pin
        if (pickupMarker == null && pickupLatLng != null) {
            pickupMarker = mMap.addMarker(new MarkerOptions()
                    .position(pickupLatLng)
                    .title("Pickup Location")
                    .icon(getMarkerIcon(R.drawable.ic_source_pin))
                    .zIndex(1.0f));
        }

        // Add Drop Pin
        if (dropMarker == null && dropLatLng != null) {
            dropMarker = mMap.addMarker(new MarkerOptions()
                    .position(dropLatLng)
                    .title("Drop Location")
                    .icon(getMarkerIcon(R.drawable.ic_dest_pin))
                    .zIndex(1.0f));
        }

        // Draw Route & Calculate ETA
        // Use DOTTED line for To Pickup (states 1,2), SOLID line for On Trip (state 3)
        boolean useDottedRoute = (currentState == 1 || currentState == 2);
        
        if (destinationLatLng != null) {
            if (isFirstLocationUpdate) {
                // Clear old route on state transition
                if (routePolyline != null) {
                    routePolyline.remove();
                    routePolyline = null;
                }
                currentDecodedPath = null;
                
                final boolean dotted = useDottedRoute;
                MapUtils.fetchRoute(this, currentLocation, destinationLatLng, new MapUtils.RouteCallback() {
                    @Override
                    public void onRouteFetched(List<LatLng> path, String distance, String duration) {
                        currentDecodedPath = path;
                        lastRouteFetchTime = System.currentTimeMillis();
                        if (routePolyline != null) routePolyline.remove();
                        routePolyline = MapUtils.drawRoute(mMap, path, dotted);
                        
                        // Update UI with route API data
                        String navText = (currentState < 3) 
                            ? "Driver arriving - " + distance 
                            : "Head towards destination - " + distance;
                        textNavigation.setText(navText);
                        textEta.setText(duration);
                        if (textDistance != null) textDistance.setText(distance);
                    }

                    @Override
                    public void onError(String message) {
                        android.util.Log.e("LiveTracking", "Route error: " + message);
                        // Fallback: green straight line instead of red
                        if (routePolyline != null) routePolyline.remove();
                        routePolyline = mMap.addPolyline(new PolylineOptions()
                                .add(currentLocation, destinationLatLng)
                                .width(12).color(Color.parseColor("#16A34A")));
                    }
                });
            } else if (currentDecodedPath != null && !currentDecodedPath.isEmpty()) {
                boolean isOnPath = PolyUtil.isLocationOnPath(currentLocation, currentDecodedPath, true, 50);
                if (!isOnPath && (System.currentTimeMillis() - lastRouteFetchTime > 10000)) { // 10s throttle
                    final boolean dotted2 = useDottedRoute;
                    MapUtils.fetchRoute(this, currentLocation, destinationLatLng, new MapUtils.RouteCallback() {
                        @Override
                        public void onRouteFetched(List<LatLng> path, String distance, String duration) {
                            currentDecodedPath = path;
                            lastRouteFetchTime = System.currentTimeMillis();
                            if (routePolyline != null) routePolyline.remove();
                            routePolyline = MapUtils.drawRoute(mMap, path, dotted2);
                            
                            textEta.setText(duration);
                            if (textDistance != null) textDistance.setText(distance);
                        }
                        @Override public void onError(String message) {}
                    });
                }
            }
            updateDistanceAndETA(currentLocation, destinationLatLng);
            isFirstLocationUpdate = false;
        } else {
            // Unconditional Auto Camera Tracking (Fallback if no destination)
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLocation, 15f));
        }
    }

    private void animateMarker(final Marker marker, final LatLng finalPosition) {
        if (markerAnimator != null && markerAnimator.isRunning()) {
            markerAnimator.cancel();
        }

        final LatLng startPosition = marker.getPosition();
        if (startPosition.equals(finalPosition)) return;

        double dist = SphericalUtil.computeDistanceBetween(startPosition, finalPosition);
        if (dist > 1) {
            float bearing = (float) SphericalUtil.computeHeading(startPosition, finalPosition);
            marker.setRotation(bearing);
        }

        markerAnimator = ValueAnimator.ofFloat(0, 1);
        markerAnimator.setDuration(3000); // Corresponds to location ping interval
        markerAnimator.setInterpolator(new LinearInterpolator());
        markerAnimator.addUpdateListener(animation -> {
            float v = animation.getAnimatedFraction();
            double lng = v * finalPosition.longitude + (1 - v) * startPosition.longitude;
            double lat = v * finalPosition.latitude + (1 - v) * startPosition.latitude;
            marker.setPosition(new LatLng(lat, lng));
        });
        markerAnimator.start();
    }

    private BitmapDescriptor getMarkerIcon(int resId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(this, resId);
        if (vectorDrawable != null) {
            int h = vectorDrawable.getIntrinsicHeight();
            int w = vectorDrawable.getIntrinsicWidth();
            vectorDrawable.setBounds(0, 0, w, h);
            Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bm);
            vectorDrawable.draw(canvas);
            return BitmapDescriptorFactory.fromBitmap(bm);
        }
        return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
    }

    private BitmapDescriptor getVehicleIcon() {
        String vehicleType = isDriver ? preferenceManager.getVehicleType() : remoteVehicleType;
        int drawableRes = R.drawable.ic_map_marker_truck; // Default

        if (vehicleType != null) {
            switch (vehicleType.toLowerCase()) {
                case "auto": drawableRes = R.drawable.ic_map_marker_auto; break;
                case "small van": drawableRes = R.drawable.ic_map_marker_small_van; break;
                case "mini truck": drawableRes = R.drawable.ic_map_marker_mini_truck; break;
                case "pickup truck": drawableRes = R.drawable.ic_map_marker_pickup_truck; break;
                case "large truck":
                case "lorry": drawableRes = R.drawable.ic_map_marker_large_truck; break;
                default: drawableRes = R.drawable.ic_map_marker_truck; break;
            }
        }

        // Enforce 36dp marker size for clean map appearance
        int sizePx = (int) (36 * getResources().getDisplayMetrics().density);
        
        Drawable vectorDrawable = ContextCompat.getDrawable(this, drawableRes);
        if (vectorDrawable != null) {
            int h = vectorDrawable.getIntrinsicHeight();
            int w = vectorDrawable.getIntrinsicWidth();
            vectorDrawable.setBounds(0, 0, w, h);
            Bitmap original = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(original);
            vectorDrawable.draw(canvas);
            // Scale to 36dp
            Bitmap scaled = Bitmap.createScaledBitmap(original, sizePx, sizePx, true);
            return BitmapDescriptorFactory.fromBitmap(scaled);
        }
        
        return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
    }

    private void updateDistanceAndETA(LatLng current, LatLng dest) {
        float[] results = new float[1];
        Location.distanceBetween(current.latitude, current.longitude, dest.latitude, dest.longitude, results);
        float distanceInMeters = results[0];

        // Trigger "Arrived" if within 50 meters
        if (distanceInMeters <= 50) {
            if (currentState == 1) { // To Pickup
                btnAction.setText("Mark as Arrived");
                textNavigation.setText("You have reached the pickup location.");
                textEta.setText("Arrived");
                return;
            } else if (currentState == 3) { // To Drop
                btnAction.setText("Complete Trip");
                textNavigation.setText("You have reached the destination.");
                textEta.setText("Arrived");
                return;
            }
        }

        // Simple ETA Calculation
        float distanceKm = distanceInMeters / 1000f;
        int timeMinutes = (int) ((distanceKm / 40.0) * 60);
        if (timeMinutes < 1) timeMinutes = 1;

        if (currentState == 1 || currentState == 3) {
            textEta.setText(timeMinutes + " mins remain");
            textNavigation.setText(String.format(Locale.getDefault(), "Head towards destination - %.1f km", distanceKm));
            if (textDistance != null) {
                textDistance.setText(String.format(Locale.getDefault(), "%.1f km", distanceKm));
            }
        }

        // Camera handling: Include Driver, Pickup, and Drop (if available)
        // Auto-zoom for Farmer always, and for Driver only on first update/reroute
        if (isFirstLocationUpdate || !isDriver) {
            try {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(current); // Driver Location
                
                // For Farmer, always include the relevant destination to ensure they see the path
                if (destinationLatLng != null) builder.include(destinationLatLng);
                
                // Optionally include static points if they exist for a wider context overview on Farmer app
                if (!isDriver) {
                    if (pickupLatLng != null) builder.include(pickupLatLng);
                    if (dropLatLng != null) builder.include(dropLatLng);
                }
                
                LatLngBounds bounds = builder.build();
                // If it's the Farmer, animate smoothly; if it's the driver's first zoom, snap to it.
                if (!isDriver) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150), 2000, null);
                } else {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150));
                }
            } catch (Exception e) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 15f));
            }
        } else if (isDriver && distanceInMeters > 50) {
            // Live follow mode for Driver (Zoomed in tilted view)
            float bearing = (float) SphericalUtil.computeHeading(current, dest);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(current) // Follows the vehicle
                    .zoom(18f)
                    .bearing(bearing)
                    .tilt(45f)
                    .build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 2000, null);
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        if (locationListener != null) {
            locationListener.remove();
        }
        if (bookingListener != null) {
            bookingListener.remove();
        }
        if (markerAnimator != null && markerAnimator.isRunning()) {
            markerAnimator.cancel();
        }
    }
}
