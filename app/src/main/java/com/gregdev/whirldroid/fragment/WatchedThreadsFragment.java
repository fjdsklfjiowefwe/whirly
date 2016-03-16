package com.gregdev.whirldroid.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.gregdev.whirldroid.FragmentTabHost;

import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.WhirlpoolApi;

public class WatchedThreadsFragment extends Fragment {
    private FragmentTabHost mTabHost;

    //Mandatory Constructor
    public WatchedThreadsFragment() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_tabs,container, false);


        mTabHost = (FragmentTabHost) rootView.findViewById(android.R.id.tabhost);
        mTabHost.setup(getActivity(), getChildFragmentManager(), R.id.realtabcontent);

        Bundle unreadBundle = new Bundle();
        unreadBundle.putInt("forum_id", WhirlpoolApi.WATCHED_THREADS);
        unreadBundle.putBoolean("hide_read", true);

        Bundle allBundle = new Bundle();
        allBundle.putInt("forum_id", WhirlpoolApi.WATCHED_THREADS);

        mTabHost.addTab(mTabHost.newTabSpec("unread").setIndicator("Unread"),
                ThreadListFragment.class, unreadBundle);
        mTabHost.addTab(mTabHost.newTabSpec("read").setIndicator("All"),
                ThreadListFragment.class, allBundle);


        return rootView;
    }
}