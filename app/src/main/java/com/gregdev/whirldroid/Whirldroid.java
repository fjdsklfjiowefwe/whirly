package com.gregdev.whirldroid;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.gregdev.whirldroid.model.Thread;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;

public class Whirldroid extends Application {

    private static Context context;
    private static WhirlpoolApi whirlpool_api;

    public static final int NEW_WHIM_NOTIFICATION_ID = 1;
    public static final int NEW_WATCHED_NOTIFICATION_ID = 2;

    public static final int LIGHT_THEME = 0;
    public static final int DARK_THEME = 1;

    private static int current_theme;
    private static int currentThemeId;
    private static boolean theme_changed = false;

    private Tracker mTracker;

    @Override
    public void onCreate() {
        super.onCreate();

        // get reference to context
        context = getApplicationContext();

        // set up Whirlpool API
        whirlpool_api = new WhirlpoolApi();
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
        Log.d("Whirldroid", "Whirldroidm " + message);
    }

    public static void updateAlarm() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        long interval = Long.parseLong(settings.getString("pref_notifyfreq", "0"));
        interval = interval * 60 * 1000;

        boolean notify_whim    = settings.getBoolean("pref_whimnotify", false);
        boolean notify_watched = settings.getBoolean("pref_watchednotify", false);

        AlarmManager am = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        Intent i = new Intent(context, com.gregdev.whirldroid.service.NotificationService.class);
        PendingIntent pi = PendingIntent.getService(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);
        am.cancel(pi);

        if (interval > 0 && (notify_whim || notify_watched)) {
            am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + (interval / 2), interval, pi);
        }
    }

    /**
     * Gets the current theme
     * @return Current theme
     */
    public static int getCurrentTheme() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Whirldroid.getContext());
        currentThemeId = Integer.parseInt(settings.getString("pref_theme", "0"));

        if (currentThemeId == 2) {
            int startTime    = Integer.parseInt(settings.getString("pref_nightmodestart", "00:00").replace(":", ""));
            int endTime      = Integer.parseInt(settings.getString("pref_nightmodeend"  , "00:00").replace(":", ""));
            int currentTime  = Integer.parseInt(Calendar.getInstance().get(Calendar.HOUR_OF_DAY) + "" + Calendar.getInstance().get(Calendar.MINUTE));

            if ((endTime <= startTime && (currentTime >= startTime || currentTime < endTime)) ||
                    (currentTime >= startTime && currentTime < endTime)) {
                currentThemeId = DARK_THEME;
            } else {
                currentThemeId = LIGHT_THEME;
            }
        }

        switch (currentThemeId) {
            case DARK_THEME:
                Whirldroid.log("Dark theme");
                current_theme = R.style.WhirldroidDarkTheme;
                break;
            case LIGHT_THEME:
            default:
                Whirldroid.log("Light theme");
                current_theme = R.style.WhirldroidLightTheme;
                break;
        }

        return current_theme;
    }

    public static int getCurrentThemeId() {
        return currentThemeId;
    }

    /**
     * Get app user's Whirlpool ID from their API key
     * @todo Change this to download user data on login and store ID in database
     */
    public static String getOwnWhirlpoolId() {
        String key_pieces[] = getApi().getApiKey().split("-");
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
    synchronized public Tracker getDefaultTracker() {
        if (mTracker == null) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            // To enable debug logging use: adb shell setprop log.tag.GAv4 DEBUG
            mTracker = analytics.newTracker(R.xml.global_tracker);
        }
        return mTracker;
    }
}