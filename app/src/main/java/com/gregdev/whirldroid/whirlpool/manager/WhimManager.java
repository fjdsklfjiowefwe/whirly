package com.gregdev.whirldroid.whirlpool.manager;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.model.Whim;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WhimManager extends Manager<Whim> {

    public WhimManager() {
        cacheFileName   = "cache_whims.txt";
        maxAge          = 10;
        items           = new ArrayList<>();
    }

    public void download() throws WhirlpoolApiException {
        download(0);
    }

    public void download(int markAsRead) throws WhirlpoolApiException {
        List<String> get = new ArrayList<>();
        Map<String, String> params = new HashMap<>();

        if (markAsRead != 0) {
            get.add("whim");
            params.put("whimid", markAsRead + "");
        }

        get.add("whims");

        Whirldroid.getApi().downloadData(get, params);
    }

}