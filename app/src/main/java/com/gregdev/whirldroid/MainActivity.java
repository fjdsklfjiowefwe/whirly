package com.gregdev.whirldroid;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.support.v7.app.ActionBarDrawerToggle;
import android.widget.Spinner;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mTitle;
    private Spinner spinner;

    public final int SEARCH_FORUMS = 0;
    public final int SEARCH_THREADS = 1;

    private int     currentSearchType;
    private int     searchForum = -1;
    private int     searchGroup = -1;
    private String  searchQuery = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(Whirldroid.getCurrentTheme());

        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        spinner = (Spinner) findViewById(R.id.spinner);
        setSupportActionBar(myToolbar);

        mTitle          = getTitle();
        mDrawerLayout   = (DrawerLayout  ) findViewById(R.id.drawer_layout);
        mNavigationView = (NavigationView) findViewById(R.id.vNavigation);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                // Closing drawer on item click
                mDrawerLayout.closeDrawers();
                Bundle bundle;

                switch (item.getItemId()) {
                    case R.id.drawer_item_forums:
                        switchFragment("ForumList", true);
                        break;

                    case R.id.drawer_item_news:
                        switchFragment("NewsList", true);
                        break;

                    case R.id.drawer_item_whims:
                        switchFragment("WhimList", true);
                        break;

                    case R.id.drawer_item_recent:
                        bundle = new Bundle();
                        bundle.putInt("forum_id", WhirlpoolApi.RECENT_THREADS);
                        switchFragment("ThreadList", true, bundle);
                        break;

                    case R.id.drawer_item_watched:
                        switchFragment("WatchedThreads", true);
                        break;

                    case R.id.drawer_item_popular:
                        bundle = new Bundle();
                        bundle.putInt("forum_id", WhirlpoolApi.POPULAR_THREADS);
                        switchFragment("ThreadList", true, bundle);
                        break;

                    case R.id.drawer_item_settings:
                        switchFragment("Settings", true);
                        break;

                    case R.id.drawer_item_about:
                        switchFragment("About", true);
                        break;

                    case R.id.drawer_item_feedback:
                        final Intent email_intent = new Intent(android.content.Intent.ACTION_SEND);

                        // add email data to the intent
                        email_intent.setType("plain/text");
                        email_intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"greg@gregdev.com.au"});
                        email_intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Whirldroid 2 feedback");
                        email_intent.putExtra(android.content.Intent.EXTRA_TEXT, "");

                        startActivity(Intent.createChooser(email_intent, "Send mail..."));
                        break;

                }

                return false;
            }
        });

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                //getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                //getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // choose which fragment to display initially
        if (Whirldroid.getApi().getApiKey() == null) {
            switchFragment("Login", false);

        } else {
            Bundle bundle;

            switch (PreferenceManager.getDefaultSharedPreferences(Whirldroid.getContext()).getString("pref_homepage", "ForumList")) {
                case "NewsList":
                    switchFragment("NewsList", false);
                    break;
                case "WhimList":
                    switchFragment("WhimList", false);
                    break;
                case "RecentThreads":
                    bundle = new Bundle();
                    bundle.putInt("forum_id", WhirlpoolApi.RECENT_THREADS);
                    switchFragment("ThreadList", false, bundle);
                    break;
                case "WatchedThreads":
                    switchFragment("WatchedThreads", false);
                    break;
                case "PopularThreads":
                    bundle = new Bundle();
                    bundle.putInt("forum_id", WhirlpoolApi.POPULAR_THREADS);
                    switchFragment("ThreadList", true, bundle);
                    break;
                case "ForumList":
                default:
                    switchFragment("ForumList", false);
                    break;
            }
        }

        Whirldroid.updateAlarm();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getIntent().getAction().equals("com.gregdev.whirldroid.notification")) {
            Bundle bundle = getIntent().getExtras();

            switch (bundle.getInt("notification")) {
                case Whirldroid.NEW_WATCHED_NOTIFICATION_ID:
                    switchFragment("WatchedThreads", true);
                    break;

                case Whirldroid.NEW_WHIM_NOTIFICATION_ID:
                    switchFragment("WhimList", true);
                    break;
            }
        }
    }

    @Override
    protected void onNewIntent(final Intent intent) {
        super.onNewIntent(intent);

        try {
            if (intent.getScheme().equals("whirldroid-thread")) {
                int thread_id = Integer.parseInt(intent.getData().getQueryParameter("threadid"));

                Bundle bundle = new Bundle();
                bundle.putInt("thread_id", thread_id);
                bundle.putString("thread_title", null);
                bundle.putInt("page_number", 1);
                bundle.putBoolean("bottom", false);
                bundle.putInt("goto_num", 0);

                switchFragment("ThreadView", true, bundle);
            }

        } catch (NullPointerException e) { }

        try {
            if (intent.getAction().equals("com.gregdev.whirldroid.notification")) {
                setIntent(intent);
            }

        } catch (NullPointerException e) { }
    }

    public void switchFragment(String fragmentName, boolean addToBackStack) {
        switchFragment(fragmentName, addToBackStack, null);
    }

    public void switchFragment(String fragmentName, boolean addToBackStack, Bundle bundle) {
        Fragment fragment;

        try {
            fragment = (Fragment) Class.forName("com.gregdev.whirldroid.fragment." + fragmentName + "Fragment").newInstance();

            if (bundle != null) {
                fragment.setArguments(bundle);
            }

            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.content_frame, fragment);

            if (addToBackStack) {
                transaction.addToBackStack(null);
            }

            transaction.commit();

        } catch (ClassNotFoundException e) {
            Whirldroid.log("Fragment " + fragmentName + " not found");
        } catch (InstantiationException e) {
            Whirldroid.log("Error instantiating fragment " + fragmentName);
        } catch (IllegalAccessException e) {
            Whirldroid.log("Illegal access to fragment " + fragmentName);
        }
    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        return super.onOptionsItemSelected(item);
    }

    public void setCurrentSearchType(int type) {
        setCurrentSearchType(type, -1, -1);
    }

    public void setCurrentSearchType(int type, int forumId, int groupId) {
        currentSearchType = type;
        searchForum = forumId;
        searchGroup = groupId;
    }

    public boolean onQueryTextSubmit(String query) {
        searchQuery = query;

        switch (currentSearchType) {
            case SEARCH_FORUMS:
                searchForum = -1;
                searchGroup = -1;

                Bundle bundle = new Bundle();
                bundle.putInt("forum_id", WhirlpoolApi.SEARCH_RESULTS);
                bundle.putString("search_query", searchQuery);
                bundle.putInt("search_forum", searchForum);
                bundle.putInt("search_group", searchGroup);

                switchFragment("ThreadList", true, bundle);

                return true;

            case SEARCH_THREADS:
                Intent search_intent;
                getSupportActionBar().setDisplayShowCustomEnabled(false);

                // private forums can't be searched, so open the browser
                if (!WhirlpoolApi.isPublicForum(searchForum)) {
                    String search_url = WhirlpoolApi.buildSearchUrl(searchForum, searchGroup, query);
                    search_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(search_url));
                    startActivity(search_intent);

                } else {

                    Bundle search_forum_bundle = new Bundle();
                    search_forum_bundle.putInt("forum_id", WhirlpoolApi.SEARCH_RESULTS);
                    search_forum_bundle.putString("search_query", query);
                    search_forum_bundle.putInt("search_forum", searchForum);
                    search_forum_bundle.putInt("search_group", searchGroup);

                    switchFragment("ForumPage", true, search_forum_bundle);
                }

                return true;
        }

        return false;
    }

    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
            mDrawerLayout.closeDrawer(mNavigationView);
        } else if (fragmentManager.getBackStackEntryCount() != 0) {
            fragmentManager.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    public boolean selectMenuItem(String item) {
        switch (item) {
            case "NewsList":
                mNavigationView.setCheckedItem(R.id.drawer_item_news);
                return true;

            case "WhimList":
                mNavigationView.setCheckedItem(R.id.drawer_item_whims);
                return true;

            case "RecentList":
                mNavigationView.setCheckedItem(R.id.drawer_item_recent);
                return true;

            case "WatchedThreads":
                mNavigationView.setCheckedItem(R.id.drawer_item_watched);
                return true;

            case "PopularList":
                mNavigationView.setCheckedItem(R.id.drawer_item_popular);
                return true;

            case "ForumList":
                mNavigationView.setCheckedItem(R.id.drawer_item_forums);
                return true;

            case "Settings":
                mNavigationView.setCheckedItem(R.id.drawer_item_settings);
                return true;

            case "About":
                mNavigationView.setCheckedItem(R.id.drawer_item_about);
                return true;
        }

        return false;
    }

    public void resetActionBar() {
        getSupportActionBar().setSubtitle("");
        getSupportActionBar().setDisplayShowTitleEnabled(true);
        spinner.setVisibility(View.GONE);
    }
}