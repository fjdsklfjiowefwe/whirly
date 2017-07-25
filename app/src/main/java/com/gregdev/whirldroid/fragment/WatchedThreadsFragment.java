package com.gregdev.whirldroid.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Refresher;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;

import java.util.HashMap;
import java.util.Map;

public class WatchedThreadsFragment extends Fragment {

    ViewPager viewPager;
    private Map<Integer, Fragment> pages = new HashMap<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        container.removeAllViews();
        View rootView = inflater.inflate(R.layout.tab_view_pager, container, false);
        viewPager = (ViewPager) rootView.findViewById(R.id.pager);
        viewPager.setAdapter(new WatchedFragmentPagerAdapter());
        setHasOptionsMenu(true);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if (position == 0) {
                    Whirldroid.getTracker().setCurrentScreen(getActivity(), "WatchedThreadsUnread", null);
                    Whirldroid.logScreenView("WatchedThreadsUnread");
                } else {
                    Whirldroid.getTracker().setCurrentScreen(getActivity(), "WatchedThreadsAll", null);
                    Whirldroid.logScreenView("WatchedThreadsAll");
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity mainActivity = ((MainActivity) getActivity());
        mainActivity.resetActionBar();
        mainActivity.setTitle("Watched Threads");

        mainActivity.selectMenuItem("WatchedThreads");
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.refresh, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                WatchedFragmentPagerAdapter adapter = (WatchedFragmentPagerAdapter) viewPager.getAdapter();
                ((Refresher) adapter.getItem(viewPager.getCurrentItem())).initiateRefresh();
        }
        return false;
    }

    public class WatchedFragmentPagerAdapter extends FragmentPagerAdapter {
        final int PAGE_COUNT = 2;

        public WatchedFragmentPagerAdapter() {
            super(getChildFragmentManager());
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

        @Override
        public Fragment getItem(int position) {
            if (pages.get(position) == null) {
                Bundle bundle = new Bundle();

                if (position == 0) {
                    bundle.putInt("forum_id", WhirlpoolApi.UNREAD_WATCHED_THREADS);
                } else {
                    bundle.putInt("forum_id", WhirlpoolApi.ALL_WATCHED_THREADS);
                }

                Fragment fragment = new ForumPageFragment();
                fragment.setArguments(bundle);
                pages.put(position, fragment);
            }

            return pages.get(position);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Unread";
                case 1:
                    return "All";
            }

            return "";
        }
    }
}