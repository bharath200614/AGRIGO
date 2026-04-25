package com.agrigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.agrigo.R;
import com.agrigo.utils.PreferenceManager;
import com.agrigo.utils.ToastUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LaborWorkerProfileActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextInputEditText editName, editPhone, editDailyWage;
    private MaterialButton btnSaveProfile, btnLogout;
    
    private LinearLayout[] categoryViews;
    private ImageView[] categoryIcons;
    private TextView[] categoryTexts;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private PreferenceManager preferenceManager;
    private String laborId;

    private final String[] workTypes = {"land preparation", "sowing/planting", "weeding", "irrigation", "harvesting"};
    private List<String> selectedWorkTypes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_labor_worker_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        preferenceManager = new PreferenceManager(this);
        laborId = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid()
                : preferenceManager.getUserId();

        initViews();
        loadProfile();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        editName = findViewById(R.id.editName);
        editPhone = findViewById(R.id.editPhone);
        editDailyWage = findViewById(R.id.editDailyWage);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);

        categoryViews = new LinearLayout[]{
                findViewById(R.id.catLandPrep),
                findViewById(R.id.catSowing),
                findViewById(R.id.catWeeding),
                findViewById(R.id.catIrrigation),
                findViewById(R.id.catHarvesting)
        };

        categoryIcons = new ImageView[]{
                findViewById(R.id.iconLandPrep),
                findViewById(R.id.iconSowing),
                findViewById(R.id.iconWeeding),
                findViewById(R.id.iconIrrigation),
                findViewById(R.id.iconHarvesting)
        };

        categoryTexts = new TextView[]{
                findViewById(R.id.tvLandPrep),
                findViewById(R.id.tvSowing),
                findViewById(R.id.tvWeeding),
                findViewById(R.id.tvIrrigation),
                findViewById(R.id.tvHarvesting)
        };

        for (int i = 0; i < categoryViews.length; i++) {
            final int index = i;
            categoryViews[i].setOnClickListener(v -> toggleCategory(index));
        }

        btnBack.setOnClickListener(v -> onBackPressed());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void toggleCategory(int index) {
        String type = workTypes[index];
        if (selectedWorkTypes.contains(type)) {
            selectedWorkTypes.remove(type);
            setCategorySelected(index, false);
        } else {
            selectedWorkTypes.add(type);
            setCategorySelected(index, true);
        }
    }

    private void setCategorySelected(int index, boolean selected) {
        if (selected) {
            categoryViews[index].setBackgroundResource(R.drawable.bg_category_selected);
            categoryTexts[index].setTextColor(0xFF16A34A);
        } else {
            categoryViews[index].setBackgroundResource(R.drawable.bg_category_unselected);
            categoryTexts[index].setTextColor(0xFF64748B);
        }
    }

    private void loadProfile() {
        db.collection("labor_workers").document(laborId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String phone = doc.getString("phone");
                        List<String> loadedWorkTypes = (List<String>) doc.get("workTypes");
                        
                        // Fallback for old single string data
                        if (loadedWorkTypes == null || loadedWorkTypes.isEmpty()) {
                            String oldWorkType = doc.getString("workType");
                            if (oldWorkType != null && !oldWorkType.isEmpty()) {
                                loadedWorkTypes = new ArrayList<>();
                                loadedWorkTypes.add(oldWorkType.toLowerCase());
                            }
                        }

                        if (loadedWorkTypes != null) {
                            for (String type : loadedWorkTypes) {
                                for (int i = 0; i < workTypes.length; i++) {
                                    if (workTypes[i].equals(type.toLowerCase())) {
                                        selectedWorkTypes.add(workTypes[i]);
                                        setCategorySelected(i, true);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> ToastUtils.showShort(this, "Failed to load profile"));
    }

    private void saveProfile() {
        String name = editName.getText() != null ? editName.getText().toString().trim() : "";
        String phone = editPhone.getText() != null ? editPhone.getText().toString().trim() : "";

        if (name.isEmpty()) {
            editName.setError("Name is required");
            return;
        }
        
        if (selectedWorkTypes.isEmpty()) {
            ToastUtils.showShort(this, "Please select at least one work skill");
            return;
        }

        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Saving...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("workTypes", selectedWorkTypes);
        // Save first selected as old workType for legacy compatibility if needed
        updates.put("workType", selectedWorkTypes.get(0));
        
        String wageStr = editDailyWage.getText() != null ? editDailyWage.getText().toString().trim() : "";
        if (!wageStr.isEmpty()) {
            try {
                updates.put("dailyWage", Double.parseDouble(wageStr));
            } catch (NumberFormatException ignored) {}
        }

        db.collection("labor_workers").document(laborId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    db.collection("users").document(laborId).update("name", name);
                    preferenceManager.setUserName(name);

                    btnSaveProfile.setEnabled(true);
                    btnSaveProfile.setText("Save Profile");
                    ToastUtils.showShort(this, "Profile updated successfully!");
                })
                .addOnFailureListener(e -> {
                    btnSaveProfile.setEnabled(true);
                    btnSaveProfile.setText("Save Profile");
                    ToastUtils.showShort(this, "Failed to save: " + e.getMessage());
                });
    }

    private void logout() {
        mAuth.signOut();
        preferenceManager.clearAll();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
