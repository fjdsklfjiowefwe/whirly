package com.gregdev.whirldroid.whirlpool.manager;

import android.content.Context;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.model.Thread;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiFactory;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PopularThreadsManager extends Manager<Thread> {

    public PopularThreadsManager(Context context) {
        super(context);

        cacheFileName   = "cache_popular_threads.txt";
        maxAge          = 10;
        items           = new ArrayList<>();
    }

    public void download() throws WhirlpoolApiException {
        ArrayList<Thread> threads = new ArrayList<Thread>();

        Document doc = WhirlpoolApiFactory.getFactory().getApi(context).downloadPage(WhirlpoolApi.POPULAR_URL);
        if (doc == null) {
            return;
        }

        Elements trs = doc.select("tr");

        String currentForum = null;
        int currentForumId = 0;

        for (Element tr : trs) {
            Set<String> trClasses = tr.classNames();

            // section - contains a forum name
            if (trClasses.contains("section")) {
                currentForum = tr.text();

                // get the forum ID
                String forumUrl         = tr.select("a").attr("href");
                Pattern forumIdRegex    = Pattern.compile("/forum/([0-9]+)");
                Matcher matcher         = forumIdRegex.matcher(forumUrl);

                while (matcher.find()) {
                    currentForumId = Integer.parseInt(matcher.group(1));
                }
            }
            // thread
            else {
                if (currentForum == null) continue;

                Thread t = WhirlpoolApiFactory.getFactory().getApi(context).getThreadFromTableRow(tr, currentForum, currentForumId);
                threads.add(t);
            }
        }

        setItems(threads);
    }

}