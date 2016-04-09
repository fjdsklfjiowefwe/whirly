package com.gregdev.whirldroid.fragment;

import android.support.design.widget.Snackbar;
import android.support.v4.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.WhirlpoolApi;
import com.gregdev.whirldroid.WhirlpoolApiException;
import com.gregdev.whirldroid.layout.SeparatedListAdapter;
import com.gregdev.whirldroid.model.Forum;
import com.gregdev.whirldroid.model.Thread;
import com.gregdev.whirldroid.task.MarkThreadReadTask;
import com.gregdev.whirldroid.task.UnwatchThreadTask;
import com.gregdev.whirldroid.task.WatchThreadTask;
import com.gregdev.whirldroid.task.WhirldroidTask;
import com.gregdev.whirldroid.task.WhirldroidTaskOnCompletedListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by Greg on 10/03/2016.
 */
public class ForumPageFragment extends ListFragment implements WhirldroidTaskOnCompletedListener {

    private SeparatedListAdapter threads_adapter;
    private ThreadAdapter threads_adapter_no_headings;
    private Forum forum;
    private List<Thread> thread_list;
    private ProgressDialog progress_dialog;
    private RetrieveThreadsTask task;
    Map<String, List<Thread>> sorted_threads;
    private String forum_title;
    private int forum_id;
    private int list_position;
    private int page = 1;
    private int current_page = 1;
    private int group = 0;
    private ListView thread_listview;
    private TextView no_threads;
    private ProgressBar loading;
    ViewPager parent;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private int search_forum = -1;
    private int search_group = -1;
    private String search_query;

    private class RetrieveThreadsTask extends AsyncTask<String, Void, List<Thread>> {

        private boolean clear_cache = false;
        private String mark_thread_as_read = null;
        private String unwatch_thread = null;

        public RetrieveThreadsTask(boolean clear_cache, String mark_thread_as_read, String unwatch_thread) {
            this.clear_cache = clear_cache;
            this.mark_thread_as_read = mark_thread_as_read;
            this.unwatch_thread = unwatch_thread;
        }

        public RetrieveThreadsTask(boolean clear_cache) {
            this.clear_cache = clear_cache;
        }

        @Override
        protected List<Thread> doInBackground(String... params) {
            if (clear_cache || Whirldroid.getApi().needToDownloadThreads(forum_id)) {
                try {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            if (!mSwipeRefreshLayout.isRefreshing()) {
                                loading.setVisibility(View.VISIBLE);
                                thread_listview.setAlpha(0.5F);
                            }
                        }
                    });
                } catch (NullPointerException e) { }

                try {
                    switch (forum_id) {
                        case WhirlpoolApi.UNREAD_WATCHED_THREADS:
                            Whirldroid.getApi().downloadWatched(WhirlpoolApi.WATCHMODE_UNREAD, mark_thread_as_read, unwatch_thread, 0);
                            break;
                        case WhirlpoolApi.ALL_WATCHED_THREADS:
                            Whirldroid.getApi().downloadWatched(WhirlpoolApi.WATCHMODE_ALL, mark_thread_as_read, unwatch_thread, 0);
                            break;
                        case WhirlpoolApi.RECENT_THREADS:
                            Whirldroid.getApi().downloadRecent();
                            break;
                        case WhirlpoolApi.POPULAR_THREADS:
                            Whirldroid.getApi().downloadPopularThreads();
                            break;
                        case WhirlpoolApi.SEARCH_RESULTS:
                            forum = new Forum(forum_id, "Search Results", 0, null);
                            thread_list = Whirldroid.getApi().searchThreads(search_forum, search_group, search_query);
                            return thread_list;
                    }
                } catch (final WhirlpoolApiException e) {
                    return null;
                }
            }

            forum = Whirldroid.getApi().getThreads(forum_id, page, group);

            if (forum == null) { // error downloading data
                return null;
            }

            thread_list = forum.getThreads();

            return thread_list;
        }

        @Override
        protected void onPostExecute(final List<Thread> result) {
            try {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (!mSwipeRefreshLayout.isRefreshing()) {
                            loading.setVisibility(View.GONE);
                            thread_listview.setAlpha(1F);
                        }

                        mSwipeRefreshLayout.setRefreshing(false);

                        if (result != null) {
                            if (parent != null) {
                                ThreadListFragment.ForumPageFragmentPagerAdapter pagerAdapter = (ThreadListFragment.ForumPageFragmentPagerAdapter) parent.getAdapter();

                                if (pagerAdapter.getHeaderForum() == null) {
                                    pagerAdapter.setHeader(forum);
                                }

                                pagerAdapter.setCount(forum.getPageCount());
                            }

                            getActivity().invalidateOptionsMenu();

                            if (thread_list != null && thread_list.size() == 0) {
                                if (forum_id == WhirlpoolApi.UNREAD_WATCHED_THREADS) {
                                    no_threads.setText(getActivity().getResources().getText(R.string.no_threads_unread));
                                } else {
                                    no_threads.setText(getActivity().getResources().getText(R.string.no_threads));
                                }
                                no_threads.setVisibility(View.VISIBLE);

                            } else {
                                no_threads.setVisibility(View.GONE);
                            }

                            if (!WhirlpoolApi.isActualForum(forum_id)) {
                                setThreads(thread_list); // display the threads in the list
                            } else if (WhirlpoolApi.isPublicForum(forum_id)) {
                                setThreadsNoHeadings(thread_list);
                            } else {
                                setThreadsNoHeadings(thread_list);
                            }
                        } else {
                            Toast.makeText(getActivity(), "Error downloading threads. Please try again", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } catch (NullPointerException e) { }
        }
    }

    /**
     * A private class to format the thread list items
     * @author Greg
     *
     */
    public class ThreadAdapter extends ArrayAdapter<Thread> {

        private List<Thread> thread_items;
        boolean ignore_own;

        public ThreadAdapter(Context context, int textViewResourceId, List<Thread> thread_items) {
            super(context, textViewResourceId, thread_items);
            this.thread_items = thread_items;

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
            ignore_own = settings.getBoolean("pref_ignoreownreplies", false);
        }

        /**
         * The next two methods are here to avoid issues caused by the system recycling views.
         * This method returns an integer which identifies the view we should use for the
         * corresponding list item
         */
        public int getItemViewType(int position) {
            Thread item = thread_items.get(position);
            if (item.isSticky()) {
                return 1; // highlight as sticky
            }

            if (item.isClosed()) {
                return 4; // highlight as closed
            }

            if (item.isMoved()) {
                return 5; // highlight as moved
            }

            if (item.isDeleted()) {
                return 3; // highlight (fade out) as deleted
            }

            return 0; // normal, no highlighting
        }

        /**
         * This method needs to return the number of different item view layouts we have
         * eg. sticky + unread + normal = 3, so return 3
         */
        public int getViewTypeCount() {
            return 6;
        }

        @Override
        public View getView(int position, View convert_view, ViewGroup parent) {
            final Thread thread = thread_items.get(position);
            int type = getItemViewType(position);

            if (convert_view == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

                switch (type) {
                    case 1:
                        convert_view = vi.inflate(R.layout.list_row_sticky, null);
                        break;
                    case 3:
                        convert_view = vi.inflate(R.layout.list_row_deleted, null);
                        break;
                    case 4:
                        convert_view = vi.inflate(R.layout.list_row_closed, null);
                        break;
                    case 5:
                        convert_view = vi.inflate(R.layout.list_row_moved, null);
                        break;
                    default:
                        convert_view = vi.inflate(R.layout.list_row_thread, null);
                        break;
                }
            }
            if (thread != null) {
                TextView title_textview = (TextView) convert_view.findViewById(R.id.top_text);
                TextView pagecount_textview = (TextView) convert_view.findViewById(R.id.bottom_right_text);
                TextView lastpost_textview = (TextView) convert_view.findViewById(R.id.bottom_left_text);

                if (title_textview != null) {
                    if (thread.getUnread() > 0) {
                        title_textview.setText(thread.getTitle() + " (" + thread.getUnread() + " unread)");
                    }
                    else {
                        title_textview.setText(thread.getTitle());
                    }
                }

                if (!thread.isDeleted() && pagecount_textview != null) {
                    pagecount_textview.setText("" + thread.getPageCount());
                }

                if (lastpost_textview != null) {
                    Date date = thread.getLastDate();
                    if (thread.isDeleted()) {
                        lastpost_textview.setText("This thread has been deleted");
                    }
                    else if (thread.isMoved()) {
                        lastpost_textview.setText("This thread has been moved");
                    }
                    else if (date != null) {
                        String time_text = Whirldroid.getTimeSince(date);
                        lastpost_textview.setText(time_text + " ago by " + thread.getLastPoster());
                    }
                    else {
                        lastpost_textview.setText(thread.getLastDateText() + " by " + thread.getLastPoster());
                    }
                }
            }

            ImageButton btn = (ImageButton) convert_view.findViewById(R.id.menu_button);
            registerForContextMenu(btn);

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(getContext(), v);

                    if (forum_id == WhirlpoolApi.UNREAD_WATCHED_THREADS || forum_id == WhirlpoolApi.ALL_WATCHED_THREADS) {
                        popupMenu.inflate(R.menu.watched_list_item);
                    } else if (forum_id == WhirlpoolApi.RECENT_THREADS) {
                        popupMenu.inflate(R.menu.recent_list_item);
                    } else {
                        popupMenu.inflate(R.menu.thread_list_item);
                    }

                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.open_browser:
                                    openThreadInBrowser(thread, 1, false, 0);
                                    return true;

                                case R.id.unwatch:
                                    getThreads(true, null, thread.getId() + "");
                                    return true;

                                case R.id.mark_read:
                                    getThreads(true, thread.getId() + "", null);
                                    return true;

                                case R.id.watch:
                                    markThreadAsWatched(thread.getId());
                                    return true;

                                case R.id.goto_last:
                                    openThread(thread, -1, true);
                                    return true;

                                case R.id.goto_forum:
                                    openForum(thread.getForumId(), thread.getForum());
                                    return true;

                                default:
                                    return false;
                            }
                        }
                    });

                    popupMenu.show();
                }
            });


            return convert_view;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.thread_list, container, false);
        setHasOptionsMenu(true);
        no_threads = (TextView) rootView.findViewById(R.id.no_threads);

        try {
            parent = (ViewPager) container;
        } catch (ClassCastException e) {
            parent = null;
        }

        Bundle bundle = getArguments();

        if (bundle != null) {
            forum_id        = bundle.getInt   ("forum_id");
            page            = bundle.getInt   ("page"           , 1     );
            group           = bundle.getInt   ("group"          , 0     );
            search_forum    = bundle.getInt   ("search_forum"   , -1    );
            search_group    = bundle.getInt   ("search_group"   , -1    );
            search_query    = bundle.getString("search_query"   , null  );
        }

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swiperefresh);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        thread_listview = getListView();

        if (!WhirlpoolApi.isActualForum(forum_id)) {
            thread_listview.setPadding(0, 0, 0, 0);
        }

        loading = (ProgressBar) view.findViewById(R.id.loading);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initiateRefresh();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if (thread_listview.getCount() == 0 || forum_id == WhirlpoolApi.UNREAD_WATCHED_THREADS || forum_id == WhirlpoolApi.ALL_WATCHED_THREADS) {
            getThreads(false);
        }

        if (forum_id == WhirlpoolApi.SEARCH_RESULTS && search_forum != -1) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.resetActionBar();
            mainActivity.setTitle("Search Results");
            mainActivity.getSupportActionBar().setSubtitle("\"" + search_query + "\"");
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Thread thread = (Thread) l.getAdapter().getItem(position);

        openThread(thread, 1, false);
    }

    public void markThreadAsWatched(int threadId) {
        progress_dialog = ProgressDialog.show(getActivity(), "Just a sec...", "Watching thread...", true, true);
        WatchThreadTask watchTask = new WatchThreadTask(threadId);
        watchTask.setOnCompletedListener(this);
        watchTask.execute();
    }

    public void getThreads(boolean clear_cache) {
        task = new RetrieveThreadsTask(clear_cache); // start new thread to retrieve threads
        task.execute();
    }

    private void getThreads(boolean clear_cache, String mark_thread_as_read, String unwatch_thread) {
        task = new RetrieveThreadsTask(clear_cache, mark_thread_as_read, unwatch_thread); // start new thread to retrieve threads
        task.execute();
    }

    private void setThreadsNoHeadings(List<Thread> thread_list) {
        if (thread_list == null || thread_list.size() == 0) { // no threads found
            return;
        }

        threads_adapter_no_headings = new ThreadAdapter(getActivity(), R.layout.list_row, thread_list);
        setListAdapter(threads_adapter_no_headings);
    }

    /**
     * Loads the thread item into the list
     * @param thread_list Threads
     */
    private void setThreads(List<Thread> thread_list) {
        long last_updated = System.currentTimeMillis() / 1000;
        switch (forum_id) {
            case WhirlpoolApi.RECENT_THREADS:
                last_updated -= Whirldroid.getApi().getRecentLastUpdated();
                break;
            case WhirlpoolApi.UNREAD_WATCHED_THREADS:
                last_updated -= Whirldroid.getApi().getUnreadWatchedLastUpdated();
                break;
            case WhirlpoolApi.ALL_WATCHED_THREADS:
                last_updated -= Whirldroid.getApi().getAllWatchedLastUpdated();
                break;
            case WhirlpoolApi.POPULAR_THREADS:
                last_updated -= Whirldroid.getApi().getPopularLastUpdated();
                break;
        }

        if (WhirlpoolApi.isPublicForum(forum_id) || forum_id == WhirlpoolApi.POPULAR_THREADS
                || forum_id == WhirlpoolApi.RECENT_THREADS || forum_id == WhirlpoolApi.ALL_WATCHED_THREADS
                || forum_id == WhirlpoolApi.UNREAD_WATCHED_THREADS) {
            if (last_updated < 10) { // updated less than 10 seconds ago
                ((MainActivity) getActivity()).getSupportActionBar().setSubtitle("Updated just a moment ago");
            }
            else {
                String ago = Whirldroid.getTimeSince(last_updated);
                ((MainActivity) getActivity()).getSupportActionBar().setSubtitle("Updated " + ago + " ago");
            }
        }

        if (thread_list == null || thread_list.size() == 0) { // no threads found
            setListAdapter(null);
            return;
        }

        if (forum_id == WhirlpoolApi.UNREAD_WATCHED_THREADS) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
            Boolean ignore_own = settings.getBoolean("pref_ignoreownreplies", false);
            List<Thread> copy = new ArrayList<Thread>(thread_list);

            for (Thread thread : thread_list) {
                if (!thread.hasUnreadPosts() || (ignore_own && thread.getLastPosterId().equals(Whirldroid.getOwnWhirlpoolId()))) {
                    copy.remove(thread);
                }
            }

            thread_list = copy;

            if (thread_list.size() == 0) {
                no_threads.setText(getActivity().getResources().getText(R.string.no_threads_unread));
                no_threads.setVisibility(View.VISIBLE);

            } else {
                no_threads.setVisibility(View.GONE);
            }
        }

        threads_adapter = new SeparatedListAdapter(getActivity());

        sorted_threads = Whirldroid.groupThreadsByForum(thread_list);

        for (Map.Entry<String, List<Thread>> entry : sorted_threads.entrySet()) {
            String forum_name = entry.getKey();
            List<Thread> threads = entry.getValue();
            ThreadAdapter ta = new ThreadAdapter(getActivity(), android.R.layout.simple_list_item_1, threads);
            threads_adapter.addSection(forum_name, ta);
        }

        setListAdapter(threads_adapter);
    }

    private void openThread(Thread thread, int page_number, boolean bottom) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());

        boolean load_in_browser        = settings.getBoolean("pref_openthreadsinbrowser", false);
        boolean load_recent_at_top     = settings.getBoolean("pref_loadattop", false);
        boolean load_watched_at_top    = settings.getBoolean("pref_loadwatchedattop", false);
        boolean load_public_at_bottom  = settings.getBoolean("pref_loadpublicatbottom", false);
        boolean load_private_at_bottom = settings.getBoolean("pref_loadprivateatbottom", false);
        boolean auto_mark_read         = settings.getBoolean("pref_watchedautomarkasread", false);

        int goto_post = 0;

        // this is a list of watched threads, and the preference is to open the thread at the last read post
        if (!bottom && !load_watched_at_top && (forum_id == WhirlpoolApi.UNREAD_WATCHED_THREADS || forum_id == WhirlpoolApi.ALL_WATCHED_THREADS)) {
            page_number = thread.getLastPage();
            goto_post = thread.getLastPost();
            bottom = false;
        }

        // this is a list of recent threads, and the preference is to open the thread at the bottom
        if (!bottom && !load_recent_at_top && forum_id == WhirlpoolApi.RECENT_THREADS) {
            page_number = -1;
            bottom = true;
        }

        // this is a public thread, and the preference is to open the thread at the bottom
        if (load_public_at_bottom && WhirlpoolApi.isActualForum(forum_id) && WhirlpoolApi.isPublicForum(forum_id)) {
            page_number = -1;
            bottom = true;
        }

        // this is a private thread, and the preference is to open the thread at the bottom
        if (load_private_at_bottom && WhirlpoolApi.isActualForum(forum_id) && !WhirlpoolApi.isPublicForum(forum_id)) {
            page_number = -1;
            bottom = true;
        }

        if (auto_mark_read && thread.hasUnreadPosts()) {
            MarkThreadReadTask markRead = new MarkThreadReadTask(thread.getId() + "");
            markRead.execute();
        }

        // set to open threads in browser
        if (load_in_browser) {
            openThreadInBrowser(thread, page_number, bottom, goto_post);
        }

        // forum is private and cannot be scraped
        else if (!WhirlpoolApi.isPublicForum(thread.getForumId())) {
            openThreadInBrowser(thread, page_number, bottom, goto_post);
        }

        // open the thread within Whirldroid
        else {
            openThreadInApp(thread, page_number, bottom, goto_post);
        }
    }

    private void openThreadInApp(Thread thread, int page_number, boolean bottom, int goto_post) {
        Bundle bundle = new Bundle();

        if (goto_post != 0) {
            goto_post = (goto_post % WhirlpoolApi.POSTS_PER_PAGE) - 1;
            if (goto_post == -1) {
                goto_post = WhirlpoolApi.POSTS_PER_PAGE;
            }
        }
        bundle.putInt("thread_id", thread.getId());
        bundle.putString("thread_title", thread.getTitle());
        bundle.putInt("page_number", page_number);
        bundle.putInt("page_count", thread.getPageCount());
        bundle.putBoolean("bottom", bottom);
        bundle.putInt("goto_num", goto_post);
        bundle.putInt("from_forum", forum_id);

        ((MainActivity) getActivity()).switchFragment("ThreadView", true, bundle);
    }

    private void openThreadInBrowser(Thread thread, int page_number, boolean bottom, int goto_post) {
        String thread_url = "http://forums.whirlpool.net.au/forum-replies.cfm?t=" + thread.getId();

        if (bottom) {
            thread_url += "&p=-1#bottom";
        }
        else if (goto_post != 0) {
            thread_url += "&p=" + page_number + "#r" + goto_post;
        }

        Intent thread_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(thread_url));

        if (Build.VERSION.SDK_INT >= 18) {
            final String EXTRA_CUSTOM_TABS_SESSION = "android.support.customtabs.extra.SESSION";
            final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR";

            Bundle extras = new Bundle();
            extras.putBinder(EXTRA_CUSTOM_TABS_SESSION, null);
            thread_intent.putExtras(extras);
            thread_intent.putExtra(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR, Color.parseColor("#3A437B"));
        }

        startActivity(thread_intent);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // hide page navigation for private forums
        if (!WhirlpoolApi.isPublicForum(forum_id)) {
            try {
                menu.findItem(R.id.menu_prev).setVisible(false);
                menu.findItem(R.id.menu_next).setVisible(false);
                menu.findItem(R.id.menu_goto_page).setVisible(false);
                menu.findItem(R.id.menu_open_browser).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
                menu.findItem(R.id.menu_new_thread).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
            catch (NullPointerException e) {
                // menu item may not exist, meh
            }
        }
        if (current_page == 1) {
            try {
                menu.findItem(R.id.menu_prev).setEnabled(false);
            }
            catch (NullPointerException e) { }
        }
    }

    private void openForum(int forum_id, String forum_name) {
        Bundle bundle = new Bundle();
        bundle.putInt("forum_id", forum_id);
        bundle.putString("forum_name", forum_name);

        ((MainActivity) getActivity()).switchFragment("ThreadList", true, bundle);
    }

    public boolean initiateRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        getThreads(true);

        return true;
    }

    @Override
    public void taskComplete(final WhirldroidTask task, final Boolean result) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (progress_dialog != null) {
                    try {
                        progress_dialog.dismiss(); // hide the progress dialog
                        progress_dialog = null;
                    } catch (Exception e) { }
                }

                if (result) {
                    switch (task.getTag()) {
                        case WhirldroidTask.TAG_THREAD_WATCH:
                            final Snackbar snackbar = Snackbar.make(thread_listview, "Added thread to watch list", Snackbar.LENGTH_LONG);

                            snackbar.setAction("UNDO", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    snackbar.dismiss();
                                    UnwatchThreadTask unwatchTask = new UnwatchThreadTask(task.getSubject() + "");
                                    unwatchTask.execute();
                                    Snackbar snackbar1 = Snackbar.make(thread_listview, "Removed thread from watch list", Snackbar.LENGTH_SHORT);
                                    snackbar1.show();
                                }
                            });

                            snackbar.show();
                            break;
                    }
                } else {
                    Toast.makeText(getActivity(), "Error downloading data", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
