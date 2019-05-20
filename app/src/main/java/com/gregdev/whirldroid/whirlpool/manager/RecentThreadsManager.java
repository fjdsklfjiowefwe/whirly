package com.gregdev.whirldroid.whirlpool.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.model.Thread;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecentThreadsManager extends Manager<Thread> {

    public RecentThreadsManager(Context context) {
        super(context);

        cacheFileName   = "cache_recent_threads.txt";
        maxAge          = 10;
        items           = new ArrayList<>();
    }

    public List<Thread> getItems() {
        super.getItems();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        boolean ignoreOwn = settings.getBoolean("pref_ignoreownrepliesrecent", false);

        List<Thread> itemsClone = new ArrayList<>();

        for (Thread t : items) {
            if (!ignoreOwn || !t.getLastPosterId().equals(Whirldroid.getOwnWhirlpoolId(context))) {
                itemsClone.add(t);
            }
        }

        items = itemsClone;

        return items;
    }

    public void download() throws WhirlpoolApiException {
        List<String> get = new ArrayList<>();
        get.add("recent");

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String recent_age = settings.getString("pref_recenthistory", "7");

        Map<String, String> params = new HashMap<>();
        params.put("recentdays", recent_age);

        WhirlpoolApiFactory.getFactory().getApi(context).downloadData(get, params);
    }

}