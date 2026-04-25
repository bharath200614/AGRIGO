package com.agrigo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agrigo.R;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LaborEarningsAdapter extends RecyclerView.Adapter<LaborEarningsAdapter.EarningViewHolder> {

    private List<DocumentSnapshot> earningList;

    public LaborEarningsAdapter(List<DocumentSnapshot> earningList) {
        this.earningList = earningList;
    }

    public void updateData(List<DocumentSnapshot> newList) {
        this.earningList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EarningViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_labor_earning, parent, false);
        return new EarningViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EarningViewHolder holder, int position) {
        DocumentSnapshot doc = earningList.get(position);

        holder.tvFarmerName.setText(doc.getString("farmerName"));
        
        Long timestamp = doc.getLong("timestamp");
        if (timestamp != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(timestamp)));
        }

        Double amount = doc.getDouble("amount");
        holder.tvAmount.setText(String.format(Locale.getDefault(), "+₹%.0f", amount != null ? amount : 0.0));
    }

    @Override
    public int getItemCount() {
        return earningList.size();
    }

    static class EarningViewHolder extends RecyclerView.ViewHolder {
        TextView tvFarmerName, tvDate, tvAmount;

        public EarningViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFarmerName = itemView.findViewById(R.id.tvFarmerName);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvAmount = itemView.findViewById(R.id.tvAmount);
        }
    }
}
