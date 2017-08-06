package com.gregdev.whirldroid.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Toast;

import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.layout.WhirldroidSpinnerAdapter;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;
import com.gregdev.whirldroid.task.MarkThreadReadTask;
import com.gregdev.whirldroid.task.UnwatchThreadTask;
import com.gregdev.whirldroid.task.WatchThreadTask;
import com.gregdev.whirldroid.task.WhirldroidTask;
import com.gregdev.whirldroid.task.WhirldroidTaskOnCompletedListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThreadViewFragment extends Fragment implements AdapterView.OnItemSelectedListener, WhirldroidTaskOnCompletedListener {

    private View rootView;
    private ViewPager viewPager;
    private int fromForum;
    private int threadId;
    private int initialPage;
    private int pageCount = 0;
    private int gotoNum = 0;
    private boolean gotoBottom = false;
    private String threadTitle = null;
    private Spinner pageSpinner;
    private Spinner filterSpinner;
    private SpinnerAdapter filterAdapter;
    private ProgressDialog progressDialog;
    private boolean initialLoad = true;

    private int currentFilter = 0;

    ArrayList<String> filterList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        container.removeAllViews();
        rootView = inflater.inflate(R.layout.view_pager, container, false);

        fromForum   = getArguments().getInt("from_forum");
        threadId    = getArguments().getInt("thread_id");
        initialPage = getArguments().getInt("page_number");
        pageCount   = getArguments().getInt("page_count");
        gotoNum     = getArguments().getInt("goto_num");
        gotoBottom  = getArguments().getBoolean("bottom");
        threadTitle = getArguments().getString("thread_title");

        viewPager = (ViewPager) rootView.findViewById(R.id.pager);
        ThreadPageFragmentPagerAdapter adapter = new ThreadPageFragmentPagerAdapter();
        adapter.setScrollToReply(getArguments().getString("scroll_to_post"));
        viewPager.setAdapter(adapter);
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
                setFilterAdapter();

                pageSpinner.setSelection(position);

                ThreadPageFragmentPagerAdapter adapter = (ThreadPageFragmentPagerAdapter) viewPager.getAdapter();
                ((ThreadPageFragment) adapter.getItem(position)).doScrollToReply();
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        if (initialPage == -1) {
            initialPage = viewPager.getAdapter().getCount();
        }

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Toolbar bottomToolbar = (Toolbar) view.findViewById(R.id.toolbar_bottom);

        bottomToolbar.inflateMenu(R.menu.thread);

        if (fromForum == WhirlpoolApi.ALL_WATCHED_THREADS || fromForum == WhirlpoolApi.UNREAD_WATCHED_THREADS) {
            bottomToolbar.getMenu().findItem(R.id.menu_watch    ).setVisible(false);
            bottomToolbar.getMenu().findItem(R.id.menu_markread ).setVisible(true);
            bottomToolbar.getMenu().findItem(R.id.menu_unwatch  ).setVisible(true);
        }

        bottomToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.menu_refresh:
                        ((ThreadPageFragment) ((ThreadPageFragmentPagerAdapter) viewPager.getAdapter()).getItem(viewPager.getCurrentItem())).initiateRefresh();
                        return true;

                    case R.id.menu_markread:
                        try {
                            MarkThreadReadTask markReadTask = new MarkThreadReadTask(threadId + "");
                            markReadTask.setOnCompletedListener(ThreadViewFragment.this);
                            markReadTask.execute();

                            progressDialog = ProgressDialog.show(getActivity(), "Just a sec...", "Marking thread as read", true, true);

                        } catch (Exception e) {
                            Toast.makeText(getActivity(), "Error marking thread as read", Toast.LENGTH_SHORT).show();
                        }

                        return true;

                    case R.id.menu_open_browser:
                        String thread_url = "https://forums.whirlpool.net.au/forum-replies.cfm?t=" + threadId;
                        Intent thread_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(thread_url));
                        startActivity(thread_intent);
                        return true;

                    case R.id.menu_goto_last:
                        viewPager.setCurrentItem(viewPager.getAdapter().getCount() - 1);
                        return true;

                    case R.id.menu_watch:
                        watchThread(threadId);
                        return true;

                    case R.id.menu_unwatch:
                        unwatchThread(threadId);
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
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        setFilterAdapter();

        Toolbar bottomToolbar = (Toolbar) rootView.findViewById(R.id.toolbar_bottom);
        pageSpinner = (Spinner) bottomToolbar.findViewById(R.id.pageList);

        bottomToolbar.setVisibility(View.VISIBLE);
        pageSpinner.setVisibility(View.VISIBLE);

        pageSpinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewPager.setCurrentItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        populatePageSpinner();

        if (initialLoad) {
            viewPager.setCurrentItem(initialPage - 1);
            initialLoad = false;
        }

        Whirldroid.getTracker().setCurrentScreen(getActivity(), "ThreadView", null);
        Whirldroid.logScreenView("ThreadView");
    }

    public class ThreadPageFragmentPagerAdapter extends FragmentStatePagerAdapter {
        private Map<Integer, Fragment> pages;
        private boolean doneInitialPage = false;
        private String scrollToReply;

        public ThreadPageFragmentPagerAdapter() {
            super(getChildFragmentManager());
            pages = new HashMap<>();
        }

        public void setScrollToReply(String replyId) {
            this.scrollToReply = replyId;
        }

        public String getScrollToReply() {
            return scrollToReply;
        }

        public void setCount(int count, String threadTitle) {
            if (count != pageCount) { // count has changed, let's do some things
                pageCount = count;
                populatePageSpinner();
                notifyDataSetChanged();
                filterList.set(0, threadTitle);
            }
        }

        public int getFilter() {
            return filterSpinner.getSelectedItemPosition();
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
                bundle.putInt("filter"          , currentFilter);

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

    public void watchThread(int threadId) {
        WatchThreadTask watchTask = new WatchThreadTask(threadId);
        watchTask.setOnCompletedListener(this);
        watchTask.execute();

        progressDialog = ProgressDialog.show(getActivity(), "Just a sec...", "Adding thread to watch list", true, true);
    }

    public void unwatchThread(int threadId) {
        UnwatchThreadTask unwatchTask = new UnwatchThreadTask(threadId + "");
        unwatchTask.setOnCompletedListener(this);
        unwatchTask.execute();

        progressDialog = ProgressDialog.show(getActivity(), "Just a sec...", "Removing thread from watch list", true, true);
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

        if (filterAdapter == null) {
            filterAdapter = new WhirldroidSpinnerAdapter(getContext(), R.layout.spinner_dropdown_item, filterList, "All posts in thread");
        }

        filterSpinner = (Spinner) getActivity().findViewById(R.id.spinner);
        filterSpinner.setAdapter(filterAdapter);
        filterSpinner.setVisibility(View.VISIBLE);
        filterSpinner.setOnItemSelectedListener(this);

        mainActivity.getSupportActionBar().setDisplayShowTitleEnabled(false);
        ((MainActivity) getActivity()).showToolbarSpinner();
    }

    @Override
    public void onWhirldroidTaskCompleted(final WhirldroidTask task, Boolean result) {
        if (progressDialog != null) {
            progressDialog.dismiss(); // hide the progress dialog
            progressDialog = null;
        }

        try {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    switch (task.getTag()) {
                        case WhirldroidTask.TAG_THREAD_READ:
                            Toast.makeText(getActivity(), "Thread marked as read", Toast.LENGTH_SHORT).show();
                            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

                            if (settings.getBoolean("pref_watchedbacktolist", false)) {
                                getActivity().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
                                getActivity().dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
                            }
                            break;

                        case WhirldroidTask.TAG_THREAD_WATCH:
                            final Snackbar watchedSnackbar = Snackbar.make(viewPager, "Added thread to watch list", Snackbar.LENGTH_LONG);

                            watchedSnackbar.setAction("UNDO", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    watchedSnackbar.dismiss();
                                    unwatchThread(Integer.parseInt(task.getSubject() + ""));
                                }
                            });

                            watchedSnackbar.show();
                            break;

                        case WhirldroidTask.TAG_THREAD_UNWATCH:
                            final Snackbar unwatchedSnackbar = Snackbar.make(viewPager, "Removed thread from watch list", Snackbar.LENGTH_LONG);

                            unwatchedSnackbar.setAction("UNDO", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    unwatchedSnackbar.dismiss();
                                    watchThread(Integer.parseInt(task.getSubject() + ""));
                                }
                            });

                            unwatchedSnackbar.show();
                            break;
                    }
                }
            });
        } catch (NullPointerException e) { }

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

        pageSpinner.setAdapter(new ArrayAdapter<>(getContext(), R.layout.spinner_dropdown_item, pages));
    }

}