package com.gregdev.whirldroid.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.gregdev.whirldroid.Whirldroid;

public class StartupIntentReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Whirldroid.log("StartupIntentReceiver onReceive");

		Whirldroid.startSchedule();
	}

}