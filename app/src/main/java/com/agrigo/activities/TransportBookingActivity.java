package com.agrigo.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.agrigo.R;
import com.agrigo.models.Crop;
import com.agrigo.models.VehicleSuggestion;
import com.agrigo.network.MLPredictionRequest;
import com.agrigo.network.MLPredictionResponse;
import com.agrigo.network.MLPredictionService;
import com.agrigo.network.RetrofitClient;
import com.agrigo.utils.BookingRecommendationEngine;
import com.agrigo.utils.PreferenceManager;
import com.agrigo.utils.ToastUtils;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import java.util.Collections;

import com.agrigo.utils.MapUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agrigo.adapters.MarketAdapter;
import com.agrigo.adapters.CropGridAdapter;
import com.agrigo.models.Market;
import com.agrigo.utils.MarketDataProvider;

public class TransportBookingActivity extends BaseActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 2001;
    private static final double RATE_PER_KM = 15.0;

    // UI Elements
    private ImageView btnBack;
    private EditText etTransportWeight;
    private EditText etDestSearch;
    private TextView tvPickupAddress, tvDistance, tvEstimatedPrice;
    private LinearLayout layoutDistancePrice;
    private FloatingActionButton fabMyLocation;
    private Button btnConfirmTransport;
    private ImageView btnLocateMeInsideSearch;
    private RecyclerView rvCropSelection;
    private CropGridAdapter cropGridAdapter;

    // ML Predict UI
    private MaterialButton btnPredictVehicle;
    private MaterialCardView cardMLResult;
    private TextView tvMLPredictedVehicle;
    private TextView tvMLEstimatedFare;
    private ImageView ivMLVehicleIcon;

    // Dispatch Overlay Views
    private LinearLayout layoutBookingForm, layoutDispatching, layoutDriverAccepted;
    private TextView tvDispatchPayload, tvDriverEta;
    private MaterialButton btnCancelDispatch;
    private TextView tvOtpDisplay, tvDriverName;
    private MaterialButton btnTrackDriver;

    // Radar Animation variables
    private android.animation.ValueAnimator radarAnimator;
    private com.google.android.gms.maps.model.Circle radarCircle;
    private List<com.google.android.gms.maps.model.Marker> driverMarkers = new ArrayList<>();

    // Vehicle Type Display
    private LinearLayout layoutVehicleType;
    private ImageView ivVehicleIcon;
    private TextView tvVehicleType, tvVehicleCapacity, tvEstimatedTime;

    // Markets
    private RecyclerView rvSuggestedMarkets;
    private MarketAdapter marketAdapter;
    private List<Market> currentMarkets = new ArrayList<>();

    // Google Maps & Firebase
    private GoogleMap transportMap;
    private FusedLocationProviderClient fusedLocationClient;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore db;
    private ListenerRegistration dispatchListener;

    private Marker sourceMarker, destMarker;
    private Polyline routePolyline, routeShadowPolyline;
    private LatLng sourceLatLng, destLatLng;
    private String sourceAddress = "";
    private String destAddress = "";
    private double calculatedDistance = 0;
    private double estimatedPrice = 0;
    private boolean mapReady = false;
    private boolean isSelectingPickup = false;

    // ML prediction result stored for booking
    private String mlPredictedVehicle = null;

    // Booking reference for navigation handoff
    private String lastBookingId = null;
    private String lastBookingOtp = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transport_booking);

        preferenceManager = new PreferenceManager(this);
        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initializeViews();
        setupListeners();
        setupTransportMap();
    }

    private void initializeViews() {
        btnBack = findViewById(R.id.btnBack);
        etTransportWeight = findViewById(R.id.etTransportWeight);
        etDestSearch = findViewById(R.id.etDestSearch);
        tvPickupAddress = findViewById(R.id.tvPickupAddress);
        tvDistance = findViewById(R.id.tvDistance);
        tvEstimatedPrice = findViewById(R.id.tvEstimatedPrice);
        layoutDistancePrice = findViewById(R.id.layoutDistancePrice);
        fabMyLocation = findViewById(R.id.fabMyLocation);
        btnConfirmTransport = findViewById(R.id.btnConfirmTransport);
        btnLocateMeInsideSearch = findViewById(R.id.btnLocateMeInsideSearch);
        rvCropSelection = findViewById(R.id.rvCropSelection);
        if (rvCropSelection != null) {
            List<Crop> cropList = com.agrigo.utils.CropUtils.getAllCrops(this);
            rvCropSelection.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 3));
            cropGridAdapter = new CropGridAdapter(this, cropList, crop -> {
                // Trigger updates when crop changes
                updateMarketSuggestions(etDestSearch.getText().toString());
                if (!etTransportWeight.getText().toString().isEmpty()) {
                    updateVehicleSuggestionDisplay();
                }
            });
            rvCropSelection.setAdapter(cropGridAdapter);
            
            // Set default selection (Rice)
            cropGridAdapter.setSelectedCrop("rice");
        }

        layoutBookingForm = findViewById(R.id.layoutBookingForm);
        layoutDispatching = findViewById(R.id.layoutDispatching);
        layoutDriverAccepted = findViewById(R.id.layoutDriverAccepted);
        tvDispatchPayload = findViewById(R.id.tvDispatchPayload);
        tvDriverEta = findViewById(R.id.tvDriverEta);
        btnCancelDispatch = findViewById(R.id.btnCancelDispatch);
        tvOtpDisplay = findViewById(R.id.tvOtpDisplay);
        tvDriverName = findViewById(R.id.tvDriverName);
        btnTrackDriver = findViewById(R.id.btnTrackDriver);

        // Call Driver button in accepted card
        ImageView btnCallDriver = findViewById(R.id.btnCallDriver);
        if (btnCallDriver != null) {
            btnCallDriver.setOnClickListener(v -> {
                if (acceptedDriverPhone != null && !acceptedDriverPhone.isEmpty()) {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL);
                    callIntent.setData(android.net.Uri.parse("tel:" + acceptedDriverPhone));
                    startActivity(callIntent);
                } else {
                    ToastUtils.showShort(this, "Driver phone not available yet");
                }
            });
        }

        if (fabMyLocation != null) {
            fabMyLocation.setVisibility(View.GONE);
        }

        // ML Prediction UI
        btnPredictVehicle = findViewById(R.id.btnPredictVehicle);
        cardMLResult = findViewById(R.id.cardMLResult);
        tvMLPredictedVehicle = findViewById(R.id.tvMLPredictedVehicle);
        tvMLEstimatedFare = findViewById(R.id.tvMLEstimatedFare);
        ivMLVehicleIcon = findViewById(R.id.ivMLVehicleIcon);

        layoutVehicleType = findViewById(R.id.layoutVehicleType);
        ivVehicleIcon = findViewById(R.id.ivVehicleIcon);
        tvVehicleType = findViewById(R.id.tvVehicleType);
        tvVehicleCapacity = findViewById(R.id.tvVehicleCapacity);
        tvEstimatedTime = findViewById(R.id.tvEstimatedTime);

        rvSuggestedMarkets = findViewById(R.id.rvSuggestedMarkets);
        rvSuggestedMarkets.setLayoutManager(new LinearLayoutManager(this));
        marketAdapter = new MarketAdapter(currentMarkets, market -> {
            rvSuggestedMarkets.setVisibility(View.GONE);
            etDestSearch.setText(market.getName());
            etDestSearch.clearFocus();
            setDestination(new LatLng(market.getLat(), market.getLng()));
            destAddress = market.getName() + ", " + market.getDistrict();
        });
        rvSuggestedMarkets.setAdapter(marketAdapter);
    }

    private void updateMarketSuggestions(String query) {
        if (sourceLatLng == null) return;
        String cropName = "Wheat";
        if (cropGridAdapter != null && cropGridAdapter.getSelectedCrop() != null) {
            cropName = cropGridAdapter.getSelectedCrop().getName();
        }
        currentMarkets = MarketDataProvider.searchMarkets(query, cropName, sourceLatLng.latitude, sourceLatLng.longitude);
        marketAdapter.updateMarkets(currentMarkets);
        if (!currentMarkets.isEmpty() && etDestSearch.hasFocus()) {
            rvSuggestedMarkets.setVisibility(View.VISIBLE);
        } else {
            rvSuggestedMarkets.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> onBackPressed());

        if (tvPickupAddress != null) {
            tvPickupAddress.setOnClickListener(v -> {
                isSelectingPickup = true;
                ToastUtils.showShort(this, "Select your pickup location on the map");
            });
        }

        if (btnLocateMeInsideSearch != null) {
            btnLocateMeInsideSearch.setOnClickListener(v -> detectCurrentLocation());
        }
        
        if (btnCancelDispatch != null) {
            btnCancelDispatch.setOnClickListener(v -> cancelDispatchSimulation());
        }

        if (btnTrackDriver != null) {
            btnTrackDriver.setOnClickListener(v -> {
                if (lastBookingId != null) {
                    Intent intent = new Intent(this, TrackingActivity.class);
                    intent.putExtra("REQUEST_ID", lastBookingId);
                    startActivity(intent);
                    finish();
                }
            });
        }

        etDestSearch.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                updateMarketSuggestions(etDestSearch.getText().toString());
            } else {
                rvSuggestedMarkets.setVisibility(View.GONE);
            }
        });

        etDestSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (etDestSearch.hasFocus()) {
                    updateMarketSuggestions(s.toString());
                }
            }
        });

        btnConfirmTransport.setOnClickListener(v -> confirmTransportBooking());

        // ML Predict button click
        btnPredictVehicle.setOnClickListener(v -> performMLPrediction());

        etTransportWeight.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (sourceLatLng != null && destLatLng != null) {
                    calculateDistanceAndPrice();
                }
                updateVehicleSuggestionDisplay();
                updateConfirmButtonState();
                // Hide old ML result when weight changes
                if (cardMLResult != null) {
                    cardMLResult.setVisibility(View.GONE);
                    mlPredictedVehicle = null;
                }
            }
        });

        updateConfirmButtonState();
    }

    // =====================================================================
    // ML PREDICTION — Calls the Render API and saves to Firestore "requests"
    // =====================================================================

    private void performMLPrediction() {
        // 1. Validate crop is selected
        if (cropGridAdapter == null || cropGridAdapter.getSelectedCrop() == null) {
            ToastUtils.showShort(this, "Please select a crop type");
            return;
        }

        // 2. Validate weight
        String weightStr = etTransportWeight.getText().toString().trim();
        if (weightStr.isEmpty()) {
            ToastUtils.showShort(this, "Please enter crop weight in kg");
            return;
        }

        double weight;
        try {
            weight = Double.parseDouble(weightStr);
            if (weight <= 0) {
                ToastUtils.showShort(this, "Weight must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            ToastUtils.showShort(this, "Invalid weight value");
            return;
        }

        String cropName = cropGridAdapter.getSelectedCrop().getName();

        // 3. Show loading state
        btnPredictVehicle.setEnabled(false);
        btnPredictVehicle.setText("Predicting...");
        cardMLResult.setVisibility(View.GONE);

        // 4. Call the Render ML API directly (no Firestore dependency)
        callRenderMLApi(cropName, weight);
    }

    private void callRenderMLApi(String cropName, double weight) {
        MLPredictionService service = RetrofitClient.getMLPredictionService();
        MLPredictionRequest request = new MLPredictionRequest(cropName, weight);

        service.predictVehicle(request).enqueue(new Callback<MLPredictionResponse>() {
            @Override
            public void onResponse(Call<MLPredictionResponse> call, Response<MLPredictionResponse> response) {
                btnPredictVehicle.setEnabled(true);
                btnPredictVehicle.setText("🤖  Predict Vehicle (ML)");

                if (response.isSuccessful() && response.body() != null) {
                    String vehicleType = response.body().getVehicleType();
                    if (vehicleType == null || vehicleType.isEmpty()) {
                        vehicleType = "Unknown";
                    }

                    mlPredictedVehicle = vehicleType;

                    // Show ML result in UI
                    showMLPredictionResult(vehicleType);

                    // Show material dialog
                    showPredictionDialog(cropName, weight, vehicleType);

                    // Save to Firestore 'requests' as best-effort (non-blocking)
                    saveRequestToFirestore(cropName, weight, vehicleType, "predicted");
                } else {
                    // API returned error — fall back to local logic
                    handleMLFallback(cropName, weight);
                }
            }

            @Override
            public void onFailure(Call<MLPredictionResponse> call, Throwable t) {
                btnPredictVehicle.setEnabled(true);
                btnPredictVehicle.setText("🤖  Predict Vehicle (ML)");
                // Network failure — fall back to local logic
                handleMLFallback(cropName, weight);
            }
        });
    }

    private void saveRequestToFirestore(String cropName, double weight, String predictedVehicle, String status) {
        try {
            String farmerId = FirebaseAuth.getInstance().getCurrentUser() != null
                    ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                    : preferenceManager.getUserId();

            Map<String, Object> requestData = new HashMap<>();
            requestData.put("cropType", cropName);
            requestData.put("weight", weight);
            requestData.put("predictedVehicle", predictedVehicle);
            requestData.put("farmerId", farmerId);
            requestData.put("farmerName", preferenceManager.getUserName());
            requestData.put("timestamp", System.currentTimeMillis());
            requestData.put("status", status);

            db.collection("requests").add(requestData);
            // Best-effort — silently ignore failures
        } catch (Exception ignored) {
        }
    }

    private void handleMLFallback(String cropName, double weight) {
        // Use local BookingRecommendationEngine as fallback
        List<VehicleSuggestion> recs = BookingRecommendationEngine.getTransportRecommendations(cropName, weight);
        String fallbackVehicle = "Transport Vehicle";
        if (!recs.isEmpty()) {
            fallbackVehicle = recs.get(0).getVehicleName();
        }
        mlPredictedVehicle = fallbackVehicle;

        showMLPredictionResult(fallbackVehicle + " (offline)");

        // Save offline prediction to Firestore as best-effort
        saveRequestToFirestore(cropName, weight, fallbackVehicle + " (offline)", "predicted_offline");

        new MaterialAlertDialogBuilder(this)
                .setTitle("⚠️ ML Server Unavailable")
                .setMessage("Could not reach the ML model. Using local prediction instead.\n\n"
                        + "Crop: " + cropName + "\n"
                        + "Weight: " + String.format(Locale.US, "%.0f kg", weight) + "\n\n"
                        + "Suggested Vehicle: " + fallbackVehicle)
                .setPositiveButton("OK", null)
                .show();
    }

    private void showMLPredictionResult(String vehicleType) {
        if (cardMLResult == null) return;

        tvMLPredictedVehicle.setText(vehicleType);

        // Set icon based on vehicle type
        String vt = vehicleType.toLowerCase();
        if (vt.contains("auto")) {
            ivMLVehicleIcon.setImageResource(R.drawable.ic_auto_vehicle);
        } else if (vt.contains("small van") || vt.contains("small_van")) {
            ivMLVehicleIcon.setImageResource(R.drawable.ic_small_van);
        } else if (vt.contains("mini")) {
            ivMLVehicleIcon.setImageResource(R.drawable.ic_mini_truck);
        } else if (vt.contains("pickup")) {
            ivMLVehicleIcon.setImageResource(R.drawable.ic_pickup_truck);
        } else if (vt.contains("large") || vt.contains("lorry") || vt.contains("heavy")) {
            ivMLVehicleIcon.setImageResource(R.drawable.ic_large_truck);
        } else if (vt.contains("truck")) {
            ivMLVehicleIcon.setImageResource(R.drawable.ic_truck);
        } else {
            ivMLVehicleIcon.setImageResource(R.drawable.ic_truck);
        }

        cardMLResult.setVisibility(View.VISIBLE);
    }

    private void showPredictionDialog(String cropName, double weight, String vehicleType) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("🚛 ML Vehicle Prediction")
                .setMessage("Based on your inputs:\n\n"
                        + "🌾 Crop: " + cropName + "\n"
                        + "⚖️ Weight: " + String.format(Locale.US, "%.0f kg", weight) + "\n\n"
                        + "The ML model recommends:\n\n"
                        + "🚚 " + vehicleType)
                .setPositiveButton("Great!", null)
                .setNeutralButton("Proceed to Book", (dialog, which) -> {
                    // Focus on destination search to continue booking flow
                    etDestSearch.requestFocus();
                })
                .show();
    }

    // =====================================================================
    // MAP SETUP
    // =====================================================================

    private void setupTransportMap() {
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        transportMap = googleMap;
        mapReady = true;

        // Apply Green Map Style (Commented out to prevent Emulator GPU ANR)
        /*
        try {
            boolean success = googleMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.map_style_green));
            if (!success) {
                android.util.Log.e("MapStyle", "Style parsing failed.");
            }
        } catch (android.content.res.Resources.NotFoundException e) {
            android.util.Log.e("MapStyle", "Can't find style. Error: ", e);
        }
        */

        LatLng india = new LatLng(20.5937, 78.9629);
        transportMap.moveCamera(CameraUpdateFactory.newLatLngZoom(india, 5f));

        transportMap.getUiSettings().setZoomControlsEnabled(true);
        transportMap.getUiSettings().setMapToolbarEnabled(false);

        transportMap.setOnMapClickListener(latLng -> {
            if (isSelectingPickup) {
                sourceLatLng = latLng;
                setSourceMarker(latLng);
                reverseGeocode(latLng, true);
                isSelectingPickup = false;
                if (destLatLng != null) {
                    calculateDistanceAndPrice();
                    drawRoute();
                }
                updateConfirmButtonState();
                ToastUtils.showShort(this, "Pickup set. Now tap maps for destination.");
            } else {
                setDestination(latLng);
                reverseGeocode(latLng, false);
            }
        });

        transportMap.setOnMapLongClickListener(latLng -> {
            setDestination(latLng);
            reverseGeocode(latLng, false);
        });

        detectCurrentLocation();
    }

    // =====================================================================
    // LOCATION
    // =====================================================================

    private void detectCurrentLocation() {
        if (tvPickupAddress != null) tvPickupAddress.setText("Detecting your location...");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            com.google.android.gms.location.FusedLocationProviderClient fused = 
                com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);
            fused.getLastLocation().addOnSuccessListener(location -> {
                if (location != null && Math.abs(location.getLatitude()) > 1.0) {
                    // Real device location
                    sourceLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                } else {
                    // Emulator fallback (Ameerpet, Hyderabad)
                    sourceLatLng = new LatLng(17.4400, 78.4500);
                }
                setSourceMarker(sourceLatLng);
                reverseGeocode(sourceLatLng, true); // Get real address
                
                if (transportMap != null) {
                    transportMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sourceLatLng, 15f));
                }
                if (destLatLng != null) {
                    calculateDistanceAndPrice();
                    drawRoute();
                }
                updateConfirmButtonState();
            }).addOnFailureListener(e -> {
                // Fallback
                sourceLatLng = new LatLng(17.4400, 78.4500);
                setSourceMarker(sourceLatLng);
                reverseGeocode(sourceLatLng, true);
                if (transportMap != null) {
                    transportMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sourceLatLng, 15f));
                }
                updateConfirmButtonState();
            });
        } else {
            // No permission fallback
            sourceLatLng = new LatLng(17.4400, 78.4500);
            setSourceMarker(sourceLatLng);
            sourceAddress = "Ameerpet, Hyderabad";
            if (tvPickupAddress != null) tvPickupAddress.setText(sourceAddress);
            if (transportMap != null) {
                transportMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sourceLatLng, 15f));
            }
            updateConfirmButtonState();
        }
    }

    private void showLocationFallbackDialog() {
        if (tvPickupAddress != null) tvPickupAddress.setText("Location unavailable");
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Location Unavailable")
                .setMessage("Turn on location for better accuracy or select manually.")
                .setPositiveButton("Select Manually", (dialog, which) -> {
                    isSelectingPickup = true;
                    LatLng hyderabad = new LatLng(17.3850, 78.4867);
                    sourceLatLng = hyderabad;
                    setSourceMarker(hyderabad);
                    reverseGeocode(hyderabad, true);
                    if (transportMap != null) {
                        transportMap.animateCamera(CameraUpdateFactory.newLatLngZoom(hyderabad, 10f));
                    }
                    ToastUtils.showShort(this, "Tap map to set manual pickup location");
                })
                .setNegativeButton("Use Current Location", (dialog, which) -> {
                    detectCurrentLocation();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            detectCurrentLocation();
        } else {
            showLocationFallbackDialog();
        }
    }

    // =====================================================================
    // MARKERS & ROUTES
    // =====================================================================

    private void setSourceMarker(LatLng latLng) {
        if (sourceMarker != null) {
            sourceMarker.remove();
        }
        sourceMarker = transportMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Pickup Location")
                .icon(getBitmapFromVector(R.drawable.ic_source_pin)));
    }

    private void setDestination(LatLng latLng) {
        destLatLng = latLng;
        if (destMarker != null) {
            destMarker.remove();
        }
        destMarker = transportMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Destination")
                .icon(getBitmapFromVector(R.drawable.ic_dest_pin)));

        if (sourceLatLng != null) {
            zoomToShowBothMarkers();
            calculateDistanceAndPrice();
            drawRoute();
        }
        updateConfirmButtonState();
    }

    private BitmapDescriptor getBitmapFromVector(int resId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(this, resId);
        if (vectorDrawable == null) return BitmapDescriptorFactory.defaultMarker();
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void zoomToShowBothMarkers() {
        if (sourceLatLng == null || destLatLng == null) return;

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(sourceLatLng);
        builder.include(destLatLng);
        LatLngBounds bounds = builder.build();

        int padding = 150;
        transportMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, padding),
                800,
                null
        );
    }

    private void drawRoute() {
        if (sourceLatLng == null || destLatLng == null || transportMap == null) return;

        MapUtils.fetchRoute(this, sourceLatLng, destLatLng, new MapUtils.RouteCallback() {
            @Override
            public void onRouteFetched(List<LatLng> path, String distance, String duration) {
                if (routeShadowPolyline != null) routeShadowPolyline.remove();
                if (routePolyline != null) routePolyline.remove();

                routePolyline = MapUtils.drawRoute(transportMap, path, true); // true = dotted
            }

            @Override
            public void onError(String message) {
                // VISUAL DIAGNOSTIC: Show toast to user so they know why it's a straight line
                Toast.makeText(TransportBookingActivity.this, "Routing Error: " + message, Toast.LENGTH_LONG).show();

                // Fallback to straight line if API fails
                if (routePolyline != null) routePolyline.remove();
                routePolyline = transportMap.addPolyline(new PolylineOptions()
                        .add(sourceLatLng, destLatLng)
                        .width(10f)
                        .color(Color.RED) // RED indicates FALLBACK
                        .geodesic(true));
            }
        });
    }

    // =====================================================================
    // GEOCODING
    // =====================================================================

    private void reverseGeocode(LatLng latLng, boolean isSource) {
        if (!Geocoder.isPresent()) {
            if (isSource) {
                sourceAddress = String.format(Locale.US, "%.4f, %.4f", latLng.latitude, latLng.longitude);
                if (tvPickupAddress != null) tvPickupAddress.setText(sourceAddress);
            }
            return;
        }

        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    if (addr.getSubLocality() != null) sb.append(addr.getSubLocality()).append(", ");
                    if (addr.getLocality() != null) sb.append(addr.getLocality());
                    else if (addr.getSubAdminArea() != null) sb.append(addr.getSubAdminArea());
                    
                    if (sb.length() == 0 && addr.getAddressLine(0) != null) {
                        sb.append(addr.getAddressLine(0));
                    }

                    String addressText = sb.toString();
                    if (addressText.isEmpty()) {
                        addressText = String.format(Locale.US, "%.4f, %.4f", latLng.latitude, latLng.longitude);
                    }

                    final String finalAddress = addressText;
                    runOnUiThread(() -> {
                        if (isSource) {
                            sourceAddress = finalAddress;
                            if (tvPickupAddress != null) tvPickupAddress.setText(finalAddress);
                        } else {
                            destAddress = finalAddress;
                            etDestSearch.setText(finalAddress);
                        }
                    });
                }
            } catch (IOException e) {
                runOnUiThread(() -> {
                    String coords = String.format(Locale.US, "%.4f, %.4f", latLng.latitude, latLng.longitude);
                    if (isSource) {
                        sourceAddress = coords;
                        if (tvPickupAddress != null) tvPickupAddress.setText(coords);
                    } else {
                        destAddress = coords;
                        etDestSearch.setText(coords);
                    }
                });
            }
        }).start();
    }

    private void searchDestination(String query) {
        if (!Geocoder.isPresent()) {
            ToastUtils.showShort(this, "Geocoder not available on this device");
            return;
        }

        ToastUtils.showShort(this, "Searching...");

        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> results = geocoder.getFromLocationName(query, 1);
                if (results != null && !results.isEmpty()) {
                    Address addr = results.get(0);
                    LatLng latLng = new LatLng(addr.getLatitude(), addr.getLongitude());

                    runOnUiThread(() -> {
                        setDestination(latLng);
                        destAddress = query;
                        if (addr.getLocality() != null) {
                            destAddress = addr.getLocality();
                            if (addr.getSubLocality() != null) {
                                destAddress = addr.getSubLocality() + ", " + addr.getLocality();
                            }
                        }
                        etDestSearch.setText(destAddress);
                    });
                } else {
                    runOnUiThread(() ->
                            ToastUtils.showShort(this, getString(R.string.location_not_found)));
                }
            } catch (IOException e) {
                runOnUiThread(() ->
                        ToastUtils.showShort(this, "Search failed. Check internet connection."));
            }
        }).start();
    }

    // =====================================================================
    // PRICE & VEHICLE SUGGESTION
    // =====================================================================

    private void calculateDistanceAndPrice() {
        if (sourceLatLng == null || destLatLng == null) return;

        float[] results = new float[1];
        Location.distanceBetween(
                sourceLatLng.latitude, sourceLatLng.longitude,
                destLatLng.latitude, destLatLng.longitude,
                results);

        calculatedDistance = results[0] / 1000.0;
        calculatedDistance = Math.round(calculatedDistance * 10.0) / 10.0;

        // Base fare based on ML prediction or default
        double baseFare = 500.0; // Default base fare
        if (mlPredictedVehicle != null) {
            String vt = mlPredictedVehicle.toLowerCase();
            if (vt.contains("lorry") || vt.contains("heavy") || vt.contains("large truck")) {
                baseFare = 1500.0;
            } else if (vt.contains("truck")) {
                baseFare = 1000.0;
            } else if (vt.contains("auto")) {
                baseFare = 200.0;
            }
        }

        estimatedPrice = baseFare + (calculatedDistance * RATE_PER_KM);
        estimatedPrice = Math.round(estimatedPrice);

        tvDistance.setText(String.format(Locale.US, "%.1f km", calculatedDistance));
        tvEstimatedPrice.setText(String.format(Locale.US, "₹%.0f", estimatedPrice));
        layoutDistancePrice.setVisibility(View.VISIBLE);

        if (tvMLEstimatedFare != null) {
            tvMLEstimatedFare.setText(String.format(Locale.US, "Est. Fare: ₹%,.0f", estimatedPrice));
            tvMLEstimatedFare.setVisibility(View.VISIBLE);
        }

        updateVehicleSuggestionDisplay();
    }

    private void updateVehicleSuggestionDisplay() {
        String weightStr = etTransportWeight.getText().toString().trim();
        if (weightStr.isEmpty() || calculatedDistance <= 0) {
            layoutVehicleType.setVisibility(View.GONE);
            return;
        }

        try {
            double weight = Double.parseDouble(weightStr);
            String cropName = "Wheat";
            if (cropGridAdapter != null && cropGridAdapter.getSelectedCrop() != null) {
                cropName = cropGridAdapter.getSelectedCrop().getName();
            }
            List<VehicleSuggestion> recommendations = BookingRecommendationEngine.getTransportRecommendations(
                    cropName, weight);

            if (!recommendations.isEmpty()) {
                VehicleSuggestion vehicle = recommendations.get(0);
                tvVehicleType.setText(vehicle.getVehicleName());
                tvVehicleCapacity.setText(String.format(Locale.US, "Capacity: %d kg", vehicle.getCapacity()));

                String vehicleType = vehicle.getVehicleType();
                if ("auto".equals(vehicleType)) {
                    ivVehicleIcon.setImageResource(R.drawable.ic_auto_vehicle);
                } else if ("small_van".equals(vehicleType) || "small van".equalsIgnoreCase(vehicleType)) {
                    ivVehicleIcon.setImageResource(R.drawable.ic_small_van);
                } else if ("mini_truck".equals(vehicleType)) {
                    ivVehicleIcon.setImageResource(R.drawable.ic_mini_truck);
                } else if ("pickup_truck".equals(vehicleType) || "pickup truck".equalsIgnoreCase(vehicleType)) {
                    ivVehicleIcon.setImageResource(R.drawable.ic_pickup_truck);
                } else if ("truck".equals(vehicleType)) {
                    ivVehicleIcon.setImageResource(R.drawable.ic_truck);
                } else if ("lorry".equals(vehicleType) || "large_truck".equals(vehicleType) || "large truck".equalsIgnoreCase(vehicleType)) {
                    ivVehicleIcon.setImageResource(R.drawable.ic_large_truck);
                } else {
                    ivVehicleIcon.setImageResource(R.drawable.ic_truck);
                }

                double estimatedTimeMinutes = (calculatedDistance / 40.0) * 60.0;
                if (estimatedTimeMinutes < 60) {
                    tvEstimatedTime.setText(String.format(Locale.US, "%.0f min", estimatedTimeMinutes));
                } else {
                    double hours = estimatedTimeMinutes / 60.0;
                    tvEstimatedTime.setText(String.format(Locale.US, "%.1f hr", hours));
                }

                layoutVehicleType.setVisibility(View.VISIBLE);
            }
        } catch (NumberFormatException e) {
            layoutVehicleType.setVisibility(View.GONE);
        }
    }

    // =====================================================================
    // BOOKING
    // =====================================================================

    private void updateConfirmButtonState() {
        boolean hasPickup = sourceLatLng != null;
        boolean hasDrop = destLatLng != null;
        String weightStr = etTransportWeight.getText().toString().trim();
        boolean hasWeight = !weightStr.isEmpty();

        boolean canConfirm = hasPickup && hasDrop && hasWeight;

        btnConfirmTransport.setEnabled(canConfirm);
        btnConfirmTransport.setAlpha(canConfirm ? 1.0f : 0.5f);

        if (canConfirm) {
            btnConfirmTransport.setText("CONFIRM BOOKING");
            btnConfirmTransport.setBackgroundColor(Color.parseColor("#16A34A"));
        } else {
            btnConfirmTransport.setText("SET PICKUP & DROP FIRST");
            btnConfirmTransport.setBackgroundColor(Color.parseColor("#4ADE80"));
        }
    }

    private void confirmTransportBooking() {
        if (sourceLatLng == null || destLatLng == null) return;
        
        String weightStr = etTransportWeight.getText().toString();
        if (weightStr.isEmpty()) return;

        btnConfirmTransport.setEnabled(false);
        btnConfirmTransport.setText("Processing...");

        calculateDistanceAndPrice();
        double weight = Double.parseDouble(weightStr);

        String cropName = "Wheat";
        if (cropGridAdapter != null && cropGridAdapter.getSelectedCrop() != null) {
            cropName = cropGridAdapter.getSelectedCrop().getName();
        }
        
        // Use ML prediction if available, otherwise fallback to local
        String vehicleType;
        if (mlPredictedVehicle != null && !mlPredictedVehicle.isEmpty()) {
            vehicleType = mlPredictedVehicle;
        } else {
            List<VehicleSuggestion> recommendations = BookingRecommendationEngine.getTransportRecommendations(
                    cropName, weight);
            vehicleType = "Transport Vehicle";
            if (!recommendations.isEmpty()) {
                vehicleType = recommendations.get(0).getVehicleName();
            }
        }

        final String finalVehicleType = vehicleType;
        final String finalWeightStr = weightStr;

        // Generate 4-digit OTP for pickup verification
        String otp = String.valueOf(1000 + new java.util.Random().nextInt(9000));
        lastBookingOtp = otp;

        String farmerId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : preferenceManager.getUserId();

        Map<String, Object> request = new HashMap<>();
        request.put("farmerId", farmerId);
        request.put("farmerName", preferenceManager.getUserName());
        request.put("farmerPhone", preferenceManager.getUserPhone());
        request.put("cropType", cropName);
        request.put("weight", weight);
        request.put("vehicleType", finalVehicleType);
        request.put("mlPredicted", mlPredictedVehicle != null);
        request.put("status", "REQUESTED");
        request.put("otp", otp);
        request.put("assignedDriverId", null);
        request.put("timestamp", System.currentTimeMillis());
        request.put("cost", estimatedPrice);
        request.put("price", estimatedPrice);
        request.put("fromLocation", sourceAddress);
        request.put("toLocation", destAddress);
        request.put("sourceLat", sourceLatLng.latitude);
        request.put("sourceLng", sourceLatLng.longitude);
        request.put("destLat", destLatLng.latitude);
        request.put("destLng", destLatLng.longitude);
        request.put("sourceAddress", sourceAddress);
        request.put("destAddress", destAddress);
        request.put("distance", calculatedDistance);
        request.put("estimatedPrice", estimatedPrice);

        // STEP 1: Create document in transport_requests with status="waiting"
        db.collection("transport_requests").add(request)
            .addOnSuccessListener(documentReference -> {
                lastBookingId = documentReference.getId();
                btnConfirmTransport.setEnabled(true);
                btnConfirmTransport.setText("CONFIRM BOOKING");

                // STEP 2: Show dispatching UI & begin driver search
                startDispatchPolling(lastBookingId, finalVehicleType, finalWeightStr);
            })
            .addOnFailureListener(e -> {
                btnConfirmTransport.setEnabled(true);
                btnConfirmTransport.setText("CONFIRM BOOKING");
                ToastUtils.showShort(TransportBookingActivity.this, "Booking Failed: " + e.getMessage());
            });
    }

    // =====================================================================
    // DISPATCH FLOW — Real-time driver search & assignment
    // =====================================================================

    private void startDispatchPolling(String requestId, String vehicle, String payload) {
        layoutBookingForm.setVisibility(View.GONE);
        layoutDispatching.setVisibility(View.VISIBLE);
        layoutDriverAccepted.setVisibility(View.GONE);

        if (tvDispatchPayload != null) {
            tvDispatchPayload.setText("Vehicle: " + vehicle + " | Payload: " + payload + "KG");
        }

        // Center map back to origin to show radar
        if (transportMap != null && sourceLatLng != null) {
            transportMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sourceLatLng, 13.5f));
            startRadarAnimation();
            generateMockDriverMarkers(sourceLatLng);
        }

        // STEP 3: Listen for real-time status changes on this transport_request
        if (dispatchListener != null) dispatchListener.remove();
        dispatchListener = db.collection("transport_requests").document(requestId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    String status = snapshot.getString("status");
                    if ("ACCEPTED".equalsIgnoreCase(status) && !isFinishing()) {
                        // Driver accepted! Show accepted UI with OTP
                        showDriverAccepted(snapshot);
                    } else if ("REJECTED".equalsIgnoreCase(status) && !isFinishing()) {
                        // Pop queue and bounce to next driver
                        List<String> queue = (List<String>) snapshot.get("driverQueue");
                        Long idxLong = snapshot.getLong("currentDriverIndex");
                        if (queue != null && idxLong != null) {
                            int nextIdx = idxLong.intValue() + 1;
                            if (nextIdx < queue.size()) {
                                Map<String, Object> bounceUpdates = new HashMap<>();
                                bounceUpdates.put("currentDriverIndex", nextIdx);
                                bounceUpdates.put("assignedDriverId", queue.get(nextIdx));
                                bounceUpdates.put("status", "REQUESTED");
                                db.collection("transport_requests").document(requestId).update(bounceUpdates);
                            } else {
                                ToastUtils.showShort(TransportBookingActivity.this, "All nearby drivers busy. Restarting search...");
                                assignAvailableDriver(requestId, vehicle);
                            }
                        }
                    }
                });

        // STEP 4: Search for an available driver and assign them
        assignAvailableDriver(requestId, vehicle);
    }

    private void assignAvailableDriver(String requestId, String vehicleType) {
        if (sourceLatLng == null) return;
        
        com.firebase.geofire.GeoLocation center = new com.firebase.geofire.GeoLocation(sourceLatLng.latitude, sourceLatLng.longitude);
        double radiusInM = 15000; // 15km for rural coverage
        
        List<com.firebase.geofire.GeoQueryBounds> bounds = com.firebase.geofire.GeoFireUtils.getGeoHashQueryBounds(center, radiusInM);
        List<com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot>> tasks = new ArrayList<>();
        
        for (com.firebase.geofire.GeoQueryBounds b : bounds) {
            com.google.firebase.firestore.Query q = db.collection("drivers")
                .whereEqualTo("vehicleType", vehicleType)
                .orderBy("vehicleType")
                .orderBy("geoHash")
                .startAt(b.startHash)
                .endAt(b.endHash);
            tasks.add(q.get());
        }
        
        com.google.android.gms.tasks.Tasks.whenAllComplete(tasks).addOnCompleteListener(t -> {
            if (isFinishing()) return;
            List<com.google.firebase.firestore.DocumentSnapshot> matchingDocs = new ArrayList<>();
            
            for (com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task : tasks) {
                if (task.isSuccessful() && task.getResult() != null) {
                    for (com.google.firebase.firestore.DocumentSnapshot doc : task.getResult().getDocuments()) {
                        Double lat = doc.getDouble("currentLat");
                        Double lng = doc.getDouble("currentLng");
                        Boolean isAvail = doc.getBoolean("isAvailable");
                        
                        if (lat != null && lng != null && Boolean.TRUE.equals(isAvail)) {
                            com.firebase.geofire.GeoLocation docLoc = new com.firebase.geofire.GeoLocation(lat, lng);
                            double dist = com.firebase.geofire.GeoFireUtils.getDistanceBetween(docLoc, center);
                            if (dist <= radiusInM) {
                                matchingDocs.add(doc);
                            }
                        }
                    }
                }
            }
            
            if (matchingDocs.isEmpty()) {
                ToastUtils.showShort(this, "No drivers found nearby. Searching again...");
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (!isFinishing() && layoutDispatching.getVisibility() == View.VISIBLE) {
                        assignAvailableDriver(requestId, vehicleType);
                    }
                }, 10000);
                return;
            }
            
            java.util.Collections.sort(matchingDocs, (d1, d2) -> {
                double dist1 = com.firebase.geofire.GeoFireUtils.getDistanceBetween(center, new com.firebase.geofire.GeoLocation(d1.getDouble("currentLat"), d1.getDouble("currentLng")));
                double dist2 = com.firebase.geofire.GeoFireUtils.getDistanceBetween(center, new com.firebase.geofire.GeoLocation(d2.getDouble("currentLat"), d2.getDouble("currentLng")));
                return Double.compare(dist1, dist2);
            });
            
            List<String> driverQueue = new ArrayList<>();
            for (com.google.firebase.firestore.DocumentSnapshot d : matchingDocs) {
                driverQueue.add(d.getId());
            }
            
            Map<String, Object> updates = new HashMap<>();
            updates.put("driverQueue", driverQueue);
            updates.put("currentDriverIndex", 0);
            updates.put("assignedDriverId", driverQueue.get(0));
            updates.put("status", "REQUESTED");
            
            db.collection("transport_requests").document(requestId).update(updates);
        });
    }

    private void cancelDispatchSimulation() {
        // Clean up Firestore listener
        if (dispatchListener != null) {
            dispatchListener.remove();
            dispatchListener = null;
        }

        // Cancel the transport request in Firestore
        if (lastBookingId != null) {
            db.collection("transport_requests").document(lastBookingId)
                .update("status", "CANCELLED");
        }

        layoutDispatching.setVisibility(View.GONE);
        layoutDriverAccepted.setVisibility(View.GONE);
        layoutBookingForm.setVisibility(View.VISIBLE);
        clearMockMarkersAndRadar();
        updateConfirmButtonState();
    }

    private String acceptedDriverPhone = null;

    private void showDriverAccepted(com.google.firebase.firestore.DocumentSnapshot snapshot) {
        clearMockMarkersAndRadar();
        layoutDispatching.setVisibility(View.GONE);
        layoutDriverAccepted.setVisibility(View.VISIBLE);

        // Display OTP prominently
        if (tvOtpDisplay != null && lastBookingOtp != null) {
            tvOtpDisplay.setText(lastBookingOtp);
        }

        // Fetch and display driver details
        String driverId = snapshot.getString("driverId");
        if (driverId != null) {
            db.collection("drivers").document(driverId).get().addOnSuccessListener(driverDoc -> {
                if (driverDoc.exists()) {
                    String name = driverDoc.getString("name");
                    String vehicleNo = driverDoc.getString("vehicleRegNumber");
                    String vehicleType = driverDoc.getString("vehicleType");
                    acceptedDriverPhone = driverDoc.getString("phone");

                    // Store driver phone in booking for TrackingActivity
                    if (lastBookingId != null && acceptedDriverPhone != null) {
                        db.collection("transport_requests").document(lastBookingId)
                            .update("driverPhone", acceptedDriverPhone,
                                    "driverName", name != null ? name : "Driver",
                                    "driverVehicleNo", vehicleNo != null ? vehicleNo : "",
                                    "driverVehicleType", vehicleType != null ? vehicleType : "");
                    }

                    if (tvDriverName != null) {
                        StringBuilder info = new StringBuilder();
                        info.append(name != null ? name : "Driver");
                        if (vehicleType != null && !vehicleType.isEmpty()) {
                            info.append("\n").append(vehicleType);
                        }
                        if (vehicleNo != null && !vehicleNo.isEmpty()) {
                            info.append(" • ").append(vehicleNo);
                        }
                        tvDriverName.setText(info.toString());
                    }
                    if (tvDriverEta != null) {
                        tvDriverEta.setText("Driver is on the way!");
                    }
                }
            });
        }
    }

    private void startRadarAnimation() {
        if (transportMap == null || sourceLatLng == null) return;
        
        radarCircle = transportMap.addCircle(new com.google.android.gms.maps.model.CircleOptions()
                .center(sourceLatLng)
                .radius(0)
                .strokeWidth(2f)
                .strokeColor(Color.parseColor("#16A34A"))
                .fillColor(Color.parseColor("#3316A34A")));

        radarAnimator = android.animation.ValueAnimator.ofFloat(0, 5000);
        radarAnimator.setDuration(3000);
        radarAnimator.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        radarAnimator.setRepeatMode(android.animation.ValueAnimator.RESTART);
        radarAnimator.addUpdateListener(animation -> {
            if (radarCircle != null) {
                radarCircle.setRadius((Float) animation.getAnimatedValue());
            }
        });
        radarAnimator.start();
    }

    private void generateMockDriverMarkers(LatLng center) {
        if (transportMap == null) return;
        
        // Create 3 fake markers within slightly scattered offsets
        LatLng[] offsets = new LatLng[]{
            new LatLng(center.latitude + 0.015, center.longitude + 0.02),
            new LatLng(center.latitude - 0.02, center.longitude + 0.01),
            new LatLng(center.latitude + 0.01, center.longitude - 0.015)
        };

        int iconRes = R.drawable.ic_map_marker_truck;
        String title = "Vehicle";
        if (tvVehicleType != null && tvVehicleType.getText() != null) {
            String vt = tvVehicleType.getText().toString().toLowerCase();
            if (vt.contains("small van")) iconRes = R.drawable.ic_map_marker_small_van;
            else if (vt.contains("mini truck")) iconRes = R.drawable.ic_map_marker_mini_truck;
            else if (vt.contains("pickup")) iconRes = R.drawable.ic_map_marker_pickup_truck;
            else if (vt.contains("large") || vt.contains("lorry")) iconRes = R.drawable.ic_map_marker_large_truck;
            else if (vt.contains("auto")) iconRes = R.drawable.ic_map_marker_auto;
            
            title = tvVehicleType.getText().toString();
        }

        for (LatLng pos : offsets) {
            com.google.android.gms.maps.model.Marker m = transportMap.addMarker(new com.google.android.gms.maps.model.MarkerOptions()
                .position(pos)
                .icon(bitmapDescriptorFromVector(iconRes))
                .title(title));
            if (m != null) driverMarkers.add(m);
        }
    }

    private com.google.android.gms.maps.model.BitmapDescriptor bitmapDescriptorFromVector(int vectorResId) {
        android.graphics.drawable.Drawable vectorDrawable = androidx.core.content.ContextCompat.getDrawable(this, vectorResId);
        if (vectorDrawable == null) return com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker();
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        android.graphics.Bitmap bitmap = android.graphics.Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas = new android.graphics.Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return com.google.android.gms.maps.model.BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    private void clearMockMarkersAndRadar() {
        if (radarAnimator != null) {
            radarAnimator.cancel();
            radarAnimator = null;
        }
        if (radarCircle != null) {
            radarCircle.remove();
            radarCircle = null;
        }
        for (com.google.android.gms.maps.model.Marker m : driverMarkers) {
            m.remove();
        }
        driverMarkers.clear();
    }

    private void resetTransportForm() {
        if (destMarker != null) {
            destMarker.remove();
            destMarker = null;
        }
        if (routePolyline != null) {
            routePolyline.remove();
            routePolyline = null;
        }
        if (routeShadowPolyline != null) {
            routeShadowPolyline.remove();
            routeShadowPolyline = null;
        }

        destLatLng = null;
        destAddress = "";
        etDestSearch.setText("");
        etTransportWeight.setText("");
        layoutDistancePrice.setVisibility(View.GONE);
        layoutVehicleType.setVisibility(View.GONE);
        if (cardMLResult != null) cardMLResult.setVisibility(View.GONE);
        mlPredictedVehicle = null;
        calculatedDistance = 0;
        estimatedPrice = 0;

        if (sourceLatLng != null) {
            transportMap.animateCamera(CameraUpdateFactory.newLatLngZoom(sourceLatLng, 14f));
        }

        updateConfirmButtonState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dispatchListener != null) {
            dispatchListener.remove();
            dispatchListener = null;
        }
    }
}
