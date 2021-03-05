package com.gregdev.whirldroid.whirlpool.manager;

import android.content.Context;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.model.Forum;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForumManager extends Manager<Forum> {

    public ForumManager(Context context) {
        super(context);

        cacheFileName   = "cache_forums.txt";
        maxAge          = 10080;
        items           = new ArrayList<>();
    }

    public void download() throws WhirlpoolApiException {
        List<String> get = new ArrayList<>();

        get.add("forum");

        WhirlpoolApiFactory.getFactory().getApi(context).downloadData(get, null);
    }

}