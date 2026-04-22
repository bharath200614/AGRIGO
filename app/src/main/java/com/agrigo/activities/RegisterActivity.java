package com.agrigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.text.method.HideReturnsTransformationMethod;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.RadioGroup;
import android.widget.RadioButton;

import androidx.appcompat.app.AppCompatActivity;

import com.agrigo.R;
import com.agrigo.models.User;
import com.agrigo.utils.PreferenceManager;
import com.agrigo.utils.ToastUtils;
import com.agrigo.utils.ValidationUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.FirebaseNetworkException;

import java.util.HashMap;
import java.util.Map;

// removed timber import

/**
 * Registration Activity - User account creation screen
 */
public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnRegisterAccount;
    private TextView tvLoginLink;
    private ImageView ivRegisterLogo;
    private RadioGroup rgRole;
    private TextView tvErrorMessage;
    private ProgressBar progressBar;
    
    // Machinery Extras
    private LinearLayout layoutMachineryExtras;
    private EditText etPhone;
    private Spinner spinnerMachineryType;

    // Labor Extras
    private LinearLayout layoutLaborExtras;
    private Spinner spinnerWorkType;

    // Driver Extras
    private LinearLayout layoutDriverExtras;
    private Spinner spinnerVehicleType;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
 
        try {
            mAuth = FirebaseAuth.getInstance();
            db = FirebaseFirestore.getInstance();
            preferenceManager = new PreferenceManager(this);
        } catch (Exception e) {
            // Error initialized handled silently
            ToastUtils.showShort(this, "Initialization failed. Please restart.");
            return;
        }

        // Initialize views
        initializeViews();

        // Set up listeners
        setupListeners();

        // Apply entrance animations
        applyEntranceAnimations();

        // No longer relying on intent to set the role display since it's a radiogroup
    }

    private void initializeViews() {
        etFullName = findViewById(R.id.et_full_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnRegisterAccount = findViewById(R.id.btn_register_account);
        tvLoginLink = findViewById(R.id.tv_login_link);
        ivRegisterLogo = findViewById(R.id.iv_register_logo);
        rgRole = findViewById(R.id.rg_role);
        tvErrorMessage = findViewById(R.id.tv_error_message);
        progressBar = findViewById(R.id.progress_bar);
        
        layoutMachineryExtras = findViewById(R.id.layout_machinery_extras);
        etPhone = findViewById(R.id.et_phone);
        spinnerMachineryType = findViewById(R.id.spinner_machinery_type);
        
        // Labor extras
        layoutLaborExtras = findViewById(R.id.layout_labor_extras);
        spinnerWorkType = findViewById(R.id.spinner_work_type);
        
        String[] machines = {"Select Machinery Type...", "Harvester", "Sprayer", "Tractor", "Cultivator"};
        int[] icons = {
                0, // No icon for default selection
                R.drawable.ic_harvester,
                R.drawable.ic_sprayer,
                R.drawable.ic_tractor,
                R.drawable.ic_cultivator
        };
        com.agrigo.adapters.MachinerySpinnerAdapter adapter = new com.agrigo.adapters.MachinerySpinnerAdapter(this, machines, icons);
        spinnerMachineryType.setAdapter(adapter);

        String[] workTypes = {"Select Work Type...", "Harvesting", "Planting", "Weeding", "Cleaning"};
        ArrayAdapter<String> laborAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, workTypes);
        spinnerWorkType.setAdapter(laborAdapter);

        // Driver extras
        layoutDriverExtras = findViewById(R.id.layout_driver_extras);
        spinnerVehicleType = findViewById(R.id.spinner_vehicle_type);
        
        String[] vehicles = {"Select Vehicle Type...", "Auto", "Mini Truck", "Truck", "Lorry"};
        ArrayAdapter<String> driverAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, vehicles);
        spinnerVehicleType.setAdapter(driverAdapter);
    }

    private void setupListeners() {
        btnRegisterAccount.setOnClickListener(v -> handleRegistration());
        tvLoginLink.setOnClickListener(v -> navigateToLogin());
        ivRegisterLogo.setOnClickListener(v -> {
            Intent intent = new Intent(this, WelcomeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
        
        rgRole.setOnCheckedChangeListener((group, checkedId) -> {
            layoutMachineryExtras.setVisibility(checkedId == R.id.rb_machinery ? View.VISIBLE : View.GONE);
            layoutLaborExtras.setVisibility(checkedId == R.id.rb_labor ? View.VISIBLE : View.GONE);
            layoutDriverExtras.setVisibility(checkedId == R.id.rb_driver ? View.VISIBLE : View.GONE);
        });
    }

    private void handleRegistration() {
        String name = etFullName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        etFullName.setError(null);
        etEmail.setError(null);
        etPassword.setError(null);
        tvErrorMessage.setVisibility(View.GONE);

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            ToastUtils.showShort(this, "Please fill out all fields");
            return;
        }

        if (!ValidationUtils.isValidEmail(email)) {
            etEmail.setError("Invalid email format");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            return;
        }

        int selectedRoleId = rgRole.getCheckedRadioButtonId();
        if (selectedRoleId == -1) {
            ToastUtils.showShort(this, "Please select a role");
            return;
        }

        String role = "farmer";
        if (selectedRoleId == R.id.rb_driver) {
            role = "driver";
        } else if (selectedRoleId == R.id.rb_labor) {
            role = "labor";
        } else if (selectedRoleId == R.id.rb_machinery) {
            role = "machinery_provider";
        }
        
        String phone = "";
        String machineryType = "";
        String workType = "";
        String vehicleType = "";

        // Driver validation
        if ("driver".equals(role)) {
            if (spinnerVehicleType.getSelectedItem() != null) {
                vehicleType = spinnerVehicleType.getSelectedItem().toString();
                if ("Select Vehicle Type...".equals(vehicleType)) {
                    ToastUtils.showShort(this, "Please select a vehicle type");
                    return;
                }
            } else {
                ToastUtils.showShort(this, "Please select a vehicle type");
                return;
            }
        }

        // Machinery provider validation
        if ("machinery_provider".equals(role)) {
            phone = etPhone.getText().toString().trim();
            if (phone.isEmpty() || phone.length() < 10) {
                etPhone.setError("Enter a valid phone number");
                return;
            }
            if (spinnerMachineryType.getSelectedItem() != null) {
                machineryType = spinnerMachineryType.getSelectedItem().toString();
                if ("Select Machinery Type...".equals(machineryType)) {
                    ToastUtils.showShort(this, "Please select a machinery type");
                    return;
                }
            } else {
                ToastUtils.showShort(this, "Please select a machinery type");
                return;
            }
        }

        // Labor worker validation
        if ("labor".equals(role)) {
            if (spinnerWorkType.getSelectedItem() != null) {
                workType = spinnerWorkType.getSelectedItem().toString();
                if ("Select Work Type...".equals(workType)) {
                    ToastUtils.showShort(this, "Please select a work type");
                    return;
                }
            } else {
                ToastUtils.showShort(this, "Please select a work type");
                return;
            }
        }

        performRegistration(name, email, password, role, phone, machineryType, workType, vehicleType);
    }

    private void performRegistration(String name, String email, String password, String role, String phone, String machineryType, String workType, String vehicleType) {
        showLoading(true);

        // Starting registration

        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        // Firebase Auth successful
                        saveUserToFirestore(firebaseUser.getUid(), name, email, role, phone, machineryType, workType, vehicleType);
                    } else {
                        // Error: FirebaseUser is null
                        showLoading(false);
                    }
                } else {
                    showLoading(false);
                    Exception exception = task.getException();
                    // Registration failed handler

                    String errorMsg;
                    if (exception instanceof com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                        errorMsg = "Account already exists. Please login.";
                    } else if (exception instanceof com.google.firebase.auth.FirebaseAuthWeakPasswordException) {
                        errorMsg = "Password is too weak. Use at least 6 characters.";
                    } else if (exception instanceof com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
                        errorMsg = "Invalid email format.";
                    } else if (exception instanceof com.google.firebase.FirebaseNetworkException) {
                        errorMsg = "Network error. Check your internet connection.";
                    } else {
                        errorMsg = "Registration failed: " + (exception != null ? exception.getMessage() : "Unknown error");
                    }
                    tvErrorMessage.setText(errorMsg);
                    tvErrorMessage.setVisibility(View.GONE); // use toast for register
                    ToastUtils.showShort(this, errorMsg);
                }
            });
    }

    private void saveUserToFirestore(String userId, String name, String email, String role, String phone, String machineryType, String workType, String vehicleType) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", userId);
        userMap.put("name", name);
        userMap.put("email", email);
        userMap.put("role", role);
        userMap.put("createdAt", System.currentTimeMillis());
        userMap.put("isOnline", false);
        if (!vehicleType.isEmpty()) userMap.put("vehicleType", vehicleType);

        db.collection("users").document(userId)
            .set(userMap)
            .addOnSuccessListener(aVoid -> {
                
                // If it is a machinery provider, save into machinery_providers collection
                if ("machinery_provider".equals(role)) {
                    Map<String, Object> providerMap = new HashMap<>();
                    providerMap.put("providerId", userId);
                    providerMap.put("name", name);
                    providerMap.put("phone", phone);
                    providerMap.put("machineryType", machineryType.toLowerCase().trim());
                    providerMap.put("currentLat", 0.0);
                    providerMap.put("currentLng", 0.0);
                    providerMap.put("geoHash", "");
                    providerMap.put("status", "FREE");
                    providerMap.put("isAvailable", true);
                    providerMap.put("isOnline", false);
                    
                    db.collection("machinery_providers").document(userId).set(providerMap);
                }

                // If it is a driver, save into drivers collection
                if ("driver".equals(role)) {
                    Map<String, Object> driverMap = new HashMap<>();
                    driverMap.put("driverId", userId);
                    driverMap.put("name", name);
                    driverMap.put("vehicleType", vehicleType);
                    driverMap.put("isAvailable", true);
                    driverMap.put("status", "FREE");
                    driverMap.put("currentLat", 0.0);
                    driverMap.put("currentLng", 0.0);
                    driverMap.put("geoHash", "");
                    driverMap.put("lastUpdated", System.currentTimeMillis());
                    
                    db.collection("drivers").document(userId).set(driverMap);
                }

                // If it is a labor worker, save into labor_workers collection
                if ("labor".equals(role)) {
                    Map<String, Object> workerMap = new HashMap<>();
                    workerMap.put("workerId", userId);
                    workerMap.put("name", name);
                    workerMap.put("workType", workType.toLowerCase().trim());
                    workerMap.put("status", "FREE");
                    workerMap.put("isAvailable", true);
                    workerMap.put("currentLat", 0.0);
                    workerMap.put("currentLng", 0.0);
                    workerMap.put("geoHash", "");
                    workerMap.put("isOnline", false);
                    
                    db.collection("labor_workers").document(userId).set(workerMap);
                }
                
                // User data saved successfully
                showLoading(false);
                // Store user data in preferences
                preferenceManager.setUserId(userId);
                preferenceManager.setUserName(name);
                preferenceManager.setUserRole(role);
                preferenceManager.setIsLoggedIn(true);

                ToastUtils.showShort(this, "Registration successful!");
                navigateToDashboard(role);
            })
            .addOnFailureListener(e -> {
                // Error saving profile
                showLoading(false);
                String msg = "Error saving profile: " + e.getMessage();
                ToastUtils.showShort(this, msg);
                // Note: If this says 'permission denied', update Firestore Rules in Firebase Console
                // to: allow read, write: if request.auth != null;
            });
    }

    private void showLoading(boolean show) {
        if (progressBar != null) progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnRegisterAccount.setEnabled(!show);
        btnRegisterAccount.setText(show ? "" : getString(R.string.register_account));
    }

    private void navigateToDashboard(String role) {
        // Navigating to dashboard
        Intent intent;
        if ("farmer".equalsIgnoreCase(role)) {
            intent = new Intent(this, FarmerDashboardActivity.class);
        } else if ("driver".equalsIgnoreCase(role)) {
            intent = new Intent(this, DriverHomeActivity.class);
        } else if ("labor".equalsIgnoreCase(role)) {
            intent = new Intent(this, LaborHomeActivity.class);
        } else if ("machinery_provider".equalsIgnoreCase(role)) {
            intent = new Intent(this, MachineryProviderActivity.class);
        } else {
            intent = new Intent(this, FarmerDashboardActivity.class);
        }
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void applyEntranceAnimations() {
        Animation slideIn = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        etFullName.startAnimation(slideIn);
        etEmail.startAnimation(slideIn);
        etPassword.startAnimation(slideIn);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
