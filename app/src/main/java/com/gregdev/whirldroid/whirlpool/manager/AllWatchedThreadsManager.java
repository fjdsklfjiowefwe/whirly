package com.gregdev.whirldroid.whirlpool.manager;

import android.content.Context;

import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;

public class AllWatchedThreadsManager extends WatchedThreadsManager {

    public AllWatchedThreadsManager(Context context) {
        super(context);

        cacheFileName = "cache_all_watched_threads.txt";
    }

    public void download() throws WhirlpoolApiException {
        download(null, null, "");
    }

    public void download(String markAsReadIds, String unwatchIds, String watchId) throws WhirlpoolApiException {
        download(WhirlpoolApi.WATCHMODE_ALL, markAsReadIds, unwatchIds, watchId);
    }

}