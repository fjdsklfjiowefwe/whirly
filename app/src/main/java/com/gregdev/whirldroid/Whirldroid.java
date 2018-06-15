package com.gregdev.whirldroid;

import com.crashlytics.android.Crashlytics;
import io.fabric.sdk.android.Fabric;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import android.app.AlarmManager;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.gregdev.whirldroid.model.Thread;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;

public class Whirldroid extends Application {

    private static Context context;
    private static WhirlpoolApi whirlpool_api;

    public static final int NEW_WHIM_NOTIFICATION_ID = 1;
    public static final int NEW_WATCHED_NOTIFICATION_ID = 2;

    public static final int LIGHT_THEME = 0;
    public static final int DARK_THEME = 1;

    private static int currentThemeId;

    public static final String WHIRLDROID_THREAD_ID = "1906307";

    private static FirebaseAnalytics mFirebaseAnalytics;

    private static FirebaseJobDispatcher dispatcher;

    @Override
    public void onCreate() {
        super.onCreate();
        Fabric.with(this, new Crashlytics());

        // get reference to context
        context = getApplicationContext();

        // set up Whirlpool API
        whirlpool_api = new WhirlpoolApi();

        // set up analytics
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Whirldroid.getContext());
        String themeName = "";

        switch (Integer.parseInt(settings.getString("pref_theme", LIGHT_THEME + ""))) {
            case 0:
                themeName = "Light";
                break;
            case 1:
                themeName = "Dark";
                break;
            case 2:
                themeName = "Night";
                break;
        }

        String whimNotifications            = settings.getBoolean("pref_whimnotify"     , false) ? "Enabled" : "Disabled";
        String watchedThreadNotifcations    = settings.getBoolean("pref_watchednotify"  , false) ? "Enabled" : "Disabled";

        mFirebaseAnalytics.setUserProperty("theme"                  , themeName                 );
        mFirebaseAnalytics.setUserProperty("whim_notifications"     , whimNotifications         );
        mFirebaseAnalytics.setUserProperty("watched_notifications"  , watchedThreadNotifcations );

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel whimChannel =
                    new NotificationChannel("whirldroid_whims", "Whirldroid Whims", NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(whimChannel);

            NotificationChannel watchedChannel =
                    new NotificationChannel("whirldroid_watched_threads", "Whirldroid Watched Threads", NotificationManager.IMPORTANCE_HIGH);
            mNotificationManager.createNotificationChannel(watchedChannel);
        }
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

    public static FirebaseAnalytics getTracker() {
        return mFirebaseAnalytics;
    }

    /**
     * Gets the current theme
     * @return Current theme
     */
    public static int getCurrentTheme() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Whirldroid.getContext());

        currentThemeId = Integer.parseInt(settings.getString("pref_theme", LIGHT_THEME + ""));

        if (currentThemeId == 2) {
            int currentHour     = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
            int currentMinute   = Calendar.getInstance().get(Calendar.MINUTE);
            String current      = currentHour + "";

            if (currentMinute < 10) {
                current += "0";
            }
            current += currentMinute;

            int currentTime  = Integer.parseInt(current);
            int startTime    = Integer.parseInt(settings.getString("pref_nightmodestart", "00:00").replace(":", ""));
            int endTime      = Integer.parseInt(settings.getString("pref_nightmodeend"  , "00:00").replace(":", ""));

            if ((endTime < startTime && (currentTime >= startTime || currentTime < endTime)) ||
                    (currentTime >= startTime && currentTime < endTime)) {
                currentThemeId = DARK_THEME;
            } else {
                currentThemeId = LIGHT_THEME;
            }
        }

        switch (currentThemeId) {
            case DARK_THEME:
                return R.style.WhirldroidDarkTheme;
            case LIGHT_THEME:
            default:
                return R.style.WhirldroidLightTheme;
        }
    }

    public static int getCurrentThemeId() {
        return currentThemeId;
    }

    /**
     * Get app user's Whirlpool ID from their API key
     */
    public static String getOwnWhirlpoolId() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        String userId = settings.getString("user_id", null);

        if (userId != null) {
            return userId;
        }

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

    public static ArrayList<Integer> stringToInts(String str) {
        // Split on `,` and then take every alternate element.
        String[] tokens = str.split(",");

        ArrayList<Integer> intList = new ArrayList<>();

        for (int i = 0; i < tokens.length; i = i + 2) {
            intList.add(Integer.parseInt(tokens[i]));
        }

        return intList;
    }

    public static boolean isGreg() {
        return encryptPassword(getApi().getApiKey() + "whirldroid").equals("a4572bd46cd80bbc30bb29796244b04595673693");
    }

    private static String encryptPassword(String password) {
        String sha1 = "";
        try {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(password.getBytes("UTF-8"));
            sha1 = byteToHex(crypt.digest());
        }
        catch(Exception e) { }
        return sha1;
    }

    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    public static void openInBrowser(String url, Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (Build.VERSION.SDK_INT >= 18) {
            final String EXTRA_CUSTOM_TABS_SESSION = "android.support.customtabs.extra.SESSION";
            final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR";

            Bundle extras = new Bundle();
            extras.putBinder(EXTRA_CUSTOM_TABS_SESSION, null);
            intent.putExtras(extras);
            intent.putExtra(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR, Color.parseColor("#3A437B"));
        }

        context.startActivity(intent);
    }

    public static void logScreenView(String screenName) {
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.ITEM_CATEGORY, "screen");
        params.putString(FirebaseAnalytics.Param.ITEM_NAME, screenName);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, params);
    }

    public static void startSchedule() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

        int interval = Integer.parseInt(settings.getString("pref_notifyfreq", "0"));
        interval = interval * 60;

        boolean notify_whim    = settings.getBoolean("pref_whimnotify"      , false);
        boolean notify_watched = settings.getBoolean("pref_watchednotify"   , false);

        if (interval == 0 || (!notify_whim && !notify_watched)) {
            dispatcher.cancel("whirldroid");

        } else {
            if (dispatcher == null) {
                dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(context));
            }

            Job job = dispatcher.newJobBuilder()
                    .setService(com.gregdev.whirldroid.service.NotificationJobService.class)
                    .setTag("whirldroid")
                    .setRecurring(true)
                    .setLifetime(Lifetime.UNTIL_NEXT_BOOT)
                    .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                    .setReplaceCurrent(true)
                    .setTrigger(Trigger.executionWindow(interval - 1, interval + 1))
                    .build();

            dispatcher.schedule(job);
        }
    }

}