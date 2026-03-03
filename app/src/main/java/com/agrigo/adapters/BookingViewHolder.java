package com.agrigo.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.agrigo.R;
import com.agrigo.models.Booking;
import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * ViewHolder for booking items
 */
public class BookingViewHolder extends RecyclerView.ViewHolder {
    
    public ImageView ivVehicleIcon;
    public TextView tvVehicleType;
    public TextView tvDate;
    public TextView tvStatus;
    public TextView tvCropType;
    public TextView tvWeight;
    public TextView tvFare;
    public LinearLayout expandedDetails;
    private boolean isExpanded = false;

    public BookingViewHolder(View itemView) {
        super(itemView);
        this.ivVehicleIcon = itemView.findViewById(R.id.ivVehicleIcon);
        this.tvVehicleType = itemView.findViewById(R.id.tvVehicleType);
        this.tvDate = itemView.findViewById(R.id.tvDate);
        this.tvStatus = itemView.findViewById(R.id.tvStatus);
        this.tvCropType = itemView.findViewById(R.id.tvCropType);
        this.tvWeight = itemView.findViewById(R.id.tvWeight);
        this.tvFare = itemView.findViewById(R.id.tvFare);
        this.expandedDetails = itemView.findViewById(R.id.expandedDetails);
    }

    public void bind(Booking booking, Context context) {
        // Set vehicle type
        tvVehicleType.setText(getVehicleTypeName(booking.getVehicleType()));
        
        // Set date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        tvDate.setText(dateFormat.format(booking.getCreatedAt()));
        
        // Set status with color
        tvStatus.setText(getStatusLabel(booking.getStatus()));
        tvStatus.setBackgroundResource(getStatusBackgroundResource(booking.getStatus()));
        tvStatus.setTextColor(getStatusTextColor(booking.getStatus()));
        
        // Set crop type and weight
        tvCropType.setText(booking.getCropType().substring(0, 1).toUpperCase() + 
                           booking.getCropType().substring(1));
        tvWeight.setText(booking.getWeight() + " kg");
        
        // Set fare
        tvFare.setText("₹" + booking.getFare());
        
        // Set vehicle icon
        setVehicleIcon(booking.getVehicleType());
        
        // Setup expandable details
        itemView.setOnClickListener(v -> toggleExpanded());
    }

    private void toggleExpanded() {
        if (isExpanded) {
            expandedDetails.setVisibility(View.GONE);
            isExpanded = false;
        } else {
            expandedDetails.setVisibility(View.VISIBLE);
            isExpanded = true;
        }
    }

    private void setVehicleIcon(String vehicleType) {
        switch (vehicleType.toLowerCase()) {
            case "auto":
                ivVehicleIcon.setImageResource(R.drawable.ic_auto_vehicle);
                break;
            case "mini_truck":
                ivVehicleIcon.setImageResource(R.drawable.ic_mini_truck);
                break;
            case "truck":
                ivVehicleIcon.setImageResource(R.drawable.ic_truck);
                break;
            case "lorry":
                ivVehicleIcon.setImageResource(R.drawable.ic_lorry);
                break;
            default:
                ivVehicleIcon.setImageResource(R.drawable.ic_truck);
        }
    }

    private String getVehicleTypeName(String type) {
        switch (type.toLowerCase()) {
            case "auto":
                return "Auto/Tuk-tuk";
            case "mini_truck":
                return "Mini Truck";
            case "truck":
                return "Standard Truck";
            case "lorry":
                return "Heavy Lorry";
            default:
                return type;
        }
    }

    private String getStatusLabel(String status) {
        switch (status.toLowerCase()) {
            case "ongoing":
                return "Ongoing";
            case "completed":
                return "Completed";
            case "requested":
                return "Requested";
            case "cancelled":
                return "Cancelled";
            default:
                return status;
        }
    }

    private int getStatusBackgroundResource(String status) {
        switch (status.toLowerCase()) {
            case "ongoing":
                return R.drawable.bg_status_ongoing;
            case "completed":
                return R.drawable.bg_status_completed;
            case "requested":
                return R.drawable.bg_status_requested;
            case "cancelled":
                return R.drawable.bg_status_cancelled;
            default:
                return R.drawable.bg_status_requested;
        }
    }

    private int getStatusTextColor(String status) {
        switch (status.toLowerCase()) {
            case "ongoing":
            case "completed":
            case "requested":
            case "cancelled":
                return Color.WHITE;
            default:
                return Color.WHITE;
        }
    }
}
