package com.agrigo.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agrigo.R;
import com.agrigo.models.ActiveJob;

import java.util.List;

public class ActiveJobsAdapter extends RecyclerView.Adapter<ActiveJobsAdapter.JobViewHolder> {

    private List<ActiveJob> activeJobsList;
    private Context context;
    private OnJobClickListener listener;

    public interface OnJobClickListener {
        void onJobClick(ActiveJob job);
    }

    public ActiveJobsAdapter(List<ActiveJob> activeJobsList, Context context, OnJobClickListener listener) {
        this.activeJobsList = activeJobsList;
        this.context = context;
        this.listener = listener;
    }

    @NonNull
    @Override
    public JobViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_active_job, parent, false);
        return new JobViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JobViewHolder holder, int position) {
        ActiveJob job = activeJobsList.get(position);
        
        holder.tvJobTitle.setText(job.getTitle());
        holder.tvJobSubtitle.setText(job.getSubtitle());
        holder.tvServiceType.setText(job.getServiceType());
        holder.tvJobStatus.setText(job.getStatus().toUpperCase());

        // Set Icon and Theme Color based on Service Type
        switch (job.getServiceType()) {
            case "Transport":
                holder.ivServiceIcon.setImageResource(R.drawable.ic_truck);
                holder.iconBackground.setBackgroundResource(R.drawable.bg_icon_circle_blue);
                holder.tvServiceType.setTextColor(Color.parseColor("#16A34A")); // Green
                break;
            case "Machinery":
                holder.ivServiceIcon.setImageResource(R.drawable.ic_tractor);
                holder.iconBackground.setBackgroundColor(Color.parseColor("#FFCC80")); 
                holder.tvServiceType.setTextColor(Color.parseColor("#15803D"));
                break;
            case "Labor":
                holder.ivServiceIcon.setImageResource(R.drawable.ic_people);
                holder.iconBackground.setBackgroundColor(Color.parseColor("#C8E6C9")); 
                holder.tvServiceType.setTextColor(Color.parseColor("#2E7D32"));
                break;
        }

        // Set Status Style
        switch (job.getStatus().toUpperCase()) {
            case "REQUESTED":
            case "PENDING":
                holder.tvJobStatus.setTextColor(Color.parseColor("#16A34A")); // Green
                holder.tvJobStatus.setBackgroundColor(Color.parseColor("#F0FDF4"));
                break;
            case "ACCEPTED":
            case "ARRIVED":
            case "ON_TRIP":
            case "WORK_STARTED":
            case "NAVIGATING_TO_PICKUP":
                holder.tvJobStatus.setTextColor(Color.parseColor("#16A34A")); // Green
                holder.tvJobStatus.setBackgroundColor(Color.parseColor("#F0FDF4"));
                break;
            case "COMPLETED":
            case "WORK_COMPLETED":
                holder.tvJobStatus.setTextColor(Color.parseColor("#2E7D32")); // Green
                holder.tvJobStatus.setBackgroundColor(Color.parseColor("#E8F5E9"));
                break;
            case "CANCELLED":
                holder.tvJobStatus.setTextColor(Color.parseColor("#D32F2F")); // Red
                holder.tvJobStatus.setBackgroundColor(Color.parseColor("#FFEBEE"));
                break;
            default:
                holder.tvJobStatus.setTextColor(Color.parseColor("#757575")); // Grey
                holder.tvJobStatus.setBackgroundColor(Color.parseColor("#F5F5F5"));
                break;
        }

        // Map Progress UI
        holder.tvProviderInfo.setText(job.getProviderName());
        holder.tvProgressText.setText(job.getProgressText());
        holder.pbJobProgress.setProgress(job.getProgressPercentage());
        
        // Hide progress block if everything is fully cancelled or completed?
        if ("CANCELLED".equalsIgnoreCase(job.getStatus())) {
            holder.layoutProgress.setVisibility(View.GONE);
        } else {
            holder.layoutProgress.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onJobClick(job);
            }
        });
    }

    @Override
    public int getItemCount() {
        return activeJobsList.size();
    }

    public static class JobViewHolder extends RecyclerView.ViewHolder {
        TextView tvJobTitle, tvJobSubtitle, tvJobStatus, tvServiceType;
        TextView tvProviderInfo, tvProgressText;
        android.widget.ProgressBar pbJobProgress;
        View layoutProgress;
        ImageView ivServiceIcon;
        View iconBackground;

        public JobViewHolder(@NonNull View itemView) {
            super(itemView);
            tvJobTitle = itemView.findViewById(R.id.tvJobTitle);
            tvJobSubtitle = itemView.findViewById(R.id.tvJobSubtitle);
            tvJobStatus = itemView.findViewById(R.id.tvJobStatus);
            tvServiceType = itemView.findViewById(R.id.tvServiceType);
            ivServiceIcon = itemView.findViewById(R.id.ivServiceIcon);
            iconBackground = itemView.findViewById(R.id.iconBackground);
            
            layoutProgress = itemView.findViewById(R.id.layoutProgress);
            tvProviderInfo = itemView.findViewById(R.id.tvProviderInfo);
            tvProgressText = itemView.findViewById(R.id.tvProgressText);
            pbJobProgress = itemView.findViewById(R.id.pbJobProgress);
        }
    }
}
