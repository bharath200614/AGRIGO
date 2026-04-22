package com.agrigo.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agrigo.R;
import com.agrigo.models.LaborListing;
import com.agrigo.utils.ToastUtils;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class LaborListingAdapter extends RecyclerView.Adapter<LaborListingAdapter.ViewHolder> {

    private Context context;
    private List<LaborListing> laborList;
    private FirebaseFirestore db;

    public LaborListingAdapter(Context context, List<LaborListing> laborList) {
        this.context = context;
        this.laborList = laborList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_labor_listing, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LaborListing labor = laborList.get(position);

        holder.tvLaborName.setText(labor.getLaborName());
        holder.tvExpertise.setText("Expertise: " + labor.getCropType());
        holder.tvPrice.setText("₹" + labor.getPrice() + "/day");
        holder.tvLocation.setText(labor.getLocation());

        holder.btnContact.setOnClickListener(v -> {
            // Fetch phone number from users collection based on laborId and initiate call
            db.collection("users").document(labor.getLaborId()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && documentSnapshot.contains("phone")) {
                        String phone = documentSnapshot.getString("phone");
                        Intent intent = new Intent(Intent.ACTION_DIAL);
                        intent.setData(Uri.parse("tel:" + phone));
                        context.startActivity(intent);
                    } else {
                        ToastUtils.showShort(context, "Contact number not available");
                    }
                })
                .addOnFailureListener(e -> ToastUtils.showShort(context, "Failed to get contact info"));
        });
    }

    @Override
    public int getItemCount() {
        return laborList.size();
    }

    public void updateList(List<LaborListing> newList) {
        this.laborList = newList;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvLaborName, tvExpertise, tvPrice, tvLocation;
        Button btnContact;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLaborName = itemView.findViewById(R.id.tv_worker_name);
            tvExpertise = itemView.findViewById(R.id.tv_expertise);
            tvPrice = itemView.findViewById(R.id.tv_daily_wage);
            tvLocation = itemView.findViewById(R.id.tv_location);
            btnContact = itemView.findViewById(R.id.btn_contact);
        }
    }
}
