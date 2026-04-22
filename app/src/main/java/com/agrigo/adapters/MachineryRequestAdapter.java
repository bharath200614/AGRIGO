package com.agrigo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agrigo.R;
import com.agrigo.models.MachineryBooking;
import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

public class MachineryRequestAdapter extends RecyclerView.Adapter<MachineryRequestAdapter.ViewHolder> {

    private final List<MachineryBooking> requestList;
    private final OnRequestClickListener listener;

    public interface OnRequestClickListener {
        void onAcceptClick(MachineryBooking booking);
    }

    public MachineryRequestAdapter(List<MachineryBooking> requestList, OnRequestClickListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_machinery_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MachineryBooking booking = requestList.get(position);
        holder.tvFarmerName.setText(booking.getFarmerName() != null ? booking.getFarmerName() : "Farmer");

        String typeDisplay = booking.getMachineryType() != null ? booking.getMachineryType() : "Unknown";
        // Capitalize first letter for display
        if (!typeDisplay.isEmpty()) {
            typeDisplay = typeDisplay.substring(0, 1).toUpperCase() + typeDisplay.substring(1);
        }
        holder.tvMachineryAndDuration.setText(typeDisplay + " • " + (booking.getDuration() != null ? booking.getDuration() : "?") + " Hours");

        // Distance
        if (booking.getDistanceKm() > 0) {
            holder.tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", booking.getDistanceKm()));
        } else {
            holder.tvDistance.setText("-- km");
        }

        // Location address
        if (booking.getAddress() != null && !booking.getAddress().isEmpty()) {
            holder.tvLocation.setText("📍 " + booking.getAddress());
        } else if (booking.getFarmerLat() != 0 && booking.getFarmerLng() != 0) {
            holder.tvLocation.setText(String.format(Locale.getDefault(), "📍 %.4f, %.4f", booking.getFarmerLat(), booking.getFarmerLng()));
        } else {
            holder.tvLocation.setText("📍 Location not available");
        }

        // Show Accept button only for REQUESTED status
        if ("REQUESTED".equals(booking.getStatus())) {
            holder.btnAcceptJob.setVisibility(View.VISIBLE);
            holder.btnAcceptJob.setOnClickListener(v -> listener.onAcceptClick(booking));
        } else {
            holder.btnAcceptJob.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFarmerName;
        TextView tvMachineryAndDuration;
        TextView tvDistance;
        TextView tvLocation;
        MaterialButton btnAcceptJob;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFarmerName = itemView.findViewById(R.id.tvFarmerName);
            tvMachineryAndDuration = itemView.findViewById(R.id.tvMachineryAndDuration);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            btnAcceptJob = itemView.findViewById(R.id.btnAcceptJob);
        }
    }
}
