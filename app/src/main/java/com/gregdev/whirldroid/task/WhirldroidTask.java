package com.gregdev.whirldroid.task;

import android.os.AsyncTask;

public abstract class WhirldroidTask<R> extends AsyncTask<String, Void, Boolean> {

    public static final int TAG_THREAD_WATCH    = 0;
    public static final int TAG_THREAD_UNWATCH  = 1;
    public static final int TAG_THREAD_READ     = 2;

    protected WhirldroidTaskOnCompletedListener listener = null;
    protected int tag;
    protected R subject;

    public WhirldroidTask(R subject) {
        this.subject = subject;
    }

    public void setOnCompletedListener(WhirldroidTaskOnCompletedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onPostExecute(final Boolean result) {
        if (listener != null) {
            listener.onWhirldroidTaskCompleted(this, result);
        }
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