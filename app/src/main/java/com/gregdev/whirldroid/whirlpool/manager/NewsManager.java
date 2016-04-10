package com.gregdev.whirldroid.whirlpool.manager;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.model.NewsArticle;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;

import java.util.ArrayList;
import java.util.List;

public class NewsManager extends Manager<NewsArticle> {

    public NewsManager() {
        cacheFileName   = "cache_news.txt";
        maxAge          = 60;
        items           = new ArrayList<>();
    }

    public void download() throws WhirlpoolApiException {
        List<String> get = new ArrayList<>();
        get.add("news");

        Whirldroid.getApi().downloadData(get, null);
    }

}