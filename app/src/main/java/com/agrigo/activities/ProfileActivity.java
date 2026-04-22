package com.agrigo.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.agrigo.R;
import com.agrigo.utils.PreferenceManager;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextInputEditText editName;
    private TextInputEditText editPhone;
    private TextInputEditText editRole;
    private TextInputEditText editVehicleType;
    private TextInputLayout layoutVehicleType;
    private CardView cardVehicleDropdown;
    private LinearLayout containerVehicleTypes;
    private MaterialButton btnSave;
    private MaterialButton btnLanguage;
    private MaterialButton btnLogout;

    private FirebaseAuth mAuth;
    private PreferenceManager preferenceManager;
    private boolean isDropdownOpen = false;
    private int selectedPosition = 0;

    // Vehicle type data — strings match the Crop/Weight/Vehicle mapping exactly
    private static final String[] VEHICLE_NAMES = {
            "Auto", "Small Van", "Mini Truck", "Pickup Truck", "Truck", "Large Truck"
    };

    private static final int[] VEHICLE_ICONS = {
            R.drawable.ic_auto_vehicle,
            R.drawable.ic_small_van,
            R.drawable.ic_mini_truck,
            R.drawable.ic_pickup_truck,
            R.drawable.ic_truck,
            R.drawable.ic_large_truck
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        preferenceManager = new PreferenceManager(this);

        initViews();
        setupListeners();
        loadUserInfo();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back);
        editName = findViewById(R.id.edit_name);
        editPhone = findViewById(R.id.edit_phone);
        editRole = findViewById(R.id.edit_role);
        editVehicleType = findViewById(R.id.edit_vehicle_type);
        layoutVehicleType = findViewById(R.id.layout_vehicle_type);
        cardVehicleDropdown = findViewById(R.id.card_vehicle_dropdown);
        containerVehicleTypes = findViewById(R.id.container_vehicle_types);
        btnSave = findViewById(R.id.btn_save);
        btnLanguage = findViewById(R.id.btn_language);
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void setupVehicleDropdown() {
        // Restore saved selection or default to "Auto" (index 0)
        String savedVehicle = preferenceManager.getVehicleType();
        if (savedVehicle != null && !savedVehicle.isEmpty()) {
            for (int i = 0; i < VEHICLE_NAMES.length; i++) {
                if (VEHICLE_NAMES[i].equals(savedVehicle)) {
                    selectedPosition = i;
                    break;
                }
            }
        }
        editVehicleType.setText(VEHICLE_NAMES[selectedPosition]);
        updateFieldIcon(selectedPosition);

        // Build vehicle rows inside the LinearLayout
        populateVehicleList();

        // Toggle dropdown on field click
        editVehicleType.setOnClickListener(v -> toggleDropdown());
        layoutVehicleType.setEndIconOnClickListener(v -> toggleDropdown());
    }

    /**
     * Programmatically inflates one row per vehicle type into the LinearLayout.
     * This avoids the ListView-inside-ScrollView height bug.
     */
    private void populateVehicleList() {
        containerVehicleTypes.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < VEHICLE_NAMES.length; i++) {
            final int position = i;
            View row = inflater.inflate(R.layout.item_vehicle_type, containerVehicleTypes, false);

            ImageView ivIcon = row.findViewById(R.id.iv_vehicle_icon);
            TextView tvName = row.findViewById(R.id.tv_vehicle_name);
            ImageView ivCheck = row.findViewById(R.id.iv_check);
            View root = row.findViewById(R.id.vehicle_item_root);

            ivIcon.setImageResource(VEHICLE_ICONS[position]);
            tvName.setText(VEHICLE_NAMES[position]);

            // Highlight selected row
            if (position == selectedPosition) {
                ivCheck.setVisibility(View.VISIBLE);
                root.setBackgroundResource(R.drawable.bg_vehicle_selected);
                tvName.setTextColor(getResources().getColor(R.color.primary_blue));
            } else {
                ivCheck.setVisibility(View.GONE);
                root.setBackgroundResource(android.R.color.transparent);
                tvName.setTextColor(0xFF333333);
            }

            row.setOnClickListener(v -> {
                selectedPosition = position;
                editVehicleType.setText(VEHICLE_NAMES[position]);
                updateFieldIcon(position);
                populateVehicleList();   // rebuild to refresh highlight
                closeDropdown();
            });

            containerVehicleTypes.addView(row);
        }
    }

    /** Updates the leading icon on the Vehicle Type TextInputLayout. */
    private void updateFieldIcon(int position) {
        if (position >= 0 && position < VEHICLE_ICONS.length) {
            layoutVehicleType.setStartIconDrawable(VEHICLE_ICONS[position]);
        }
    }

    // --------------- dropdown animation ---------------

    private void toggleDropdown() {
        if (isDropdownOpen) {
            closeDropdown();
        } else {
            openDropdown();
        }
    }

    private void openDropdown() {
        isDropdownOpen = true;
        populateVehicleList(); // refresh highlight before showing
        cardVehicleDropdown.setVisibility(View.VISIBLE);
        cardVehicleDropdown.setAlpha(0f);
        cardVehicleDropdown.setTranslationY(-12f);
        cardVehicleDropdown.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .start();
    }

    private void closeDropdown() {
        isDropdownOpen = false;
        cardVehicleDropdown.animate()
                .alpha(0f)
                .translationY(-12f)
                .setDuration(150)
                .withEndAction(() -> cardVehicleDropdown.setVisibility(View.GONE))
                .start();
    }



    // --------------- listeners & data ---------------

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnLanguage.setOnClickListener(v -> {
            Toast.makeText(this, "Language selection coming soon (English/Hindi)", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> logout());

        btnSave.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            String phone = editPhone.getText().toString().trim();

            if (name.isEmpty()) {
                Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            preferenceManager.setUserName(name);
            preferenceManager.setUserPhone(phone);

            // Conditional save for Driver
            if ("driver".equalsIgnoreCase(preferenceManager.getUserRole())) {
                String vehicleType = editVehicleType.getText().toString().trim();
                preferenceManager.setVehicleType(vehicleType);
            }

            Toast.makeText(this, "Profile Saved Successfully!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void logout() {
        mAuth.signOut();
        preferenceManager.clearAll();

        android.content.Intent intent = new android.content.Intent(this, LoginActivity.class);
        intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void loadUserInfo() {
        String name = preferenceManager.getUserName();
        String phone = preferenceManager.getUserPhone();
        String role = preferenceManager.getUserRole();

        if (name != null && !name.isEmpty()) editName.setText(name);
        if (phone != null && !phone.isEmpty()) editPhone.setText(phone);
        if (role != null && !role.isEmpty()) editRole.setText(role);

        // Dynamic Visibility for Vehicle Type
        if ("driver".equalsIgnoreCase(role)) {
            layoutVehicleType.setVisibility(View.VISIBLE);
            setupVehicleDropdown();
        } else {
            layoutVehicleType.setVisibility(View.GONE);
        }
    }

    @Override
    public void onBackPressed() {
        if (isDropdownOpen) {
            closeDropdown();
        } else {
            super.onBackPressed();
        }
    }
}
