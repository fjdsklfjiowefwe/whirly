package com.gregdev.whirldroid.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;

public class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences preferences;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((MainActivity) getActivity()).selectMenuItem("Settings");

        addPreferencesFromResource(R.xml.preferences);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume() {
        super.onResume();
        preferences.registerOnSharedPreferenceChangeListener(this);

    }

    @Override
    public void onPause() {
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
        if (key.equals("pref_theme")) {
            getActivity().finish();
            getActivity().startActivity(new Intent(getActivity(), getActivity().getClass()));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}