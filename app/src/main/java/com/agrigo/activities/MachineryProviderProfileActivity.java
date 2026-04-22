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

public class MachineryProviderProfileActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextInputEditText editName, editPhone;
    private Spinner spinnerMachineryType;
    private MaterialButton btnSaveProfile, btnLogout;
    private TextView tvProfileStatus;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private PreferenceManager preferenceManager;
    private String providerId;

    private final String[] machineryTypes = {"Harvester", "Sprayer", "Tractor", "Cultivator"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_machinery_provider_profile);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        preferenceManager = new PreferenceManager(this);
        providerId = mAuth.getCurrentUser() != null
                ? mAuth.getCurrentUser().getUid()
                : preferenceManager.getUserId();

        initViews();
        loadProfile();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        editName = findViewById(R.id.editName);
        editPhone = findViewById(R.id.editPhone);
        spinnerMachineryType = findViewById(R.id.spinnerMachineryType);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogout = findViewById(R.id.btnLogout);
        tvProfileStatus = findViewById(R.id.tvProfileStatus);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, machineryTypes);
        spinnerMachineryType.setAdapter(adapter);

        btnBack.setOnClickListener(v -> onBackPressed());
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> logout());
    }

    private void loadProfile() {
        db.collection("machinery_providers").document(providerId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String name = doc.getString("name");
                        String phone = doc.getString("phone");
                        String machineryType = doc.getString("machineryType");
                        String status = doc.getString("status");
                        Boolean isOnline = doc.getBoolean("isOnline");

                        if (name != null) editName.setText(name);
                        if (phone != null) editPhone.setText(phone);

                        // Set spinner to matching type
                        if (machineryType != null) {
                            for (int i = 0; i < machineryTypes.length; i++) {
                                if (machineryTypes[i].toLowerCase().equals(machineryType.toLowerCase())) {
                                    spinnerMachineryType.setSelection(i);
                                    break;
                                }
                            }
                        }

                        // Set status display
                        if (isOnline != null && isOnline) {
                            tvProfileStatus.setText("Online");
                            tvProfileStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"));
                        } else {
                            tvProfileStatus.setText("Offline");
                            tvProfileStatus.setTextColor(android.graphics.Color.parseColor("#E53935"));
                        }
                    }
                })
                .addOnFailureListener(e -> ToastUtils.showShort(this, "Failed to load profile"));
    }

    private void saveProfile() {
        String name = editName.getText() != null ? editName.getText().toString().trim() : "";
        String phone = editPhone.getText() != null ? editPhone.getText().toString().trim() : "";
        String machineryType = spinnerMachineryType.getSelectedItem() != null
                ? spinnerMachineryType.getSelectedItem().toString().toLowerCase().trim()
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
        updates.put("machineryType", machineryType);

        db.collection("machinery_providers").document(providerId).update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Also update users collection
                    db.collection("users").document(providerId).update("name", name);
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
        // Stop dispatch service
        Intent serviceIntent = new Intent(this, com.agrigo.services.DriverDispatchService.class);
        stopService(serviceIntent);

        mAuth.signOut();
        preferenceManager.clearAll();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
