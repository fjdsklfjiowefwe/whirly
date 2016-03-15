package com.gregdev.whirldroid.fragment;

import android.annotation.TargetApi;
import android.app.backup.BackupManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.MenuItem;

import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;

public class SettingsFragment extends PreferenceFragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.layout.preferences);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
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