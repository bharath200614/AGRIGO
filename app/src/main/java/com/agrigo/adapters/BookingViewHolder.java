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
import com.agrigo.utils.CropUtils;
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
        this.ivVehicleIcon = itemView.findViewById(R.id.iv_booking_icon);
        this.tvVehicleType = itemView.findViewById(R.id.tv_booking_type);
        this.tvDate = itemView.findViewById(R.id.tv_booking_date);
        this.tvStatus = itemView.findViewById(R.id.tv_status_badge);
        this.tvCropType = itemView.findViewById(R.id.tv_crop_type);
        this.tvWeight = itemView.findViewById(R.id.tv_weight);
        this.tvFare = itemView.findViewById(R.id.tv_booking_cost);
        this.expandedDetails = itemView.findViewById(R.id.expandedDetails);
    }

    public void bind(Booking booking, Context context) {
        // Set vehicle type
        tvVehicleType.setText(getVehicleTypeName(booking.getVehicleType()));
        
        // Set date
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        tvDate.setText(dateFormat.format(booking.getTimestamp()));
        
        // Set status with color
        tvStatus.setText(getStatusLabel(booking.getStatus()));
        // Note: New premium style uses bg_card_blue_tint and primary_blue text
        tvStatus.setBackgroundResource(R.drawable.bg_card_blue_tint);
        tvStatus.setTextColor(context.getResources().getColor(R.color.primary_blue));
        
        // Set crop type and weight
        String cropName = booking.getCropType();
        tvCropType.setText(cropName.substring(0, 1).toUpperCase() + cropName.substring(1));
        tvWeight.setText(booking.getWeight() + " kg");
        
        // Set cost
        tvFare.setText("₹" + (int)booking.getCost());
        
        // Set crop icon using CropUtils
        int cropIcon = CropUtils.getCropIcon(context, cropName);
        ivVehicleIcon.setImageResource(cropIcon);
        
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
            case "small van":
            case "small_van":
                ivVehicleIcon.setImageResource(R.drawable.ic_small_van);
                break;
            case "mini truck":
            case "mini_truck":
                ivVehicleIcon.setImageResource(R.drawable.ic_mini_truck);
                break;
            case "pickup truck":
            case "pickup_truck":
                ivVehicleIcon.setImageResource(R.drawable.ic_pickup_truck);
                break;
            case "truck":
                ivVehicleIcon.setImageResource(R.drawable.ic_truck);
                break;
            case "large truck":
            case "large_truck":
            case "lorry":
                ivVehicleIcon.setImageResource(R.drawable.ic_large_truck);
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
        if (status == null) return "Unknown";
        switch (status.toLowerCase()) {
            case "ongoing":
                return "Ongoing";
            case "completed":
                return "Completed";
            case "requested":
                return "Requested";
            case "accepted":
                return "Accepted";
            case "cancelled":
                return "Cancelled";
            default:
                return status.substring(0, 1).toUpperCase() + status.substring(1).toLowerCase();
        }
    }

    private int getStatusBackgroundResource(String status) {
        if (status == null) return R.drawable.bg_status_requested;
        switch (status.toLowerCase()) {
            case "ongoing":
                return R.drawable.bg_status_ongoing;
            case "completed":
                return R.drawable.bg_status_completed;
            case "requested":
            case "accepted":
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
