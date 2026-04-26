package com.agrigo.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agrigo.R;
import com.agrigo.adapters.LaborEarningsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DriverEarningsActivity extends BaseActivity {

    private static final String TAG = "DriverEarnings";

    private TextView tvTotalEarnings, tvTodayEarnings, tvWeeklyEarnings, tvCompletedJobs;
    private RecyclerView rvEarnings;
    private View layoutEmptyState;
    
    private FirebaseFirestore db;
    private String driverId;
    private LaborEarningsAdapter adapter;
    private ListenerRegistration summaryListener;
    private ListenerRegistration historyListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_driver_earnings);

        db = FirebaseFirestore.getInstance();
        driverId = FirebaseAuth.getInstance().getUid();

        initViews();
        fetchSummary();
        listenToEarningsHistory();
    }

    private void initViews() {
        tvTotalEarnings = findViewById(R.id.tvTotalEarnings);
        tvTodayEarnings = findViewById(R.id.tvTodayEarnings);
        tvWeeklyEarnings = findViewById(R.id.tvWeeklyEarnings);
        tvCompletedJobs = findViewById(R.id.tvCompletedJobs);
        
        rvEarnings = findViewById(R.id.rvEarnings);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvEarnings.setLayoutManager(new LinearLayoutManager(this));
        // We reuse LaborEarningsAdapter as it just shows generic earnings docs
        adapter = new LaborEarningsAdapter(new ArrayList<>());
        rvEarnings.setAdapter(adapter);
    }

    private void fetchSummary() {
        if (driverId == null) return;

        summaryListener = db.collection("drivers").document(driverId)
                .addSnapshotListener((doc, error) -> {
                    if (error != null || doc == null || !doc.exists()) return;

                    Double total = 0.0;
                    if (doc.contains("totalEarnings")) {
                        total = doc.getDouble("totalEarnings");
                    }
                    tvTotalEarnings.setText(String.format(Locale.getDefault(), "₹%.2f", total != null ? total : 0.0));
                });
    }

    private void listenToEarningsHistory() {
        if (driverId == null) return;

        // Drivers don't have a separate earnings collection yet in the same way, 
        // we can either create one or query completed transport_requests.
        // Let's use completed transport_requests for now as history.
        historyListener = db.collection("transport_requests")
                .whereEqualTo("driverId", driverId)
                .whereEqualTo("status", "COMPLETED")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening to earnings", error);
                        return;
                    }

                    if (value != null) {
                        List<DocumentSnapshot> earnings = value.getDocuments();
                        adapter.updateData(earnings);

                        if (earnings.isEmpty()) {
                            layoutEmptyState.setVisibility(View.VISIBLE);
                            rvEarnings.setVisibility(View.GONE);
                        } else {
                            layoutEmptyState.setVisibility(View.GONE);
                            rvEarnings.setVisibility(View.VISIBLE);
                        }

                        calculatePeriodicEarnings(earnings);
                    }
                });
    }

    private void calculatePeriodicEarnings(List<DocumentSnapshot> earnings) {
        double today = 0;
        double weekly = 0;
        int count = earnings.size();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long todayStart = cal.getTimeInMillis();

        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        long weekStart = cal.getTimeInMillis();

        for (DocumentSnapshot doc : earnings) {
            Long ts = doc.getLong("timestamp");
            Double amount = 0.0;
            
            if (doc.contains("fare")) {
                amount = doc.getDouble("fare");
            } else if (doc.contains("estimatedPrice")) {
                try {
                    amount = Double.parseDouble(doc.getString("estimatedPrice").replaceAll("[^0-9.]", ""));
                } catch (Exception e) {}
            }
            
            if (ts != null && amount != null) {
                if (ts >= todayStart) today += amount;
                if (ts >= weekStart) weekly += amount;
            }
        }

        tvTodayEarnings.setText(String.format(Locale.getDefault(), "₹%.0f", today));
        tvWeeklyEarnings.setText(String.format(Locale.getDefault(), "₹%.0f", weekly));
        tvCompletedJobs.setText(String.valueOf(count));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (summaryListener != null) summaryListener.remove();
        if (historyListener != null) historyListener.remove();
    }
}
