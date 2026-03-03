package com.agrigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.agrigo.R;
import com.agrigo.adapters.BookingAdapter;
import com.agrigo.models.Booking;
import com.agrigo.utils.PreferenceManager;
import com.agrigo.utils.ToastUtils;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * My Bookings Activity - Display list of farmer's bookings
 */
public class MyBookingsActivity extends AppCompatActivity {

    // UI Elements
    private TextView tvFarmerName;
    private ImageButton btnMenu, btnBack;
    private RecyclerView rvBookings;
    private TextView tvNoBookings;
    
    // Data and adapters
    private PreferenceManager preferenceManager;
    private BookingAdapter bookingAdapter;
    private List<Booking> bookingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);
        
        preferenceManager = new PreferenceManager(this);
        
        initializeViews();
        setupListeners();
        loadUserData();
        loadBookings();
        applyEntranceAnimations();
    }

    private void initializeViews() {
        tvFarmerName = findViewById(R.id.tvFarmerName);
        btnMenu = findViewById(R.id.btnMenu);
        btnBack = findViewById(R.id.btnBack);
        rvBookings = findViewById(R.id.rvBookings);
        tvNoBookings = findViewById(R.id.tvNoBookings);
        
        // Setup RecyclerView
        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        bookingAdapter = new BookingAdapter(this, new ArrayList<>());
        rvBookings.setAdapter(bookingAdapter);
    }

    private void loadUserData() {
        String userName = preferenceManager.getUserName();
        if (userName != null && !userName.isEmpty()) {
            tvFarmerName.setText(userName + "'s Bookings");
        }
    }

    private void setupListeners() {
        btnMenu.setOnClickListener(v -> navigateToHome());
        btnBack.setOnClickListener(v -> onBackPressed());
    }

    private void loadBookings() {
        bookingList = generateMockBookings();
        
        if (bookingList.isEmpty()) {
            tvNoBookings.setVisibility(View.VISIBLE);
            rvBookings.setVisibility(View.GONE);
        } else {
            tvNoBookings.setVisibility(View.GONE);
            rvBookings.setVisibility(View.VISIBLE);
            bookingAdapter.setBookings(bookingList);
            bookingAdapter.setOnBookingSelectListener(this::handleBookingSelected);
        }
    }

    private List<Booking> generateMockBookings() {
        List<Booking> bookings = new ArrayList<>();
        
        // Ongoing booking
        Booking booking1 = new Booking();
        booking1.setId("B001");
        booking1.setFarmerId(preferenceManager.getUserId());
        booking1.setCropType("paddy");
        booking1.setWeight(1500);
        booking1.setVehicleType("truck");
        booking1.setStatus("ongoing");
        booking1.setFare(500);
        booking1.setCreatedAt(System.currentTimeMillis() - 86400000); // Yesterday
        booking1.setUpdatedAt(System.currentTimeMillis());
        bookings.add(booking1);
        
        // Completed booking
        Booking booking2 = new Booking();
        booking2.setId("B002");
        booking2.setFarmerId(preferenceManager.getUserId());
        booking2.setCropType("tomato");
        booking2.setWeight(800);
        booking2.setVehicleType("mini_truck");
        booking2.setStatus("completed");
        booking2.setFare(300);
        booking2.setCreatedAt(System.currentTimeMillis() - 604800000); // 1 week ago
        booking2.setUpdatedAt(System.currentTimeMillis() - 518400000); // 6 days ago
        bookings.add(booking2);
        
        // Requested booking
        Booking booking3 = new Booking();
        booking3.setId("B003");
        booking3.setFarmerId(preferenceManager.getUserId());
        booking3.setCropType("sugarcane");
        booking3.setWeight(3000);
        booking3.setVehicleType("lorry");
        booking3.setStatus("requested");
        booking3.setFare(800);
        booking3.setCreatedAt(System.currentTimeMillis() - 3600000); // 1 hour ago
        booking3.setUpdatedAt(System.currentTimeMillis());
        bookings.add(booking3);
        
        // Completed booking 2
        Booking booking4 = new Booking();
        booking4.setId("B004");
        booking4.setFarmerId(preferenceManager.getUserId());
        booking4.setCropType("banana");
        booking4.setWeight(1200);
        booking4.setVehicleType("truck");
        booking4.setStatus("completed");
        booking4.setFare(450);
        booking4.setCreatedAt(System.currentTimeMillis() - 1209600000); // 2 weeks ago
        booking4.setUpdatedAt(System.currentTimeMillis() - 1123200000);
        bookings.add(booking4);
        
        // Cancelled booking
        Booking booking5 = new Booking();
        booking5.setId("B005");
        booking5.setFarmerId(preferenceManager.getUserId());
        booking5.setCropType("paddy");
        booking5.setWeight(500);
        booking5.setVehicleType("auto");
        booking5.setStatus("cancelled");
        booking5.setFare(150);
        booking5.setCreatedAt(System.currentTimeMillis() - 2592000000L); // 30 days ago
        booking5.setUpdatedAt(System.currentTimeMillis() - 2592000000L);
        bookings.add(booking5);
        
        return bookings;
    }

    private void handleBookingSelected(Booking booking) {
        String message = "Booking " + booking.getId() + " - " + booking.getCropType().toUpperCase();
        ToastUtils.showShort(this, message);
        // TODO: Can navigate to booking details screen here
    }

    private void navigateToHome() {
        startActivity(new Intent(this, FarmerDashboardActivity.class));
        finish();
    }

    private void applyEntranceAnimations() {
        android.view.animation.Animation slideIn = 
                android.view.animation.AnimationUtils.loadAnimation(this, R.anim.slide_in_right);
        rvBookings.startAnimation(slideIn);
    }
}
