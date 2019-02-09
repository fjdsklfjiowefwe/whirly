package com.gregdev.whirldroid.receiver;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiFactory;

import java.util.ArrayList;

public class MarkWhimReadReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Whirldroid.NEW_WHIM_NOTIFICATION_ID);

        final ArrayList<Integer> whimIds = intent.getExtras().getIntegerArrayList("ids");

        Thread thread = new Thread(new Runnable(){
            @Override
            public void run() {
                try {
                    for (int whimId : whimIds) {
                        WhirlpoolApiFactory.getFactory().getApi(context).getWhimManager().download(whimId);
                    }
                } catch (WhirlpoolApiException e) { }
            }
        });

        thread.start();
    }

}