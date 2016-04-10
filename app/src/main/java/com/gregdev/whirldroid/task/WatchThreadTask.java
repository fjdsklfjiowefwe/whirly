package com.gregdev.whirldroid.task;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;

public class WatchThreadTask extends WhirldroidTask<Integer> {

    public WatchThreadTask(int subject) {
        super(subject);
        setTag(WhirldroidTask.TAG_THREAD_WATCH);
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            Whirldroid.getApi().downloadWatched(WhirlpoolApi.WATCHMODE_UNREAD, null, null, subject);
            return true;
        } catch (final WhirlpoolApiException e) {
            return false;
        }
    }
}