package com.agrigo.adapters;

import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agrigo.R;
import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;
import java.util.Locale;

public class LaborJobAdapter extends RecyclerView.Adapter<LaborJobAdapter.JobViewHolder> {

    private List<DocumentSnapshot> jobList;
    private OnJobAcceptListener listener;
    private Location currentLocation;

    public interface OnJobAcceptListener {
        void onAcceptClicked(DocumentSnapshot document);
        void onRejectClicked(DocumentSnapshot document);
    }

    public LaborJobAdapter(List<DocumentSnapshot> jobList, OnJobAcceptListener listener, Location initialLocation) {
        this.jobList = jobList;
        this.listener = listener;
        this.currentLocation = initialLocation;
    }

    public void updateData(List<DocumentSnapshot> newList, Location location) {
        this.jobList = newList;
        if (location != null) {
            this.currentLocation = location;
        }
        notifyDataSetChanged();
    }

    public void updateCurrentLocation(Location newLocation) {
        if (newLocation != null) {
            this.currentLocation = newLocation;
            notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_labor_job, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        DocumentSnapshot doc = jobList.get(position);
        
        String rawWorkType = doc.getString("workType");
        String typeDisplay = rawWorkType != null ? rawWorkType : "Unknown";
        if (!typeDisplay.isEmpty()) {
            typeDisplay = typeDisplay.substring(0, 1).toUpperCase() + typeDisplay.substring(1);
        }
        holder.tvWorkType.setText(typeDisplay);
        
        String duration = doc.getString("duration");
        holder.tvDuration.setText(duration + " " + (duration != null && duration.equals("1") ? holder.itemView.getContext().getString(R.string.day) : holder.itemView.getContext().getString(R.string.days)));
        
        holder.tvFarmerName.setText(holder.itemView.getContext().getString(R.string.posted_by) + doc.getString("farmerName"));
        
        // Location address
        String address = doc.getString("address");
        Double fLat = doc.getDouble("farmerLat");
        Double fLng = doc.getDouble("farmerLng");

        if (address != null && !address.isEmpty()) {
            holder.tvLocation.setText("📍 " + address);
        } else if (fLat != null && fLng != null) {
            holder.tvLocation.setText(String.format(Locale.getDefault(), "📍 %.4f, %.4f", fLat, fLng));
        } else {
            holder.tvLocation.setText("📍 " + holder.itemView.getContext().getString(R.string.location_not_available));
        }

        // Distance Calculation
        if (currentLocation != null && fLat != null && fLng != null) {
            double distMeters = com.firebase.geofire.GeoFireUtils.getDistanceBetween(
                    new com.firebase.geofire.GeoLocation(fLat, fLng),
                    new com.firebase.geofire.GeoLocation(currentLocation.getLatitude(), currentLocation.getLongitude())
            );
            holder.tvDistance.setText(String.format(Locale.getDefault(), "%.1f km", distMeters / 1000.0));
        } else {
            holder.tvDistance.setText("-- km");
        }

        Long req = doc.getLong("workersRequired");
        Long acc = doc.getLong("workersAccepted");
        long required = req != null ? req : 1;
        long accepted = acc != null ? acc : 0;
        
        holder.tvWorkersCount.setText(accepted + "/" + required + " " + holder.itemView.getContext().getString(R.string.workers_label));
        
        String price = doc.getString("estimatedPrice");
        holder.tvEstimatedPrice.setText(price != null ? price : "₹0.00");

        // Disable accept button if full
        if (accepted >= required) {
            holder.btnAcceptJob.setEnabled(false);
            holder.btnAcceptJob.setText(holder.itemView.getContext().getString(R.string.job_full));
            holder.tvWorkersCount.setTextColor(0xFFD32F2F); // Red
        } else {
            holder.btnAcceptJob.setEnabled(true);
            holder.btnAcceptJob.setText(holder.itemView.getContext().getString(R.string.accept_job));
            holder.tvWorkersCount.setTextColor(0xFF2E7D32); // Green
        }

        holder.btnAcceptJob.setOnClickListener(v -> {
            if(listener != null) {
                listener.onAcceptClicked(doc);
            }
        });

        holder.btnRejectJob.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRejectClicked(doc);
            }
        });
    }

    @Override
    public int getItemCount() {
        return jobList.size();
    }

    static class JobViewHolder extends RecyclerView.ViewHolder {
        TextView tvWorkType, tvDuration, tvFarmerName, tvWorkersCount, tvEstimatedPrice;
        TextView tvDistance, tvLocation;
        MaterialButton btnAcceptJob, btnRejectJob;

        public JobViewHolder(@NonNull View itemView) {
            super(itemView);
            tvWorkType = itemView.findViewById(R.id.tvWorkType);
            tvDuration = itemView.findViewById(R.id.tvDuration);
            tvFarmerName = itemView.findViewById(R.id.tvFarmerName);
            tvWorkersCount = itemView.findViewById(R.id.tvWorkersCount);
            tvEstimatedPrice = itemView.findViewById(R.id.tvEstimatedPrice);
            tvDistance = itemView.findViewById(R.id.tvDistance);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            btnAcceptJob = itemView.findViewById(R.id.btnAcceptJob);
            btnRejectJob = itemView.findViewById(R.id.btnRejectJob);
        }
    }
}
