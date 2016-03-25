package com.gregdev.whirldroid.fragment;

import android.support.v4.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.method.DigitsKeyListener;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.WhirlpoolApi;
import com.gregdev.whirldroid.WhirlpoolApiException;
import com.gregdev.whirldroid.layout.SeparatedListAdapter;
import com.gregdev.whirldroid.model.Forum;
import com.gregdev.whirldroid.model.Thread;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by Greg on 10/03/2016.
 */
public class ThreadListFragment extends ListFragment {

    private SeparatedListAdapter threads_adapter;
    private ThreadAdapter threads_adapter_no_headings;
    private Forum forum;
    private List<Thread> thread_list;
    private ProgressDialog progress_dialog;
    private RetrieveThreadsTask task;
    private WatchThreadTask watch_task;
    Map<String, List<Thread>> sorted_threads;
    private String forum_title;
    private int forum_id;
    private int list_position;
    private int current_page = 1;
    private int current_group = 0;
    private ListView thread_listview;
    private TextView no_threads;
    private GroupAdapter group_adapter;
    private Map<String, Integer> groups;
    private boolean hide_read = false;

    private int search_forum = -1;
    private int search_group = -1;
    private String search_query;

    private Tracker mTracker;

    private class WatchThreadTask extends AsyncTask<String, Void, Boolean> {
        private int thread_id;

        public WatchThreadTask(int thread_id) {
            this.thread_id = thread_id;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                Whirldroid.getApi().downloadWatched(0, 0, thread_id);
                return true;
            }
            catch (final WhirlpoolApiException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    if (progress_dialog != null) {
                        try {
                            progress_dialog.dismiss(); // hide the progress dialog
                            progress_dialog = null;
                        } catch (Exception e) {
                        }
                    }
                    if (result) {
                        Toast.makeText(getActivity(), "Added thread to watch list", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "Error downloading data", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    private class MarkReadTask extends AsyncTask<String, Void, Boolean> {
        private int thread_id;

        public MarkReadTask(int thread_id) {
            this.thread_id = thread_id;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                Whirldroid.getApi().downloadWatched(thread_id, 0, 0);
                return true;
            }
            catch (final WhirlpoolApiException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            // do nothing
        }
    }

    private class RetrieveThreadsTask extends AsyncTask<String, Void, List<Thread>> {

        private boolean clear_cache = false;
        private int mark_thread_as_read = 0;
        private int unwatch_thread = 0;
        //private String error_message = "";

        public RetrieveThreadsTask(boolean clear_cache, int mark_thread_as_read, int unwatch_thread) {
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
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            String message;
                            if (forum_id == WhirlpoolApi.SEARCH_RESULTS) {
                                message = "Searching threads...";
                            } else {
                                message = "Loading threads...";
                            }
                            progress_dialog = ProgressDialog.show(getActivity(), "Just a sec...", message, true, true);
                        } catch (WindowManager.BadTokenException e) {
                        }
                    }
                });
                try {
                    switch (forum_id) {
                        case WhirlpoolApi.WATCHED_THREADS:
                            Whirldroid.getApi().downloadWatched(mark_thread_as_read, unwatch_thread, 0);
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
                }
                catch (final WhirlpoolApiException e) {
                    //error_message = e.getMessage();
                    return null;
                }
            }

            forum = Whirldroid.getApi().getThreads(forum_id, current_page, current_group);

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
                        if (progress_dialog != null) {
                            try {
                                progress_dialog.dismiss(); // hide the progress dialog
                                progress_dialog = null;
                            } catch (Exception e) {
                            }

                            if (result != null && !isActualForum() && forum_id != WhirlpoolApi.SEARCH_RESULTS) {
                                Toast.makeText(getActivity(), "Threads refreshed", Toast.LENGTH_SHORT).show();
                            }
                        }

                        if (result != null) {
                            if (groups == null) {
                                groups = forum.getGroups();
                            }
                            if (groups != null) {
                                group_adapter.clear();
                                group_adapter.add(forum_title);

                                String current_group_name = "";
                                for (Map.Entry<String, Integer> group : groups.entrySet()) {
                                    group_adapter.add(group.getKey());
                                    if (group.getValue() == current_group) {
                                        current_group_name = group.getKey();
                                    }
                                }
                                if (current_group != 0) {
                                    getActivity().getActionBar().setSelectedNavigationItem(group_adapter.getPosition(current_group_name));
                                }
                            }

                            getActivity().invalidateOptionsMenu();

                            if (thread_list != null && thread_list.size() == 0) {
                                if (forum_id == WhirlpoolApi.WATCHED_THREADS && hide_read) {
                                    no_threads.setText(getActivity().getResources().getText(R.string.no_threads_unread));
                                } else {
                                    no_threads.setText(getActivity().getResources().getText(R.string.no_threads));
                                }
                                no_threads.setVisibility(View.VISIBLE);

                            } else {
                                no_threads.setVisibility(View.GONE);
                            }

                            if (!isActualForum()) {
                                setThreads(thread_list); // display the threads in the list
                            } else if (WhirlpoolApi.isPublicForum(forum_id)) {
                                ((MainActivity) getActivity()).getSupportActionBar().setSubtitle("Page " + current_page + " of " + forum.getPageCount());
                                setThreadsNoHeadings(thread_list);
                            } else {
                                setThreadsNoHeadings(thread_list);
                            }
                        } else {
                            Toast.makeText(getActivity(), "Error downloading threads. Please try again", Toast.LENGTH_LONG).show();
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

            else if (!hide_read && item.hasUnreadPosts()) {
                if (item.getLastPosterId().equals(Whirldroid.getOwnWhirlpoolId()) && ignore_own) {
                    return 0; // last reply was by us, no highlighting
                }
                return 2; // highlight as unread
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

                    if (forum_id == WhirlpoolApi.WATCHED_THREADS) {
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
                                    getThreads(true, 0, thread.getId());
                                    return true;

                                case R.id.mark_read:
                                    getThreads(true, thread.getId(), 0);
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
                    String subtitle_value = "Page " + current_page;
                    if (forum != null) {
                        subtitle_value += " of " + forum.getPageCount();
                    }
                    subtitle.setText(subtitle_value);
                }
            }
            return convert_view;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Obtain the shared Tracker instance.
        Whirldroid application = (Whirldroid) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.thread_list, container, false);
        setHasOptionsMenu(true);
        no_threads = (TextView) rootView.findViewById(R.id.no_threads);

        Bundle bundle = getArguments();

        if (bundle != null) {
            forum_id = bundle.getInt("forum_id");
            hide_read = bundle.getBoolean("hide_read", false);
        }

        //registerForContextMenu(getListView());

        if (isActualForum() && WhirlpoolApi.isPublicForum(forum_id)) {
            Context context = ((AppCompatActivity) getActivity()).getSupportActionBar().getThemedContext();

            ArrayList<String> group_list = new ArrayList<String>();
            group_list.add(bundle.getString("forum_name"));

            group_adapter = new GroupAdapter(context, R.layout.spinner_item, group_list);

            /*group_adapter.setDropDownViewResource(R.layout.sherlock_spinner_dropdown_item);

            getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            getActivity().getActionBar().setListNavigationCallbacks(group_adapter, this);*/
        }

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        thread_listview = getListView();

        if (this.isActualForum()) {
            ActionMenuView actionMenuView = (ActionMenuView) view.findViewById(R.id.menuBar);
            MenuBuilder menuBuilder = (MenuBuilder) actionMenuView.getMenu();

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

        } else {
            thread_listview.setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (thread_listview.getCount() == 0 || forum_id == WhirlpoolApi.WATCHED_THREADS) {
            getThreads(false);
        }

        if (forum_id != WhirlpoolApi.WATCHED_THREADS) {
            if (forum_id == WhirlpoolApi.POPULAR_THREADS) {
                mTracker.setScreenName("PopularThreads");
            } else if (forum_id == WhirlpoolApi.RECENT_THREADS) {
                mTracker.setScreenName("RecentThreads");
            } else {
                mTracker.setScreenName("ThreadList");
            }

            mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        }

        MainActivity mainActivity = ((MainActivity) getActivity());

        mainActivity.resetActionBar();

        Bundle bundle = getArguments();

        switch(forum_id) {
            case WhirlpoolApi.WATCHED_THREADS:
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
                forum_title = bundle.getString("forum_name");
                getActivity().setTitle(forum_title);
                break;
        }

        if (forum != null && WhirlpoolApi.isPublicForum(forum_id)) {
            mainActivity.getSupportActionBar().setSubtitle("Page " + current_page + " of " + forum.getPageCount());
        }

        mainActivity.setCurrentSearchType(mainActivity.SEARCH_THREADS, forum_id);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Thread thread = (Thread) l.getAdapter().getItem(position);

        openThread(thread, 1, false);
    }

    public void markThreadAsWatched(int thread_id) {
        progress_dialog = ProgressDialog.show(getActivity(), "Just a sec...", "Watching thread...", true, true);
        //progress_dialog.setOnCancelListener(new CancelTaskOnCancelListener(task));
        watch_task = new WatchThreadTask(thread_id); // start new thread to retrieve threads
        watch_task.execute();
    }

    private void getThreads(boolean clear_cache) {
        task = new RetrieveThreadsTask(clear_cache); // start new thread to retrieve threads
        task.execute();
    }

    private void getThreads(boolean clear_cache, int mark_thread_as_read, int unwatch_thread) {
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
            case WhirlpoolApi.WATCHED_THREADS:
                last_updated -= Whirldroid.getApi().getWatchedLastUpdated();
                break;
            case WhirlpoolApi.POPULAR_THREADS:
                last_updated -= Whirldroid.getApi().getPopularLastUpdated();
                break;
        }

        if (WhirlpoolApi.isPublicForum(forum_id) || forum_id == WhirlpoolApi.POPULAR_THREADS
                || forum_id == WhirlpoolApi.RECENT_THREADS || forum_id == WhirlpoolApi.WATCHED_THREADS) {
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

        if (hide_read) {
            List<Thread> copy = new ArrayList<Thread>(thread_list);

            for (Thread thread : thread_list) {
                if (!thread.hasUnreadPosts()) {
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
        if (!bottom && !load_watched_at_top && forum_id == WhirlpoolApi.WATCHED_THREADS) {
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
        if (load_public_at_bottom && isActualForum() && WhirlpoolApi.isPublicForum(forum_id)) {
            page_number = -1;
            bottom = true;
        }

        // this is a private thread, and the preference is to open the thread at the bottom
        if (load_private_at_bottom && isActualForum() && !WhirlpoolApi.isPublicForum(forum_id)) {
            page_number = -1;
            bottom = true;
        }

        if (auto_mark_read && thread.hasUnreadPosts()) {
            MarkReadTask mark_read = new MarkReadTask(thread.getId()); // start new thread to retrieve threads
            mark_read.execute();
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (isActualForum()) {
            //Create the search view
            SearchView search_view = new SearchView(((MainActivity) getActivity()).getSupportActionBar().getThemedContext());
            search_view.setQueryHint("Search for threads…");
            search_view.setOnQueryTextListener((MainActivity) getActivity());

            menu.add("Search")
                    .setIcon(R.drawable.ic_search_white_24dp)
                    .setActionView(search_view)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        } else {
            inflater.inflate(R.menu.refresh, menu);
        }
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                long now = System.currentTimeMillis() / 1000;
                // don't refresh too often
                if (now - Whirldroid.getApi().getRecentLastUpdated() > WhirlpoolApi.REFRESH_INTERVAL) {
                    getThreads(true);
                }
                else {
                    Toast.makeText(getActivity(), "Wait " + WhirlpoolApi.REFRESH_INTERVAL + " seconds before refreshing", Toast.LENGTH_LONG).show();
                }
                return true;

            case R.id.menu_prev:
                current_page--;
                getThreads(false, 0, 0);
                return true;

            case R.id.menu_goto_page:
                final EditText input = new EditText(getActivity());
                input.setKeyListener(new DigitsKeyListener());
                new AlertDialog.Builder(getActivity())
                        .setTitle("Jump to page...")
                        .setMessage("Enter a page number to load")
                        .setView(input)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Editable value = input.getText();
                                try {
                                    current_page = Integer.parseInt(value.toString());
                                }
                                catch (Exception e) { }
                                getThreads(false, 0, 0);
                            }
                        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Do nothing.
                    }
                }).show();
                return true;

            case R.id.menu_next:
                current_page++;
                getThreads(false, 0, 0);
                return true;

            case R.id.menu_new_thread:
                Intent newthread_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(WhirlpoolApi.NEWTHREAD_URL + forum_id));
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
                Intent thread_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(WhirlpoolApi.FORUM_URL + forum_id));
                startActivity(thread_intent);
                return true;
        }
        return false;
    }

    /**
     * Checks if the current threads are from an actual forum on Whirlpool,
     * or are Whirldroid-specific (recent, watched, popular, etc)
     * @return
     */
    private boolean isActualForum() {
        if (
                forum_id == WhirlpoolApi.RECENT_THREADS ||
                        forum_id == WhirlpoolApi.WATCHED_THREADS ||
                        forum_id == WhirlpoolApi.POPULAR_THREADS ||
                        forum_id == WhirlpoolApi.SEARCH_RESULTS
                ) {
            return false;
        }

        return true;
    }

    private void openForum(int forum_id, String forum_name) {
        Bundle bundle = new Bundle();
        bundle.putInt("forum_id", forum_id);
        bundle.putString("forum_name", forum_name);

        ((MainActivity) getActivity()).switchFragment("ThreadList", true, bundle);
    }

    public boolean onNavigationItemSelected(int item_position, long item_id) {
        try {
            if (item_position == 0 && current_group == 0) {
                return false;
            }
            else if (item_position == 0) {
                current_group = 0;
                current_page = 1;
                getThreads(true);
                return true;
            }

            int counter = 1;
            for (Map.Entry<String, Integer> group : groups.entrySet()) {
                if (counter == item_position) {
                    if (current_group == group.getValue()) {
                        return false;
                    }

                    current_group = group.getValue();
                    current_page = 1;
                    getThreads(true);
                    return true;
                }
                counter++;
            }
            return false;
        }
        catch (NullPointerException e) {
            return false;
        }
    }

    public boolean onQueryTextSubmit(String query) {
        Intent search_intent;

        // private forums can't be searched, so open the browser
        if (!WhirlpoolApi.isPublicForum(forum_id)) {
            String search_url = WhirlpoolApi.buildSearchUrl(forum_id, -1, query);
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

}
