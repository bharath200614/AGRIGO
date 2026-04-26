package com.agrigo.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agrigo.R;
import com.agrigo.activities.LaborTrackingActivity;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LaborMyBookingsAdapter extends RecyclerView.Adapter<LaborMyBookingsAdapter.BookingViewHolder> {

    public interface OnBookingActionListener {
        void onAccept(DocumentSnapshot doc);
        void onReject(DocumentSnapshot doc);
    }

    private List<DocumentSnapshot> bookingList;
    private Context context;
    private String laborId;
    private OnBookingActionListener listener;

    public LaborMyBookingsAdapter(List<DocumentSnapshot> bookingList, Context context, String laborId, OnBookingActionListener listener) {
        this.bookingList = bookingList;
        this.context = context;
        this.laborId = laborId;
        this.listener = listener;
    }

    public void updateData(List<DocumentSnapshot> newList) {
        this.bookingList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookingViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_labor_booking, parent, false);
        return new BookingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingViewHolder holder, int position) {
        DocumentSnapshot doc = bookingList.get(position);

        String workType = doc.getString("workType");
        if (workType != null && !workType.isEmpty()) {
            workType = workType.substring(0, 1).toUpperCase() + workType.substring(1);
        }
        holder.tvWorkType.setText(workType != null ? workType : "Farm Work");

        String status = doc.getString("status");
        
        boolean isAssignedToMe = false;
        List<String> assigned = (List<String>) doc.get("assignedWorkers");
        if (assigned != null && assigned.contains(laborId)) {
            isAssignedToMe = true;
        }

        // Status styling and Button logic
        if ("COMPLETED".equalsIgnoreCase(status)) {
            holder.tvStatus.setText("COMPLETED");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_completed);
            holder.btnAction.setVisibility(View.GONE);
            holder.btnReject.setVisibility(View.GONE);
        } else if ("ACCEPTED".equalsIgnoreCase(status) || isAssignedToMe) {
            holder.tvStatus.setText("ACCEPTED");
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_ongoing);
            holder.btnAction.setVisibility(View.VISIBLE);
            holder.btnAction.setText("Track");
            holder.btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF3B82F6)); // Blue
            holder.btnReject.setVisibility(View.VISIBLE);
            holder.btnReject.setText("Reject");
        } else {
            // REQUESTED or PENDING and NOT assigned to me
            holder.tvStatus.setBackgroundResource(R.drawable.bg_status_requested);
            holder.tvStatus.setText("PENDING");
            holder.btnAction.setVisibility(View.VISIBLE);
            holder.btnAction.setText("Accept");
            holder.btnAction.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF16A34A)); // Green
            holder.btnReject.setVisibility(View.VISIBLE);
            holder.btnReject.setText("Reject");
        }

        holder.tvFarmerName.setText("Farmer: " + doc.getString("farmerName"));
        
        Long timestamp = doc.getLong("timestamp");
        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
            holder.tvDateTime.setText(sdf.format(new Date(timestamp)));
        } else {
            holder.tvDateTime.setText("Date not available");
        }

        String address = doc.getString("address");
        holder.tvLocation.setText("📍 " + (address != null ? address : "Location not available"));

        String earnings = doc.getString("estimatedPrice");
        holder.tvEarnings.setText("Earnings: " + (earnings != null ? earnings : "₹0"));

        holder.btnAction.setOnClickListener(v -> {
            boolean amIAssigned = false;
            List<String> as = (List<String>) doc.get("assignedWorkers");
            if (as != null && as.contains(laborId)) amIAssigned = true;

            if ("ACCEPTED".equalsIgnoreCase(doc.getString("status")) || amIAssigned) {
                Intent intent = new Intent(context, LaborTrackingActivity.class);
                intent.putExtra("BOOKING_ID", doc.getId());
                context.startActivity(intent);
            } else {
                if (listener != null) listener.onAccept(doc);
            }
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(doc);
        });
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    static class BookingViewHolder extends RecyclerView.ViewHolder {
        TextView tvWorkType, tvStatus, tvFarmerName, tvDateTime, tvLocation, tvEarnings;
        MaterialButton btnAction, btnReject;

        public BookingViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWorkType = itemView.findViewById(R.id.tvWorkType);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvFarmerName = itemView.findViewById(R.id.tvFarmerName);
            tvDateTime = itemView.findViewById(R.id.tvDateTime);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvEarnings = itemView.findViewById(R.id.tvEarnings);
            btnAction = itemView.findViewById(R.id.btnAction);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
