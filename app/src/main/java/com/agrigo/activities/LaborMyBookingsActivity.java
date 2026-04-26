package com.agrigo.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.agrigo.R;
import com.agrigo.adapters.LaborMyBookingsAdapter;
import com.agrigo.utils.ToastUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class LaborMyBookingsActivity extends BaseActivity implements LaborMyBookingsAdapter.OnBookingActionListener {

    private static final String TAG = "LaborMyBookings";

    private RecyclerView rvBookings;
    private View layoutEmptyState;
    private ProgressBar pbLoading;
    
    private FirebaseFirestore db;
    private String laborId;
    private List<String> workerWorkTypesList = new ArrayList<>();
    private LaborMyBookingsAdapter adapter;
    private ListenerRegistration bookingsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_labor_my_bookings);

        db = FirebaseFirestore.getInstance();
        laborId = FirebaseAuth.getInstance().getUid();

        initViews();
        fetchWorkerDetails();
    }

    private void initViews() {
        rvBookings = findViewById(R.id.rvBookings);
        layoutEmptyState = findViewById(R.id.layoutEmptyState);
        pbLoading = findViewById(R.id.pbLoading);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvBookings.setLayoutManager(new LinearLayoutManager(this));
        adapter = new LaborMyBookingsAdapter(new ArrayList<>(), this, laborId, this);
        rvBookings.setAdapter(adapter);
    }

    private void fetchWorkerDetails() {
        if (laborId == null) return;
        db.collection("labor_workers").document(laborId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<String> types = (List<String>) doc.get("workTypes");
                        
                        // Fallback for older profiles
                        if (types == null || types.isEmpty()) {
                            String oldWorkType = doc.getString("workType");
                            if (oldWorkType != null && !oldWorkType.isEmpty()) {
                                types = new ArrayList<>();
                                types.add(oldWorkType.toLowerCase().trim());
                            }
                        }
                        
                        if (types != null && !types.isEmpty()) {
                            workerWorkTypesList = new ArrayList<>();
                            for (String t : types) {
                                workerWorkTypesList.add(t.toLowerCase().trim());
                            }
                        }
                        listenToBookings();
                    }
                });
    }

    private void listenToBookings() {
        if (laborId == null) return;

        pbLoading.setVisibility(View.VISIBLE);
        
        // Listen for jobs where worker is assigned OR jobs that are requested for their type
        // Note: Manual filtering or composite query needed. We'll use a listener on labor_bookings.
        bookingsListener = db.collection("labor_bookings")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    pbLoading.setVisibility(View.GONE);
                    if (error != null) {
                        Log.e(TAG, "Error listening to bookings", error);
                        return;
                    }

                    if (value != null) {
                        List<DocumentSnapshot> allBookings = value.getDocuments();
                        List<DocumentSnapshot> filteredBookings = new ArrayList<>();

                        for (DocumentSnapshot doc : allBookings) {
                            String status = doc.getString("status");
                            String workType = doc.getString("workType");
                            List<String> assigned = (List<String>) doc.get("assignedWorkers");
                            List<String> rejected = (List<String>) doc.get("rejectedBy");

                            boolean isAssigned = assigned != null && assigned.contains(laborId);
                            boolean isRequestedForMe = "REQUESTED".equalsIgnoreCase(status) 
                                    && workType != null && workerWorkTypesList.contains(workType.toLowerCase().trim())
                                    && (rejected == null || !rejected.contains(laborId));

                            if (isAssigned || isRequestedForMe) {
                                filteredBookings.add(doc);
                            }
                        }

                        adapter.updateData(filteredBookings);

                        if (filteredBookings.isEmpty()) {
                            layoutEmptyState.setVisibility(View.VISIBLE);
                            rvBookings.setVisibility(View.GONE);
                        } else {
                            layoutEmptyState.setVisibility(View.GONE);
                            rvBookings.setVisibility(View.VISIBLE);
                        }
                    }
                });
    }

    @Override
    public void onAccept(DocumentSnapshot doc) {
        String bookingId = doc.getId();
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(db.collection("labor_bookings").document(bookingId));
            
            Long req = snapshot.getLong("workersRequired");
            Long acc = snapshot.getLong("workersAccepted");
            List<String> assigned = (List<String>) snapshot.get("assignedWorkers");
            
            if (assigned == null) assigned = new ArrayList<>();
            if (assigned.contains(laborId)) return null; // Already accepted
            
            long required = req != null ? req : 0;
            long accepted = acc != null ? acc : 0;
            
            if (accepted < required) {
                assigned.add(laborId);
                transaction.update(snapshot.getReference(), 
                    "workersAccepted", accepted + 1,
                    "assignedWorkers", assigned);
                
                if (accepted + 1 >= required) {
                    transaction.update(snapshot.getReference(), "status", "ACCEPTED");
                }
            } else {
                throw new RuntimeException("Job is already full");
            }
            return null;
        }).addOnSuccessListener(aVoid -> {
            ToastUtils.showShort(this, "Job Accepted!");
        }).addOnFailureListener(e -> {
            ToastUtils.showShort(this, "Failed: " + e.getMessage());
        });
    }

    @Override
    public void onReject(DocumentSnapshot doc) {
        String bookingId = doc.getId();
        String status = doc.getString("status");
        
        List<String> assigned = (List<String>) doc.get("assignedWorkers");
        boolean amIAssigned = assigned != null && assigned.contains(laborId);

        if (amIAssigned) {
            // Un-assign from the job
            db.runTransaction(transaction -> {
                DocumentSnapshot snapshot = transaction.get(db.collection("labor_bookings").document(bookingId));
                List<String> currentAssigned = (List<String>) snapshot.get("assignedWorkers");
                Long acc = snapshot.getLong("workersAccepted");
                
                if (currentAssigned != null && currentAssigned.contains(laborId)) {
                    currentAssigned.remove(laborId);
                    long newAcc = (acc != null ? acc : 1) - 1;
                    
                    // If we drop below required, status goes back to REQUESTED
                    transaction.update(snapshot.getReference(), 
                        "assignedWorkers", currentAssigned,
                        "workersAccepted", newAcc,
                        "status", "REQUESTED");
                }
                return null;
            }).addOnSuccessListener(aVoid -> ToastUtils.showShort(this, "Job Rejected"));
            
        } else if ("REQUESTED".equalsIgnoreCase(status)) {
            // Not assigned, just reject it so it hides from feed
            db.collection("labor_bookings").document(bookingId)
                    .update("rejectedBy", com.google.firebase.firestore.FieldValue.arrayUnion(laborId))
                    .addOnSuccessListener(aVoid -> ToastUtils.showShort(this, "Job Rejected"));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bookingsListener != null) {
            bookingsListener.remove();
        }
    }
}
