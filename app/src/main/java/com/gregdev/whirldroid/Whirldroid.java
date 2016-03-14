package com.gregdev.whirldroid;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.gregdev.whirldroid.models.Thread;

/*import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;*/

public class Whirldroid extends Application {

    private static Context context;
    private static WhirlpoolApi whirlpool_api;

    public static final int NEW_WHIM_NOTIFICATION_ID = 1;
    public static final int NEW_WATCHED_NOTIFICATION_ID = 2;

    public static final int LIGHT_THEME = 0;
    public static final int DARK_THEME = 1;

    private static int current_theme;
    private static int current_theme_id;
    private static boolean theme_changed = false;

    @Override
    public void onCreate() {
        super.onCreate();

        // get reference to context
        context = getApplicationContext();

        // set up Whirlpool API
        whirlpool_api = new WhirlpoolApi();

        // set current theme
        setCurrentTheme(false);
        setTheme(current_theme); // I don't know if this even does anything
    }

    /**
     * Gets reference to application context
     * @return context object
     */
    public static Context getContext() {
        return context;
    }

    /**
     * Gets reference to Whirlpool API
     * @return Whirlpool API object
     */
    public static WhirlpoolApi getApi() {
        return whirlpool_api;
    }

    /**
     * Logs something to the console; Greg's debugging
     */
    public static void log(String message) {
        Log.d("Whirldroid", message);
    }

    /**
     * Sets the current theme based on the user's preference
     *
     * Called when the app first starts so as to reduce the reading of shared
     * preferences, which can be slow.
     *
     * @param update Whether the theme has changed between Activities
     */
    public static void setCurrentTheme(boolean update) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Whirldroid.getContext());
        current_theme_id = Integer.parseInt(settings.getString("pref_theme", "0"));

        /*int new_theme;
        switch (current_theme_id) {
            case DARK_THEME:
                new_theme = R.style.WhirldroidDarkTheme;
                break;
            case LIGHT_THEME:
            default:
                new_theme = R.style.WhirldroidLightTheme;
                break;
        }

        // theme changed
        if (new_theme != current_theme) {
            current_theme = new_theme;
            setThemeChanged(update);
        }*/
    }

    /**
     * Gets the ID of the current theme
     * @return Theme ID
     */
    public static int getCurrentThemeId() {
        return current_theme_id;
    }

    /**
     * Gets the current theme
     * @return Current theme
     */
    public static int getWhirldroidTheme() {
        return current_theme;
    }

    /**
     * Checks if the theme has changed between activities
     * @return True if theme has changed
     */
    public static boolean hasThemeChanged() {
        return theme_changed;
    }

    /**
     * Sets the theme changed variable
     * @param changed
     */
    public static void setThemeChanged(boolean changed) {
        theme_changed = changed;
    }

    /**
     * Get app user's Whirlpool ID from their API key
     * @todo Change this to download user data on login and store ID in database
     */
    public static String getOwnWhirlpoolId() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Whirldroid.getContext());
        String api_key = settings.getString("pref_apikey", null);
        String key_pieces[] = api_key.split("-");
        return key_pieces[0];
    }

    /**
     * Calculates the date from a timestamp string
     * From http://stackoverflow.com/questions/8735214
     * @param long_date_time Datetime string
     * @return Local date representation
     */
    public static Date getLocalDateFromString(String long_date_time) {
        // date format for the Whirlpool API
        SimpleDateFormat date_format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+SSSS");

        long when = 0;
        try {
            // time, adjusting for AEST (Whirlpool default timezone)
            when = date_format.parse(long_date_time).getTime() - 10*60*60*1000;
        }
        catch (Exception e) {
            return null;
        }

        // adjust for daylight savings time (this sure looks confusing)
        Date local_date = new Date(when + TimeZone.getDefault().getRawOffset() + (TimeZone.getDefault().inDaylightTime(new Date()) ? TimeZone.getDefault().getDSTSavings() : 0));

        return local_date;
    }

    /**
     * Gets the time difference between now and a timestamp in seconds
     * @param seconds Timestamp to get difference of
     * @return Difference, formatted in minutes, hours, days, etc.
     */
    public static String getTimeSince(long seconds) {
        long time = seconds;
        String time_text = "second";

        if (time >= 60) { // 1 minute or more ago
            time = time / 60;
            time_text = "minute";
        }
        if (time >= 60) { // 1 hour or more ago
            time = time / 60;
            time_text = "hour";
        }
        if (time >= 24 && time_text.equals("hour")) { // 1 day or more ago
            time = time / 24;
            time_text = "day";
        }
        if (time >= 7 && time_text.equals("day")) { // 1 week or more ago
            time = time / 7;
            time_text = "week";
        }

        if (time != 1) { // pluralise time text if time isn't 1
            time_text = time_text + "s";
        }

        return time + " " + time_text;
    }

    /**
     * Gets the time difference between now and a Date object
     * @param date
     * @return Time difference
     */
    public static String getTimeSince(Date date) {
        long time = (System.currentTimeMillis() - date.getTime()) / 1000;
        return getTimeSince(time);
    }

    /**
     * Uses timestamps to check if a notification for a given whim or watched thread
     * reply has already been sent to the user
     * @param date The date of the whim or watched thread
     * @return True if a notification has already been sent
     */
    public static boolean hasBeenNotified(Date date) {
        long current_timestamp = System.currentTimeMillis();
        long event_timestamp = date.getTime();
        long system_uptime = SystemClock.elapsedRealtime();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        long notification_interval = Long.parseLong(settings.getString("pref_notifyfreq", "0")) * 60000;

        /**
         * If the system hasn't been turned on long enough to check for anything, assume that
         * the pending notification hasn't already be shown to the usre
         */
        if (system_uptime < notification_interval) {
            return false;
        }

        /**
         * if the current timestamp subtract the time of the event is less than the notification interval,
         * then the event must have happened after the last notification.
         */
        if (current_timestamp - event_timestamp < notification_interval) {
            return false; // we haven't notified for this event yet
        }

        return true; // already notified for this event
    }

    /**
     * Replaces (some common) HTML characters with the actual character
     * @param text String to search for HTML characters
     * @return String with HTML characters replaced with actual characters
     */
    public static String removeCommonHtmlChars(String text) {
        String[] chars = {
                "&quot;",  "\"",
                "&amp;",   "&",
                "&frasl;", "/",
                "&lt;",    "<",
                "&gt;",    ">",
                "&ndash;", "-",
                "&nbsp;",  " "
        };

        for (int i = 0; i < chars.length; i = i + 2) {
            text = text.replace(chars[i], chars[i + 1]);
        }

        return text;
    }

    /**
     * Groups threads by the forum the thread was posted in
     * @param thread_list
     * @return Map of threads with forum names as keys
     */
    public static Map<String, List<Thread>> groupThreadsByForum(List<Thread> thread_list) {
        Map<String, List<Thread>> sorted_threads = new HashMap<String, List<Thread>>();

        Collections.sort(thread_list);

        for (Thread t : thread_list) {
            if (sorted_threads.containsKey(t.getForum())) {
                sorted_threads.get(t.getForum()).add(t);
            }
            else {
                List<Thread> threads = new ArrayList<Thread>();
                threads.add(t);
                sorted_threads.put(t.getForum(), threads);
            }
        }

        return sorted_threads;
    }

    /**
     * Gets the offset for selecting a thread when list headings are taken into account.
     * This is a nasty hack, but it works.
     * @param pos
     * @param sorted_threads
     * @return
     */
    public static int calculateOffset(int pos, Map<String, ArrayList<Thread>> sorted_threads) {
        int list_item_count = 0;
        int forum_count = 0;
        for (List<Thread> threads : sorted_threads.values()) {
            forum_count++;
            list_item_count++;
            if (list_item_count >= pos) return forum_count;
            list_item_count += threads.size();
            if (list_item_count >= pos) return forum_count;
        }
        return -1;
    }

    /**
     * Gets the thread at a given list position, taking list headings into account
     * @param pos
     * @param sorted_items
     * @return The thread at the given position
     */
    public static Thread getThreadAtPosition(int pos, Map<String, List<Thread>> sorted_items) {
        int i = 0;
        for (List<Thread> items : sorted_items.values()) {
            i++;
            for (Thread item : items) {
                if (i == pos) {
                    return item;
                }
                i++;
            }
        }
        return null;
    }


    /** Google Analytics **/

    /*HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();

    public synchronized Tracker getTracker(TrackerName trackerId) {

        if (!mTrackers.containsKey(trackerId)) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker t = analytics.newTracker(R.xml.app_tracker);
            mTrackers.put(trackerId, t);
        }
        return mTrackers.get(trackerId);
    }

    public enum TrackerName {
        APP_TRACKER,
        GLOBAL_TRACKER,
        E_COMMERCE_TRACKER,
    }*/
}