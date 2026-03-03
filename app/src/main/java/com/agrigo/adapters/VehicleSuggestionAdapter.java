package com.agrigo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.recyclerview.widget.RecyclerView;
import com.agrigo.R;
import com.agrigo.models.VehicleSuggestion;
import java.util.List;

/**
 * RecyclerView adapter for vehicle suggestions
 */
public class VehicleSuggestionAdapter extends RecyclerView.Adapter<VehicleSuggestionViewHolder> {
    
    private List<VehicleSuggestion> vehicleList;
    private Context context;
    private OnVehicleSelectListener listener;

    public VehicleSuggestionAdapter(Context context, List<VehicleSuggestion> vehicleList) {
        this.context = context;
        this.vehicleList = vehicleList;
    }

    @Override
    public VehicleSuggestionViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new VehicleSuggestionViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.item_vehicle_suggestion, parent, false));
    }

    @Override
    public void onBindViewHolder(VehicleSuggestionViewHolder holder, int position) {
        VehicleSuggestion vehicle = vehicleList.get(position);
        holder.bind(vehicle, context);
        
        holder.btnSelectVehicle.setOnClickListener(v -> {
            if (listener != null) {
                listener.onVehicleSelected(vehicle);
            }
        });
    }

    @Override
    public int getItemCount() {
        return vehicleList != null ? vehicleList.size() : 0;
    }

    public void setVehicles(List<VehicleSuggestion> vehicles) {
        this.vehicleList = vehicles;
        notifyDataSetChanged();
    }

    public void setOnVehicleSelectListener(OnVehicleSelectListener listener) {
        this.listener = listener;
    }

    public interface OnVehicleSelectListener {
        void onVehicleSelected(VehicleSuggestion vehicle);
    }
}
