package com.gregdev.whirldroid.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.WhirlpoolApi;

public class WatchedThreadsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.view_pager, container, false);
        ViewPager viewPager = (ViewPager) rootView.findViewById(R.id.pager);
        viewPager.setAdapter(new SampleFragmentPagerAdapter());

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity mainActivity = ((MainActivity) getActivity());
        mainActivity.resetActionBar();
        mainActivity.setTitle("Watched Threads");
    }

    public class SampleFragmentPagerAdapter extends FragmentPagerAdapter {
        final int PAGE_COUNT = 2;

        public SampleFragmentPagerAdapter() {
            super(getChildFragmentManager());
        }

        @Override
        public int getCount() {
            return PAGE_COUNT;
        }

        @Override
        public Fragment getItem(int position) {
            Bundle bundle = new Bundle();
            bundle.putInt("forum_id", WhirlpoolApi.WATCHED_THREADS);

            if (position == 0) {
                bundle.putBoolean("hide_read", true);
            }

            Fragment fragment = new ThreadListFragment();
            fragment.setArguments(bundle);
            return fragment;
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