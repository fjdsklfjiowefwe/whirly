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
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.WhirlpoolApi;
import com.gregdev.whirldroid.WhirlpoolApiException;

import java.util.HashMap;
import java.util.Map;

public class ThreadListFragment extends Fragment {

    private ViewPager viewPager;
    private Tracker mTracker;
    private int currentIndex;
    private int pageCount = 0;
    private MenuBuilder menuBuilder;

    private int forumId;
    private Boolean hideRead;

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
        View rootView = inflater.inflate(R.layout.view_pager, container, false);

        forumId     = getArguments().getInt("forum_id");
        hideRead    = getArguments().getBoolean("hide_read");

        viewPager = (ViewPager) rootView.findViewById(R.id.pager);
        viewPager.setAdapter(new ForumPageFragmentPagerAdapter());
        viewPager.setOffscreenPageLimit(3);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                currentIndex = position;

                //((MainActivity) getActivity()).getSupportActionBar().setSubtitle("Page " + (currentIndex + 1) + " of " + pageCount);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

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

        getActivity().getMenuInflater().inflate(R.menu.thread_list, menuBuilder);
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity mainActivity = ((MainActivity) getActivity());
        mainActivity.resetActionBar();

        mTracker.setScreenName("ThreadList");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
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
        private boolean doneInitialPage = false;

        public ForumPageFragmentPagerAdapter() {
            super(getChildFragmentManager());
            pages = new HashMap<>();
        }

        public void setCount(int count) {
            if (count != pageCount) { // count has changed, let's do some things
                pageCount = count;
                notifyDataSetChanged();
                ((MainActivity) getActivity()).getSupportActionBar().setSubtitle("Page " + (viewPager.getCurrentItem() + 1) + " of " + pageCount);
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
                ((ForumPageFragment) ((ForumPageFragmentPagerAdapter) viewPager.getAdapter()).getItem(viewPager.getCurrentItem())).getThreads(false);
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

}