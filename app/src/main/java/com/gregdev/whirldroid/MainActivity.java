package com.gregdev.whirldroid;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.support.design.widget.NavigationView;
import android.support.v4.widget.DrawerLayout;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Pair;
import android.view.MenuItem;
import android.view.View;
import android.support.v7.app.ActionBarDrawerToggle;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements SearchView.OnQueryTextListener {

    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
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
        mNavigationView = (NavigationView) findViewById(R.id.vNavigation);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                if (item.isChecked()) item.setChecked(!item.isChecked());

                //Closing drawer on item click
                mDrawerLayout.closeDrawers();

                switch (item.getItemId()) {
                    case R.id.drawer_item_forums:
                        switchFragment("ForumList", true);
                        break;

                    case R.id.drawer_item_news:
                        switchFragment("NewsList", true);
                        break;

                    case R.id.drawer_item_popular:
                        Bundle bundle = new Bundle();
                        bundle.putInt("forum_id", WhirlpoolApi.POPULAR_THREADS);

                        switchFragment("ThreadList", true, bundle);
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

        // choose which fragment to display initially
        if (Whirldroid.getApi().getApiKey() == null) {
            switchFragment("Login", false);

        } else {
            switchFragment("ForumList", false);
        }

    }

    public void switchFragment(String fragmentName, boolean addToBackStack) {
        switchFragment(fragmentName, addToBackStack, null);
    }

    public void switchFragment(String fragmentName, boolean addToBackStack, Bundle bundle) {
        Fragment fragment;

        try {
            fragment = (Fragment) Class.forName("com.gregdev.whirldroid.fragments." + fragmentName + "Fragment").newInstance();

            if (bundle != null) {
                fragment.setArguments(bundle);
            }

            FragmentTransaction transaction = getFragmentManager().beginTransaction()
                    .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                    .replace(R.id.content_frame, fragment);

            if (addToBackStack) {
                transaction.addToBackStack(null);
            }

            switch (fragmentName) {
                case "NewsList":
                    mNavigationView.getMenu().findItem(R.id.drawer_item_news).setChecked(true);
                    break;

                case "ForumList":
                    mNavigationView.getMenu().findItem(R.id.drawer_item_forums).setChecked(true);
                    break;
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

        if (mDrawerLayout.isDrawerOpen(mNavigationView)) {
            mDrawerLayout.closeDrawer(mNavigationView);
        } else if (fragmentManager.getBackStackEntryCount() != 0) {
            fragmentManager.popBackStack();
        } else {
            super.onBackPressed();
        }
    }
}