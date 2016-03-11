package com.gregdev.whirldroid;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.support.v7.app.ActionBarDrawerToggle;

import com.gregdev.whirldroid.fragments.ForumListFragment;
import com.gregdev.whirldroid.layout.IconArrayAdapter;
import com.gregdev.whirldroid.fragments.LoginFragment;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private CharSequence mDrawerTitle;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mTitle;

    private ArrayList<Pair<String, Integer>> menuItems = new ArrayList<Pair<String, Integer>>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTitle = mDrawerTitle = getTitle();
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.string.drawer_open, R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        menuItems.add(Pair.create("Industry News"   , R.drawable.ic_newspaper_black_24dp));
        menuItems.add(Pair.create("Whims"           , R.drawable.ic_chat_black_24dp));
        menuItems.add(Pair.create("Recent Threads"  , R.drawable.ic_today_black_24dp));
        menuItems.add(Pair.create("Watched Threads" , R.drawable.ic_visibility_black_24dp));
        menuItems.add(Pair.create("Popular Threads" , R.drawable.ic_show_chart_black_24dp));
        menuItems.add(Pair.create("Forums"          , R.drawable.ic_question_answer_black_24dp));

        // Set the adapter for the list view
        mDrawerList.setAdapter(new IconArrayAdapter(this, menuItems, 80));
        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());


        // choose which fragment to display initially
        if (Whirldroid.getApi().getApiKey() == null) {
            switchFragment("Login");

        } else {
            switchFragment("ForumList");
        }

    }

    public void switchFragment(String fragmentName) {
        Fragment fragment;

        try {
            fragment = (Fragment) Class.forName("com.gregdev.whirldroid.fragments." + fragmentName + "Fragment").newInstance();
            FragmentManager fragmentManager = getFragmentManager();
            fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

        } catch (ClassNotFoundException e) {
            Whirldroid.log("Fragment " + fragmentName + " not found");
        } catch (InstantiationException e) {
            Whirldroid.log("Error instantiating fragment " + fragmentName);
        } catch (IllegalAccessException e) {
            Whirldroid.log("Illegal access to fragment " + fragmentName);
        }
    }

    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        // update the main content by replacing fragments
        Fragment fragment = new ForumListFragment();

        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().replace(R.id.content_frame, fragment).commit();

        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle("Forums");
        mDrawerLayout.closeDrawer(mDrawerList);
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

    public boolean onQueryTextSubmit(String query) {
        Intent search_intent;

        // private forums can't be searched, so open the browser
        /*if (!WhirlpoolApi.isPublicForum(forum_id)) {
            String search_url = WhirlpoolApi.buildSearchUrl(forum_id, -1, query);
            search_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(search_url));
        }
        else {
            search_intent = new Intent(this, ThreadList.class);

            Bundle bundle = new Bundle();
            bundle.putInt("forum_id", WhirlpoolApi.SEARCH_RESULTS);
            bundle.putString("search_query", query);
            bundle.putInt("search_forum", forum_id);
            bundle.putInt("search_group", -1);

            search_intent.putExtras(bundle);
        }

        startActivity(search_intent);*/

        return true;
    }

    public boolean onQueryTextChange(String newText) {
        return false;
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getFragmentManager();

        if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
            mDrawerLayout.closeDrawer(mDrawerList);
        } else if (fragmentManager.getBackStackEntryCount() != 0) {
            fragmentManager.popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}