package com.gregdev.whirldroid.forumlist;

import android.support.v7.recyclerview.extensions.AsyncDifferConfig;
import android.support.v7.util.DiffUtil;

import com.gregdev.whirldroid.adapters.CardViewAdapter;
import com.gregdev.whirldroid.model.Forum;

public class ForumListAdapter extends CardViewAdapter<Forum> {

    public ForumListAdapter(DiffUtil.ItemCallback<Forum> diffCallback) {
        super(diffCallback);
    }

    public ForumListAdapter(AsyncDifferConfig<Forum> config) {
        super(config);
    }

    public void onBindViewHolder(CardViewAdapter<Forum>.ViewHolder viewHolder, int position) {

    }

}
