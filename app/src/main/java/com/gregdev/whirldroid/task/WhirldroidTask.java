package com.gregdev.whirldroid.task;

import android.os.AsyncTask;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;

import com.gregdev.whirldroid.R;

public abstract class WhirldroidTask<R> extends AsyncTask<String, Void, Boolean> {

    public static final int TAG_THREAD_WATCH    = 0;
    public static final int TAG_THREAD_UNWATCH  = 1;
    public static final int TAG_THREAD_READ     = 2;

    protected WhirldroidTaskComplete caller;
    protected int tag;
    protected R subject;

    public WhirldroidTask(WhirldroidTaskComplete caller, R subject) {
        this.caller     = caller;
        this.subject    = subject;
    }

    @Override
    protected void onPostExecute(final Boolean result) {
        caller.taskComplete(this, result);
    }

    public R getSubject() {
        return subject;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    public int getTag() {
        return tag;
    }

}