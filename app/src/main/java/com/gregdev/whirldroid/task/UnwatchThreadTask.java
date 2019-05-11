package com.gregdev.whirldroid.task;

import android.content.Context;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiFactory;

public class UnwatchThreadTask extends WhirldroidTask<String> {

    public UnwatchThreadTask(String subject, Context context) {
        super(subject, context);
        setTag(WhirldroidTask.TAG_THREAD_UNWATCH);
    }

    @Override
    protected Boolean doInBackground(String... params) {
        WhirlpoolApiFactory.getFactory().getApi(context).getUnreadWatchedThreadsManager().unwatch(subject);
        WhirlpoolApiFactory.getFactory().getApi(context).getAllWatchedThreadsManager().unwatch(subject);

        try {
            WhirlpoolApiFactory.getFactory().getApi(context).getUnreadWatchedThreadsManager().download(null, subject, "");
            return true;

        } catch (final WhirlpoolApiException e) {
            return false;
        }
    }
}