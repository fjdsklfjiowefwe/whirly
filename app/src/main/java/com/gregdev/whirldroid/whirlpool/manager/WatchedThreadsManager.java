package com.gregdev.whirldroid.whirlpool.manager;

import android.content.Context;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.model.Thread;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class WatchedThreadsManager extends Manager<Thread> {

    public WatchedThreadsManager(Context context) {
        super(context);

        maxAge = 10;
        items  = new ArrayList<>();
    }

    public void download(int mode, String markAsReadIds, String unwatchIds, String watchId) throws WhirlpoolApiException {
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

        if (watchId != "") {
            params.put("watchedadd", watchId + "");
        }

        WhirlpoolApiFactory.getFactory().getApi(context).downloadData(get, params);
    }

    public void markRead(String ids) {
        List<String> threadIds = Arrays.asList(ids.split(","));

        for (Thread thread : items) {
            if (threadIds.contains(thread.getId())) {
                thread.setUnread(0);
            }
        }
    }

    public void unwatch(String ids) {
        List<String> threadIds = Arrays.asList(ids.split(","));
        List<Thread> newItems  = new ArrayList<>();

        for (Thread thread : items) {
            if (!threadIds.contains(thread.getId())) {
                newItems.add(thread);
            }
        }

        setItems(newItems);
    }

}