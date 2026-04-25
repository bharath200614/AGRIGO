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

public class LaborSettingsActivity extends AppCompatActivity {

    private TextInputEditText editWages, editAvailability;
    private Slider sliderDistance;
    private TextView txtDistanceValue;
    private AutoCompleteTextView dropdownWorkType;
    private MaterialButton btnSave;
    private View btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_labor_settings);

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
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupDropdown() {
        String[] workTypes = {"Harvesting", "Plowing", "Planting"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, workTypes);
        dropdownWorkType.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        sliderDistance.addOnChangeListener((slider, value, fromUser) -> {
            txtDistanceValue.setText((int) value + " KM");
        });

        btnSave.setOnClickListener(v -> saveSettings());
    }

    private void loadSettings() {
        // Load default or saved values
        editWages.setText("600");
        editAvailability.setText("7");
        sliderDistance.setValue(10);
        txtDistanceValue.setText("10 KM");
        dropdownWorkType.setText("Harvesting", false);
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

        // Logic to save settings (e.g., to SharedPreferences or Firestore)
        Toast.makeText(this, "Settings Saved Successfully!", Toast.LENGTH_SHORT).show();
        
        // Return to previous screen after a short delay
        btnSave.postDelayed(this::finish, 1000);
    }
}
