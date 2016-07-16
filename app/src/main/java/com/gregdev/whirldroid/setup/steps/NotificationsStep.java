package com.gregdev.whirldroid.setup.steps;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;

import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;

public class NotificationsStep extends SetupStep {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setLayoutResource(R.layout.setup_step_notifications);
        setStepTitle("Notifications");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        final SharedPreferences.Editor settingsEditor = settings.edit();

        view.findViewById(R.id.notify_whims).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsEditor.putBoolean("pref_whimnotify", ((CheckBox) v).isChecked());
                settingsEditor.apply();
            }
        });

        view.findViewById(R.id.notify_threads).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                settingsEditor.putBoolean("pref_watchednotify", ((CheckBox) v).isChecked());
                settingsEditor.apply();
            }
        });

        ((Spinner) view.findViewById(R.id.notify_freq)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String[] frequencyValues = getResources().getStringArray(R.array.values_pref_notifyfreq);
                settingsEditor.putString("pref_notifyfreq", frequencyValues[position]);
                settingsEditor.apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
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