package com.gregdev.whirldroid.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;

import com.gregdev.whirldroid.TimePreference;
import com.gregdev.whirldroid.TimePreferenceDialogFragmentCompat;
import com.gregdev.whirldroid.Whirldroid;

public class SettingsPageFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences preferences;
    private int preferenceResource;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        preferenceResource = getArguments().getInt("preference_resource");
        setPreferencesFromResource(preferenceResource, rootKey);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        preferences.registerOnSharedPreferenceChangeListener(this);

        if (preferences.getString("pref_theme", "0").equals("2")) {
            try {
                getPreferenceScreen().findPreference("pref_nightmodestart").setEnabled(true);
                getPreferenceScreen().findPreference("pref_nightmodeend").setEnabled(true);

            } catch (NullPointerException e) { } // not on the notifications preference screen; ignore
        }
    }

    @Override
    public void onPause() {
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (key.equals("pref_theme")) {
            if (!preferences.getString("pref_theme", "0").equals("2")) {
                getActivity().finish();
                getActivity().startActivity(new Intent(getActivity(), getActivity().getClass()));

            } else {
                getPreferenceScreen().findPreference("pref_nightmodestart").setEnabled(true);
                getPreferenceScreen().findPreference("pref_nightmodeend").setEnabled(true);
            }
        }

        if (key.equals("pref_whimnotify") || key.equals("pref_watchednotify") || key.equals("pref_notifyfreq")) {
            Whirldroid.updateAlarm();
        }
    }

    // http://stackoverflow.com/a/34398747/602734
    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        DialogFragment dialogFragment = null;

        if (preference instanceof TimePreference) {
            dialogFragment = new TimePreferenceDialogFragmentCompat();
            Bundle bundle = new Bundle(1);
            bundle.putString("key", preference.getKey());
            dialogFragment.setArguments(bundle);
        }

        if (dialogFragment != null) {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(this.getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }
}