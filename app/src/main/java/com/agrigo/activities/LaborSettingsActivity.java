package com.agrigo.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.agrigo.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import androidx.cardview.widget.CardView;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.view.LayoutInflater;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

public class LaborSettingsActivity extends AppCompatActivity {

    private TextInputEditText editWages, editAvailability, dropdownWorkType;
    private TextInputLayout layoutWorkType;
    private CardView cardWorkDropdown;
    private LinearLayout containerWorkTypes;
    private Slider sliderDistance;
    private TextView txtDistanceValue;
    private MaterialButton btnSave;
    private View btnBack;

    private int selectedWorkPosition = 0;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private String laborId;

    private static final String[] WORK_NAMES = {
            "Land Preparation",
            "Sowing/Planting",
            "Weeding",
            "Irrigation",
            "Harvesting"
    };

    private static final int[] WORK_ICONS = {
            R.drawable.ic_labor_land_prep,
            R.drawable.ic_labor_sowing,
            R.drawable.ic_labor_weeding,
            R.drawable.ic_labor_irrigation,
            R.drawable.ic_labor_harvesting
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_labor_settings);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        if (mAuth.getCurrentUser() != null) {
            laborId = mAuth.getCurrentUser().getUid();
        }

        initViews();
        setupDropdown();
        setupListeners();
        loadSettings();
    }

    private void initViews() {
        editWages = findViewById(R.id.editWages);
        editAvailability = findViewById(R.id.editAvailability);
        sliderDistance = findViewById(R.id.sliderDistance);
        txtDistanceValue = findViewById(R.id.txtDistanceValue);
        dropdownWorkType = findViewById(R.id.dropdownWorkType);
        layoutWorkType = findViewById(R.id.layout_work_type);
        cardWorkDropdown = findViewById(R.id.card_work_dropdown);
        containerWorkTypes = findViewById(R.id.container_work_types);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupDropdown() {
        populateWorkList();
        
        dropdownWorkType.setOnClickListener(v -> toggleDropdown());
        layoutWorkType.setEndIconOnClickListener(v -> toggleDropdown());
    }

    private void toggleDropdown() {
        if (cardWorkDropdown.getVisibility() == View.VISIBLE) {
            cardWorkDropdown.setVisibility(View.GONE);
            layoutWorkType.setEndIconDrawable(R.drawable.ic_dropdown_arrow);
        } else {
            cardWorkDropdown.setVisibility(View.VISIBLE);
            layoutWorkType.setEndIconDrawable(R.drawable.ic_cancel); // or an up arrow
        }
    }

    private void populateWorkList() {
        containerWorkTypes.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < WORK_NAMES.length; i++) {
            final int position = i;
            View row = inflater.inflate(R.layout.item_vehicle_type, containerWorkTypes, false);

            ImageView ivIcon = row.findViewById(R.id.iv_vehicle_icon);
            TextView tvName = row.findViewById(R.id.tv_vehicle_name);
            ImageView ivCheck = row.findViewById(R.id.iv_check);
            View root = row.findViewById(R.id.vehicle_item_root);

            ivIcon.setImageResource(WORK_ICONS[position]);
            tvName.setText(WORK_NAMES[position]);

            if (position == selectedWorkPosition) {
                ivCheck.setVisibility(View.VISIBLE);
                root.setBackgroundResource(R.drawable.bg_vehicle_selected);
                tvName.setTextColor(getResources().getColor(R.color.primary_blue));
            } else {
                ivCheck.setVisibility(View.GONE);
                root.setBackgroundResource(android.R.color.transparent);
                tvName.setTextColor(0xFF333333);
            }

            row.setOnClickListener(v -> {
                selectedWorkPosition = position;
                dropdownWorkType.setText(WORK_NAMES[position]);
                updateFieldIcon(position);
                populateWorkList();
                toggleDropdown();
            });

            containerWorkTypes.addView(row);
        }
    }

    private void updateFieldIcon(int position) {
        if (position >= 0 && position < WORK_ICONS.length) {
            int drawableId = WORK_ICONS[position];
            int targetSizePx = (int) (28 * getResources().getDisplayMetrics().density);
            
            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), drawableId);
            if (bitmap != null) {
                float ratio = Math.min(
                    (float) targetSizePx / bitmap.getWidth(),
                    (float) targetSizePx / bitmap.getHeight()
                );
                int width = Math.round(ratio * bitmap.getWidth());
                int height = Math.round(ratio * bitmap.getHeight());
                if (width <= 0) width = 1;
                if (height <= 0) height = 1;
                
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
                BitmapDrawable d = new BitmapDrawable(getResources(), scaledBitmap);
                layoutWorkType.setStartIconDrawable(d);
            } else {
                layoutWorkType.setStartIconDrawable(drawableId);
            }
            layoutWorkType.setStartIconTintList(null);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        sliderDistance.addOnChangeListener((slider, value, fromUser) -> {
            txtDistanceValue.setText((int) value + " KM");
        });

        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void loadSettings() {
        if (laborId == null) return;
        
        // Show loading state or disable button
        btnSave.setEnabled(false);
        db.collection("labor_workers").document(laborId).get()
            .addOnSuccessListener(doc -> {
                btnSave.setEnabled(true);
                if (doc.exists()) {
                    Long wages = doc.getLong("dailyWage");
                    Long avail = doc.getLong("maxAvailabilityDays");
                    Double dist = doc.getDouble("maxDistanceKm");
                    List<String> types = (List<String>) doc.get("workTypes");
                    
                    if (wages != null) editWages.setText(String.valueOf(wages));
                    if (avail != null) editAvailability.setText(String.valueOf(avail));
                    if (dist != null) {
                        float distVal = dist.floatValue();
                        if (distVal > 30) distVal = 30; // Max slider value
                        sliderDistance.setValue(distVal);
                        txtDistanceValue.setText((int) distVal + " KM");
                    }
                    
                    if (types != null && !types.isEmpty()) {
                        String type = types.get(0).toLowerCase().trim();
                        for (int i = 0; i < WORK_NAMES.length; i++) {
                            if (WORK_NAMES[i].toLowerCase().trim().equals(type)) {
                                selectedWorkPosition = i;
                                break;
                            }
                        }
                    }
                } else {
                    // Default values if no profile yet
                    editWages.setText("500");
                    editAvailability.setText("7");
                    sliderDistance.setValue(10);
                    txtDistanceValue.setText("10 KM");
                }
                dropdownWorkType.setText(WORK_NAMES[selectedWorkPosition]);
                updateFieldIcon(selectedWorkPosition);
                populateWorkList();
            })
            .addOnFailureListener(e -> {
                btnSave.setEnabled(true);
                // Default to first item
                selectedWorkPosition = 0;
                dropdownWorkType.setText(WORK_NAMES[selectedWorkPosition]);
                updateFieldIcon(selectedWorkPosition);
                populateWorkList();
            });
    }

    private void saveSettings() {
        String wages = editWages.getText().toString();
        String availability = editAvailability.getText().toString();
        float distance = sliderDistance.getValue();
        String workType = dropdownWorkType.getText().toString();

        if (wages.isEmpty() || availability.isEmpty() || workType.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        if (laborId == null) return;
        
        btnSave.setEnabled(false);
        
        int dailyWage = Integer.parseInt(wages);
        int maxAvail = Integer.parseInt(availability);
        
        List<String> workTypesList = new ArrayList<>();
        workTypesList.add(workType.toLowerCase().trim());
        
        Map<String, Object> data = new HashMap<>();
        data.put("dailyWage", dailyWage);
        data.put("maxAvailabilityDays", maxAvail);
        data.put("maxDistanceKm", distance);
        data.put("workTypes", workTypesList);
        data.put("workType", workType.toLowerCase().trim()); // legacy fallback

        db.collection("labor_workers").document(laborId)
            .set(data, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(this, "Settings Saved Successfully!", Toast.LENGTH_SHORT).show();
                btnSave.postDelayed(this::finish, 500);
            })
            .addOnFailureListener(e -> {
                btnSave.setEnabled(true);
                Toast.makeText(this, "Failed to save: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}
