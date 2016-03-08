package com.gregdev.whirldroid.services;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;

public class StartupIntentReceiver extends BroadcastReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
	    long interval = Long.parseLong(settings.getString("pref_notifyfreq", "0"));
	    interval = interval * 60 * 1000;

	    boolean notifyWhim    = settings.getBoolean("pref_whimnotify", false);
		boolean notifyWatched = settings.getBoolean("pref_watchednotify", false);
		
		/*if (interval > 0 && (notifyWhim || notifyWatched)) {
			AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
			
			Intent i = new Intent(context, com.gregdev.whirldroid.services.NotificationService.class);
		    PendingIntent pi = PendingIntent.getService(context, 0, i, 0);
		    
			am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
					SystemClock.elapsedRealtime(), interval, pi);
		}*/
	}
}
