package com.gregdev.whirldroid.whirlpool.manager;

import android.content.Context;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.model.NewsArticle;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiFactory;

import java.util.ArrayList;
import java.util.List;

public class NewsManager extends Manager<NewsArticle> {

    public NewsManager(Context context) {
        super(context);

        cacheFileName   = "cache_news.txt";
        maxAge          = 60;
        items           = new ArrayList<>();
    }

    public void download() throws WhirlpoolApiException {
        List<String> get = new ArrayList<>();
        get.add("news");

        WhirlpoolApiFactory.getFactory().getApi(context).downloadData(get, null);
    }

}