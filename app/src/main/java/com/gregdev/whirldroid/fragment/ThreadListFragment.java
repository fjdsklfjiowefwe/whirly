package com.gregdev.whirldroid.fragment;

import android.content.Context;
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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.WhirlpoolApi;
import com.gregdev.whirldroid.WhirlpoolApiException;
import com.gregdev.whirldroid.layout.TwoLineSpinnerAdapter;
import com.gregdev.whirldroid.model.Forum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreadListFragment extends Fragment implements AdapterView.OnItemSelectedListener {

    private ViewPager viewPager;
    private Tracker mTracker;
    private int currentGroup = 0;
    private int currentIndex = 0;
    private int pageCount = 0;
    private MenuBuilder menuBuilder;
    private TwoLineSpinnerAdapter groupAdapter;
    Spinner spinner;
    View rootView;

    private Forum forum;
    private int forumId;
    private String forumTitle;
    private Boolean hideRead;

    private int searchForum = -1;
    private int searchGroup = -1;
    private String searchQuery;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Obtain the shared Tracker instance.
        Whirldroid application = (Whirldroid) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.view_pager, container, false);

        forumId     = getArguments().getInt("forum_id");
        hideRead    = getArguments().getBoolean("hide_read");
        forumTitle  = getArguments().getString("forum_name");

        viewPager = (ViewPager) rootView.findViewById(R.id.pager);
        viewPager.setAdapter(new ForumPageFragmentPagerAdapter());
        viewPager.setOffscreenPageLimit(1);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                currentIndex = position;
                ((ForumPageFragmentPagerAdapter) viewPager.getAdapter()).setHeader(forum);

                String subtitle = "Page " + (currentIndex + 1);
                if (forum != null) {
                    subtitle += " of " + forum.getPageCount();
                }

                ((MainActivity) getActivity()).setTwoLineSubtitle(subtitle);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        if (WhirlpoolApi.isActualForum(forumId)) {
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

            getActivity().getMenuInflater().inflate(R.menu.thread_list, menuBuilder);

            // private forums don't have pages, so hide pagination
            if (WhirlpoolApi.isActualForum(forumId) && !WhirlpoolApi.isPublicForum(forumId)) {
                menuBuilder.findItem(R.id.menu_prev).setVisible(false);
                menuBuilder.findItem(R.id.menu_next).setVisible(false);
                menuBuilder.findItem(R.id.menu_goto_page).setVisible(false);
                menuBuilder.findItem(R.id.menu_open_browser).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity mainActivity = ((MainActivity) getActivity());
        mainActivity.resetActionBar();

        if (forumId != WhirlpoolApi.UNREAD_WATCHED_THREADS && forumId != WhirlpoolApi.ALL_WATCHED_THREADS) {
            if (forumId == WhirlpoolApi.POPULAR_THREADS) {
                mTracker.setScreenName("PopularThreads");
            } else if (forumId == WhirlpoolApi.RECENT_THREADS) {
                mTracker.setScreenName("RecentThreads");
            } else {
                mTracker.setScreenName("ThreadList");
            }

            mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        }

        mainActivity.resetActionBar();

        Bundle bundle = getArguments();

        switch(forumId) {
            case WhirlpoolApi.UNREAD_WATCHED_THREADS:
            case WhirlpoolApi.ALL_WATCHED_THREADS:
                getActivity().setTitle("Watched Threads");
                break;

            case WhirlpoolApi.RECENT_THREADS:
                getActivity().setTitle("Recent Threads");
                mainActivity.selectMenuItem("RecentList");
                break;

            case WhirlpoolApi.POPULAR_THREADS:
                getActivity().setTitle("Popular Threads");
                mainActivity.selectMenuItem("PopularList");
                break;

            case WhirlpoolApi.SEARCH_RESULTS:
                getActivity().setTitle("Search Results");
                searchQuery = bundle.getString("search_query");
                searchForum = bundle.getInt("search_forum");
                searchGroup = bundle.getInt("search_group");
                mainActivity.getSupportActionBar().setSubtitle("\"" + searchQuery + "\"");
                break;

            default:
                if (WhirlpoolApi.isActualForum(forumId) && WhirlpoolApi.isPublicForum(forumId)) {

                    String subtitle = "Page " + (currentIndex + 1);
                    if (forum != null) {
                        subtitle += " of " + forum.getPageCount();
                    }

                    ArrayList<String> group_list = new ArrayList<>();
                    group_list.add(bundle.getString("forum_name"));
                    groupAdapter = new TwoLineSpinnerAdapter(getActivity(), R.layout.spinner_item, group_list);
                    groupAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);

                    ((MainActivity) getActivity()).showTwoLineSpinner();
                    ((MainActivity) getActivity()).setTwoLineSubtitle(subtitle);

                    spinner = (Spinner) getActivity().findViewById(R.id.spinner);
                    spinner.setAdapter(groupAdapter);

                    spinner.setOnItemSelectedListener(this);

                } else if (!WhirlpoolApi.isPublicForum(forumId)) {
                    getActivity().setTitle(forumTitle);
                }
        }

        mainActivity.setCurrentSearchType(mainActivity.SEARCH_THREADS, forumId, currentGroup);

        if (forum != null && forumId != WhirlpoolApi.SEARCH_RESULTS) {
            ((ForumPageFragmentPagerAdapter) viewPager.getAdapter()).setHeader(forum);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (WhirlpoolApi.isActualForum(forumId)) {
            //Create the search view
            SearchView search_view = new SearchView(((MainActivity) getActivity()).getSupportActionBar().getThemedContext());
            search_view.setQueryHint("Search for threads…");
            search_view.setOnQueryTextListener((MainActivity) getActivity());

            menu.add("Search")
                    .setIcon(R.drawable.ic_search_white_24dp)
                    .setActionView(search_view)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        } else if (forumId != WhirlpoolApi.ALL_WATCHED_THREADS && forumId != WhirlpoolApi.UNREAD_WATCHED_THREADS) {
            inflater.inflate(R.menu.refresh, menu);
        }
    }

    public class ForumPageFragmentPagerAdapter extends FragmentStatePagerAdapter {
        private Map<Integer, Fragment> pages;

        public ForumPageFragmentPagerAdapter() {
            super(getChildFragmentManager());
            pages = new HashMap<>();

            if (forum != null) {
                setHeader(forum);
            }
        }

        public Forum getHeaderForum() {
            return forum;
        }

        public void setHeader(Forum f) {
            try {
                forum = f;

                if (forum.getGroups() != null) {
                    if (groupAdapter.getCount() == 1) {
                        int currentGroupIndex = 0;
                        int i = 0;

                        for (Map.Entry<String, Integer> group : forum.getGroups().entrySet()) {
                            groupAdapter.add(group.getKey());

                            if (group.getValue() == currentGroup) {
                                currentGroupIndex = i;
                            }

                            i++;
                        }

                        if (currentGroup != 0) {
                            try {
                                spinner.setSelection(currentGroupIndex);
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            } catch (NullPointerException e) { }
        }

        public void setCount(int count) {
            if (count != pageCount) { // count has changed, let's do some things
                pageCount = count;
                notifyDataSetChanged();

                String subtitle = "Page 1";
                if (forum != null) {
                    subtitle += " of " + pageCount;
                }

                ((MainActivity) getActivity()).setTwoLineSubtitle(subtitle);
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

                bundle.putInt    ("forum_id", forumId       );
                bundle.putInt    ("page"    , position + 1  );
                bundle.putInt    ("group"   , currentGroup  );
                bundle.putBoolean("hideRead", hideRead      );

                bundle.putInt   ("search_forum", searchForum);
                bundle.putInt   ("search_group", searchGroup);
                bundle.putString("search_query", searchQuery);

                Fragment fragment = new ForumPageFragment();
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
                initiateRefresh();
                return true;

            case R.id.menu_prev:
                viewPager.setCurrentItem(viewPager.getCurrentItem() - 1);
                return true;

            case R.id.menu_goto_page:
                final EditText input = new EditText(getActivity());
                input.setKeyListener(new DigitsKeyListener());
                new AlertDialog.Builder(getActivity())
                        .setTitle("Jump to page...")
                        .setMessage("Enter a page number to load")
                        .setView(input)
                        .setPositiveButton("Go for it", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Editable value = input.getText();
                                int input;
                                try {
                                    input = Integer.parseInt(value.toString());
                                    viewPager.setCurrentItem(input - 1);
                                }
                                catch (Exception e) { }
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                return true;

            case R.id.menu_next:
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
                return true;

            case R.id.menu_new_thread:
                Intent newthread_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(WhirlpoolApi.NEWTHREAD_URL + forumId));
                if (Build.VERSION.SDK_INT >= 18) {
                    final String EXTRA_CUSTOM_TABS_SESSION = "android.support.customtabs.extra.SESSION";
                    final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR";

                    Bundle extras = new Bundle();
                    extras.putBinder(EXTRA_CUSTOM_TABS_SESSION, null);
                    newthread_intent.putExtras(extras);
                    newthread_intent.putExtra(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR, Color.parseColor("#3A437B"));
                }

                startActivity(newthread_intent);
                return true;

            case R.id.menu_open_browser:
                Intent thread_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(WhirlpoolApi.FORUM_URL + forumId));
                startActivity(thread_intent);
                return true;
        }
        return false;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int itemPosition, long itemId) {
        try {
            if (itemPosition == 0 && currentGroup == 0) {
                return;

            } else if (itemPosition == 0) {
                currentGroup = 0;
                viewPager.setAdapter(null);
                viewPager.setAdapter(new ForumPageFragmentPagerAdapter());
                return;
            }

            int counter = 1;
            for (Map.Entry<String, Integer> group : forum.getGroups().entrySet()) {
                if (counter == itemPosition) {
                    if (currentGroup == group.getValue()) {
                        return;
                    }

                    currentGroup = group.getValue();
                    viewPager.setAdapter(null);
                    viewPager.setAdapter(new ForumPageFragmentPagerAdapter());

                    return;
                }
                counter++;
            }

        } catch (NullPointerException e) { }
    }

    public void onNothingSelected (AdapterView<?> parent) { }

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

    public void initiateRefresh() {
        try {
            ForumPageFragmentPagerAdapter adapter = (ForumPageFragmentPagerAdapter) viewPager.getAdapter();
            ((ForumPageFragment) adapter.getItem(viewPager.getCurrentItem())).initiateRefresh();
        } catch (NullPointerException e) { }
    }

}