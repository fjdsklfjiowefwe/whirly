package com.gregdev.whirldroid.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiFactory;

import java.util.ArrayList;

public class MarkWatchedReadReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Whirldroid.NEW_WATCHED_NOTIFICATION_ID);

        ArrayList<Integer> threadIds = intent.getExtras().getIntegerArrayList("ids");
        String markReadIds = "";

        for (int threadId : threadIds) {
            if (!markReadIds.equals("")) markReadIds += ",";
            markReadIds += threadId;
        }

        final String finalMarkReadIds = markReadIds;

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    WhirlpoolApiFactory.getFactory().getApi(context).getUnreadWatchedThreadsManager().download(finalMarkReadIds, null, 0);
                } catch (WhirlpoolApiException e) { }
            }
        });

        thread.start();
    }

}