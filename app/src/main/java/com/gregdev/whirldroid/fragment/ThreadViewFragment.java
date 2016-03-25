package com.gregdev.whirldroid.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreadViewFragment extends Fragment {

    private Tracker mTracker;
    private int threadId;
    private int pageNumber;
    private int pageCount;
    private String threadTitle;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Obtain the shared Tracker instance.
        Whirldroid application = (Whirldroid) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.view_pager, container, false);

        threadId = getArguments().getInt("thread_id");
        pageNumber = getArguments().getInt("page_number");
        pageCount = getArguments().getInt("page_count");
        threadTitle = getArguments().getString("thread_title");

        ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.pager);
        viewPager.setAdapter(new ThreadPageFragmentPagerAdapter());
        viewPager.setOffscreenPageLimit(3);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {

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

        mTracker.setScreenName("ThreadView");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public class ThreadPageFragmentPagerAdapter extends FragmentStatePagerAdapter {
        private Map<Integer, Fragment> pages;

        public ThreadPageFragmentPagerAdapter() {
            super(getChildFragmentManager());
            pages = new HashMap<>();
        }

        @Override
        public int getCount() {
            return pageCount;
        }

        @Override
        public Fragment getItem(int position) {
            if (pages.get(position + 1) == null) {
                Bundle bundle = new Bundle();

                bundle.putInt("thread_id", threadId);
                bundle.putString("thread_title", threadTitle);
                bundle.putInt("page_number", position + 1);

                Fragment fragment = new ThreadPageFragment();
                fragment.setArguments(bundle);

                pages.put(position + 1, fragment);
            }

            return pages.get(position + 1);
        }
    }
}