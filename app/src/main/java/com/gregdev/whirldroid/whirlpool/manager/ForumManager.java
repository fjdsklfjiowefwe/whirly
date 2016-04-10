package com.gregdev.whirldroid.whirlpool.manager;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.model.Forum;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForumManager extends Manager<Forum> {

    public ForumManager() {
        cacheFileName   = "cache_forums.txt";
        maxAge          = 10080;
        items           = new ArrayList<>();
    }

    public void download() throws WhirlpoolApiException {
        List<String> get = new ArrayList<>();

        get.add("forum");

        Whirldroid.getApi().downloadData(get, null);
    }

}