package com.gregdev.whirldroid.task;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;

public class UnwatchThreadTask extends WhirldroidTask<String> {

    public UnwatchThreadTask(String subject) {
        super(subject);
        setTag(WhirldroidTask.TAG_THREAD_UNWATCH);
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            Whirldroid.getApi().downloadWatched(WhirlpoolApi.WATCHMODE_UNREAD, null, subject, 0);
            return true;
        } catch (final WhirlpoolApiException e) {
            return false;
        }
    }
}