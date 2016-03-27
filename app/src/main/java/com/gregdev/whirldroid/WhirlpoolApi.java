package com.gregdev.whirldroid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.gregdev.whirldroid.model.Forum;
import com.gregdev.whirldroid.model.NewsArticle;
import com.gregdev.whirldroid.model.Post;
import com.gregdev.whirldroid.model.Thread;
import com.gregdev.whirldroid.model.User;
import com.gregdev.whirldroid.model.Whim;
import com.gregdev.whirldroid.service.HttpFetch;

/**
 * Whirlpool API - dowloads, caches and retrieves all data from Whirlpool.
 *
 * Data is downloaded using either the public Whirlpool API or by directly
 * scraping a page from the Whirlpool site.
 *
 * Tread lightly, here be dragons.
 *
 * @author greg
 */
@SuppressLint("UseSparseArrays") // there's no nice way to iterate through a SparseArray, so they annoy me.
public class WhirlpoolApi extends Activity {

    // minimum time in seconds between manual refreshes of data,
    // to avoid hitting the Whirlpool servers too often
    public static final int REFRESH_INTERVAL = 10;

    // Whirldroid-specific forum IDs (so a single activity can be used to display all thread listings)
    public static final int WATCHED_THREADS = -1;
    public static final int RECENT_THREADS  = -2;
    public static final int POPULAR_THREADS = -3;
    public static final int SEARCH_RESULTS  = -4;

    private HttpFetch http;
    private SharedPreferences settings;

    // store the data
    private ArrayList<NewsArticle> news_articles;
    private ArrayList<Whim>        whims;
    private ArrayList<Thread>      watched_threads;
    private ArrayList<Thread>      recent_threads;
    private ArrayList<Thread>      popular_threads;
    private ArrayList<Forum>       forums;
    private ArrayList<Thread>      forum_threads;

    // cache file names
    private static final String NEWS_CACHE_FILE    = "cache_news.txt";
    private static final String WHIM_CACHE_FILE    = "cache_whims.txt";
    private static final String RECENT_CACHE_FILE  = "cache_recent_threads.txt";
    private static final String WATCHED_CACHE_FILE = "cache_watched_threads.txt";
    private static final String POPULAR_CACHE_FILE = "cache_popular_threads.txt";
    private static final String FORUMS_CACHE_FILE  = "cache_forums.txt";

    // keep track of the last time we downloaded each set of data
    private long last_update_news    = 0;
    private long last_update_whims   = 0;
    private long last_update_watched = 0;
    private long last_update_recent  = 0;
    private long last_update_popular = 0;
    private long last_update_forums  = 0;

    // maximum time in minutes that each type of data can be out of date by before we download it again
    private final int MAX_AGE_NEWs    = 60;
    private final int MAX_AGE_WHIMS   = 14;
    private final int MAX_AGE_RECENT  = 14;
    private final int MAX_AGE_WATCHED = 14;
    private final int MAX_AGE_POPULAR = 14;
    private final int MAX_AGE_FORUMS  = 10080;

    // these forum IDs are public, and we can scrape the data from them
    private static int[] PUBLIC_FORUMS = {92, 100, 142, 82, 9, 107, 135, 80, 136, 125, 116, 63,
            127, 139, 7, 129, 130, 131, 10, 38, 39, 91, 87, 112, 132, 8, 83, 138, 143, 133, 58, 106,
            126, 71, 118, 137, 114, 123, 128, 141, 140, 144, 18, 14, 15, 68, 72, 94, 90, 102, 105,
            109, 108, 147, 31, 67, 5, 148, 149, 150};

    // some URLs - most of these shouldn't be hardcoded, but meh, half the app
    // will break if anything is changed on the Whirlpool side anyway
    private static final String POPULAR_URL   = "http://forums.whirlpool.net.au/forum/?action=popular_views";
    public static final  String FORUM_URL     = "http://forums.whirlpool.net.au/forum/";
    public static final  String THREAD_URL    = "http://forums.whirlpool.net.au/forum-replies.cfm?t=";
    public static final  String REPLY_URL     = "http://forums.whirlpool.net.au/forum/index.cfm?action=reply&t=";
    public static final  String NEWTHREAD_URL = "http://forums.whirlpool.net.au/forum/index.cfm?action=newthread&f=";

    // number of posts Whirlpool displays on each page
    public static final int POSTS_PER_PAGE = 20;

    public WhirlpoolApi() {
        settings = PreferenceManager.getDefaultSharedPreferences(Whirldroid.getContext());

        http = new HttpFetch();

        news_articles   = new ArrayList<NewsArticle>();
        whims           = new ArrayList<Whim>();
        watched_threads = new ArrayList<Thread>();
        recent_threads  = new ArrayList<Thread>();
        popular_threads = new ArrayList<Thread>();
        forums          = new ArrayList<Forum>();
    }

    public String getApiKey() {
        String api_key = settings.getString("pref_apikey", null);

        if (api_key != null) {
            api_key.trim().replace(' ', '-');
        }

        return api_key;
    }

    /**
     * Checks if a forum is public (ie. it can be scraped)
     * @param forum_id
     * @return True if forum is public
     */
    public static boolean isPublicForum(int forum_id) {
        for (int i = 0; i < PUBLIC_FORUMS.length; i++) {
            if (PUBLIC_FORUMS[i] == forum_id) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<NewsArticle> getNewsArticles() {
        if (news_articles == null || news_articles.isEmpty()) { // no data in memory, get from cache file
            news_articles = (ArrayList<NewsArticle>) readFromCacheFile(NEWS_CACHE_FILE);
        }

        return news_articles;
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Whim> getWhims() {
        if (whims == null || whims.isEmpty()) { // no data in memory, get from cache file
            whims = (ArrayList<Whim>) readFromCacheFile(WHIM_CACHE_FILE);
        }

        return whims;
    }

    @SuppressWarnings("unchecked")
    public Forum getThreads(int forum_id, int page_number, int group_id) {
        Forum forum = null;

        switch (forum_id) {
            case RECENT_THREADS:
                if (recent_threads == null || recent_threads.isEmpty()) { // no data in memory, get from cache file
                    recent_threads = (ArrayList<Thread>) readFromCacheFile(RECENT_CACHE_FILE);
                }

                forum = new Forum(forum_id, "Recent Threads", 0, null);
                forum.setThreads(recent_threads);
                return forum;

            case WATCHED_THREADS:
                if (watched_threads == null || watched_threads.isEmpty()) { // no data in memory, get from cache file
                    watched_threads = (ArrayList<Thread>) readFromCacheFile(WATCHED_CACHE_FILE);
                }

                forum = new Forum(forum_id, "Recent Threads", 0, null);
                forum.setThreads(watched_threads);
                return forum;

            case POPULAR_THREADS:
                if (popular_threads == null || popular_threads.isEmpty()) { // no data in memory, get from cache file
                    popular_threads = (ArrayList<Thread>) readFromCacheFile(POPULAR_CACHE_FILE);
                }

                forum = new Forum(forum_id, "Recent Threads", 0, null);
                forum.setThreads(popular_threads);
                return forum;

            default:
                if (isPublicForum(forum_id)) { // we can scrape this forum
                    return scrapeThreadsFromForum(forum_id, page_number, group_id);
                }
                else { // can't scrape, use the API instead
                    return downloadThreadsFromForum(forum_id);
                }
        }
    }

    @SuppressWarnings("unchecked")
    public ArrayList<Forum> getForums() {
        if (forums == null || forums.isEmpty()) { // no data in memory, get from cache file
            forums = (ArrayList<Forum>) readFromCacheFile(FORUMS_CACHE_FILE);
        }

        return forums;
    }

    private boolean needToDownload(String cache_file, ArrayList<?> cache_data, long last_update_time, int max_age) {
        long cache_file_age = getCacheFileAge(cache_file);
        if (cache_file_age != -1) { // cache file exists
            if (cache_file_age < 60 * max_age) { // file not too old
                return false;
            }
        }

        if (cache_data != null && cache_data.size() > 0) { // data in memory
            if ((System.currentTimeMillis() / 1000) - last_update_time < 60 * max_age) { // memory data not too old
                return false;
            }
        }

        // if we get here, then we have don't useable cache data
        return true;
    }

    public boolean needToDownloadNews() {
        return needToDownload(NEWS_CACHE_FILE, news_articles, last_update_news, MAX_AGE_NEWs);
    }

    public boolean needToDownloadWhims() {
        return needToDownload(WHIM_CACHE_FILE, whims, last_update_whims, MAX_AGE_WHIMS);
    }

    public boolean needToDownloadThreads(int forum_id) {
        String cache_file = null;
        ArrayList<?> threads = null;
        long last_update = 0;
        int max_age = 0;

        switch (forum_id) {
            case RECENT_THREADS:
                cache_file = RECENT_CACHE_FILE;
                threads = recent_threads;
                last_update = last_update_recent;
                max_age = MAX_AGE_RECENT;
                break;
            case WATCHED_THREADS:
                cache_file = WATCHED_CACHE_FILE;
                threads = watched_threads;
                last_update = last_update_watched;
                max_age = MAX_AGE_WATCHED;
                break;
            case POPULAR_THREADS:
                cache_file = POPULAR_CACHE_FILE;
                threads = popular_threads;
                last_update = last_update_popular;
                max_age = MAX_AGE_POPULAR;
                break;
            default:
                return true; // don't cache forum threads
        }
        return needToDownload(cache_file, threads, last_update, max_age);
    }

    public boolean needToDownloadForums() {
        return needToDownload(FORUMS_CACHE_FILE, forums, last_update_forums, MAX_AGE_FORUMS);
    }

    public long getNewsLastUpdated() {
        if (last_update_news != 0) {
            return last_update_news;
        }
        return Whirldroid.getContext().getFileStreamPath(NEWS_CACHE_FILE).lastModified() / 1000;
    }

    public long getWhimsLastUpdated() {
        if (last_update_whims != 0) {
            return last_update_whims;
        }
        return Whirldroid.getContext().getFileStreamPath(WHIM_CACHE_FILE).lastModified() / 1000;
    }

    public long getRecentLastUpdated() {
        if (last_update_recent != 0) {
            return last_update_recent;
        }
        return Whirldroid.getContext().getFileStreamPath(RECENT_CACHE_FILE).lastModified() / 1000;
    }

    public long getWatchedLastUpdated() {
        if (last_update_watched != 0) {
            return last_update_watched;
        }
        return Whirldroid.getContext().getFileStreamPath(WATCHED_CACHE_FILE).lastModified() / 1000;
    }

    public long getPopularLastUpdated() {
        if (last_update_popular != 0) {
            return last_update_popular;
        }
        return Whirldroid.getContext().getFileStreamPath(POPULAR_CACHE_FILE).lastModified() / 1000;
    }

    public long getForumsLastUpdated() {
        if (last_update_forums != 0) {
            return last_update_forums;
        }
        return Whirldroid.getContext().getFileStreamPath(FORUMS_CACHE_FILE).lastModified() / 1000;
    }

    private Thread getThreadFromTableRow(Element tr, String forum, int forum_id) {
        int id = 0;
        String title = "";
        String last_poster = "";
        String last_post_date = "";
        String first_poster = "";
        String first_post_date = "";
        int page_count = 1;

        Elements tds = tr.children();
        // title reps reads oldest newest
        for (Element td : tds) {
            Set<String> td_classes = td.classNames();

            if (td_classes.contains("title")) {

                String url = "";

                try {
                    for (Element child : td.children()) {
                        if (child.hasClass("title")) {
                            title = child.text();
                            url = child.attr("href");
                        }
                    }

                } catch (Exception e) {
                    title = td.text();
                    url = td.select("a").get(0).attr("href");
                }

                Pattern thread_id_regex = Pattern.compile("(t=([0-9]+))|(/archive/([0-9]+))");
                Matcher m = thread_id_regex.matcher(url);
                while (m.find()) {
                    try {
                        id = Integer.parseInt(m.group(2));
                    }
                    catch (NumberFormatException e) {
                        id = Integer.parseInt(m.group(4));
                    }
                }

                // get thread page count
                try {
                    Element page_element = td.select("script").get(0);

                    Pattern page_count_regex = Pattern.compile("([0-9]+),([0-9]+)");
                    Matcher page_matcher = page_count_regex.matcher(page_element.html());
                    while (page_matcher.find()) {
                        page_count = Integer.parseInt(page_matcher.group(2));
                    }

                    //page_count = Integer.parseInt(page_element.text().replace(" ", ""));
                }
                catch (NullPointerException e) {
                    // no page list, must only be 1 page
                }
                catch (IndexOutOfBoundsException e) {
                    // no page list
                }
                catch (NumberFormatException e) {
                    // not a number, probably a deleted thread; ignore
                }
            }

            else if (td_classes.contains("newest")) {
                try {
                    last_poster = td.child(0).child(0).text();
                    last_post_date = td.child(0).ownText();
                } catch (Exception e) { }
            }

            else if (td_classes.contains("oldest")) {
                try {
                    first_poster = td.child(0).text();
                    first_post_date = td.ownText();
                } catch (Exception e) {}
            }
        }

        if (last_poster.equals("")) { // no replies yet
            // set the first poster as the last poster (since there's only 1 post)
            last_poster = first_poster;
            last_post_date = first_post_date;
        }

        Thread t = new Thread(id, title, null, last_poster, forum, forum_id);
        t.setLastDateText(last_post_date);
        t.setPageCount(page_count);

        if (title == "") {
            return null;
        }

        return t;
    }

    private Forum scrapeThreadsFromForum(int forum_id, int page_number, int group_id) {
        ArrayList<Thread> threads = new ArrayList<Thread>();

        int page_count = -1;

        String forum_url = FORUM_URL + forum_id + "?p=" + page_number;

        if (group_id != 0) {
            forum_url += "&g=" + group_id;
        }

        Document doc = downloadPage(forum_url);
        if (doc == null) {
            return null;
        }

        // get the number of pages in this forum
        // see if there's a select box with page options
        try {
            Element p = doc.select("select[name=p] option").last(); // get the last option
            page_count = Integer.parseInt(p.text());
        }
        catch (NullPointerException e) { }

        if (page_count == -1) {
            // no select box present, get pagination list elements
            try {
                Element p = doc.select("ul.pagination li").last();
                page_count = Integer.parseInt(p.text().replace("\u00a0", "").trim());
            }
            catch (NullPointerException e) { }
        }

        // get the forum title
        String forum_title;

        try {
            forum_title = doc.select("ul#breadcrumb li").last().text();
        }
        catch (NullPointerException e) {
            forum_title = "";
        }

        // get the groups in this forum
        Map<String, Integer> groups = null;
        try {
            groups = new LinkedHashMap<String, Integer>();

            Elements group_options = doc.select("select[name=g] option");

            for (Element group_option : group_options) {
                if (!group_option.attr("value").equals("0")) {
                    groups.put(group_option.text(), Integer.parseInt(group_option.attr("value")));
                }
            }
        }
        catch (NullPointerException e) {
            // no groups in this forum, do nothing
        }

        Elements trs = doc.select("tr");

        for (Element tr : trs) {
            Set<String> tr_classes = tr.classNames();

            Thread t = getThreadFromTableRow(tr, null, forum_id);

            if (tr_classes.contains("sticky")) {
                t.setSticky(true);
            }
            if (tr_classes.contains("closed")) {
                t.setClosed(true);
            }
            if (tr_classes.contains("deleted")) {
                t.setDeleted(true);
            }
            if (tr_classes.contains("pointer")) {
                t.setMoved(true);
            }

            if (t != null) {
                threads.add(t);
            }
        }

        Forum forum = new Forum(forum_id, forum_title, 0, null);
        forum.setPageCount(page_count);
        forum.setGroups(groups);
        forum.setThreads(threads);

        return forum;
    }

    public Map<String, String> scrapeUserInfo(String user_id) {
        Map<String, String> user_info = new LinkedHashMap<String, String>();

        String url = "http://forums.whirlpool.net.au/user/" + user_id;

        Document doc = downloadPage(url);
        if (doc == null) {
            return null;
        }

        Elements divs = doc.select("#userprofile > div");

        Element info_div = divs.get(1);
        Elements table_rows = info_div.select("tr");

        for (Element row : table_rows) {
            Elements columns = row.children();

            String column_key = columns.get(0).text();
            String column_value = columns.get(1).text();

            if (!column_key.equals("Whim:")) {
                user_info.put(column_key, column_value);
            }
        }

        return user_info;
    }

    public Forum downloadThreadsFromForum(int forum_id) {
        List<String> get = new ArrayList<String>();
        get.add("threads");
        Map<String, String> params = new HashMap<String, String>();
        params.put("forumids", "" + forum_id);
        params.put("threadcount", "50");

        try {
            downloadData(get, params);
        }
        catch (WhirlpoolApiException e) {
            return null;
        }

        Forum forum = new Forum(forum_id, "", 0, null);
        forum.setThreads(forum_threads);
        return forum;
    }

    public static String buildSearchUrl(int forum_id, int group_id, String query) {
        URI uri;
        try {
            uri = new URI("http", "forums.whirlpool.net.au", "/forum/", "action=threads_search&f=" + forum_id + "&fg=" + group_id + "&q=" + query, null);
        }
        catch (URISyntaxException e) {
            return null;
        }
        return uri.toASCIIString();
    }

    public List<Thread> searchThreads(int forum_id, int group_id, String query) {
        String search_url = buildSearchUrl(forum_id, group_id, query);

        if (search_url == null) {
            return null;
        }

        List<Thread> threads = new ArrayList<Thread>();

        Document doc = downloadPage(search_url);
        if (doc == null) {
            return null;
        }

        Elements trs = doc.select("tr");

        String current_forum = null;
        int current_forum_id = 0;

        for (Element tr : trs) {
            Set<String> tr_classes = tr.classNames();

            // section - contains a forum name
            if (tr_classes.contains("section")) {
                current_forum = tr.text();

                // get the forum ID
                String forum_url = tr.select("a").attr("href");
                Pattern forum_id_regex = Pattern.compile("/forum/([0-9]+)");
                Matcher m = forum_id_regex.matcher(forum_url);
                while (m.find()) {
                    current_forum_id = Integer.parseInt(m.group(1));
                }
            }
            // thread
            else {
                if (current_forum == null) continue;

                Thread t = getThreadFromTableRow(tr, current_forum, current_forum_id);
                threads.add(t);
            }
        }

        return threads;
    }

    public boolean downloadPopularThreads() {
        ArrayList<Thread> threads = new ArrayList<Thread>();

        Document doc = downloadPage(POPULAR_URL);
        if (doc == null) {
            return false;
        }

        Elements trs = doc.select("tr");

        String current_forum = null;
        int current_forum_id = 0;

        for (Element tr : trs) {
            Set<String> tr_classes = tr.classNames();

            // section - contains a forum name
            if (tr_classes.contains("section")) {
                current_forum = tr.text();

                // get the forum ID
                String forum_url = tr.select("a").attr("href");
                Pattern forum_id_regex = Pattern.compile("/forum/([0-9]+)");
                Matcher m = forum_id_regex.matcher(forum_url);
                while (m.find()) {
                    current_forum_id = Integer.parseInt(m.group(1));
                }
            }
            // thread
            else {
                if (current_forum == null) continue;

                Thread t = getThreadFromTableRow(tr, current_forum, current_forum_id);
                threads.add(t);
            }
        }

        setPopularThreads(threads);

        return true;
    }

    public Thread downloadThread(int thread_id, String thread_title) throws WhirlpoolApiException {
        return downloadThread(thread_id, thread_title, 1);
    }

    public Thread downloadThread(int thread_id, String thread_title, int page) throws WhirlpoolApiException {

        ArrayList<Post> posts = new ArrayList<Post>();

        String thread_url = THREAD_URL + thread_id;
        if (page != 1) {
            thread_url += "&p=" + page;
        }

        Document doc = downloadPage(thread_url);
        if (doc == null) {
            throw new WhirlpoolApiException("Error downloading data");
        }

        // check for an error message
        Elements alert = doc.select("#alert");
        if (alert != null && alert.size() > 0) {
            throw new WhirlpoolApiException("Private forum");
        }

        if (thread_title == null) { // no thread title was passed
            // scrape the title from the page
            Elements breadcrumb_elements = doc.select("#breadcrumb li");
            thread_title = breadcrumb_elements.last().text();
        }

        // get page count
        Elements pagination_elements = doc.select("#top_pagination li");
        int page_count = pagination_elements.size() - 2; // list elements, subtract first date and last date elements
        if (page_count <= 0) page_count = 1;

        // get notebar (thread header that mods put there sometimes)
        String notebar = null;
        try {
            notebar = doc.select(".notebar").get(0).html();
        }
        catch (IndexOutOfBoundsException e) {
            // no notebar
        }

        Elements replies = doc.select("#replylist > div");

        for (Element reply : replies) {
            String id = "";
            String user_id = "";
            String user_name = "";
            String posted_time = "";
            String content = "";
            boolean edited = false;
            boolean op = false;
            boolean deleted = false;

            // get reply ID
            id = reply.attr("id").replace("r", "");

            // get author name
            Element user_name_element;
            try {
                user_name_element = reply.select(".bu_name").get(0);
            }
            catch (IndexOutOfBoundsException e) {
                // username not found, probably a deleted post
                user_name_element = reply.select(".replyuser > a > b").get(0);
            }
            user_name = user_name_element.text();

            // get author ID
            Element user_id_element = reply.select(".userid").get(0);
            user_id = user_id_element.text().replace("User #", "");

            // check if this author is the OP
            Elements op_element = reply.select(".op");
            if (!op_element.isEmpty()) { // user is the OP
                op = true;
                op_element.get(0).remove(); // remove element so the text doesn't show up
            }

            // check if this post has been edited
            Elements edited_elements = reply.select(".edited");
            if (!edited_elements.isEmpty()) {
                edited = true;

                // remove elements so text doesn't show up
                for (Element edited_element : edited_elements) {
                    edited_element.remove();
                }
            }

            // get the poster's user group
            String user_group = "";
            try {
                user_group = reply.select(".usergroup").get(0).text();
            }
            catch (IndexOutOfBoundsException e) {
                // no usergroup, probably a deleted post
            }

            // get posted time
            try {
                Element date_element = reply.select(".date").get(0);
                posted_time = date_element.ownText();
            }
            catch (IndexOutOfBoundsException e) {
                // no post date, probably a deleted post
                deleted = true;
            }

            // get the reply content
            Element content_element = reply.select(".replytext").get(0);
            content = content_element.html();

            User user = new User(user_id, user_name);
            user.setGroup(user_group);

            Post post = new Post(id, user, posted_time, content, edited, op);

            post.setDeleted(deleted);

            posts.add(post);
        }

        Thread thread = new Thread(thread_id, thread_title, null, "", "", 0);
        thread.setPageCount(page_count);
        thread.setNotebar(notebar);
        thread.setPosts(posts);

        return thread;
    }

    /**
     * Downloads data from Whirlpool and adds the resulting threads/news/whims/etc
     * to the database.
     *
     * @param get    Get parameters (url, forum, news, watched, user, recent,
     *               contacts, threads, whims, whim
     * @param params Key,Value pairs (watchmode, userid, recentdays, forumids, threadcount, whimid)
     * @throws WhirlpoolApiException
     */
    public boolean downloadData(List<String> get, Map<String, String> params) throws WhirlpoolApiException {
        // URL to fetch data from
        String url = "https://whirlpool.net.au/api/?key=" + getApiKey() + "&output=json&get=";

        // add each parameter to the URL data is fetched from
        for (int i = 0; i < get.size(); i++) {
            if (i > 0) url += "+"; // if this isn't the first param, add a plus sign separator
            url += get.get(i); // append parameter to URL
        }

        // add each key/value pair to the URL
        if (params != null) {
            for (Map.Entry<String, String> param : params.entrySet()) {
                url += "&" + param.getKey() + "=" + param.getValue();
            }
        }

        // fetch the data
        String data = null;
        try {
            data = http.getDatafromURL(url);
        }
        catch (WhirlpoolApiException e) {
            throw new WhirlpoolApiException(e.getMessage());
        }

        // no data downloaded
        if (data == null) {
            return false;
        }

        JSONObject json = null;

        try {
            json = new JSONObject(data);
        }
        catch (JSONException e) {
            return false;
        }

        /** Extract Forums **/
        try {
            JSONArray json_forums = json.getJSONArray("FORUM");
            setForums(getForumsFromJson(json_forums));
        }
        catch (JSONException e) {
            // no forums in fetched data
        }

        /** Extract News Articles **/
        try {
            JSONArray json_news = json.getJSONArray("NEWS");
            setNewsArticles(getNewsFromJson(json_news));
        }
        catch (JSONException e) {
            // no news in fetched data
        }

        JSONArray json_whim = null;
        /** Extract Single Whims (for marking as read) **/
        try {
            json_whim = json.getJSONArray("WHIM");
        }
        catch (JSONException e) {
            // no single whim in fetched data
        }

        /** Extract Whims **/
        try {
            JSONArray json_whims = json.getJSONArray("WHIMS");
            setWhims(getWhimsFromJson(json_whims, json_whim));
        }
        catch (JSONException e) {
            // no whims in fetched data
        }

        /** Extract Watched Threads **/
        try {
            JSONArray json_watched = json.getJSONArray("WATCHED");
            setWatchedThreads(getThreadsFromJson(json_watched));
        }
        catch (JSONException e) {
            // no watched threads in fetched data
        }

        /** Extract Recent Threads **/
        try {
            JSONArray json_recent = json.getJSONArray("RECENT");
            setRecentThreads(getThreadsFromJson(json_recent));
        }
        catch (JSONException e) {
            // no recent threads in fetched data
        }

        /** Extract Forum Threads **/
        try {
            JSONArray json_threads = json.getJSONArray("THREADS");
            forum_threads = getThreadsFromJson(json_threads);
        }
        catch (JSONException e) {
            // no forum threads in fetched data
        }

        return true;
    }

    public void downloadNews() throws WhirlpoolApiException {
        List<String> get = new ArrayList<String>();
        get.add("news");

        downloadData(get, null);
    }

    public void downloadForums() throws WhirlpoolApiException {
        List<String> get = new ArrayList<String>();
        get.add("forum");

        downloadData(get, null);
    }

    public void downloadWhims(String mark_as_read) throws WhirlpoolApiException {
        List<String> get = new ArrayList<String>();
        Map<String, String> params = new HashMap<String, String>();

        if (mark_as_read != null) {
            get.add("whim");
            params.put("whimid", mark_as_read);
        }

        get.add("whims");

        downloadData(get, params);
    }

    public void downloadRecent() throws WhirlpoolApiException {
        List<String> get = new ArrayList<String>();
        get.add("recent");

        String recent_age = settings.getString("pref_recenthistory", "7");

        Map<String, String> params = new HashMap<String, String>();
        params.put("recentdays", recent_age);

        downloadData(get, params);
    }

    public void downloadWatched(int mark_thread_as_read, int unwatch_thread, int watch_thread) throws WhirlpoolApiException {
        List<String> get = new ArrayList<String>();
        Map<String, String> params = new HashMap<String, String>();

        get.add("watched");
        params.put("watchedmode", "1");

        if (mark_thread_as_read != 0) {
            params.put("watchedread", mark_thread_as_read + "");
        }

        if (unwatch_thread != 0) {
            params.put("watchedremove", unwatch_thread + "");
        }

        if (watch_thread != 0) {
            params.put("watchedadd", watch_thread + "");
        }

        downloadData(get, params);
    }

    private ArrayList<NewsArticle> getNewsFromJson(JSONArray json_news) throws JSONException {
        ArrayList<NewsArticle> articles = new ArrayList<NewsArticle>();

        for (int i = 0; i < json_news.length(); i++) {
            JSONObject e = json_news.getJSONObject(i);

            String id = e.getString("ID");
            String title = e.getString("TITLE");
            String source = e.getString("SOURCE");
            String blurb = e.getString("BLURB");
            String date_time = e.getString("DATE");

            Date date = Whirldroid.getLocalDateFromString(date_time);

            articles.add(new NewsArticle(id, title, source, blurb, date));
        }

        return articles;
    }

    private ArrayList<Whim> getWhimsFromJson(JSONArray json_whims, JSONArray json_whim) throws JSONException {
        // check if we have an individual whim (we'll assume just one)
        String single_whim_id = null;
        if (json_whim != null) {
            JSONObject single_whim = json_whim.getJSONObject(0);
            single_whim_id = single_whim.getString("ID");
        }

        ArrayList<Whim> whims = new ArrayList<Whim>();

        for (int i = 0; i < json_whims.length(); i++) {
            JSONObject e = json_whims.getJSONObject(i);
            JSONObject from = e.getJSONObject("FROM");

            String id = e.getString("ID");
            String from_id = from.getString("ID");
            String from_name = from.getString("NAME");
            int viewed = e.getInt("VIEWED");
            int replied = e.getInt("REPLIED");
            String date_time = e.getString("DATE");
            String content = e.getString("MESSAGE").replace('\r', ' ');

            if (single_whim_id != null && id.equals(single_whim_id)) {
                viewed = 1;
            }

            Date date = Whirldroid.getLocalDateFromString(date_time);

            whims.add(new Whim(id, from_id, from_name, viewed, replied, date, content));
        }

        return whims;
    }

    private ArrayList<Thread> getThreadsFromJson(JSONArray json_threads) throws JSONException {
        ArrayList<Thread> threads = new ArrayList<Thread>();

        for (int i = 0; i < json_threads.length(); i++) {
            JSONObject e = json_threads.getJSONObject(i);
            JSONObject last = e.getJSONObject("LAST");

            int id = e.getInt("ID");
            String title = e.getString("TITLE");
            String dateTime = e.getString("LAST_DATE");
            String last_poster = last.getString("NAME");
            String last_poster_id = last.getString("ID");
            String forum = e.getString("FORUM_NAME");
            int forum_id = e.getInt("FORUM_ID");
            int reply_count = e.getInt("REPLIES");

            int unread = 0;
            int last_page = 0;
            int last_post = 0;
            try {
                unread = e.getInt("UNREAD");
                last_page = e.getInt("LASTPAGE");
                last_post = e.getInt("LASTREAD");
            }
            catch (JSONException ex) {
                // thread wasn't a watched thread, okay to continue
            }

            Date last_date = Whirldroid.getLocalDateFromString(dateTime);

            Thread thread = new Thread(id, title, last_date, last_poster, forum, forum_id, unread, last_page, last_post);

            thread.setLastPosterId(last_poster_id);

            int page_count = (reply_count + 1) / POSTS_PER_PAGE;
            if ((reply_count + 1) % POSTS_PER_PAGE != 0) {
                page_count = page_count + 1;
            }
            thread.setPageCount(page_count);

            threads.add(thread);
        }

        return threads;
    }

    private ArrayList<Forum> getForumsFromJson(JSONArray json_forums) throws JSONException {
        ArrayList<Forum> forums = new ArrayList<Forum>();

        for (int i = 0; i < json_forums.length(); i++) {
            JSONObject e = json_forums.getJSONObject(i);
            int id = e.getInt("ID");
            String title = e.getString("TITLE");
            int sort = e.getInt("SORT");
            String section = e.getString("SECTION");

            forums.add(new Forum(id, title, sort, section));
        }

        return forums;
    }

    private void setNewsArticles(ArrayList<NewsArticle> news_articles) {
        last_update_news = System.currentTimeMillis() / 1000;
        writeToCacheFile(NEWS_CACHE_FILE, news_articles);
        this.news_articles = news_articles;
    }

    private void setWhims(ArrayList<Whim> whims) {
        last_update_whims = System.currentTimeMillis() / 1000;
        writeToCacheFile(WHIM_CACHE_FILE, whims);
        this.whims = whims;
    }

    private void setRecentThreads(ArrayList<Thread> recent_threads) {
        ArrayList<Thread> new_recent_threads = new ArrayList<Thread>();

        boolean ignore_own = settings.getBoolean("pref_ignoreownrepliesrecent", false);

        if (ignore_own) {
            for (Thread t : recent_threads) {
                if (!t.getLastPosterId().equals(Whirldroid.getOwnWhirlpoolId())) {
                    new_recent_threads.add(t);
                }
            }

            recent_threads = new_recent_threads;
        }

        last_update_recent = System.currentTimeMillis() / 1000;
        writeToCacheFile(RECENT_CACHE_FILE, recent_threads);
        this.recent_threads = recent_threads;
    }

    private void setWatchedThreads(ArrayList<Thread> watched_threads) {
        last_update_watched = System.currentTimeMillis() / 1000;

        writeToCacheFile(WATCHED_CACHE_FILE, watched_threads);
        this.watched_threads = watched_threads;
    }

    private void setPopularThreads(ArrayList<Thread> popular_threads) {
        last_update_popular = System.currentTimeMillis() / 1000;
        writeToCacheFile(POPULAR_CACHE_FILE, popular_threads);
        this.popular_threads = popular_threads;
    }

    private void setForums(ArrayList<Forum> forums) {
        last_update_forums = System.currentTimeMillis() / 1000;
        writeToCacheFile(FORUMS_CACHE_FILE, forums);
        this.forums = forums;
    }

    /**
     * Returns the age of the cache file in milliseconds
     * @param cache_file Cache file to check
     * @return file age in seconds
     */
    private long getCacheFileAge(File cache_file) {
        long now = System.currentTimeMillis();
        long file_last_modified = cache_file.lastModified();

        if (file_last_modified == 0) return -1; // cache file doesn't exist

        long diff = (now - file_last_modified); // diff in milliseconds

        return diff / 1000;
    }

    private long getCacheFileAge(String cache_file) {
        File f = Whirldroid.getContext().getFileStreamPath(cache_file);
        return this.getCacheFileAge(f);
    }

    private void writeToCacheFile(String cache_file, ArrayList<?> data) {
        FileOutputStream fos = null;
        ObjectOutputStream out = null;
        try {
            fos = Whirldroid.getContext().openFileOutput(cache_file, MODE_PRIVATE);
            out = new ObjectOutputStream(fos);
            out.writeObject(data);
            out.close();
        }
        catch (IOException ex) {
            // error writing cache, meh
        }
    }

    private ArrayList<?> readFromCacheFile(String cache_file) {
        FileInputStream fis = null;
        ObjectInputStream in = null;
        ArrayList<?> data = null;

        try {
            fis = Whirldroid.getContext().openFileInput(cache_file);
            in = new ObjectInputStream(fis);
            data = (ArrayList<?>) in.readObject();
            in.close();
        }
        catch (Exception e) {

        }

        return data;
    }

    private Document downloadPage(String url) {
        int connection_attempts = 3;

        while (connection_attempts > 0) {
            try {
                return Jsoup.connect(url).timeout(7000).get();
            }
            catch (IOException e) {
                // error connecting, try again
            }

            connection_attempts--;
        }

        return null;
    }

}
