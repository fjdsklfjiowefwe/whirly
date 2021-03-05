package com.gregdev.whirldroid.task;

import android.content.Context;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiFactory;

public class MarkThreadReadTask extends WhirldroidTask<String> {

    public MarkThreadReadTask(String subject, Context context) {
        super(subject, context);
        setTag(WhirldroidTask.TAG_THREAD_READ);
    }

    @Override
    protected Boolean doInBackground(String... params) {
        WhirlpoolApiFactory.getFactory().getApi(context).getUnreadWatchedThreadsManager().markRead(subject);
        WhirlpoolApiFactory.getFactory().getApi(context).getAllWatchedThreadsManager().markRead(subject);

        try {
            WhirlpoolApiFactory.getFactory().getApi(context).getUnreadWatchedThreadsManager().download(subject, null, "");
            return true;

        } catch (final WhirlpoolApiException e) {
            return false;
        }
    }
}