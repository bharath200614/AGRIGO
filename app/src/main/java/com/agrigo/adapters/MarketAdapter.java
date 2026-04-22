package com.agrigo.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.agrigo.R;
import com.agrigo.models.Market;

import java.util.List;
import java.util.Locale;

public class MarketAdapter extends RecyclerView.Adapter<MarketAdapter.ViewHolder> {

    private List<Market> markets;
    private OnMarketClickListener listener;

    public interface OnMarketClickListener {
        void onMarketClick(Market market);
    }

    public MarketAdapter(List<Market> markets, OnMarketClickListener listener) {
        this.markets = markets;
        this.listener = listener;
    }

    public void updateMarkets(List<Market> newMarkets) {
        this.markets = newMarkets;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_market_suggestion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Market market = markets.get(position);
        holder.tvMarketName.setText(market.getName());
        holder.tvMarketDistrict.setText(market.getDistrict());
        holder.tvMarketDistance.setText(String.format(Locale.US, "%.0f km", market.getDistanceKm()));
        holder.tvMarketPrice.setText(String.format(Locale.US, "₹%,.0f/Q", market.getPricePerQuintal()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onMarketClick(market);
            }
        });
    }

    @Override
    public int getItemCount() {
        return markets != null ? markets.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvMarketName, tvMarketDistrict, tvMarketDistance, tvMarketPrice;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMarketName = itemView.findViewById(R.id.tvMarketName);
            tvMarketDistrict = itemView.findViewById(R.id.tvMarketDistrict);
            tvMarketDistance = itemView.findViewById(R.id.tvMarketDistance);
            tvMarketPrice = itemView.findViewById(R.id.tvMarketPrice);
        }
    }
}
