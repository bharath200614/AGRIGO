package com.agrigo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.agrigo.R;
import com.agrigo.models.Crop;

import java.util.List;

public class CropSpinnerAdapter extends BaseAdapter {
    private Context context;
    private List<Crop> crops;
    private LayoutInflater inflater;

    public CropSpinnerAdapter(Context context, List<Crop> crops) {
        this.context = context;
        this.crops = crops;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return crops.size();
    }

    @Override
    public Object getItem(int position) {
        return crops.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_crop_spinner, parent, false);
        }

        ImageView icon = convertView.findViewById(R.id.iv_crop_icon);
        TextView name = convertView.findViewById(R.id.tv_crop_name);

        Crop crop = crops.get(position);
        icon.setImageResource(crop.getIconResId());
        name.setText(crop.getName());

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_crop_spinner_dropdown, parent, false);
        }

        ImageView icon = convertView.findViewById(R.id.iv_crop_icon);
        TextView name = convertView.findViewById(R.id.tv_crop_name);

        Crop crop = crops.get(position);
        icon.setImageResource(crop.getIconResId());
        name.setText(crop.getName());

        return convertView;
    }
}
