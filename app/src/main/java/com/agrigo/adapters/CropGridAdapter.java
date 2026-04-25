package com.agrigo.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.agrigo.R;
import com.agrigo.models.Crop;
import com.google.android.material.card.MaterialCardView;
import java.util.List;

public class CropGridAdapter extends RecyclerView.Adapter<CropGridAdapter.CropViewHolder> {

    private List<Crop> cropList;
    private Context context;
    private OnCropSelectedListener listener;
    private int selectedPosition = -1;

    public interface OnCropSelectedListener {
        void onCropSelected(Crop crop);
    }

    public CropGridAdapter(Context context, List<Crop> cropList, OnCropSelectedListener listener) {
        this.context = context;
        this.cropList = cropList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CropViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_crop_card, parent, false);
        return new CropViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CropViewHolder holder, int position) {
        Crop crop = cropList.get(position);
        holder.tvCropName.setText(crop.getName());

        // Use emoji if available, otherwise fall back to drawable icon
        String emoji = crop.getEmoji();
        if (emoji != null && !emoji.isEmpty()) {
            holder.tvCropEmoji.setText(emoji);
            holder.tvCropEmoji.setVisibility(View.VISIBLE);
            holder.ivCropIcon.setVisibility(View.GONE);
        } else {
            holder.tvCropEmoji.setVisibility(View.GONE);
            holder.ivCropIcon.setVisibility(View.VISIBLE);
            holder.ivCropIcon.setImageResource(crop.getIconResId());
        }

        // Selection styling
        if (position == selectedPosition) {
            holder.cardCrop.setStrokeColor(ContextCompat.getColor(context, R.color.primary_green));
            holder.cardCrop.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
            holder.tvCropName.setTextColor(ContextCompat.getColor(context, R.color.primary_green));
        } else {
            holder.cardCrop.setStrokeColor(Color.TRANSPARENT);
            holder.cardCrop.setCardBackgroundColor(ContextCompat.getColor(context, R.color.gray_very_light));
            holder.tvCropName.setTextColor(ContextCompat.getColor(context, R.color.text_primary));
        }

        holder.itemView.setOnClickListener(v -> {
            int previousSelected = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(previousSelected);
            notifyItemChanged(selectedPosition);
            if (listener != null) {
                listener.onCropSelected(crop);
            }
        });
    }

    @Override
    public int getItemCount() {
        return cropList.size();
    }

    public void setSelectedCrop(String cropId) {
        for (int i = 0; i < cropList.size(); i++) {
            if (cropList.get(i).getId().equalsIgnoreCase(cropId)) {
                int prev = selectedPosition;
                selectedPosition = i;
                notifyItemChanged(prev);
                notifyItemChanged(selectedPosition);
                break;
            }
        }
    }

    public Crop getSelectedCrop() {
        if (selectedPosition != -1 && selectedPosition < cropList.size()) {
            return cropList.get(selectedPosition);
        }
        return null;
    }

    static class CropViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardCrop;
        TextView tvCropEmoji;
        ImageView ivCropIcon;
        TextView tvCropName;

        public CropViewHolder(@NonNull View itemView) {
            super(itemView);
            cardCrop = itemView.findViewById(R.id.cardCrop);
            tvCropEmoji = itemView.findViewById(R.id.tvCropEmoji);
            ivCropIcon = itemView.findViewById(R.id.ivCropIcon);
            tvCropName = itemView.findViewById(R.id.tvCropName);
        }
    }
}
