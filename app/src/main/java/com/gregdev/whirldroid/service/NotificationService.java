package com.gregdev.whirldroid.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
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

    private WakeLock wake_lock;
    private boolean whim_notify;
    private boolean watched_notify;

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
        wake_lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WhirldroidNotificationService");
        wake_lock.acquire();

        // check the global background data setting
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (!cm.getBackgroundDataSetting()) {
            stopSelf();
            return;
        }

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
            whim_notify = settings.getBoolean("pref_whimnotify", false);
            watched_notify = settings.getBoolean("pref_watchednotify", false);

            try {
                ArrayList<String> get = new ArrayList<String>();
                HashMap<String, String> params = new HashMap<String, String>();

                if (whim_notify) { // whim notifications are enabled
                    get.add("whims"); // download whims
                }

                if (watched_notify) { // watched thread notifications enabled
                    get.add("watched"); // download watched threads
                    boolean hide_read = settings.getBoolean("pref_hidewatchedunread", false);
                    if (hide_read) {
                        params.put("watchedmode", "0");
                    }
                    else {
                        params.put("watchedmode", "1");
                    }
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

            if (watched_notify) {
                Forum forum = Whirldroid.getApi().getThreads(WhirlpoolApi.WATCHED_THREADS, 0, 0);
                List<Thread> watched_threads = forum.getThreads();

                int unread_thread_count = 0;
                int unread_reply_count = 0;
                String thread_titles = "";
                boolean need_to_notify = false;

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
                boolean ignore_own = settings.getBoolean("pref_ignoreownreplies", false);

                for (Thread t : watched_threads) {
                    // check if this forum has any unread posts
                    if (t.hasUnreadPosts()) {
                        unread_thread_count++;
                        unread_reply_count += t.getUnread();

                        if (!thread_titles.equals("")) {
                            thread_titles += ", ";
                        }
                        thread_titles += t.getTitle();

                        if (!Whirldroid.hasBeenNotified(t.getLastDate())) {
                            if (t.getLastPosterId().equals(Whirldroid.getOwnWhirlpoolId()) && ignore_own) {
                                need_to_notify = false;
                            }
                            else {
                                need_to_notify = true;
                            }
                        }
                    }
                }

                // there's at least one thread with unread replies
                if (need_to_notify) {
                    String notification_title = null;

                    if (unread_thread_count == 1) { // only one unread thread
                        String plural_reply = "post";
                        if (unread_reply_count > 1) {
                            plural_reply = "posts";
                        }
                        notification_title = unread_reply_count + " unread " + plural_reply;
                    }
                    else { // multiple unread threads
                        notification_title = unread_reply_count + " new replies in " + unread_thread_count + " threads";
                    }

                    sendNotification("New watched thread reply", notification_title, thread_titles, Whirldroid.NEW_WATCHED_NOTIFICATION_ID, R.drawable.btn_forums, WhirlpoolApi.WATCHED_THREADS);
                }

                // no unread threads, cancel existing notification (if any)
                if (unread_thread_count == 0) {
                    String ns = Context.NOTIFICATION_SERVICE;
                    NotificationManager nm = (NotificationManager) getSystemService(ns);
                    nm.cancel(Whirldroid.NEW_WATCHED_NOTIFICATION_ID);
                }
            }

            if (whim_notify) {
                ArrayList<Whim> whims = Whirldroid.getApi().getWhims();

                int new_whim_count = 0;
                String whim_from = "";
                boolean need_to_notify = false;

                for (Whim w : whims) {
                    // check if this whim has been read
                    if (!w.isRead()) {
                        new_whim_count++;
                        // check if we have already sent a notification for this whim
                        if (!Whirldroid.hasBeenNotified(w.getDate())) {
                            need_to_notify = true;
                            whim_from = w.getFromName();
                        }
                    }
                }

                // if there is at least one new notification that we haven't notified for
                if (need_to_notify) {
                    String whim_title;
                    if (new_whim_count == 1) { // only one whim, show who it's from
                        whim_title = "New whim from " + whim_from;
                    }
                    else { // multiple whims, show count
                        whim_title = new_whim_count + " new whims";
                    }

                    sendNotification("New whim", whim_title, "", Whirldroid.NEW_WHIM_NOTIFICATION_ID, R.drawable.btn_whims, 0);
                }

                // no unread whims, cancel existing notification (if any)
                if (new_whim_count == 0) {
                    String ns = Context.NOTIFICATION_SERVICE;
                    NotificationManager nm = (NotificationManager) getSystemService(ns);
                    nm.cancel(Whirldroid.NEW_WHIM_NOTIFICATION_ID);
                }
            }

            stopSelf();
        }
    }

    private void sendNotification(String ticker, String title, String text, int id, int icon, int forum_id) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

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
        bundle.putInt("notification", id);
        bundle.putInt("forum_id", forum_id);

        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtras(bundle);
        PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, 0);

        builder.setContentIntent(resultPendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
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
        wake_lock.release();
    }
}