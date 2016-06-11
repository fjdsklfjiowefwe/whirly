package com.gregdev.whirldroid.login.steps;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.TextView;

import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;

public class NotificationsStep extends LoginStep {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setLayoutResource(R.layout.login_step_notifications);
        setStepTitle("Notifications");
    }

    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Whirldroid.getContext());

        try {
            TextView loggedInAs = (TextView) getView().findViewById(R.id.logged_in_as);
            loggedInAs.setText(String.format("Logged in as %1$s (#%2$s)", settings.getString("user_name", ""), settings.getString("user_id", "")));

        } catch (NullPointerException e) { }
    }

}