package com.gregdev.whirldroid.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;

import java.util.HashMap;
import java.util.Map;

public class SettingsFragment extends Fragment {

    private Tracker mTracker;
    ViewPager viewPager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Obtain the shared Tracker instance.
        Whirldroid application = (Whirldroid) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.tab_view_pager, container, false);
        viewPager = (ViewPager) rootView.findViewById(R.id.pager);
        viewPager.setAdapter(new SettingsPagerAdapter());

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity mainActivity = ((MainActivity) getActivity());

        mainActivity.resetActionBar();
        getActivity().setTitle("Settings");

        mainActivity.selectMenuItem("Settings");

        mTracker.setScreenName("Settings");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public class SettingsPagerAdapter extends FragmentPagerAdapter {

        // preference file name, tab title
        Map<Integer, SettingsPage> pages;

        public SettingsPagerAdapter() {
            super(getChildFragmentManager());

            pages = new HashMap<>();

            pages.put(0, new SettingsPage(R.xml.preferences_general         , "General"          ));
            pages.put(1, new SettingsPage(R.xml.preferences_notifications   , "Notifications"    ));
            pages.put(2, new SettingsPage(R.xml.preferences_threads         , "Threads"          ));
            pages.put(3, new SettingsPage(R.xml.preferences_recent_threads  , "Recent Threads"   ));
            pages.put(4, new SettingsPage(R.xml.preferences_watched_threads , "Watched Threads"  ));
            pages.put(5, new SettingsPage(R.xml.preferences_whims           , "Whims"            ));
        }

        @Override
        public int getCount() {
            return pages.size();
        }

        @Override
        public Fragment getItem(int position) {
            return pages.get(position).getFragment();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return pages.get(position).getTitle();
        }
    }

    private class SettingsPage {

        int preferenceResource;
        String title;
        Fragment fragment = null;

        public SettingsPage(int preferenceResource, String title) {
            this.preferenceResource = preferenceResource;
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public Fragment getFragment() {
            if (fragment == null) {
                Bundle bundle = new Bundle();
                bundle.putInt("preference_resource", preferenceResource);

                fragment = new SettingsPageFragment();
                fragment.setArguments(bundle);
            }

            return fragment;
        }

    }
}