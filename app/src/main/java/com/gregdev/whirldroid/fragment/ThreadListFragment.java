package com.gregdev.whirldroid.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;

import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Refresher;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.layout.WhirldroidSpinnerAdapter;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;
import com.gregdev.whirldroid.model.Forum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreadListFragment extends Fragment implements AdapterView.OnItemSelectedListener, Refresher {

    private ForumPageFragmentPagerAdapter adapter;
    private ViewPager viewPager;
    private int currentGroup = 0;
    private int pageCount = 0;
    private Spinner pageSpinner;
    private SpinnerAdapter groupAdapter;
    private SparseArray<Fragment> pages = new SparseArray<>();
    ArrayList<String> groups = new ArrayList<>();
    Spinner spinner;
    View rootView;
    private Boolean doneInitialSelect = false;

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
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.view_pager, container, false);

        forumId     = getArguments().getInt("forum_id");
        hideRead    = getArguments().getBoolean("hide_read");
        forumTitle  = getArguments().getString("forum_name");

        if (adapter == null) {
            adapter = new ForumPageFragmentPagerAdapter();
        }

        viewPager = (ViewPager) rootView.findViewById(R.id.pager);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(1);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                ((ForumPageFragmentPagerAdapter) viewPager.getAdapter()).setHeader(forum);

                pageSpinner.setSelection(position);
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
            Toolbar bottomToolbar = (Toolbar) view.findViewById(R.id.toolbar_bottom);

            bottomToolbar.inflateMenu(R.menu.thread_list);

            bottomToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.menu_refresh:
                            initiateRefresh();
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
            });

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        doneInitialSelect = false;

        MainActivity mainActivity = ((MainActivity) getActivity());
        mainActivity.resetActionBar();

        if (forumId != WhirlpoolApi.UNREAD_WATCHED_THREADS && forumId != WhirlpoolApi.ALL_WATCHED_THREADS) {
            if (forumId == WhirlpoolApi.POPULAR_THREADS) {
                Whirldroid.getTracker().setCurrentScreen(getActivity(), "PopularThreads", null);
                Whirldroid.logScreenView("About");
            } else if (forumId == WhirlpoolApi.RECENT_THREADS) {
                Whirldroid.getTracker().setCurrentScreen(getActivity(), "RecentThreads", null);
            } else {
                Whirldroid.getTracker().setCurrentScreen(getActivity(), "ThreadList", null);
            }
        }

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

                    groups.clear();
                    groups.add(bundle.getString("forum_name"));

                    if (groupAdapter == null) {
                        groupAdapter = new WhirldroidSpinnerAdapter(getContext(), R.layout.spinner_dropdown_item, groups, "All groups");
                    }

                    spinner = (Spinner) getActivity().findViewById(R.id.spinner);
                    spinner.setAdapter(groupAdapter);

                    if (currentGroup != 0) {
                        int currentGroupIndex = 0;
                        int i = 1;
                        spinner.setOnItemSelectedListener(null);

                        for (Map.Entry<String, Integer> group : forum.getGroups().entrySet()) {
                            if (group.getValue() == currentGroup) {
                                currentGroupIndex = i;
                            }

                            i++;
                        }

                        if (currentGroupIndex != 0) {
                            try {
                                spinner.setSelection(currentGroupIndex);
                            } catch (Exception e) { }
                        }
                    }

                    spinner.setOnItemSelectedListener(this);
                    ((MainActivity) getActivity()).showToolbarSpinner();

                } else if (!WhirlpoolApi.isPublicForum(forumId)) {
                    getActivity().setTitle(forumTitle);
                }
        }

        mainActivity.setCurrentSearchType(mainActivity.SEARCH_THREADS, forumId, currentGroup);

        if (forum != null && forumId != WhirlpoolApi.SEARCH_RESULTS) {
            ((ForumPageFragmentPagerAdapter) viewPager.getAdapter()).setHeader(forum);
        }

        Toolbar bottomToolbar = (Toolbar) rootView.findViewById(R.id.toolbar_bottom);
        pageSpinner = (Spinner) bottomToolbar.findViewById(R.id.pageList);

        if (WhirlpoolApi.isActualForum(forumId)) {
            bottomToolbar.setVisibility(View.VISIBLE);
        } else {
            bottomToolbar.setVisibility(View.INVISIBLE);
        }

        if (WhirlpoolApi.isPublicForum(forumId)) {
            pageSpinner.setVisibility(View.VISIBLE);
        } else {
            pageSpinner.setVisibility(View.INVISIBLE);
        }

        populatePageSpinner();

        pageSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewPager.setCurrentItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
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
        public ForumPageFragmentPagerAdapter() {
            super(getChildFragmentManager());

            pages.clear();

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
                        int currentGroupIndex = 1;
                        int i = 0;

                        for (Map.Entry<String, Integer> group : forum.getGroups().entrySet()) {
                            groups.add(group.getKey());

                            if (group.getValue() == currentGroup) {
                                currentGroupIndex = i;
                            }

                            i++;
                        }

                        if (currentGroup != 0) {
                            try {
                                spinner.setSelection(currentGroupIndex);
                            } catch (Exception e) { }
                        }
                    }
                }
            } catch (NullPointerException e) { }
        }

        public void setCount(int count) {
            if (count != pageCount) { // count has changed, let's do some things
                pageCount = count;
                notifyDataSetChanged();

                populatePageSpinner();
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
    public void onItemSelected(AdapterView<?> parent, View view, int itemPosition, long itemId) {
        // this method is triggered when the fragment first loads, so ignore the first instance of a selection
        if (!doneInitialSelect) {
            doneInitialSelect = true;
            return;
        }

        try {
            if (itemPosition == 0 && currentGroup == 0) { // no selection changed and we're not filtering
                return;

            } else if (itemPosition == 0) { // remove filtering
                currentGroup = 0;
                viewPager.setAdapter(null);
                viewPager.setAdapter(new ForumPageFragmentPagerAdapter());
                return;
            }

            int counter = 1;
            for (Map.Entry<String, Integer> group : forum.getGroups().entrySet()) {
                if (counter == itemPosition) {
                    // selection hasn't changed; do nothing
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

    public boolean initiateRefresh() {
        ForumPageFragmentPagerAdapter adapter = (ForumPageFragmentPagerAdapter) viewPager.getAdapter();
        ForumPageFragment fragment = (ForumPageFragment) adapter.getItem(viewPager.getCurrentItem());
        fragment.initiateRefresh();

        return true;
    }

    protected void populatePageSpinner() {
        List<String> pages = new ArrayList<>();

        // we don't know the page count yet, so just say we're on page one for now
        if (pageCount == 0) {
            pages.add("Page 1");
        }

        for (int i = 0; i < pageCount; i++) {
            int page = i + 1;
            pages.add("Page " + page + " of " + pageCount);
        }

        pageSpinner.setAdapter(new ArrayAdapter<>(pageSpinner.getContext(), R.layout.spinner_dropdown_item, pages));
        pageSpinner.setSelection(viewPager.getCurrentItem());
    }

}