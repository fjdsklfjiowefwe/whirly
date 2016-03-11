package com.gregdev.whirldroid.layout;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.gregdev.whirldroid.R;

import java.util.ArrayList;

public class IconArrayAdapter extends BaseAdapter {

    Context context;
    private ArrayList<Pair<String, Integer>> items = new ArrayList<Pair<String, Integer>>();
    int alpha = 100;

    public IconArrayAdapter(Context context, ArrayList<Pair<String, Integer>> items, int alpha) {
        this.context = context;
        this.items = items;
    }

    @Override
    public View getView(int position, View coverView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.drawer_list_item, parent, false);

        TextView text1 = (TextView) rowView.findViewById(android.R.id.text1);

        Drawable icon = ContextCompat.getDrawable(context, items.get(position).second);
        icon.setAlpha(alpha);

        text1.setText(items.get(position).first);
        text1.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);

        return rowView;
    }

    public long getItemId(int position) {
        return position;
    }

    public Object getItem(int position) {
        return items.get(position);
    }

    public int getCount() {
        return items.size();
    }

}