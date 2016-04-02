package com.gregdev.whirldroid.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;

import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.WhirlpoolApi;
import com.gregdev.whirldroid.WhirlpoolApiException;
import com.gregdev.whirldroid.model.Forum;
import com.gregdev.whirldroid.model.Thread;
import com.gregdev.whirldroid.model.Whim;

/**
 * Whirldroid Notification Service
 * Uses the awesome skeleton service from
 * http://it-ride.blogspot.com.au/2010/10/android-implementing-notification.html
 */

public class NotificationService extends Service {

    private WakeLock wakeLock;
    private boolean whimNotify;
    private boolean watchedNotify;

    /**
     * Simply return null, since our Service will not be communicating with
     * any other components. It just does its work silently.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * This is where we initialise. We call this when onStart/onStartCommand is
     * called by the system. We won't do anything with the intent here, and you
     * probably won't, either.
     */
    private void handleIntent(Intent intent) {
        // obtain the wake lock
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WhirldroidNotificationService");
        wakeLock.acquire();

        // do the actual work, in a separate thread
        new PollTask().execute();
    }

    private class PollTask extends AsyncTask<Void, Void, Void> {
        /**
         * Thread to check for new Whims or new replies to watched threads
         */
        @Override
        protected Void doInBackground(Void... parameters) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
            whimNotify      = settings.getBoolean("pref_whimnotify"     , false);
            watchedNotify   = settings.getBoolean("pref_watchednotify"  , false);

            try {
                ArrayList<String> get = new ArrayList<String>();
                HashMap<String, String> params = new HashMap<String, String>();

                if (whimNotify) { // whim notifications are enabled
                    get.add("whims"); // download whims
                }

                if (watchedNotify) { // watched thread notifications enabled
                    get.add("watched"); // download watched threads
                    params.put("watchedmode", "0");
                }

                Whirldroid.getApi().downloadData(get, params);
            }
            catch (WhirlpoolApiException e) {
                return null;
            }

            return null;
        }

        /**
         * In here you should interpret whatever you fetched in doInBackground
         * and push any notifications you need to the status bar, using the
         * NotificationManager. I will not cover this here, go check the docs on
         * NotificationManager.
         *
         * What you HAVE to do is call stopSelf() after you've pushed your
         * notification(s). This will:
         * 1) Kill the service so it doesn't waste precious resources
         * 2) Call onDestroy() which will release the wake lock, so the device
         *    can go to sleep again and save precious battery.
         */
        @Override
        protected void onPostExecute(Void result) {
            if (watchedNotify) {
                Forum forum = Whirldroid.getApi().getThreads(WhirlpoolApi.UNREAD_WATCHED_THREADS, 0, 0);
                List<Thread> watchedThreads = forum.getThreads();

                int unreadThreadCount = 0;
                int unreadReplyCount  = 0;
                String threadTitles   = "";
                boolean needToNotify  = false;

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                boolean ignoreOwnReplies = settings.getBoolean("pref_ignoreownreplies", false);

                for (Thread thread : watchedThreads) {
                    // check if this thread has any unread posts
                    if (thread.hasUnreadPosts()) {
                        if (!ignoreOwnReplies || !thread.getLastPosterId().equals(Whirldroid.getOwnWhirlpoolId())) {
                            unreadThreadCount++;
                            unreadReplyCount += thread.getUnread();

                            if (!threadTitles.equals("")) {
                                threadTitles += ", ";
                            }
                            threadTitles += thread.getTitle();

                            if (!hasBeenNotified(Whirldroid.NEW_WATCHED_NOTIFICATION_ID, thread.getLastDate())) {
                                needToNotify = true;
                            }
                        }
                    }
                }

                // there's at least one thread with unread replies
                if (needToNotify) {
                    String notification_title = null;

                    if (unreadThreadCount == 1) { // only one unread thread
                        String plural_reply = "post";
                        if (unreadReplyCount > 1) {
                            plural_reply = "posts";
                        }
                        notification_title = unreadReplyCount + " unread " + plural_reply;
                    }
                    else { // multiple unread threads
                        notification_title = unreadReplyCount + " new replies in " + unreadThreadCount + " threads";
                    }

                    sendNotification("New watched thread reply", notification_title, threadTitles, Whirldroid.NEW_WATCHED_NOTIFICATION_ID, R.drawable.ic_visibility_white_24dp);
                }

                // no unread threads, cancel existing notification (if any)
                if (unreadThreadCount == 0) {
                    String ns = Context.NOTIFICATION_SERVICE;
                    NotificationManager nm = (NotificationManager) getSystemService(ns);
                    nm.cancel(Whirldroid.NEW_WATCHED_NOTIFICATION_ID);
                }
            }

            if (whimNotify) {
                ArrayList<Whim> whims = Whirldroid.getApi().getWhims();

                int newWhimCount        = 0;
                String whimFrom         = "";
                boolean needToNotify    = false;

                for (Whim whim : whims) {
                    // check if this whim has been read
                    if (!whim.isRead()) {
                        newWhimCount++;

                        // check if we have already sent a notification for this whim
                        if (!hasBeenNotified(Whirldroid.NEW_WHIM_NOTIFICATION_ID, whim.getDate())) {
                            needToNotify = true;
                            whimFrom = whim.getFromName();
                        }
                    }
                }

                // if there is at least one new notification that we haven't notified for
                if (needToNotify) {
                    String whimTitle;
                    if (newWhimCount == 1) { // only one whim, show who it's from
                        whimTitle = "New whim from " + whimFrom;
                    }
                    else { // multiple whims, show count
                        whimTitle = newWhimCount + " new whims";
                    }

                    sendNotification("New whim", whimTitle, "", Whirldroid.NEW_WHIM_NOTIFICATION_ID, R.drawable.ic_chat_white_24dp);
                }

                // no unread whims, cancel existing notification (if any)
                if (newWhimCount == 0) {
                    String ns = Context.NOTIFICATION_SERVICE;
                    NotificationManager nm = (NotificationManager) getSystemService(ns);
                    nm.cancel(Whirldroid.NEW_WHIM_NOTIFICATION_ID);
                }
            }

            stopSelf();
        }
    }

    private boolean hasBeenNotified(int notificationType, Date date) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        long lastNotifiedTime = 0;

        switch (notificationType) {
            case Whirldroid.NEW_WATCHED_NOTIFICATION_ID:
                lastNotifiedTime = settings.getLong("last_watched_notify_time", 0);
                break;
            case Whirldroid.NEW_WHIM_NOTIFICATION_ID:
                lastNotifiedTime = settings.getLong("last_whim_notify_time", 0);
                break;
        }

        return (date.getTime() <= lastNotifiedTime);
    }

    private void sendNotification(String ticker, String title, String text, int notificationType, int icon) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = settings.edit();

        switch (notificationType) {
            case Whirldroid.NEW_WATCHED_NOTIFICATION_ID:
                editor.putLong("last_watched_notify_time", System.currentTimeMillis());
                break;
            case Whirldroid.NEW_WHIM_NOTIFICATION_ID:
                editor.putLong("last_whim_notify_time", System.currentTimeMillis());
                break;
        }

        editor.apply();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setTicker(ticker)
                .setOnlyAlertOnce(true)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true);

        // do we want to vibrate?
        boolean do_vibrate = settings.getBoolean("pref_notifyvibrate", false);
        if (do_vibrate) {
            long[] v = {500,1000};
            builder.setVibrate(v);
        } else {
            builder.setVibrate(new long[]{0l});
        }

        // play a sound?
        /*String ringtonePreference = settings.getString("pref_notifytone", "DEFAULT_RINGTONE_URI");
        builder.setSound(Uri.parse(ringtonePreference));*/

        if (settings.getBoolean("pref_notifysound", false)) {
            builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        }

        // do we want to flash the LED?
        boolean do_led = settings.getBoolean("pref_notifyled", false);
        if (do_led) {
            builder.setLights(0xff0000ff, 300, 10000);
        }

        Bundle bundle = new Bundle();
        bundle.putInt("notification", notificationType);

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtras(bundle);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(resultPendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationType, builder.build());
    }

    /**
     * This is called on 2.0+ (API level 5 or higher). Returning
     * START_NOT_STICKY tells the system to not restart the service if it is
     * killed because of poor resource (memory/cpu) conditions.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleIntent(intent);
        return START_NOT_STICKY;
    }

    /**
     * In onDestroy() we release our wake lock. This ensures that whenever the
     * Service stops (killed for resources, stopSelf() called, etc.), the wake
     * lock will be released.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        wakeLock.release();
    }
}