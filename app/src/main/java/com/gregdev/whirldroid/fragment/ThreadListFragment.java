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
import android.support.v7.view.SupportMenuInflater;
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
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.WhirlpoolApi;
import com.gregdev.whirldroid.WhirlpoolApiException;
import com.gregdev.whirldroid.model.Forum;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreadListFragment extends Fragment implements ActionBar.OnNavigationListener {

    private ViewPager viewPager;
    private Tracker mTracker;
    private int currentGroup = 0;
    private int currentIndex = 0;
    private int pageCount = 0;
    private MenuBuilder menuBuilder;
    private GroupAdapter groupAdapter;

    private Forum forum;
    private int forumId;
    private String forumTitle;
    private Boolean hideRead;

    private int search_forum = -1;
    private int search_group = -1;
    private String search_query;

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
        View rootView = inflater.inflate(R.layout.view_pager, container, false);

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
                search_query = bundle.getString("search_query");
                search_forum = bundle.getInt("search_forum");
                search_group = bundle.getInt("search_group");
                mainActivity.getSupportActionBar().setSubtitle("\"" + search_query + "\"");
                break;

            default:
                if (WhirlpoolApi.isActualForum(forumId) && WhirlpoolApi.isPublicForum(forumId)) {
                    getActivity().setTitle("");
                    Context context = mainActivity.getSupportActionBar().getThemedContext();

                    ArrayList<String> group_list = new ArrayList<>();
                    group_list.add(bundle.getString("forum_name") + "  ");

                    groupAdapter = new GroupAdapter(context, R.layout.spinner_item, group_list);
                    groupAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);

                    mainActivity.getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                    mainActivity.getSupportActionBar().setListNavigationCallbacks(groupAdapter, this);

                } else if (!WhirlpoolApi.isPublicForum(forumId)) {
                    getActivity().setTitle(forumTitle);
                }
        }

        mainActivity.setCurrentSearchType(mainActivity.SEARCH_THREADS, forumId);

        if (forum != null) {
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
            forum = f;

            if (forum.getGroups() != null) {
                groupAdapter.clear();
                groupAdapter.add(forumTitle + "  ");
                String currentGroupName = "";

                for (Map.Entry<String, Integer> group : forum.getGroups().entrySet()) {
                    groupAdapter.add(group.getKey());

                    if (group.getValue() == currentGroup) {
                        currentGroupName = group.getKey();
                    }
                }

                if (currentGroup != 0) {
                    ((MainActivity) getActivity()).getSupportActionBar().setSelectedNavigationItem(groupAdapter.getPosition(currentGroupName));
                }
            }
        }

        public void setCount(int count) {
            if (count != pageCount) { // count has changed, let's do some things
                pageCount = count;
                notifyDataSetChanged();
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

                bundle.putInt("forum_id"    , forumId);
                bundle.putInt("page"        , position + 1);
                bundle.putInt("group"       , currentGroup);
                bundle.putBoolean("hideRead", hideRead);

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

    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        try {
            if (itemPosition == 0 && currentGroup == 0) {
                return false;

            } else if (itemPosition == 0) {
                currentGroup = 0;
                viewPager.setAdapter(null);
                viewPager.setAdapter(new ForumPageFragmentPagerAdapter());
                return true;
            }

            int counter = 1;
            for (Map.Entry<String, Integer> group : forum.getGroups().entrySet()) {
                if (counter == itemPosition) {
                    if (currentGroup == group.getValue()) {
                        return false;
                    }

                    currentGroup = group.getValue();
                    viewPager.setAdapter(null);
                    viewPager.setAdapter(new ForumPageFragmentPagerAdapter());

                    return true;
                }
                counter++;
            }

            return false;
        } catch (NullPointerException e) {
            return false;
        }
    }

    public boolean onQueryTextSubmit(String query) {
        Intent search_intent;

        // private forums can't be searched, so open the browser
        if (!WhirlpoolApi.isPublicForum(forumId)) {
            String search_url = WhirlpoolApi.buildSearchUrl(forumId, -1, query);
            search_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(search_url));
        }
        else {
           /* search_intent = new Intent(this, ThreadList.class);

            Bundle bundle = new Bundle();
            bundle.putInt("forum_id", WhirlpoolApi.SEARCH_RESULTS);
            bundle.putString("search_query", query);
            bundle.putInt("search_forum", forum_id);
            bundle.putInt("search_group", -1);

            search_intent.putExtras(bundle);*/
        }

        //startActivity(search_intent);

        return true;
    }

    public boolean onQueryTextChange(String newText) {
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
                Whirldroid.getApi().downloadWatched(mode, mark_as_read, unwatch, watch);
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

    public class GroupAdapter extends ArrayAdapter<String> {

        List<String> group_items;
        Context context;

        public GroupAdapter(Context context, int resource, List<String> group_items) {
            super(context, resource, group_items);
            this.group_items = group_items;
            this.context = context;
        }

        @Override
        public View getView(int position, View convert_view, ViewGroup parent) {
            String group_name = group_items.get(position);

            if (convert_view == null) {
                LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convert_view = vi.inflate(R.layout.spinner_item, null);
            }
            if (group_name != null) {
                TextView title = (TextView) convert_view.findViewById(R.id.title);
                TextView subtitle = (TextView) convert_view.findViewById(R.id.subtitle);

                if (title != null) {
                    title.setText(group_name);
                }
                if (subtitle != null) {
                    String subtitle_value = "Page " + (currentIndex + 1);
                    if (forum != null) {
                        subtitle_value += " of " + forum.getPageCount();
                    }
                    subtitle.setText(subtitle_value);
                }
            }
            return convert_view;
        }
    }

    public void initiateRefresh() {
        try {
            ForumPageFragmentPagerAdapter adapter = (ForumPageFragmentPagerAdapter) viewPager.getAdapter();
            ((ForumPageFragment) adapter.getItem(viewPager.getCurrentItem())).initiateRefresh();
        } catch (NullPointerException e) { }
    }

}