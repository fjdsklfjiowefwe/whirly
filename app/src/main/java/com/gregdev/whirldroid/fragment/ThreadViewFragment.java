package com.gregdev.whirldroid.fragment;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.ActionMenuView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.WhirlpoolApi;
import com.gregdev.whirldroid.WhirlpoolApiException;
import com.gregdev.whirldroid.layout.TwoLineSpinnerAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ThreadViewFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private ViewPager viewPager;
    private Tracker mTracker;
    private int fromForum;
    private int threadId;
    private int initialPage;
    private int currentIndex;
    private int pageCount = 0;
    private int gotoNum = 0;
    private boolean gotoBottom = false;
    private String threadTitle = null;
    private MenuBuilder menuBuilder;
    private Spinner filterSpinner;
    private TwoLineSpinnerAdapter filterAdapter;

    private int currentFilter = 0;

    ArrayList<String> filterList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Obtain the shared Tracker instance.
        Whirldroid application = (Whirldroid) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        container.removeAllViews();
        View rootView = inflater.inflate(R.layout.view_pager, container, false);

        fromForum   = getArguments().getInt("from_forum");
        threadId    = getArguments().getInt("thread_id");
        initialPage = getArguments().getInt("page_number");
        pageCount   = getArguments().getInt("page_count");
        gotoNum     = getArguments().getInt("goto_num");
        gotoBottom = getArguments().getBoolean("bottom");
        threadTitle = getArguments().getString("thread_title");

        viewPager = (ViewPager) rootView.findViewById(R.id.pager);
        viewPager.setAdapter(new ThreadPageFragmentPagerAdapter());
        viewPager.setOffscreenPageLimit(3);

        filterList.clear();

        if (threadTitle != null){
            filterList.add(threadTitle);
        } else {
            filterList.add("Thread");
        }

        filterList.add("Posts by me");
        filterList.add("Posts by moderators");
        filterList.add("Posts by reps");

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                currentIndex = position;
                setFilterAdapter();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        if (initialPage == -1) {
            initialPage = viewPager.getAdapter().getCount();
        }
        viewPager.setCurrentItem(initialPage - 1);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        ActionMenuView actionMenuView = (ActionMenuView) view.findViewById(R.id.menuBar);
        menuBuilder = (MenuBuilder) actionMenuView.getMenu();

        menuBuilder.setCallback(new MenuBuilder.Callback() {
            @Override
            public boolean onMenuItemSelected(MenuBuilder menuBuilder, MenuItem menuItem) {
                return onOptionsItemSelected(menuItem);
            }

            @Override
            public void onMenuModeChange(MenuBuilder menuBuilder) {

            }
        });

        getActivity().getMenuInflater().inflate(R.menu.thread, menuBuilder);

        if (fromForum == WhirlpoolApi.ALL_WATCHED_THREADS || fromForum == WhirlpoolApi.UNREAD_WATCHED_THREADS) {
            menuBuilder.findItem(R.id.menu_watch).setVisible(false);
            menuBuilder.findItem(R.id.menu_markread).setVisible(true);
            menuBuilder.findItem(R.id.menu_unwatch).setVisible(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        setFilterAdapter();

        mTracker.setScreenName("ThreadView");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public class ThreadPageFragmentPagerAdapter extends FragmentStatePagerAdapter {
        private Map<Integer, Fragment> pages;
        private boolean doneInitialPage = false;

        public ThreadPageFragmentPagerAdapter() {
            super(getChildFragmentManager());
            pages = new HashMap<>();
        }

        public void setCount(int count) {
            if (count != pageCount) { // count has changed, let's do some things
                pageCount = count;
                notifyDataSetChanged();
                filterAdapter.setSubtitleValue("Page " + (viewPager.getCurrentItem() + 1) + " of " + pageCount);
                filterAdapter.refreshSubtitle();
            }
        }

        @Override
        public int getCount() {
            if (pageCount == 0) {
                // If the page count is 0, it means we don't know how many pages are in the thread.
                // Default to one page so we load the first, which allows us to determine the real count.
                return 1;
            }

            return pageCount;
        }

        @Override
        public Fragment getItem(int position) {
            if (pages.get(position + 1) == null) {
                Bundle bundle = new Bundle();

                bundle.putString("thread_title", threadTitle);
                bundle.putInt("thread_id"       , threadId);
                bundle.putInt("page_number"     , position + 1);
                bundle.putInt("page_count"      , pageCount);
                bundle.putInt("filter", currentFilter);

                if (!doneInitialPage && (position + 1) == initialPage) {
                    bundle.putInt("goto_num", gotoNum);
                    bundle.putBoolean("bottom", gotoBottom);
                    doneInitialPage = true;
                }

                Fragment fragment = new ThreadPageFragment();
                fragment.setArguments(bundle);

                pages.put(position + 1, fragment);
            }

            return pages.get(position + 1);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                ((ThreadPageFragment) ((ThreadPageFragmentPagerAdapter) viewPager.getAdapter()).getItem(viewPager.getCurrentItem())).initiateRefresh();
                return true;

            case R.id.menu_next:
                if (viewPager.getCurrentItem() < viewPager.getAdapter().getCount()) {
                    viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                }
                return true;

			case R.id.menu_goto_page:
				final CharSequence[] pages = new CharSequence[pageCount];
				for (int i = 0; i < pages.length; i++) {
					pages[i] = "" + (i + 1);
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				builder.setTitle("Jump to page...");
				builder.setItems(pages, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						viewPager.setCurrentItem(Integer.parseInt((String) pages[item]) - 1);
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
				return true;

            case R.id.menu_markread:
                try {
                    WatchedThreadTask markread_task = new WatchedThreadTask(WhirlpoolApi.WATCHMODE_UNREAD, threadId, 0, 0);
                    markread_task.execute();
                    Toast.makeText(getActivity(), "Marking thread as read", Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    Toast.makeText(getActivity(), "Error marking thread as read", Toast.LENGTH_SHORT).show();
                }

                return true;

            case R.id.menu_open_browser:
                String thread_url = "http://forums.whirlpool.net.au/forum-replies.cfm?t=" + threadId;
                Intent thread_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(thread_url));
                startActivity(thread_intent);
                return true;

            case R.id.menu_prev:
                viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                return true;

            case R.id.menu_goto_last:
                viewPager.setCurrentItem(viewPager.getAdapter().getCount() - 1);
                return true;

            case R.id.menu_watch:
                WatchedThreadTask watch_task = new WatchedThreadTask(WhirlpoolApi.WATCHMODE_ALL, 0, 0, threadId);
                watch_task.execute();
                Toast.makeText(getActivity(), "Adding thread to watch list", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.menu_unwatch:
                WatchedThreadTask unwatch_task = new WatchedThreadTask(WhirlpoolApi.WATCHMODE_ALL, 0, threadId, 0);
                unwatch_task.execute();
                Toast.makeText(getActivity(), "Removing thread from watch list", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.menu_replythread:
                String replythread_url = WhirlpoolApi.REPLY_URL + threadId;
                Intent replythread_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(replythread_url));

                if (Build.VERSION.SDK_INT >= 18) {
                    final String EXTRA_CUSTOM_TABS_SESSION = "android.support.customtabs.extra.SESSION";
                    final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR";

                    Bundle extras = new Bundle();
                    extras.putBinder(EXTRA_CUSTOM_TABS_SESSION, null);
                    replythread_intent.putExtras(extras);
                    replythread_intent.putExtra(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR, Color.parseColor("#3A437B"));
                }

                startActivity(replythread_intent);
                return true;
        }
        return false;
    }

    private class WatchedThreadTask extends AsyncTask<String, Void, Void> {

        private int mark_as_read = 0;
        private int unwatch = 0;
        public int watch = 0;
        public int mode = 0;

        public WatchedThreadTask(int mode, int mark_as_read, int unwatch, int watch) {
            this.mark_as_read = mark_as_read;
            this.unwatch = unwatch;
            this.watch = watch;
            this.mode = mode;
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                Whirldroid.getApi().downloadWatched(mode, mark_as_read + "", unwatch + "", watch);
            }
            catch (final WhirlpoolApiException e) {
                return null;
            }
            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {

        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int itemPosition, long itemId) {
        try {
            if (itemPosition == currentFilter) { // selected item didn't change
                return;

            } else {
                currentFilter = itemPosition;
                viewPager.setAdapter(null);
                viewPager.setAdapter(new ThreadPageFragmentPagerAdapter());
            }

        } catch (NullPointerException e) { }
    }

    public void onNothingSelected (AdapterView<?> parent) { }

    public void setFilterUser(String userName, String userId) {
        Bundle bundle = new Bundle();

        bundle.putString("thread_title"     , threadTitle);
        bundle.putInt("thread_id", threadId);
        bundle.putInt("page_number"         , 1);
        bundle.putInt("page_count"          , 1);
        bundle.putString("filter_user_id"   , userId);
        bundle.putString("filter_user"      , userName);

        ((MainActivity) getActivity()).switchFragment("ThreadPage", true, bundle);
    }

    private void setFilterAdapter() {
        MainActivity mainActivity = ((MainActivity) getActivity());

        String subtitle = "";

        if (pageCount != 0) {
            subtitle = "Page " + (currentIndex + 1) + " of " + pageCount;
        }

        filterAdapter = new TwoLineSpinnerAdapter(getActivity(), R.layout.spinner_item, filterList, subtitle, "All posts in thread");
        filterAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

        filterSpinner = (Spinner) getActivity().findViewById(R.id.spinner);
        filterSpinner.setAdapter(filterAdapter);
        filterSpinner.setVisibility(View.VISIBLE);
        filterSpinner.setOnItemSelectedListener(this);

        mainActivity.getSupportActionBar().setDisplayShowTitleEnabled(false);
    }

}