package com.gregdev.whirldroid.task;

import android.content.Context;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiFactory;

public class WatchThreadTask extends WhirldroidTask<String> {

    public WatchThreadTask(String subject, Context context) {
        super(subject, context);
        setTag(WhirldroidTask.TAG_THREAD_WATCH);
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            WhirlpoolApiFactory.getFactory().getApi(context).getUnreadWatchedThreadsManager().download(null, null, subject);
            return true;

        } catch (final WhirlpoolApiException e) {
            return false;
        }
    }
}