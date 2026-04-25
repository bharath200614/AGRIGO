package com.agrigo.adapters;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.agrigo.R;
import com.agrigo.models.VehicleSuggestion;
import com.agrigo.utils.CropUtils;

/**
 * ViewHolder for vehicle suggestion items
 */
public class VehicleSuggestionViewHolder extends RecyclerView.ViewHolder {
    
    public ImageView ivVehicleIcon;
    public TextView tvVehicleName;
    public TextView tvVehicleType;
    public TextView tvEstimatedCost;
    public Button btnSelectVehicle;

    public VehicleSuggestionViewHolder(View itemView) {
        super(itemView);
        this.ivVehicleIcon = itemView.findViewById(R.id.iv_vehicle_icon);
        this.tvVehicleName = itemView.findViewById(R.id.tv_vehicle_name);
        this.tvVehicleType = itemView.findViewById(R.id.tv_vehicle_type);
        this.tvEstimatedCost = itemView.findViewById(R.id.tv_price);
        this.btnSelectVehicle = itemView.findViewById(R.id.btnSelectVehicle);
    }

    public void bind(VehicleSuggestion vehicle, Context context) {
        // Set vehicle name
        tvVehicleName.setText(vehicle.getVehicleName());
        
        // Set vehicle capacity
        tvVehicleType.setText(context.getString(R.string.capacity_format, vehicle.getCapacity()));
        
        // Set estimated cost
        tvEstimatedCost.setText(String.format("₹%.0f", vehicle.getEstimatedCost()));
        
        // Set vehicle icon based on type
        setVehicleIcon(vehicle.getVehicleType());
    }

    private void setVehicleIcon(String vehicleType) {
        // Set different icons based on vehicle type
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
}
