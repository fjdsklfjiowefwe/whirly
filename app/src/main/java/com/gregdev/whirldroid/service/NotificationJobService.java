package com.gregdev.whirldroid.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.text.Html;

import com.crashlytics.android.Crashlytics;
import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.model.Forum;
import com.gregdev.whirldroid.model.Thread;
import com.gregdev.whirldroid.model.Whim;
import com.gregdev.whirldroid.receiver.MarkWatchedReadReceiver;
import com.gregdev.whirldroid.receiver.MarkWhimReadReceiver;
import com.gregdev.whirldroid.receiver.UnwatchReceiver;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class NotificationJobService extends JobService {

    private boolean whimNotify;
    private boolean watchedNotify;

    private static final int ACTION_WHIM_MARK_READ = 0;
    private static final int ACTION_WATCHED_MARK_READ = 1;
    private static final int ACTION_UNWATCH = 2;
    private static final int ACTION_REPLY = 3;

    private JobParameters job;

    @Override
    public boolean onStartJob(JobParameters job) {
        this.job = job;

        new java.lang.Thread(new PollTask()).start();

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false;
    }

    class PollTask implements Runnable {
        /**
         * Thread to check for new Whims or new replies to watched threads
         */

        @Override
        public void run() {
            Whirldroid.log("checking...");
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

                WhirlpoolApiFactory.getFactory().getApi(getApplicationContext()).downloadData(get, params);
            }
            catch (WhirlpoolApiException e) {
                Crashlytics.logException(e);
            }

            if (watchedNotify) {
                NotificationCompat.InboxStyle watchedInboxStyle = new NotificationCompat.InboxStyle();
                ArrayList<NotificationCompat.Action> actions = new ArrayList<>();
                ArrayList<String> threadIds = new ArrayList<>();

                Forum forum = WhirlpoolApiFactory.getFactory().getApi(getBaseContext()).getThreads(WhirlpoolApi.UNREAD_WATCHED_THREADS, 0, 0);
                List<Thread> watchedThreads = forum.getThreads();

                int unreadThreadCount = 0;
                int unreadReplyCount  = 0;
                boolean needToNotify  = false;

                boolean ignoreOwnReplies = settings.getBoolean("pref_ignoreownreplies", false);

                if (watchedThreads != null) {
                    for (Thread thread : watchedThreads) {
                        // check if this thread has any unread posts
                        if (thread.hasUnreadPosts()) {
                            if (!ignoreOwnReplies || !thread.getLastPosterId().equals(Whirldroid.getOwnWhirlpoolId(getBaseContext()))) {
                                unreadThreadCount++;
                                unreadReplyCount += thread.getUnread();
                                threadIds.add(thread.getId());

                                watchedInboxStyle.addLine(thread.getTitle());

                                if (!hasBeenNotified(Whirldroid.NEW_WATCHED_NOTIFICATION_ID, thread.getLastDate())) {
                                    needToNotify = true;
                                }
                            }
                        }
                    }
                }

                // there's at least one thread with unread replies
                if (needToNotify) {
                    String text;
                    String title;

                    if (unreadThreadCount == 1) { // only one unread thread
                        title = "New watched thread reply";

                        String plural_reply = "post";
                        if (unreadReplyCount > 1) {
                            plural_reply = "posts";
                        }
                        text = unreadReplyCount + " unread " + plural_reply;

                    } else { // multiple unread threads
                        title = "New watched thread replies";
                        text = unreadReplyCount + " new replies in " + unreadThreadCount + " threads";
                    }

                    Intent markRead = new Intent(getApplicationContext(), MarkWatchedReadReceiver.class);
                    markRead.putExtra("ids", threadIds);
                    PendingIntent pendingMarkRead = PendingIntent.getBroadcast(getApplicationContext(), 1, markRead, PendingIntent.FLAG_UPDATE_CURRENT);

                    Intent unwatch = new Intent(getApplicationContext(), UnwatchReceiver.class);
                    unwatch.putExtra("ids", threadIds);
                    PendingIntent pendingUnwatch = PendingIntent.getBroadcast(getApplicationContext(), 1, unwatch, PendingIntent.FLAG_UPDATE_CURRENT);

                    actions.add(new NotificationCompat.Action(R.drawable.ic_done_white_24dp, "Mark Read", pendingMarkRead));
                    actions.add(new NotificationCompat.Action(R.drawable.ic_visibility_off_white_24dp, "Unwatch", pendingUnwatch));

                    sendNotification(title, text, Whirldroid.NEW_WATCHED_NOTIFICATION_ID, R.drawable.ic_visibility_white_24dp, watchedInboxStyle, actions, threadIds);
                }

                // no unread threads, cancel existing notification (if any)
                if (unreadThreadCount == 0) {
                    String ns = Context.NOTIFICATION_SERVICE;
                    NotificationManager nm = (NotificationManager) getSystemService(ns);
                    nm.cancel(Whirldroid.NEW_WATCHED_NOTIFICATION_ID);
                }
            }

            if (whimNotify) {
                NotificationCompat.InboxStyle whimInboxStyle = new NotificationCompat.InboxStyle();
                ArrayList<NotificationCompat.Action> actions = new ArrayList<>();
                ArrayList<String> whimIds = new ArrayList<>();

                int newWhimCount        = 0;
                String whimFrom         = "";
                boolean needToNotify    = false;

                if (WhirlpoolApiFactory.getFactory().getApi(getBaseContext()).getWhimManager().getItems() != null) {
                    for (Whim whim : WhirlpoolApiFactory.getFactory().getApi(getBaseContext()).getWhimManager().getItems()) {
                        // check if this whim has been read
                        if (!whim.isRead()) {
                            newWhimCount++;
                            whimIds.add(whim.getId() + "");
                            whimFrom = whim.getFromName();

                            // check if we have already sent a notification for this whim
                            if (!hasBeenNotified(Whirldroid.NEW_WHIM_NOTIFICATION_ID, whim.getDate())) {
                                needToNotify = true;
                                whimInboxStyle.addLine(Html.fromHtml("<b>" + whim.getFromName() + "</b> " + whim.getContent()));
                            }
                        }
                    }
                }

                // if there is at least one new notification that we haven't notified for
                if (needToNotify) {

                    Intent markRead = new Intent(getApplicationContext(), MarkWhimReadReceiver.class);
                    markRead.putExtra("ids", whimIds);
                    PendingIntent pendingMarkRead = PendingIntent.getBroadcast(getApplicationContext(), 0, markRead, PendingIntent.FLAG_UPDATE_CURRENT);

                    Intent reply = new Intent(Intent.ACTION_VIEW, Uri.parse("https://whirlpool.net.au/whim/?action=write&rt=" + whimIds.get(0)));
                    PendingIntent pendingReply = PendingIntent.getActivity(getApplicationContext(), 1, reply, PendingIntent.FLAG_UPDATE_CURRENT);

                    String whimTitle;
                    if (newWhimCount == 1) { // only one whim, show who it's from
                        whimTitle = "New whim from " + whimFrom;

                        actions.add(new NotificationCompat.Action(R.drawable.ic_done_white_24dp, "Mark Read", pendingMarkRead));
                        actions.add(new NotificationCompat.Action(R.drawable.ic_reply_white_24dp, "Reply", pendingReply));

                    } else { // multiple whims, show count
                        whimTitle = newWhimCount + " new whims";

                        actions.add(new NotificationCompat.Action(R.drawable.ic_done_white_24dp, "Mark All Read", pendingMarkRead));
                    }

                    sendNotification("New whim", whimTitle, Whirldroid.NEW_WHIM_NOTIFICATION_ID, R.drawable.ic_chat_white_24dp, whimInboxStyle, actions, whimIds);
                }

                // no unread whims, cancel existing notification (if any)
                if (newWhimCount == 0) {
                    String ns = Context.NOTIFICATION_SERVICE;
                    NotificationManager nm = (NotificationManager) getSystemService(ns);
                    nm.cancel(Whirldroid.NEW_WHIM_NOTIFICATION_ID);
                }
            }
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

    private void sendNotification(String title, String text, int notificationType, int icon, NotificationCompat.InboxStyle inboxLayout, ArrayList<NotificationCompat.Action> actions, ArrayList<String> actionItemIds) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        SharedPreferences.Editor editor = settings.edit();

        String notificationChannelId = "";

        switch (notificationType) {
            case Whirldroid.NEW_WATCHED_NOTIFICATION_ID:
                notificationChannelId = "whirldroid_watched_threads";
                editor.putLong("last_watched_notify_time", System.currentTimeMillis());
                break;
            case Whirldroid.NEW_WHIM_NOTIFICATION_ID:
                notificationChannelId = "whirldroid_whims";
                editor.putLong("last_whim_notify_time", System.currentTimeMillis());
                break;
        }

        editor.apply();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, notificationChannelId);
        builder.setSmallIcon(icon)
                .setContentTitle(title)
                .setContentText(text)
                .setOnlyAlertOnce(true)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setColor(getApplicationContext().getResources().getColor(R.color.colorPrimary))
                .setStyle(inboxLayout)
                .setPriority(NotificationCompat.PRIORITY_MAX)
        ;

        for (NotificationCompat.Action action : actions) {
            builder.addAction(action);
        }

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

        Intent intent = new Intent();
        intent.putExtras(bundle);
        intent.setAction("com.gregdev.whirldroid.notification");
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(notificationType, builder.build());
    }
}
