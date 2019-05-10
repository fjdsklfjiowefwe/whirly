package com.gregdev.whirldroid.adapters;

import android.support.annotation.NonNull;
import android.support.v7.recyclerview.extensions.AsyncDifferConfig;
import android.support.v7.recyclerview.extensions.AsyncListDiffer;
import android.support.v7.recyclerview.extensions.ListAdapter;
import android.support.v7.util.AdapterListUpdateCallback;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public abstract class CardViewAdapter<T> extends ListAdapter<T, CardViewAdapter<T>.ViewHolder> {

    protected int rowLayoutResource;

    public CardViewAdapter(DiffUtil.ItemCallback<T> diffCallback) {
        super(diffCallback);
    }

    public CardViewAdapter(AsyncDifferConfig<T> config) {
        super(config);
    }

    public void setRowLayoutResource(int rowLayoutResource) {
        this.rowLayoutResource = rowLayoutResource;
    }

    public T getItem(int position) {
        return super.getItem(position);
    }

    @Override
    public CardViewAdapter<T>.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemLayoutView = LayoutInflater.from(parent.getContext()).inflate(rowLayoutResource, parent, false);

        return new ViewHolder(itemLayoutView);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewAdapter<T>.ViewHolder viewHolder, int position) {
        T item = getItem(position);
        viewHolder.setItem(item);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        View itemLayoutView;
        T    item;

        public void setItem(T item) {
            this.item = item;
        }

        public ViewHolder(final View itemLayoutView) {
            super(itemLayoutView);

            this.itemLayoutView = itemLayoutView;
        }

        public View getView(int id) {
            return itemLayoutView.findViewById(id);
        }
    }

}

