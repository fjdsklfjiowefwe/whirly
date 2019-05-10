package com.gregdev.whirldroid.whirlpool;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Pair;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.model.Forum;
import com.gregdev.whirldroid.model.NewsArticle;
import com.gregdev.whirldroid.model.Post;
import com.gregdev.whirldroid.model.Thread;
import com.gregdev.whirldroid.model.User;
import com.gregdev.whirldroid.model.Whim;
import com.gregdev.whirldroid.service.HttpFetch;
import com.gregdev.whirldroid.whirlpool.manager.AllWatchedThreadsManager;
import com.gregdev.whirldroid.whirlpool.manager.ForumManager;
import com.gregdev.whirldroid.whirlpool.manager.NewsManager;
import com.gregdev.whirldroid.whirlpool.manager.PopularThreadsManager;
import com.gregdev.whirldroid.whirlpool.manager.RecentThreadsManager;
import com.gregdev.whirldroid.whirlpool.manager.UnreadWatchedThreadsManager;
import com.gregdev.whirldroid.whirlpool.manager.WhimManager;

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
public class WhirlpoolApi {

    // minimum time in seconds between manual refreshes of data, to avoid hitting the Whirlpool servers too often
    public static final int REFRESH_INTERVAL = 10;

    // Whirldroid-specific forum IDs (so a single activity can be used to display all thread listings)
    public static final int ALL_WATCHED_THREADS     = -5;
    public static final int UNREAD_WATCHED_THREADS  = -1;
    public static final int RECENT_THREADS          = -2;
    public static final int POPULAR_THREADS         = -3;
    public static final int SEARCH_RESULTS          = -4;

    public static final int WATCHMODE_UNREAD    = 0;
    public static final int WATCHMODE_ALL       = 1;

    private HttpFetch http;
    private SharedPreferences settings;

    // store the data
    private ArrayList<Thread> forum_threads;

    public static final int FILTER_NONE = 0;
    public static final int FILTER_ME   = 1;
    public static final int FILTER_MOD  = 2;
    public static final int FILTER_REP  = 3;

    // these forum IDs are public, and we can scrape the data from them
    // @todo don't hardcode these!
    private static int[] PUBLIC_FORUMS = {92, 100, 142, 82, 9, 107, 135, 80, 136, 125, 116, 63,
            127, 139, 7, 129, 130, 131, 10, 38, 39, 91, 87, 112, 132, 8, 83, 138, 143, 133, 58, 106,
            126, 71, 118, 137, 114, 123, 128, 141, 140, 144, 18, 14, 15, 68, 72, 94, 90, 102, 105,
            109, 108, 147, 31, 67, 5, 148, 149, 150, 151, 152};

    // some URLs - most of these shouldn't be hardcoded, but meh, half the app
    // will break if anything is changed on the Whirlpool side anyway
    public static final String POPULAR_URL      = "https://forums.whirlpool.net.au/forum/?action=popular_views";
    public static final String FORUM_URL        = "https://forums.whirlpool.net.au/forum/";
    public static final String THREAD_URL       = "https://forums.whirlpool.net.au/thread/";
    public static final String REPLY_URL        = "https://forums.whirlpool.net.au/forum/?action=reply&t=";
    public static final String NEWTHREAD_URL    = "https://forums.whirlpool.net.au/forum/?action=newthread&f=";
    public static final String POST_URL         = "https://forums.whirlpool.net.au/forum/?action=replies&r=";

    // number of posts Whirlpool displays on each page
    public static final int POSTS_PER_PAGE = 20;

    private Context context;

    private NewsManager                 newsManager                 ;
    private WhimManager                 whimManager                 ;
    private ForumManager                forumManager                ;
    private RecentThreadsManager        recentThreadsManager        ;
    private PopularThreadsManager       popularThreadsManager       ;
    private AllWatchedThreadsManager    allWatchedThreadsManager    ;
    private UnreadWatchedThreadsManager unreadWatchedThreadsManager ;

    public WhirlpoolApi(Context context) {
        this.context = context;

        settings = PreferenceManager.getDefaultSharedPreferences(context);
        http     = new HttpFetch();
    }

    public NewsManager getNewsManager() {
        if (newsManager == null) {
            newsManager = new NewsManager(context);
        }

        return newsManager;
    }

    public WhimManager getWhimManager() {
        if (whimManager == null) {
            whimManager = new WhimManager(context);
        }

        return whimManager;
    }

    public ForumManager getForumManager() {
        if (forumManager == null) {
            forumManager = new ForumManager(context);
        }

        return forumManager;
    }

    public RecentThreadsManager getRecentThreadsManager() {
        if (recentThreadsManager == null) {
            recentThreadsManager = new RecentThreadsManager(context);
        }

        return recentThreadsManager;
    }

    public PopularThreadsManager getPopularThreadsManager() {
        if (popularThreadsManager == null) {
            popularThreadsManager = new PopularThreadsManager(context);
        }

        return popularThreadsManager;
    }

    public AllWatchedThreadsManager getAllWatchedThreadsManager() {
        if (allWatchedThreadsManager == null) {
            allWatchedThreadsManager = new AllWatchedThreadsManager(context);
        }

        return allWatchedThreadsManager;
    }

    public UnreadWatchedThreadsManager getUnreadWatchedThreadsManager() {
        if (unreadWatchedThreadsManager == null) {
            unreadWatchedThreadsManager = new UnreadWatchedThreadsManager(context);
        }

        return unreadWatchedThreadsManager;
    }

    /**
     * Whirlpool gives us post times in the following formats:
     *  - Yesterday at 3:43 pm
     *  - Today at 2:38 am
     *  - 2017-Apr-21, 4:30 pm
     *  - 2 minutes ago
     *
     *  Since the Whirlroid scraper isn't logged in, these times are relative to AEST.
     *
     * @param whirlpoolTime
     * @return
     */
    public static String getShortTimeFromWhirlpoolTimeString(String whirlpoolTime) {
        return "";
    }

    public String getApiKey() {
        String api_key = settings.getString("pref_apikey", null);

        if (api_key != null) {
            api_key = api_key.trim();
            api_key = api_key.replace(' ', '-');
        }

        return api_key;
    }

    /**
     * Checks if a forum is public (ie. it can be scraped)
     * @param forum_id
     * @return True if forum is public
     */
    public static boolean isPublicForum(int forum_id) {
        for (int id : PUBLIC_FORUMS) {
            if (id == forum_id) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the current threads are from an actual forum on Whirlpool,
     * or are Whirldroid-specific (recent, watched, popular, etc)
     * @return
     */
    public static boolean isActualForum(int forum_id) {
        return
            forum_id != WhirlpoolApi.RECENT_THREADS &&
            forum_id != WhirlpoolApi.UNREAD_WATCHED_THREADS &&
            forum_id != WhirlpoolApi.ALL_WATCHED_THREADS &&
            forum_id != WhirlpoolApi.POPULAR_THREADS &&
            forum_id != WhirlpoolApi.SEARCH_RESULTS;
    }

    @SuppressWarnings("unchecked")
    public Forum getThreads(int forum_id, int page_number, int group_id) {
        Forum forum;

        switch (forum_id) {
            case RECENT_THREADS:
                forum = new Forum(forum_id, "Recent Threads", null);
                forum.setThreads(getRecentThreadsManager().getItems());
                return forum;

            case ALL_WATCHED_THREADS:
                forum = new Forum(forum_id, "Watched Threads", null);
                forum.setThreads(getAllWatchedThreadsManager().getItems());
                return forum;

            case UNREAD_WATCHED_THREADS:
                forum = new Forum(forum_id, "Watched Threads", null);
                forum.setThreads(getUnreadWatchedThreadsManager().getItems());
                return forum;

            case POPULAR_THREADS:
                forum = new Forum(forum_id, "Popular Threads", null);
                forum.setThreads(getPopularThreadsManager().getItems());
                return forum;

            default:
                if (isPublicForum(forum_id)) { // we can scrape this forum
                    return scrapeThreadsFromForum(forum_id, page_number, group_id);
                } else { // can't scrape, use the API instead
                    return downloadThreadsFromForum(forum_id);
                }
        }
    }

    public boolean needToDownloadThreads(int forum_id) {
        switch (forum_id) {
            case RECENT_THREADS:
                return getRecentThreadsManager().needToDownload();
            case ALL_WATCHED_THREADS:
                return getAllWatchedThreadsManager().needToDownload();
            case UNREAD_WATCHED_THREADS:
                return getUnreadWatchedThreadsManager().needToDownload();
            case POPULAR_THREADS:
                return getPopularThreadsManager().needToDownload();
            default:
                return true; // don't cache forum threads
        }
    }

    public Thread getThreadFromTableRow(Element tr, String forum, int forum_id) {
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

                Pattern thread_id_regex = Pattern.compile("(/thread/([0-9]+))|(/archive/([0-9]+))");
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
                } catch (NullPointerException e) {
                    // no page list, must only be 1 page
                } catch (IndexOutOfBoundsException e) {
                    // no page list
                } catch (NumberFormatException e) {
                    // not a number, probably a deleted thread; ignore
                }

            } else if (td_classes.contains("newest")) {
                try {
                    last_poster = td.child(0).child(0).text();
                    last_post_date = td.child(0).ownText();
                } catch (Exception e) { }

            } else if (td_classes.contains("oldest")) {
                try {
                    first_poster = td.child(0).text();
                    first_post_date = td.ownText();
                } catch (Exception e) { }
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

        if (title.equals("")) {
            return null;
        }

        return t;
    }

    private Forum scrapeThreadsFromForum(int forum_id, int page_number, int group_id) {
        ArrayList<Thread> threads = new ArrayList<>();

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
        } catch (NullPointerException e) { }

        if (page_count == -1) {
            // no select box present, get pagination list elements
            try {
                Element p = doc.select("ul.pagination li").last();
                page_count = Integer.parseInt(p.text().replace("\u00a0", "").trim());
            } catch (NullPointerException e) { }
        }

        // get the forum title
        String forum_title;

        try {
            forum_title = doc.select("ul#breadcrumb li").last().text();
        } catch (NullPointerException e) {
            forum_title = "";
        }

        // get the groups in this forum
        Map<String, Integer> groups = null;
        try {
            groups = new LinkedHashMap<>();

            Elements group_options = doc.select("select[name=g] option");

            for (Element group_option : group_options) {
                if (!group_option.attr("value").equals("0")) {
                    groups.put(group_option.text(), Integer.parseInt(group_option.attr("value")));
                }
            }
        } catch (NullPointerException e) {
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

        Forum forum = new Forum(forum_id, forum_title, null);
        forum.setPageCount(page_count);
        forum.setGroups(groups);
        forum.setThreads(threads);

        return forum;
    }

    public Map<String, String> scrapeUserInfo(String user_id) {
        Map<String, String> user_info = new LinkedHashMap<>();

        String url = "https://forums.whirlpool.net.au/user/" + user_id;

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
        List<String> get = new ArrayList<>();
        get.add("threads");
        Map<String, String> params = new HashMap<String, String>();
        params.put("forumids", "" + forum_id);
        params.put("threadcount", "50");

        try {
            downloadData(get, params);
        } catch (WhirlpoolApiException e) {
            return null;
        }

        Forum forum = new Forum(forum_id, "", null);
        forum.setThreads(forum_threads);
        return forum;
    }

    public static String buildSearchUrl(int forum_id, int group_id, String query) {
        URI uri;
        try {
            uri = new URI("https", "forums.whirlpool.net.au", "/forum/", "action=search&f=" + forum_id + "&fg=" + group_id + "&q=" + query + "&o=list", null);
        } catch (URISyntaxException e) {
            return null;
        }

        return uri.toASCIIString();
    }

    public List<Thread> searchThreads(int forum_id, int group_id, String query) {
        String search_url = buildSearchUrl(forum_id, group_id, query);

        if (search_url == null) {
            return null;
        }

        List<Thread> threads = new ArrayList<>();

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

            } else { // thread
                if (current_forum == null) continue;

                Thread t = getThreadFromTableRow(tr, current_forum, current_forum_id);
                threads.add(t);
            }
        }

        return threads;
    }

    public Thread downloadThread(int thread_id, String thread_title) throws WhirlpoolApiException {
        return downloadThread(thread_id, thread_title, 1, FILTER_NONE, null);
    }

    public Thread downloadThread(int thread_id, String thread_title, int page) throws WhirlpoolApiException {
        return downloadThread(thread_id, thread_title, page, FILTER_NONE, null);
    }

    public Thread downloadThread(int thread_id, String thread_title, int page, int filter, String filter_user_id) throws WhirlpoolApiException {

        ArrayList<Post> posts = new ArrayList<>();

        String thread_url = THREAD_URL + thread_id + "?";

        if (filter_user_id != null) {
            thread_url += "&ux=" + filter_user_id;

        } else if (filter != FILTER_NONE) {
            switch (filter) {
                case FILTER_ME:
                    thread_url += "&ux=" + Whirldroid.getOwnWhirlpoolId(context);
                    break;

                case FILTER_MOD:
                    thread_url += "&um=1";
                    break;

                case FILTER_REP:
                    thread_url += "&ur=1";
                    break;
            }

        } else {
            if (page != 1) {
                thread_url += "&p=" + page;
            }
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
        } catch (IndexOutOfBoundsException e) {
            // no notebar
        }

        Elements replies = doc.select("#replylist > div");

        for (Element reply : replies) {
            String id           = "";
            String user_id      = "";
            String user_name    = "";
            String posted_time  = "";
            String content      = "";
            boolean edited      = false;
            boolean op          = false;
            boolean deleted     = false;

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
            } catch (IndexOutOfBoundsException e) {
                // no usergroup, probably a deleted post
            }

            // get posted time
            try {
                Element date_element = reply.select(".date").get(0);
                posted_time = date_element.ownText();
            } catch (IndexOutOfBoundsException e) {
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

    public Pair<Integer, Integer> getPostLocation(String postId) throws WhirlpoolApiException {
        String postUrl = POST_URL + postId;
        int pageNumber  = 1;
        int threadId    = 0;
        Document doc    = downloadPage(postUrl);

        Elements currentPageElement = doc.select("#top_pagination li.current");

        try {
            pageNumber = Integer.parseInt(currentPageElement.text().replace("\u00a0", ""));
        } catch (NumberFormatException e) { }

        String archiveUrl = doc.select(".buttons.bfoot a").first().attr("href");

        try {
            threadId = Integer.parseInt(archiveUrl.replace("/archive/", ""));
        } catch (NumberFormatException e) { }

        return new Pair<>(threadId, pageNumber);
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
        } catch (WhirlpoolApiException e) {
            throw new WhirlpoolApiException(e.getMessage());
        }

        // no data downloaded
        if (data == null) {
            return false;
        }

        JSONObject json = null;

        try {
            json = new JSONObject(data);
        } catch (JSONException e) {
            return false;
        }

        /** Extract User Info **/
        try {
            JSONObject json_user = json.getJSONObject("USER");

            SharedPreferences.Editor settingsEditor = settings.edit();
            settingsEditor.putString("user_id"  , json_user.getString("ID"  ));
            settingsEditor.putString("user_name", json_user.getString("NAME"));
            settingsEditor.apply();

        } catch (JSONException e) {
            // no user info in fetched data
        }

        /** Extract Forums **/
        try {
            JSONArray json_forums = json.getJSONArray("FORUM");
            getForumManager().setItems(getForumsFromJson(json_forums));
        } catch (JSONException e) {
            // no forums in fetched data
        }

        /** Extract News Articles **/
        try {
            JSONArray json_news = json.getJSONArray("NEWS");
            getNewsManager().setItems(getNewsFromJson(json_news));
        } catch (JSONException e) {
            // no news in fetched data
        }

        JSONArray json_whim = null;
        /** Extract Single Whims (for marking as read) **/
        try {
            json_whim = json.getJSONArray("WHIM");
        } catch (JSONException e) {
            // no single whim in fetched data
        }

        /** Extract Whims **/
        try {
            JSONArray json_whims = json.getJSONArray("WHIMS");
            getWhimManager().setItems(getWhimsFromJson(json_whims, json_whim));
        } catch (JSONException e) {
            // no whims in fetched data
        }

        /** Extract Watched Threads **/
        try {
            JSONArray json_watched = json.getJSONArray("WATCHED");

            if (params.containsKey("watchedmode") && params.get("watchedmode").equals("1")) {
                getAllWatchedThreadsManager().setItems(getThreadsFromJson(json_watched));
            } else {
                getUnreadWatchedThreadsManager().setItems(getThreadsFromJson(json_watched));
            }
        } catch (Exception e) {
            // no watched threads in fetched data
        }

        /** Extract Recent Threads **/
        try {
            JSONArray json_recent = json.getJSONArray("RECENT");
            getRecentThreadsManager().setItems(getThreadsFromJson(json_recent));
        } catch (JSONException e) {
            // no recent threads in fetched data
        }

        /** Extract Forum Threads **/
        try {
            JSONArray json_threads = json.getJSONArray("THREADS");
            forum_threads = getThreadsFromJson(json_threads);
        } catch (JSONException e) {
            // no forum threads in fetched data
        }

        return true;
    }

    private ArrayList<NewsArticle> getNewsFromJson(JSONArray json_news) throws JSONException {
        ArrayList<NewsArticle> articles = new ArrayList<>();

        for (int i = 0; i < json_news.length(); i++) {
            JSONObject e = json_news.getJSONObject(i);

            String id           = e.getString("ID");
            String title        = e.getString("TITLE");
            String source       = e.getString("SOURCE");
            String blurb        = e.getString("BLURB");
            String date_time    = e.getString("DATE");

            Date date = Whirldroid.getLocalDateFromString(date_time);

            articles.add(new NewsArticle(id, title, source, blurb, date));
        }

        return articles;
    }

    private ArrayList<Whim> getWhimsFromJson(JSONArray json_whims, JSONArray json_whim) throws JSONException {
        // check if we have an individual whim (we'll assume just one)
        int single_whim_id = 0;

        if (json_whim != null) {
            JSONObject single_whim  = json_whim.getJSONObject(0);
            single_whim_id          = single_whim.getInt("ID");
        }

        ArrayList<Whim> whims = new ArrayList<>();

        for (int i = 0; i < json_whims.length(); i++) {
            JSONObject e    = json_whims.getJSONObject(i);
            JSONObject from = e.getJSONObject("FROM");

            int id              = e.getInt("ID");
            int from_id         = from.getInt("ID");
            String from_name    = from.getString("NAME");
            int viewed          = e.getInt("VIEWED");
            int replied         = e.getInt("REPLIED");
            String date_time    = e.getString("DATE");
            String content      = e.getString("MESSAGE").replace('\r', ' ');

            if (single_whim_id != 0 && id == single_whim_id) {
                viewed = 1;
            }

            Date date = Whirldroid.getLocalDateFromString(date_time);

            whims.add(new Whim(id, from_id, from_name, viewed, replied, date, content));
        }

        return whims;
    }

    private ArrayList<Thread> getThreadsFromJson(JSONArray json_threads) throws JSONException {
        ArrayList<Thread> threads = new ArrayList<>();

        for (int i = 0; i < json_threads.length(); i++) {
            JSONObject e    = json_threads.getJSONObject(i);
            JSONObject last = e.getJSONObject("LAST");

            int id                  = e.getInt("ID");
            String title            = e.getString("TITLE");
            String dateTime         = e.getString("LAST_DATE");
            String last_poster      = last.getString("NAME");
            String last_poster_id   = last.getString("ID");
            String forum            = e.getString("FORUM_NAME");
            int forum_id            = e.getInt("FORUM_ID");
            int reply_count         = e.getInt("REPLIES");

            int unread      = 0;
            int last_page   = 0;
            int last_post   = 0;

            try {
                unread      = e.getInt("UNREAD"     );
                last_page   = e.getInt("LASTPAGE"   );
                last_post   = e.getInt("LASTREAD"   );

            } catch (JSONException ex) {
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
        ArrayList<Forum> forums = new ArrayList<>();

        for (int i = 0; i < json_forums.length(); i++) {
            JSONObject e = json_forums.getJSONObject(i);

            int id          = e.getInt("ID");
            String title    = e.getString("TITLE");
            String section  = e.getString("SECTION");

            forums.add(new Forum(id, title, section));
        }

        return forums;
    }

    public Document downloadPage(String url) {
        int connection_attempts = 3;

        while (connection_attempts > 0) {
            try {
                return Jsoup.connect(url).timeout(7000).get();
            } catch (IOException e) {
                // error connecting, try again
            }

            connection_attempts--;
        }

        return null;
    }

}
