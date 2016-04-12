package com.gregdev.whirldroid.fragment;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.TimePreference;
import com.gregdev.whirldroid.TimePreferenceDialogFragmentCompat;
import com.gregdev.whirldroid.Whirldroid;

public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences preferences;
    private Tracker mTracker;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Obtain the shared Tracker instance.
        Whirldroid application = (Whirldroid) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Set the default white background in the view so as to avoid transparency
        view.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.material_grey_50));
    }

    @Override
    public void onResume() {
        super.onResume();
        preferences.registerOnSharedPreferenceChangeListener(this);

        MainActivity mainActivity = ((MainActivity) getActivity());

        mainActivity.resetActionBar();
        getActivity().setTitle("Settings");

        mainActivity.selectMenuItem("Settings");

        mTracker.setScreenName("Settings");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        if (preferences.getString("pref_theme", "0").equals("2")) {
            getPreferenceScreen().findPreference("pref_nightmodestart").setEnabled(true);
            getPreferenceScreen().findPreference("pref_nightmodeend").setEnabled(true);
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // http://stackoverflow.com/a/34398747/602734
    @Override
    public void onDisplayPreferenceDialog(Preference preference)
    {
        DialogFragment dialogFragment = null;
        if (preference instanceof TimePreference)
        {
            dialogFragment = new TimePreferenceDialogFragmentCompat();
            Bundle bundle = new Bundle(1);
            bundle.putString("key", preference.getKey());
            dialogFragment.setArguments(bundle);
        }

        if (dialogFragment != null)
        {
            dialogFragment.setTargetFragment(this, 0);
            dialogFragment.show(this.getFragmentManager(), "android.support.v7.preference.PreferenceFragment.DIALOG");
        }
        else
        {
            super.onDisplayPreferenceDialog(preference);
        }
    }
}