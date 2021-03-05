package com.gregdev.whirldroid.layout;

import java.util.LinkedHashMap;
import java.util.Map;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;

import com.gregdev.whirldroid.R;

/**
 * A BaseAdapter that contains multiple other adapters, together
 * with a heading for each adapter.
 *
 * @author http://jsharkey.org/blog/2008/08/18/separating-lists-with-headers-in-android-09/
 *
 */
public class SeparatedListAdapter extends BaseAdapter {

    public final Map<String, Adapter> sections = new LinkedHashMap<String, Adapter>();
    public final ArrayAdapter<String> headers;
    public final static int TYPE_SECTION_HEADER = 0;
    private boolean clickableHeaders;

    public SeparatedListAdapter(Context context) {
        this(context, false);
    }

    public SeparatedListAdapter(Context context, boolean clickableHeaders) {
        headers = new ArrayAdapter<>(context, R.layout.list_header);
        this.clickableHeaders = clickableHeaders;
    }

    public void addSection(String section, Adapter adapter) {
        if (!sections.containsKey(section)) {
            headers.add(section);
            sections.put(section, adapter);
        }
    }

    public Object getItem(int position) {
        for (Object section : this.sections.keySet()) {
            Adapter adapter = sections.get(section);
            int size = adapter.getCount() + 1;

            // check if position inside this section
            if(position == 0) return section;
            if(position < size) return adapter.getItem(position - 1);

            // otherwise jump into next section
            position -= size;
        }
        return null;
    }

    public int getCount() {
        // total together all sections, plus one for each section header
        int total = 0;
        for(Adapter adapter : this.sections.values())
            total += adapter.getCount() + 1;
        return total;
    }

    @Override
    public int getViewTypeCount() {
        // assume that headers count as one, then total all sections
        int total = 1;
        for (Adapter adapter : this.sections.values())
            total += adapter.getViewTypeCount();
        return total;
    }

    @Override
    public int getItemViewType(int position) {
        int type = 1;
        for (Object section : this.sections.keySet()) {
            Adapter adapter = sections.get(section);
            int size = adapter.getCount() + 1;

            // check if position inside this section
            if(position == 0) return TYPE_SECTION_HEADER;
            if(position < size) return type + adapter.getItemViewType(position - 1);

            // otherwise jump into next section
            position -= size;
            type += adapter.getViewTypeCount();
        }
        return -1;
    }

    public boolean areAllItemsSelectable() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return clickableHeaders || (getItemViewType(position) != TYPE_SECTION_HEADER);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        int sectionnum = 0;

        for (Object section : this.sections.keySet()) {
            Adapter adapter = sections.get(section);
            int size = adapter.getCount() + 1;

            // check if position inside this section
            if (position == 0) {
                return headers.getView(sectionnum, convertView, parent);
            }

            if (position < size) {
                return adapter.getView(position - 1, convertView, parent);
            }

            // otherwise jump into next section
            position -= size;
            sectionnum++;
        }

        return null;
    }

    public long getItemId(int position) {
        return position;
    }

}