package com.gregdev.whirldroid.task;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.WhirlpoolApi;
import com.gregdev.whirldroid.WhirlpoolApiException;

public class MarkThreadReadTask extends WhirldroidTask<String> {

    public MarkThreadReadTask(WhirldroidTaskCompletedListener caller, String subject) {
        super(caller, subject);
        setTag(WhirldroidTask.TAG_THREAD_READ);
    }

    @Override
    protected Boolean doInBackground(String... params) {
        try {
            Whirldroid.getApi().downloadWatched(WhirlpoolApi.WATCHMODE_UNREAD, subject, null, 0);
            return true;
        } catch (final WhirlpoolApiException e) {
            return false;
        }
    }
}