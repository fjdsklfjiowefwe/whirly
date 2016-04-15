package com.gregdev.whirldroid.whirlpool.manager;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.model.Thread;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class WatchedThreadsManager extends Manager<Thread> {

    public WatchedThreadsManager() {
        maxAge = 10;
        items  = new ArrayList<>();
    }

    public void download(int mode, String markAsReadIds, String unwatchIds, int watchId) throws WhirlpoolApiException {
        List<String> get = new ArrayList<>();
        Map<String, String> params = new HashMap<>();

        get.add("watched");
        params.put("watchedmode", mode + "");

        if (markAsReadIds != null && !markAsReadIds.equals("0")) {
            params.put("watchedread", markAsReadIds);
        }

        if (unwatchIds != null && !unwatchIds.equals("0")) {
            params.put("watchedremove", unwatchIds);
        }

        if (watchId != 0) {
            params.put("watchedadd", watchId + "");
        }

        Whirldroid.getApi().downloadData(get, params);
    }

    public void markRead(String ids) {
        ArrayList<Integer> threadIds = Whirldroid.stringToInts(ids);

        for (Thread thread : items) {
            if (threadIds.contains(thread.getId())) {
                thread.setUnread(0);
            }
        }
    }

    public void unwatch(String ids) {
        ArrayList<Integer> threadIds = Whirldroid.stringToInts(ids);
        ArrayList<Thread>  newItems  = new ArrayList<>();

        for (Thread thread : items) {
            if (!threadIds.contains(thread.getId())) {
                newItems.add(thread);
            }
        }

        setItems(newItems);
    }

}