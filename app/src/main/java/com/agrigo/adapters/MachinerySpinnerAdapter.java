package com.agrigo.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.agrigo.R;

public class MachinerySpinnerAdapter extends BaseAdapter {

    private final Context context;
    private final String[] names;
    private final int[] icons;

    public MachinerySpinnerAdapter(Context context, String[] names, int[] icons) {
        this.context = context;
        this.names = names;
        this.icons = icons;
    }

    @Override
    public int getCount() {
        return names.length;
    }

    @Override
    public String getItem(int position) {
        return names[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return createView(position, convertView, parent);
    }

    private View createView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_machinery_spinner, parent, false);
        }

        ImageView iconView = convertView.findViewById(R.id.icon_machinery);
        TextView textView = convertView.findViewById(R.id.text_machinery);

        textView.setText(names[position]);

        if (icons[position] != 0) {
            iconView.setImageResource(icons[position]);
            iconView.setVisibility(View.VISIBLE);
        } else {
            iconView.setVisibility(View.GONE);
        }

        return convertView;
    }
}
