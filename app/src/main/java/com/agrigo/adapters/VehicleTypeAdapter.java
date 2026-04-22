package com.agrigo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.agrigo.R;

/**
 * Custom adapter for displaying vehicle types with icons in a dropdown popup.
 */
public class VehicleTypeAdapter extends BaseAdapter {

    private final Context context;
    private final String[] vehicleNames;
    private final int[] vehicleIcons;
    private int selectedPosition = 0;

    public VehicleTypeAdapter(Context context, String[] vehicleNames, int[] vehicleIcons) {
        this.context = context;
        this.vehicleNames = vehicleNames;
        this.vehicleIcons = vehicleIcons;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }

    public int getSelectedPosition() {
        return selectedPosition;
    }

    @Override
    public int getCount() {
        return vehicleNames.length;
    }

    @Override
    public String getItem(int position) {
        return vehicleNames[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_vehicle_type, parent, false);
            holder = new ViewHolder();
            holder.ivIcon = convertView.findViewById(R.id.iv_vehicle_icon);
            holder.tvName = convertView.findViewById(R.id.tv_vehicle_name);
            holder.ivCheck = convertView.findViewById(R.id.iv_check);
            holder.root = convertView.findViewById(R.id.vehicle_item_root);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.ivIcon.setImageResource(vehicleIcons[position]);
        holder.tvName.setText(vehicleNames[position]);

        // Show/hide check and highlight selected row
        if (position == selectedPosition) {
            holder.ivCheck.setVisibility(View.VISIBLE);
            holder.root.setBackgroundResource(R.drawable.bg_vehicle_selected);
            holder.tvName.setTextColor(context.getResources().getColor(R.color.primary_blue));
        } else {
            holder.ivCheck.setVisibility(View.GONE);
            holder.root.setBackgroundResource(android.R.color.transparent);
            holder.tvName.setTextColor(0xFF333333);
        }

        return convertView;
    }

    private static class ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        ImageView ivCheck;
        View root;
    }
}
