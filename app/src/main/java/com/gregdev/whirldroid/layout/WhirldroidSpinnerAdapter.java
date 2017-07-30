package com.gregdev.whirldroid.layout;

import android.widget.ArrayAdapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;

import java.util.List;

public class WhirldroidSpinnerAdapter extends ArrayAdapter<String> {

    protected List<String> items;
    protected Context context;
    private String firstItemDropdownValue;

    public WhirldroidSpinnerAdapter(Context context, int resource, List<String> items) {
        super(context, resource, items);
        this.items = items;
        this.context = context;
        firstItemDropdownValue = null;
    }

    public WhirldroidSpinnerAdapter(Context context, int resource, List<String> items, String firstItemDropdownValue) {
        super(context, resource, items);
        this.items = items;
        this.context = context;
        this.firstItemDropdownValue = firstItemDropdownValue;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String groupName = items.get(position);

        if (convertView == null) {
            LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = vi.inflate(R.layout.spinner_item, null);
        }
        if (groupName != null) {
            TextView title = (TextView) convertView.findViewById(R.id.title);

            if (title != null) {
                title.setText(groupName);
            }
        }

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (firstItemDropdownValue != null && position == 0) {
            if (convertView == null) {
                LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = vi.inflate(R.layout.spinner_dropdown_item, null);
            }
            TextView title = (TextView) convertView.findViewById(android.R.id.text1);

            final float scale = getContext().getResources().getDisplayMetrics().density;
            int minHeight = (int) (48 * scale + 0.5f);

            title.setMinHeight(minHeight);
            title.setText(firstItemDropdownValue);

            return convertView;
        }

        return super.getDropDownView(position, null, parent);
    }
}