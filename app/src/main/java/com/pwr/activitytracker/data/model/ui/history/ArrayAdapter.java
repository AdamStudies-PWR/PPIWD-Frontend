package com.pwr.activitytracker.data.model.ui.history;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pwr.activitytracker.R;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ArrayAdapter extends android.widget.ArrayAdapter<HistoryData> {

    private Context mContext;
    private int mResources;
    public ArrayAdapter(@NonNull Context context, int resource, @NonNull ArrayList<HistoryData> objects) {
        super(context, resource, objects);
        mContext=context;
        mResources=resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        convertView =layoutInflater.inflate(mResources,parent,false);
        TextView duration = convertView.findViewById(R.id.row_duration);
        TextView count = convertView.findViewById(R.id.row_count);
        TextView date = convertView.findViewById(R.id.row_date);

        duration.setText(getItem(position).getDuration());
        count.setText(String.valueOf(getItem(position).getCount()));
        date.setText(getItem(position).getData());

        return convertView;

    }
}
