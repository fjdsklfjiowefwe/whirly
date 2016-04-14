package com.gregdev.whirldroid.whirlpool.manager;

import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;

public class UnreadWatchedThreadsManager extends WatchedThreadsManager {

    public UnreadWatchedThreadsManager() {
        cacheFileName = "cache_unread_watched_threads.txt";
    }

    public void download() throws WhirlpoolApiException {
        download(null, null, 0);
    }

    public void download(String markAsReadIds, String unwatchIds, int watchId) throws WhirlpoolApiException {
        download(WhirlpoolApi.WATCHMODE_UNREAD, markAsReadIds, unwatchIds, watchId);
    }

}