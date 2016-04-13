package com.gregdev.whirldroid.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;

import java.util.ArrayList;

public class UnwatchReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        NotificationManager notificationManager = (NotificationManager) Whirldroid.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Whirldroid.NEW_WATCHED_NOTIFICATION_ID);

        ArrayList<Integer> threadIds = intent.getExtras().getIntegerArrayList("ids");
        String unwatchIds = "";

        for (int threadId : threadIds) {
            if (!unwatchIds.equals("")) unwatchIds += ",";
            unwatchIds += threadId;
        }

        final String finalUnwatchIds = unwatchIds;

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    Whirldroid.getApi().getUnreadWatchedThreadsManager().download(null, finalUnwatchIds, 0);
                } catch (WhirlpoolApiException e) { }
            }
        });

        thread.start();
    }

}