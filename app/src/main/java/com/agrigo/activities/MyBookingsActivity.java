package com.agrigo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agrigo.R;
import com.agrigo.adapters.ActiveJobsAdapter;
import com.agrigo.models.ActiveJob;
import com.agrigo.utils.PreferenceManager;
import com.agrigo.utils.ToastUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MyBookingsActivity extends AppCompatActivity {

    private static final String TAG = "MyBookingsActivity";

    // UI Elements
    private RecyclerView rvBookings;
    private View layoutEmptyState;
    private View layoutLoadingState;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private BottomNavigationView bottomNavigationView;

    // Data and adapters
    private PreferenceManager preferenceManager;
    private ActiveJobsAdapter activeJobsAdapter;
    private List<ActiveJob> activeJobList;
    private FirebaseFirestore db;
    
    // Listeners
    private ListenerRegistration transportListener;
    private ListenerRegistration machineryListener;
    private ListenerRegistration laborListener;

    // Concurrent merge state
    private Map<String, ActiveJob> transportJobs = new HashMap<>();
    private Map<String, ActiveJob> machineryJobs = new HashMap<>();
    private Map<String, ActiveJob> laborJobs = new HashMap<>();
    
    private String farmerId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_bookings);

        preferenceManager = new PreferenceManager(this);
        db = FirebaseFirestore.getInstance();
        
        farmerId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : preferenceManager.getUserId();

        initializeViews();
        setupBottomNavigation();
        
        // Initial state
        layoutLoadingState.setVisibility(View.VISIBLE);
        rvBookings.setVisibility(View.GONE);
        layoutEmptyState.setVisibility(View.GONE);

        listenToActiveJobs();
    }

    private void initializeViews() {
        rvBookings = findViewById(R.id.rvBookings);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        layoutLoadingState = findViewById(R.id.layoutLoadingState);
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        
        tvEmptyTitle = layoutEmptyState.findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = layoutEmptyState.findViewById(R.id.tvEmptySubtitle);
        if (tvEmptyTitle != null) tvEmptyTitle.setText("No Active Bookings");
        if (tvEmptySubtitle != null) tvEmptySubtitle.setText("You don't have any ongoing requests at the moment.");

        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        activeJobList = new ArrayList<>();
        activeJobsAdapter = new ActiveJobsAdapter(activeJobList, this, this::handleJobClick);
        rvBookings.setAdapter(activeJobsAdapter);
    }
    
    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_bookings);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, FarmerDashboardActivity.class));
                return true;
            } else if (itemId == R.id.nav_bookings) {
                return true;
            } else if (itemId == R.id.nav_track) {
                return true;
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    private void listenToActiveJobs() {
        // 1. Listen Transport
        transportListener = db.collection("transport_requests")
                .whereEqualTo("farmerId", farmerId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        transportJobs.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String cType = doc.getString("cropType");
                            double wgt = doc.getDouble("weight") != null ? doc.getDouble("weight") : 0.0;
                            String status = doc.getString("status");
                            long ts = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0;
                            
                            String title = (cType != null ? cType : "Crops") + " • " + wgt + " Tons";
                            String subtitle = doc.getString("source") + " to " + doc.getString("destination");
                            
                            ActiveJob job = new ActiveJob(doc.getId(), "Transport", title, subtitle, status != null ? status : "UNKNOWN", ts);
                            
                            // Map Progress for Transport
                            job.setProviderName(doc.getString("driverName") != null ? doc.getString("driverName") : "Assigning driver...");
                            if ("REQUESTED".equals(status)) { job.setProgressPercentage(10); job.setProgressText("Waiting for acceptance"); }
                            else if ("ACCEPTED".equals(status)) { job.setProgressPercentage(30); job.setProgressText("Driver Assigned"); }
                            else if ("ON_TRIP".equals(status)) { job.setProgressPercentage(70); job.setProgressText("In Transit"); }
                            else if ("COMPLETED".equals(status)) { job.setProgressPercentage(100); job.setProgressText("Delivered"); }
                            
                            transportJobs.put(doc.getId(), job);
                        }
                        updateMergedFeed();
                    }
                });

        // 2. Listen Machinery
        machineryListener = db.collection("machinery_bookings")
                .whereEqualTo("farmerId", farmerId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        machineryJobs.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String mType = doc.getString("machineryType");
                            String dur = doc.getString("duration");
                            String status = doc.getString("status");
                            long ts = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0;
                            
                            String title = (mType != null ? mType : "Machinery") + " • " + dur + " Hrs";
                            String subtitle = doc.getString("address") != null ? doc.getString("address") : "Farm Location";
                            
                            ActiveJob job = new ActiveJob(doc.getId(), "Machinery", title, subtitle, status != null ? status : "UNKNOWN", ts);
                            
                            // Map Progress
                            job.setProviderName("Assigning provider...");
                            if ("REQUESTED".equals(status)) { job.setProgressPercentage(10); job.setProgressText("Searching for providers"); }
                            else if ("ACCEPTED".equals(status)) { job.setProgressPercentage(40); job.setProgressText("Provider En-route"); }
                            else if ("ARRIVED".equals(status) || "WORK_STARTED".equals(status)) { job.setProgressPercentage(70); job.setProgressText("Work in Progress"); }
                            else if ("WORK_COMPLETED".equals(status)) { job.setProgressPercentage(100); job.setProgressText("Done"); }

                            machineryJobs.put(doc.getId(), job);
                        }
                        updateMergedFeed();
                    }
                });

        // 3. Listen Labor
        laborListener = db.collection("labor_bookings")
                .whereEqualTo("farmerId", farmerId)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;
                    if (value != null) {
                        laborJobs.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            String wType = doc.getString("workType");
                            Long reqWorkers = doc.getLong("workersRequired");
                            Long accWorkers = doc.getLong("workersAccepted");
                            String status = doc.getString("status");
                            long ts = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0;
                            
                            long req = reqWorkers != null ? reqWorkers : 0;
                            long acc = accWorkers != null ? accWorkers : 0;
                            
                            String title = (wType != null ? wType : "Farm Work") + " • " + req + " Workers";
                            String subtitle = "Labor Dispatch";
                            
                            ActiveJob job = new ActiveJob(doc.getId(), "Labor", title, subtitle, status != null ? status : "UNKNOWN", ts);
                            
                            // Map Progress
                            job.setProviderName(acc + " out of " + req + " workers accepted");
                            if (req > 0) {
                                int pct = (int) ((acc * 100) / req);
                                job.setProgressPercentage(pct);
                                job.setProgressText(acc + " / " + req);
                            }
                            
                            laborJobs.put(doc.getId(), job);
                        }
                        updateMergedFeed();
                    }
                });
    }

    private void updateMergedFeed() {
        activeJobList.clear();
        activeJobList.addAll(transportJobs.values());
        activeJobList.addAll(machineryJobs.values());
        activeJobList.addAll(laborJobs.values());

        // Sort descending by timestamp
        Collections.sort(activeJobList);
        
        layoutLoadingState.setVisibility(View.GONE);
        activeJobsAdapter.notifyDataSetChanged();
        
        if (activeJobList.isEmpty()) {
            layoutEmptyState.setVisibility(View.VISIBLE);
            rvBookings.setVisibility(View.GONE);
        } else {
            layoutEmptyState.setVisibility(View.GONE);
            rvBookings.setVisibility(View.VISIBLE);
        }
    }

    private void handleJobClick(ActiveJob job) {
        Intent intent;
        switch (job.getServiceType()) {
            case "Transport":
                if ("ACCEPTED".equalsIgnoreCase(job.getStatus()) || "ON_TRIP".equalsIgnoreCase(job.getStatus())) {
                    intent = new Intent(this, TrackingActivity.class);
                    // Legacy tracking expects REQUEST_ID. Look at TrackingActivity payload if needed.
                    intent.putExtra("REQUEST_ID", job.getId());
                    startActivity(intent);
                } else {
                    ToastUtils.showShort(this, "Tracking available after acceptance");
                }
                break;
            case "Machinery":
                if (!"REQUESTED".equalsIgnoreCase(job.getStatus()) && !"CANCELLED".equalsIgnoreCase(job.getStatus())) {
                    intent = new Intent(this, MachineryTrackingActivity.class);
                    intent.putExtra("BOOKING_ID", job.getId());
                    startActivity(intent);
                } else {
                    ToastUtils.showShort(this, "Tracking available after acceptance");
                }
                break;
            case "Labor":
                ToastUtils.showShort(this, "Labor tracking coming soon");
                // Navigate to a dedicated labor view if we have one
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_bookings);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (transportListener != null) transportListener.remove();
        if (machineryListener != null) machineryListener.remove();
        if (laborListener != null) laborListener.remove();
    }
}
