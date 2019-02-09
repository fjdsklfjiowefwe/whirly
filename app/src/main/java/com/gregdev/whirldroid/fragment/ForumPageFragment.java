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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Refresher;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;
import com.gregdev.whirldroid.layout.SeparatedListAdapter;
import com.gregdev.whirldroid.model.Forum;
import com.gregdev.whirldroid.model.Thread;
import com.gregdev.whirldroid.task.MarkThreadReadTask;
import com.gregdev.whirldroid.task.UnwatchThreadTask;
import com.gregdev.whirldroid.task.WatchThreadTask;
import com.gregdev.whirldroid.task.WhirldroidTask;
import com.gregdev.whirldroid.task.WhirldroidTaskOnCompletedListener;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by Greg on 10/03/2016.
 */
public class ForumPageFragment extends ListFragment implements WhirldroidTaskOnCompletedListener, Refresher {

    private SeparatedListAdapter threads_adapter;
    private ThreadAdapter threads_adapter_no_headings;
    private Forum forum;
    private List<Thread> thread_list;
    private ProgressDialog progressDialog;
    private RetrieveThreadsTask task;
    Map<String, List<Thread>> sorted_threads;
    private int forum_id;
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

        public RetrieveThreadsTask(boolean clear_cache) {
            this.clear_cache = clear_cache;
        }

        @Override
        protected List<Thread> doInBackground(String... params) {
            if (clear_cache || WhirlpoolApiFactory.getFactory().getApi(getContext()).needToDownloadThreads(forum_id)) {
                try {
                    switch (forum_id) {
                        case WhirlpoolApi.UNREAD_WATCHED_THREADS:
                            WhirlpoolApiFactory.getFactory().getApi(getContext()).getUnreadWatchedThreadsManager().download();
                            break;
                        case WhirlpoolApi.ALL_WATCHED_THREADS:
                            WhirlpoolApiFactory.getFactory().getApi(getContext()).getAllWatchedThreadsManager().download();
                            break;
                        case WhirlpoolApi.RECENT_THREADS:
                            WhirlpoolApiFactory.getFactory().getApi(getContext()).getRecentThreadsManager().download();
                            break;
                        case WhirlpoolApi.POPULAR_THREADS:
                            WhirlpoolApiFactory.getFactory().getApi(getContext()).getPopularThreadsManager().download();
                            break;
                        case WhirlpoolApi.SEARCH_RESULTS:
                            forum = new Forum(forum_id, "Search Results", null);
                            thread_list = WhirlpoolApiFactory.getFactory().getApi(getContext()).searchThreads(search_forum, search_group, search_query);
                            return thread_list;
                    }
                } catch (final WhirlpoolApiException e) {
                    return null;
                }
            }

            forum = WhirlpoolApiFactory.getFactory().getApi(getContext()).getThreads(forum_id, page, group);

            if (forum == null) { // error downloading data
                return null;
            }

            thread_list = forum.getThreads();

            return thread_list;
        }

        @Override
        protected void onPostExecute(final List<Thread> result) {
            if (!mSwipeRefreshLayout.isRefreshing()) {
                loading.setVisibility(View.GONE);
            }

            mSwipeRefreshLayout.setRefreshing(false);

            if (result != null) {
                if (parent != null) {
                    try {
                        ThreadListFragment.ForumPageFragmentPagerAdapter pagerAdapter = (ThreadListFragment.ForumPageFragmentPagerAdapter) parent.getAdapter();

                        if (pagerAdapter.getHeaderForum() == null) {
                            pagerAdapter.setHeader(forum);
                        }

                        pagerAdapter.setCount(forum.getPageCount());
                    } catch (ClassCastException e) { } // must be viewing watched threads, which means we don't care about page counts
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
                                    openThread(thread, 1, false, true);
                                    return true;

                                case R.id.unwatch:
                                    unwatchThread(thread.getId());
                                    return true;

                                case R.id.mark_read:
                                    progressDialog = ProgressDialog.show(getActivity(), "Just a sec...", "Marking thread as read...", true, true);
                                    MarkThreadReadTask markReadTask = new MarkThreadReadTask(Integer.toString(thread.getId()), getContext());
                                    markReadTask.setOnCompletedListener(ForumPageFragment.this);
                                    markReadTask.execute();
                                    return true;

                                case R.id.watch:
                                    watchThread(thread.getId());
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

    public void unwatchThread(int threadId) {
        progressDialog = ProgressDialog.show(getActivity(), "Just a sec...", "Removing thread from watch list...", true, true);
        UnwatchThreadTask unwatchTask = new UnwatchThreadTask(threadId + "", getContext());
        unwatchTask.setOnCompletedListener(ForumPageFragment.this);
        unwatchTask.execute();
    }

    public void watchThread(int threadId) {
        progressDialog = ProgressDialog.show(getActivity(), "Just a sec...", "Adding thread to watch list...", true, true);
        WatchThreadTask watchTask = new WatchThreadTask(threadId, getContext());
        watchTask.setOnCompletedListener(ForumPageFragment.this);
        watchTask.execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.thread_list, container, false);
        mSwipeRefreshLayout = rootView.findViewById(R.id.swiperefresh);
        setHasOptionsMenu(true);
        no_threads = rootView.findViewById(R.id.no_threads);

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

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        thread_listview = getListView();

        thread_listview.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                try {
                    view.findViewById(R.id.menu_button).callOnClick();
                    return true;
                } catch (NullPointerException e) {
                    return false;
                }
            }
        });

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

        if (l.getAdapter() instanceof SeparatedListAdapter && l.getAdapter().getItemViewType(position) == SeparatedListAdapter.TYPE_SECTION_HEADER) {
            // get the thread following the clicked title
            try {
                Thread thread = (Thread) l.getAdapter().getItem(position + 1);
                openForum(thread.getForumId(), thread.getForum());
            } catch (NullPointerException e) { }

        } else {
            Thread thread = (Thread) l.getAdapter().getItem(position);
            openThread(thread, 1, false);
        }
    }

    public void getThreads(boolean clear_cache) {
        if (!mSwipeRefreshLayout.isRefreshing()) {
            loading.setVisibility(View.VISIBLE);
        }

        task = new RetrieveThreadsTask(clear_cache); // start new thread to retrieve threads
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
                last_updated -= WhirlpoolApiFactory.getFactory().getApi(getContext()).getRecentThreadsManager().getLastUpdated();
                break;
            case WhirlpoolApi.UNREAD_WATCHED_THREADS:
                last_updated -= WhirlpoolApiFactory.getFactory().getApi(getContext()).getUnreadWatchedThreadsManager().getLastUpdated();
                break;
            case WhirlpoolApi.ALL_WATCHED_THREADS:
                last_updated -= WhirlpoolApiFactory.getFactory().getApi(getContext()).getAllWatchedThreadsManager().getLastUpdated();
                break;
            case WhirlpoolApi.POPULAR_THREADS:
                last_updated -= WhirlpoolApiFactory.getFactory().getApi(getContext()).getPopularThreadsManager().getLastUpdated();
                break;
        }

        if (!WhirlpoolApi.isActualForum(forum_id)) {
            if (last_updated < 10) { // updated less than 10 seconds ago
                ((MainActivity) getActivity()).getSupportActionBar().setSubtitle("Updated just a moment ago");
            } else if (forum_id != WhirlpoolApi.SEARCH_RESULTS) {
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
                if (!thread.hasUnreadPosts() || (ignore_own && thread.getLastPosterId().equals(Whirldroid.getOwnWhirlpoolId(getContext())))) {
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

        threads_adapter = new SeparatedListAdapter(getActivity(), true);

        sorted_threads = Whirldroid.groupThreadsByForum(thread_list);

        for (Map.Entry<String, List<Thread>> entry : sorted_threads.entrySet()) {
            String forum_name = entry.getKey();
            List<Thread> threads = entry.getValue();
            ThreadAdapter ta = new ThreadAdapter(getActivity(), android.R.layout.simple_list_item_1, threads);
            threads_adapter.addSection(forum_name, ta);
        }

        setListAdapter(threads_adapter);
    }

    private void openThread(Thread thread, int pageNumber, boolean bottom) {
        openThread(thread, pageNumber, bottom, false);
    }

    private void openThread(Thread thread, int pageNumber, boolean bottom, boolean forceBrowser) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());

        boolean loadInBrowser       = settings.getBoolean("pref_openthreadsinbrowser"   , false);
        boolean loadRecentAtTop     = settings.getBoolean("pref_loadattop"              , false);
        boolean loadWatchedAtTop    = settings.getBoolean("pref_loadwatchedattop"       , false);
        boolean loadPublicAtBottom  = settings.getBoolean("pref_loadpublicatbottom"     , false);
        boolean loadPrivateAtBottom = settings.getBoolean("pref_loadprivateatbottom"    , false);
        boolean autoMarkRead        = settings.getBoolean("pref_watchedautomarkasread"  , false);

        if (forceBrowser) {
            loadInBrowser = true;
        }

        int gotoPost = 0;

        // this is a list of watched threads, and the preference is to open the thread at the last read post
        if (!bottom && !loadWatchedAtTop && (forum_id == WhirlpoolApi.UNREAD_WATCHED_THREADS || forum_id == WhirlpoolApi.ALL_WATCHED_THREADS)) {
            pageNumber  = thread.getLastPage();
            gotoPost    = thread.getLastPost();
            bottom      = false;
        }

        // this is a list of recent threads, and the preference is to open the thread at the bottom
        if (!bottom && !loadRecentAtTop && forum_id == WhirlpoolApi.RECENT_THREADS) {
            pageNumber  = -1;
            bottom      = true;
        }

        // this is a public thread, and the preference is to open the thread at the bottom
        if (loadPublicAtBottom && WhirlpoolApi.isActualForum(forum_id) && WhirlpoolApi.isPublicForum(forum_id)) {
            pageNumber  = -1;
            bottom      = true;
        }

        // this is a private thread, and the preference is to open the thread at the bottom
        if (loadPrivateAtBottom && WhirlpoolApi.isActualForum(forum_id) && !WhirlpoolApi.isPublicForum(forum_id)) {
            pageNumber  = -1;
            bottom      = true;
        }

        if (autoMarkRead && thread.hasUnreadPosts()) {
            MarkThreadReadTask markRead = new MarkThreadReadTask(Integer.toString(thread.getId()), getContext());
            markRead.execute();
        }

        // set to open threads in browser
        if (loadInBrowser) {
            openThreadInBrowser(thread, pageNumber, bottom, gotoPost);

        } else if (!WhirlpoolApi.isPublicForum(thread.getForumId())) { // forum is private and cannot be scraped
            openThreadInBrowser(thread, pageNumber, bottom, gotoPost);

        } else { // open the thread within Whirldroid
            openThreadInApp(thread, pageNumber, bottom, gotoPost);
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
        String thread_url = "https://forums.whirlpool.net.au/thread/" + thread.getId();

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
    public void onWhirldroidTaskCompleted(final WhirldroidTask task, final Boolean result) {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                if (progressDialog != null) {
                    try {
                        progressDialog.dismiss(); // hide the progress dialog
                        progressDialog = null;
                    } catch (Exception e) { }
                }

                if (result) {
                    switch (task.getTag()) {
                        case WhirldroidTask.TAG_THREAD_WATCH:
                            final Snackbar watchSnackbar = Snackbar.make(thread_listview, "Added thread to watch list", Snackbar.LENGTH_LONG);

                            watchSnackbar.setAction("UNDO", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    watchSnackbar.dismiss();
                                    unwatchThread(Integer.parseInt(task.getSubject() + ""));
                                }
                            });

                            watchSnackbar.show();
                            if (forum_id == WhirlpoolApi.ALL_WATCHED_THREADS) {
                                getThreads(true);
                            }

                            break;

                        case WhirldroidTask.TAG_THREAD_UNWATCH:
                            final Snackbar unwatchSnackbar = Snackbar.make(thread_listview, "Removed thread from watch list", Snackbar.LENGTH_LONG);

                            unwatchSnackbar.setAction("UNDO", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    unwatchSnackbar.dismiss();
                                    watchThread(Integer.parseInt(task.getSubject() + ""));
                                }
                            });

                            unwatchSnackbar.show();
                            getThreads(false);
                            break;

                        case WhirldroidTask.TAG_THREAD_READ:
                            Toast.makeText(getActivity(), "Thread marked as read", Toast.LENGTH_SHORT).show();
                            getThreads(false);
                            break;
                    }
                } else {
                    Toast.makeText(getActivity(), "Error downloading data", Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
