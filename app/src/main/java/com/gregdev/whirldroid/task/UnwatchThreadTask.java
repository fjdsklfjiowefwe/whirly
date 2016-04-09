package com.gregdev.whirldroid.task;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.WhirlpoolApi;
import com.gregdev.whirldroid.WhirlpoolApiException;

public class UnwatchThreadTask extends WhirldroidTask<String> {

    public UnwatchThreadTask(WhirldroidTaskComplete caller, String subject) {
        super(caller, subject);
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