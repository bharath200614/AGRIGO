package com.agrigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.agrigo.R;
import com.agrigo.utils.PreferenceManager;
import com.agrigo.utils.ToastUtils;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LaborWorkerProfileActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextInputEditText editName, editPhone, editDailyWage;
    private Spinner spinnerWorkType;
    private MaterialButton btnSaveProfile, btnLogout;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private PreferenceManager preferenceManager;
    private String laborId;

    private final String[] workTypes = {"Harvesting", "Planting", "Cleaning", "Weeding"};

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
        spinnerWorkType = findViewById(R.id.spinnerWorkType);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, workTypes);
        spinnerWorkType.setAdapter(adapter);

        btnBack.setOnClickListener(v -> onBackPressed());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadProfile() {
        db.collection("labor_workers").document(laborId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String phone = doc.getString("phone");
                        String workType = doc.getString("workType");
                        Double wage = doc.getDouble("dailyWage");

                        if (name != null) editName.setText(name);
                        if (phone != null) editPhone.setText(phone);
                        if (wage != null) editDailyWage.setText(String.valueOf(wage.intValue()));

                        if (workType != null) {
                            for (int i = 0; i < workTypes.length; i++) {
                                if (workTypes[i].toLowerCase().equals(workType.toLowerCase())) {
                                    spinnerWorkType.setSelection(i);
                                    break;
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
        String workType = spinnerWorkType.getSelectedItem() != null
                ? spinnerWorkType.getSelectedItem().toString().toLowerCase().trim()
                : "";

        if (name.isEmpty()) {
            editName.setError("Name is required");
            return;
        }

        btnSaveProfile.setEnabled(false);
        btnSaveProfile.setText("Saving...");

        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("workType", workType);
        
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
