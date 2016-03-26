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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
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

public class ThreadViewFragment extends Fragment {

    private ViewPager viewPager;
    private Tracker mTracker;
    private int fromForum;
    private int threadId;
    private int pageNumber;
    private int currentIndex;
    private int pageCount = 0;
    private String threadTitle = null;
    private MenuBuilder menuBuilder;

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

        fromForum   = getArguments().getInt("from_forum");
        threadId    = getArguments().getInt("thread_id");
        pageNumber  = getArguments().getInt("page_number");
        pageCount   = getArguments().getInt("page_count");
        threadTitle = getArguments().getString("thread_title");

        viewPager = (ViewPager) rootView.findViewById(R.id.pager);
        viewPager.setAdapter(new ThreadPageFragmentPagerAdapter());
        viewPager.setOffscreenPageLimit(3);

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                currentIndex = position;

                ((MainActivity) getActivity()).getSupportActionBar().setSubtitle("Page " + (currentIndex + 1) + " of " + pageCount);

                if (currentIndex == 0) {
                    menuBuilder.findItem(R.id.menu_prev).setEnabled(false);
                } else {
                    menuBuilder.findItem(R.id.menu_prev).setEnabled(true);
                }
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

        getActivity().getMenuInflater().inflate(R.menu.thread, menuBuilder);

        if (fromForum == WhirlpoolApi.WATCHED_THREADS) {
            menuBuilder.findItem(R.id.menu_watch).setVisible(false);
            menuBuilder.findItem(R.id.menu_markread).setVisible(true);
            menuBuilder.findItem(R.id.menu_unwatch).setVisible(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        MainActivity mainActivity = ((MainActivity) getActivity());
        mainActivity.resetActionBar();

        if (threadTitle != null){
            mainActivity.setTitle(threadTitle);
        } else {
            mainActivity.setTitle("Thread");
        }

        if (pageCount != 0) {
            mainActivity.getSupportActionBar().setSubtitle("Page " + (currentIndex + 1) + " of " + pageCount);
        }

        mTracker.setScreenName("ThreadView");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    public class ThreadPageFragmentPagerAdapter extends FragmentStatePagerAdapter {
        private Map<Integer, Fragment> pages;

        public ThreadPageFragmentPagerAdapter() {
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

                bundle.putString("thread_title" , threadTitle);
                bundle.putInt("thread_id"       , threadId);
                bundle.putInt("page_number"     , position + 1);
                bundle.putInt("page_count"      , pageCount);

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
                ((ThreadPageFragment) ((ThreadPageFragmentPagerAdapter) viewPager.getAdapter()).getItem(viewPager.getCurrentItem())).getThread();
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
                    WatchedThreadTask markread_task = new WatchedThreadTask(threadId, 0, 0);
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
                WatchedThreadTask watch_task = new WatchedThreadTask(0, 0, threadId);
                watch_task.execute();
                Toast.makeText(getActivity(), "Adding thread to watch list", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.menu_unwatch:
                WatchedThreadTask unwatch_task = new WatchedThreadTask(0, threadId, 0);
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

        public WatchedThreadTask(int mark_as_read, int unwatch, int watch) {
            this.mark_as_read = mark_as_read;
            this.unwatch = unwatch;
            this.watch = watch;
        }

        @Override
        protected Void doInBackground(String... params) {
            try {
                Whirldroid.getApi().downloadWatched(mark_as_read, unwatch, watch);
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