package com.agrigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.textfield.TextInputEditText;
import com.agrigo.R;
import com.agrigo.adapters.VehicleSuggestionAdapter;
import com.agrigo.models.Crop;
import com.agrigo.models.VehicleSuggestion;
import com.agrigo.network.ApiResponse;
import com.agrigo.utils.CropUtils;
import com.agrigo.utils.PreferenceManager;
import com.agrigo.utils.ToastUtils;
import com.agrigo.utils.ValidationUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Farmer dashboard main activity
 * Displays crop selection, weight input, and vehicle suggestions
 */
public class FarmerDashboardActivity extends AppCompatActivity {

    // UI Elements
    private TextView tvFarmerName, tvGreeting;
    private ImageButton btnMenu;
    private GridLayout cropGridLayout;
    private TextInputEditText etWeight;
    private Button btnGetSuggestion;
    private TextView tvWeightError;
    private TextView tvSuggestedVehicles;
    private RecyclerView rvVehicleSuggestions;
    private ProgressBar progressLoading;
    
    // Navigation Drawer
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    private LinearLayout navigationDrawer;
    private LinearLayout menuHome, menuBookings, menuTrack, menuHelp, menuSettings, menuLogout;
    private TextView tvDrawerUserName, tvDrawerUserPhone, tvUserInitial;
    
    // Data and adapters
    private PreferenceManager preferenceManager;
    private VehicleSuggestionAdapter vehicleSuggestionAdapter;
    private List<Crop> crops;
    private String selectedCropId = CropUtils.CROP_PADDY; // Default crop
    private double weightValue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_farmer_dashboard);
        
        // Initialize preferences
        preferenceManager = new PreferenceManager(this);
        
        // Initialize views
        initializeViews();
        
        // Load crops
        crops = CropUtils.getAllCrops(this);
        
        // Setup listeners
        setupListeners();
        
        // Load user data
        loadUserData();
        
        // Setup drawer
        setupDrawerLayout();
        
        // Apply entrance animations
        applyEntranceAnimations();
    }

    /**
     * Initialize all UI views
     */
    private void initializeViews() {
        tvFarmerName = findViewById(R.id.tvFarmerName);
        tvGreeting = findViewById(R.id.tvGreeting);
        btnMenu = findViewById(R.id.btnMenu);
        cropGridLayout = findViewById(R.id.cropGridLayout);
        etWeight = findViewById(R.id.etWeight);
        btnGetSuggestion = findViewById(R.id.btnGetSuggestion);
        tvWeightError = findViewById(R.id.tvWeightError);
        tvSuggestedVehicles = findViewById(R.id.tvSuggestedVehicles);
        rvVehicleSuggestions = findViewById(R.id.rvVehicleSuggestions);
        progressLoading = findViewById(R.id.progressLoading);
        
        // Setup RecyclerView
        rvVehicleSuggestions.setLayoutManager(new LinearLayoutManager(this));
        vehicleSuggestionAdapter = new VehicleSuggestionAdapter(this, new ArrayList<>());
        rvVehicleSuggestions.setAdapter(vehicleSuggestionAdapter);
    }

    /**
     * Load user data from SharedPreferences
     */
    private void loadUserData() {
        String userName = preferenceManager.getUserName();
        String userPhone = preferenceManager.getUserPhone();
        
        if (userName != null && !userName.isEmpty()) {
            tvFarmerName.setText(userName);
        }
        
        // Set drawer user info too
        if (navigationDrawer != null) {
            tvDrawerUserName.setText(userName);
            tvDrawerUserPhone.setText(userPhone != null ? userPhone : "");
            
            // Set user initial
            if (userName != null && !userName.isEmpty()) {
                String initial = String.valueOf(userName.charAt(0)).toUpperCase();
                tvUserInitial.setText(initial);
            }
        }
    }

    /**
     * Setup drawer layout and menu items
     */
    private void setupDrawerLayout() {
        // Get drawer layout from activity layout - wrap content in DrawerLayout
        // For now, we'll create it programmatically
        drawerLayout = (DrawerLayout) findViewById(android.R.id.content).getParent();
        if (drawerLayout == null) {
            // Drawer wasn't set up, handle gracefully
            return;
        }
        
        navigationDrawer = new LinearLayout(this);
        // Note: In production, inflate from drawer_navigation_menu.xml
        
        try {
            // navigationDrawer = findViewById(R.id.navigationDrawer);
            // tvDrawerUserName = navigationDrawer.findViewById(R.id.tvDrawerUserName);
            // tvDrawerUserPhone = navigationDrawer.findViewById(R.id.tvDrawerUserPhone);
            // tvUserInitial = navigationDrawer.findViewById(R.id.tvUserInitial);
            // 
            // menuHome = navigationDrawer.findViewById(R.id.menuHome);
            // menuBookings = navigationDrawer.findViewById(R.id.menuBookings);
            // menuTrack = navigationDrawer.findViewById(R.id.menuTrack);
            // menuHelp = navigationDrawer.findViewById(R.id.menuHelp);
            menuSettings = navigationDrawer.findViewById(R.id.menuSettings);
            menuLogout = navigationDrawer.findViewById(R.id.menuLogout);
            
            // Setup menu click listeners
            if (menuHome != null) menuHome.setOnClickListener(v -> handleMenuHome());
            if (menuBookings != null) menuBookings.setOnClickListener(v -> handleMenuBookings());
            if (menuTrack != null) menuTrack.setOnClickListener(v -> handleMenuTrack());
            if (menuHelp != null) menuHelp.setOnClickListener(v -> handleMenuHelp());
            if (menuSettings != null) menuSettings.setOnClickListener(v -> handleMenuSettings());
            if (menuLogout != null) menuLogout.setOnClickListener(v -> handleMethodLogout());
            
        } catch (Exception e) {
            // Drawer layout not found in XML, skip drawer setup
        }
    }

    /**
     * Setup all click listeners
     */
    private void setupListeners() {
        // Menu button
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.START);
            } else {
                ToastUtils.showShort(this, "Menu not available");
            }
        });
        
        // Get suggestion button
        btnGetSuggestion.setOnClickListener(v -> handleGetSuggestion());
        
        // Crop selection listeners
        setupCropSelectionListeners();
        
        // Weight input listeners
        etWeight.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                tvWeightError.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Setup crop selection card listeners
     */
    private void setupCropSelectionListeners() {
        for (int i = 0; i < cropGridLayout.getChildCount(); i++) {
            View child = cropGridLayout.getChildAt(i);
            if (child instanceof FrameLayout) {
                FrameLayout cropCard = (FrameLayout) child;
                String cropId = (String) cropCard.getTag();
                
                cropCard.setOnClickListener(v -> selectCrop(cropId, cropCard));
            }
        }
    }

    /**
     * Handle crop selection
     */
    private void selectCrop(String cropId, FrameLayout selectedCard) {
        selectedCropId = cropId;
        
        // Update visual state for all crop cards
        for (int i = 0; i < cropGridLayout.getChildCount(); i++) {
            View child = cropGridLayout.getChildAt(i);
            if (child instanceof FrameLayout) {
                FrameLayout card = (FrameLayout) child;
                if (card == selectedCard) {
                    // Selected state - darker green
                    card.setBackgroundResource(R.drawable.bg_button_green);
                    card.setAlpha(1.0f);
                } else {
                    // Unselected state - lighter
                    card.setAlpha(0.6f);
                }
            }
        }
        
        // Clear weight and error
        etWeight.setText("");
        tvWeightError.setVisibility(View.GONE);
        
        // Show crop selection feedback
        Crop crop = CropUtils.getCropById(cropId, this);
        if (crop != null) {
            String message = getString(R.string.crop_selected, crop.getName());
            ToastUtils.showShort(this, message);
        }
    }

    /**
     * Handle get vehicle suggestion button click
     */
    private void handleGetSuggestion() {
        // Validate weight
        String weightStr = etWeight.getText().toString().trim();
        
        if (weightStr.isEmpty()) {
            tvWeightError.setText(getString(R.string.error_required_field));
            tvWeightError.setVisibility(View.VISIBLE);
            return;
        }
        
        try {
            weightValue = Double.parseDouble(weightStr);
        } catch (NumberFormatException e) {
            tvWeightError.setText(getString(R.string.error_invalid_weight));
            tvWeightError.setVisibility(View.VISIBLE);
            return;
        }
        
        // Validate weight for selected crop
        if (!CropUtils.isValidWeightForCrop(weightValue, selectedCropId, this)) {
            String errorMsg = CropUtils.getWeightErrorMessage(weightValue, selectedCropId, this);
            tvWeightError.setText(errorMsg);
            tvWeightError.setVisibility(View.VISIBLE);
            return;
        }
        
        // Clear errors
        tvWeightError.setVisibility(View.GONE);
        
        // Show loading state
        progressLoading.setVisibility(View.VISIBLE);
        tvSuggestedVehicles.setVisibility(View.GONE);
        rvVehicleSuggestions.setVisibility(View.GONE);
        btnGetSuggestion.setEnabled(false);
        
        // Simulate API call with delay
        fetchVehicleSuggestions();
    }

    /**
     * Fetch vehicle suggestions from API (simulated)
     */
    private void fetchVehicleSuggestions() {
        // Simulated API call with 1.5 second delay
        new android.os.Handler().postDelayed(() -> {
            // Create mock suggestions based on weight
            List<VehicleSuggestion> suggestions = generateMockSuggestions();
            
            // Update UI
            progressLoading.setVisibility(View.GONE);
            
            if (suggestions != null && !suggestions.isEmpty()) {
                tvSuggestedVehicles.setVisibility(View.VISIBLE);
                rvVehicleSuggestions.setVisibility(View.VISIBLE);
                vehicleSuggestionAdapter.setVehicles(suggestions);
                vehicleSuggestionAdapter.setOnVehicleSelectListener(this::handleVehicleSelected);
            } else {
                ToastUtils.showShort(FarmerDashboardActivity.this, getString(R.string.no_vehicles_available));
            }
            
            btnGetSuggestion.setEnabled(true);
        }, 1500);
    }

    /**
     * Generate mock vehicle suggestions based on weight
     */
    private List<VehicleSuggestion> generateMockSuggestions() {
        List<VehicleSuggestion> vehicles = new ArrayList<>();
        
        // Auto for small weight
        if (weightValue <= 500) {
            VehicleSuggestion auto = new VehicleSuggestion("auto", getString(R.string.vehicle_auto), 500);
            auto.setEstimatedCost(150);
            auto.setAvailableCount(3);
            vehicles.add(auto);
        }
        
        // Mini truck for medium weight
        if (weightValue <= 1500) {
            VehicleSuggestion miniTruck = new VehicleSuggestion("mini_truck", getString(R.string.vehicle_mini_truck), 1500);
            miniTruck.setEstimatedCost(300);
            miniTruck.setAvailableCount(5);
            vehicles.add(miniTruck);
        }
        
        // Standard truck for large weight
        if (weightValue <= 3000) {
            VehicleSuggestion truck = new VehicleSuggestion("truck", getString(R.string.vehicle_truck), 3000);
            truck.setEstimatedCost(500);
            truck.setAvailableCount(4);
            vehicles.add(truck);
        }
        
        // Lorry for very heavy weight
        if (weightValue > 2000) {
            VehicleSuggestion lorry = new VehicleSuggestion("lorry", getString(R.string.vehicle_lorry), 8000);
            lorry.setEstimatedCost(1000);
            lorry.setAvailableCount(2);
            vehicles.add(lorry);
        }
        
        return vehicles;
    }

    /**
     * Handle vehicle selection
     */
    private void handleVehicleSelected(VehicleSuggestion vehicle) {
        // In next module, this will create a booking and navigate to booking confirmation
        String message = getString(R.string.vehicle_selected, vehicle.getVehicleName());
        ToastUtils.showShort(this, message);
    }

    /**
     * Handle drawer menu items
     */
    private void handleMenuHome() {
        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
        // Stay on home
    }

    private void handleMenuBookings() {
        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
        startActivity(new Intent(this, MyBookingsActivity.class));
    }

    private void handleMenuTrack() {
        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
        // TODO: Navigate to TrackingActivity when ready
        ToastUtils.showShort(this, getString(R.string.track_vehicle));
    }

    private void handleMenuHelp() {
        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
        ToastUtils.showShort(this, getString(R.string.help_support));
    }

    private void handleMenuSettings() {
        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
        ToastUtils.showShort(this, getString(R.string.settings));
    }

    private void handleMethodLogout() {
        if (drawerLayout != null) drawerLayout.closeDrawer(GravityCompat.START);
        
        // Clear user preferences
        preferenceManager.clearAll();
        
        // Navigate back to welcome
        startActivity(new Intent(this, WelcomeActivity.class));
        finish();
    }

    /**
     * Apply entrance animations
     */
    private void applyEntranceAnimations() {
        // Animate crop grid
        android.view.animation.Animation slideInAnimation = 
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        cropGridLayout.startAnimation(slideInAnimation);
        
        // Animate button
        android.view.animation.Animation scaleAnimation = 
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.scale_enter);
        btnGetSuggestion.startAnimation(scaleAnimation);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
