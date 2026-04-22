package com.agrigo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agrigo.R;
import com.agrigo.models.DriverRequest;
import com.agrigo.utils.CropUtils;
import com.google.android.material.button.MaterialButton;
import android.widget.ImageView;

import java.util.List;

public class DriverRequestAdapter extends RecyclerView.Adapter<DriverRequestAdapter.ViewHolder> {

    private List<DriverRequest> requestList;
    private OnRequestClickListener listener;

    public interface OnRequestClickListener {
        void onAcceptClick(DriverRequest request);
        void onDeclineClick(DriverRequest request);
    }

    public DriverRequestAdapter(List<DriverRequest> requestList, OnRequestClickListener listener) {
        this.requestList = requestList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_driver_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DriverRequest request = requestList.get(position);

        holder.tvFarmerName.setText(request.getFarmerName());
        
        String route = "Route unknown";
        if (request.getSourceAddress() != null && !request.getSourceAddress().isEmpty()) {
            route = request.getSourceAddress() + " ➔ " + request.getDestAddress();
        } else if (request.getFromLocation() != null) {
            route = request.getFromLocation() + " ➔ " + request.getToLocation();
        }
        holder.tvRoute.setText(route);
        
        // Format timestamp to date string
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a");
        String dateString = sdf.format(new java.util.Date(request.getTimestamp()));
        holder.tvDate.setText(dateString);
        
        holder.tvVehicleDetail.setText("Vehicle: " + request.getVehicleType());
        holder.tvRequestCost.setText("₹" + (int)request.getPrice());

        // Set Crop Icon
        int cropIcon = CropUtils.getCropIcon(holder.itemView.getContext(), request.getCropType());
        holder.ivCropIcon.setImageResource(cropIcon);

        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAcceptClick(request);
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onDeclineClick(request);
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFarmerName;
        TextView tvRoute;
        TextView tvDate;
        TextView tvVehicleDetail;
        TextView tvRequestCost;
        ImageView ivCropIcon;
        MaterialButton btnAccept;
        MaterialButton btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFarmerName = itemView.findViewById(R.id.tv_farmer_name);
            tvRoute = itemView.findViewById(R.id.tv_route);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvVehicleDetail = itemView.findViewById(R.id.tv_vehicle_detail);
            tvRequestCost = itemView.findViewById(R.id.tv_request_cost);
            ivCropIcon = itemView.findViewById(R.id.iv_crop_icon);
            btnAccept = itemView.findViewById(R.id.btn_accept);
            btnReject = itemView.findViewById(R.id.btn_reject);
        }
    }
}
