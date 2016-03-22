package com.gregdev.whirldroid.fragment;

import java.util.ArrayList;
import java.util.List;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.widget.ActionMenuView;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.BadTokenException;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.WhirlpoolApi;
import com.gregdev.whirldroid.WhirlpoolApiException;
import com.gregdev.whirldroid.model.Post;
import com.gregdev.whirldroid.model.Thread;

/**
 * Displays the latest Whirlpool whims in a nice list format
 * @author Greg
 *
 */
public class ThreadViewFragment extends ListFragment {

    private ArrayAdapter<Post> posts_adapter;
    private ProgressDialog progress_dialog;
    private RetrieveThreadTask task;
    private int thread_id;
    private String thread_title;
    private Thread thread = null;
    private long last_updated = 0;
    private int current_page = 1;
    private int page_count = 0;
    private boolean bottom = false;
    private int goto_num = 0;
    private int from_forum;
    private PageAdapter page_adapter;
    private boolean pages_loaded = false;
    private boolean no_page_select = true;
    private String font_size_option = "0";

    /**
     * Private class to retrieve threads in the background
     * @author Greg
     *
     */
    private class RetrieveThreadTask extends AsyncTask<String, Void, Thread> {

        private String error_message = "";

        public RetrieveThreadTask() {

        }

        @Override
        protected Thread doInBackground(String... params) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        progress_dialog = ProgressDialog.show(getActivity(), "Just a sec...", "Loading thread...", true, true);
                        progress_dialog.setOnCancelListener(new CancelTaskOnCancelListener(task));
                    } catch (BadTokenException e) { }
                }
            });

            Thread thread = null;
            try {
                thread = Whirldroid.getApi().downloadThread(thread_id, thread_title, current_page);
            }
            catch (final WhirlpoolApiException e) {
                error_message = e.getMessage();

                if (error_message.equals("Private forum")) {
                    String thread_url = "http://forums.whirlpool.net.au/forum-replies.cfm?t=" + thread_id;
                    Intent thread_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(thread_url));
                    startActivity(thread_intent);
                    getActivity().finish();
                }

                return null;
            }

            return thread;
        }

        @Override
        protected void onPostExecute(final Thread result) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    if (progress_dialog != null) {
                        try {
                            progress_dialog.dismiss(); // hide the progress dialog
                            progress_dialog = null;
                        } catch (Exception e) {
                        }
                    }
                    if (result != null) {
                        last_updated = System.currentTimeMillis() / 1000;
                        getActivity().invalidateOptionsMenu();

                        page_count = result.getPageCount();

                        if (current_page == -1) { // -1 indicates we're on the last page
                            current_page = page_count;
                        }

                        thread_title = result.getTitle();

                        page_adapter.clear(); // clear existing page item list
                        // add items for each page
                        for (int i = 1; i <= page_count; i++) {
                            page_adapter.add("Page " + i);
                        }

                        // select the current page
                        if (current_page != 1) {
                            no_page_select = true;
                            ((MainActivity) getActivity()).getSupportActionBar().setSelectedNavigationItem(current_page - 1);
                            no_page_select = false;
                        }

                        thread = result;
                        setPosts(result.getPosts()); // display the posts in the list
                    } else {
                        Toast.makeText(getActivity(), error_message, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
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

    /**
     * A private class to format the thread list items
     * @author Greg
     *
     */
    public class PostsAdapter extends ArrayAdapter<Post> {

        private ArrayList<Post> post_items;

        public PostsAdapter(Context context, int textViewResourceId, ArrayList<Post> post_items) {
            super(context, textViewResourceId, post_items);
            this.post_items = post_items;
        }

        @Override
        public View getView(int position, View convert_view, ViewGroup parent) {
            Post post = post_items.get(position);

            if (convert_view == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convert_view = vi.inflate(R.layout.list_row_post, null);
            }
            if (post != null) {
                TextView top_left_text = (TextView) convert_view.findViewById(R.id.top_left_text);
                TextView top_right_text = (TextView) convert_view.findViewById(R.id.top_right_text);
                TextView middle_left_text = (TextView) convert_view.findViewById(R.id.middle_left_text);
                TextView middle_right_text = (TextView) convert_view.findViewById(R.id.middle_right_text);
                TextView bottom_text = (TextView) convert_view.findViewById(R.id.bottom_text);

                top_left_text.setText(post.getUser().getName());
                top_right_text.setText(post.getPostedTime());

                if (font_size_option.equals("1")) {
                    bottom_text.setTextSize(20);
                } if (font_size_option.equals("2")) {
                    bottom_text.setTextSize(22);
                }

                if (post.isOp()) {
                    middle_left_text.setText("OP / " + post.getUser().getGroup());
                } else {
                    middle_left_text.setText(post.getUser().getGroup());
                }

                if (post.isEdited()) {
                    middle_right_text.setText(R.string.edited_text);
                } else {
                    middle_right_text.setText("");
                }

                if (!post.isEdited() && post.getUser().getGroup() == "") {
                    middle_left_text.setVisibility(View.GONE);
                    middle_right_text.setVisibility(View.GONE);
                }

                String content = post.getContent();
                content = content.replace("\n", "").replace("\r", "");

                String user_quote_colour;
                switch (Whirldroid.getCurrentThemeId()) {
                    case Whirldroid.DARK_THEME:
                        user_quote_colour = getResources().getString(R.string.user_quote_colour_dark);
                        break;
                    case Whirldroid.LIGHT_THEME:
                    default:
                        user_quote_colour = getResources().getString(R.string.user_quote_colour_light);
                        break;
                }

                // user quote name
                content = content.replaceAll("<p class=\"reference\">(.*?)</p>", "<p><font color='" + user_quote_colour + "'><b>$1</b></font></p>");

                // user quote text
                content = content.replaceAll("<span class=\"wcrep1\">(.*?)</span>", "<font color='" + user_quote_colour + "'>$1</font>");

                // other quote text
                content = content.replaceAll("<span class=\"wcrep2\">(.*?)</span>", "<font color='#9F6E19'>$1</font>");

                // lists
                content = content.replace("<ul><li>", "<ul><li> â€¢ ");
                content = content.replace("<li>", "<br><li> â€¢ ");

                // links to other threads
                String url_replace = "whirldroid-thread://com.gregdev.whirldroid?threadid=";

                // wiki links
                content = content.replace("href=\"/wiki/", "href=\"https://whirlpool.net.au/wiki/");
                content = content.replace("href=\"//", "href=\"https://");

                content = content.replace("http://forums.whirlpool.net.au/forum-replies.cfm?t=", url_replace);
                content = content.replace("https://forums.whirlpool.net.au/forum-replies.cfm?t=", url_replace);
                content = content.replace("href=\"/forum-replies.cfm?t=", "href=\"" + url_replace);
                content = content.replace("href=\"forum-replies.cfm?t=", "href=\"" + url_replace);

                try {
                    bottom_text.setText(Html.fromHtml(content));
                }
                // weird Jelly Bean bug
                catch (ArrayIndexOutOfBoundsException e) {
                    // just pull out a bunch of style tags and hope the problem goes away
                    content = content.replace("<b>", "");
                    content = content.replace("</b>", "");
                    content = content.replace("<strong>", "");
                    content = content.replace("</strong>", "");
                    content = content.replace("<i>", "");
                    content = content.replace("</i>", "");
                    content = content.replace("<em>", "");
                    content = content.replace("</em>", "");

                    bottom_text.setText(Html.fromHtml(content));
                }

                bottom_text.setMovementMethod(LinkMovementMethod.getInstance());
                bottom_text.setLinksClickable(true);
            }
            return convert_view;
        }
    }

    public class PageAdapter extends ArrayAdapter<String> {

        List<String> page_items;
        Context context;

        public PageAdapter(Context context, int resource, List<String> group_items) {
            super(context, resource, group_items);
            this.page_items = group_items;
            this.context = context;
        }

        @Override
        public View getView(int position, View convert_view, ViewGroup parent) {
            String item_string = page_items.get(position);

            if (convert_view == null) {
                LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convert_view = vi.inflate(R.layout.spinner_item, null);
            }

            TextView title = (TextView) convert_view.findViewById(R.id.title);
            TextView subtitle = (TextView) convert_view.findViewById(R.id.subtitle);

            if (title != null) {
                title.setText(thread_title);
            }
            if (subtitle != null && item_string != "") {
                String subtitle_value = "Page " + (position + 1);
                if (current_page == -1) {
                    subtitle_value = "Page 1";
                }
                if (page_count != 0) {
                    subtitle_value += " of " + page_count;
                }
                subtitle.setText(subtitle_value);
            }

            return convert_view;
        }
    }

    /**
     * Cancels the fetching of posts if the back button is pressed
     * @author Greg
     *
     */
    private class CancelTaskOnCancelListener implements OnCancelListener {
        private AsyncTask<?, ?, ?> task;
        public CancelTaskOnCancelListener(AsyncTask<?, ?, ?> task) {
            this.task = task;
        }

        public void onCancel(DialogInterface dialog) {
            if (task != null) {
                task.cancel(true);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.thread_list, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        Bundle bundle = getArguments();
        if (bundle != null) {
            thread_id = bundle.getInt("thread_id");
            thread_title = bundle.getString("thread_title");
            current_page = bundle.getInt("page_number");
            bottom = bundle.getBoolean("bottom");
            goto_num = bundle.getInt("goto_num");
            from_forum = bundle.getInt("from_forum");

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Whirldroid.getContext());
            font_size_option = settings.getString("pref_postfontsize", "0");

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

            getActivity().getMenuInflater().inflate(R.menu.thread, menuBuilder);
        }

        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();

        Context context = actionBar.getThemedContext();

        ArrayList<String> page_list = new ArrayList<String>();
        page_list.add("");

        page_adapter = new PageAdapter(context, R.layout.spinner_item, page_list);

        page_adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(page_adapter, new android.support.v7.app.ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                return false;
            }
        });

        registerForContextMenu(getListView());
    }

    @Override
    public void onResume() {
        super.onResume();

        // thread title is set using PageAdapter, so set this to nothing
        getActivity().setTitle("");
        ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
        actionBar.setSubtitle("");

        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        actionBar.setListNavigationCallbacks(page_adapter, new android.support.v7.app.ActionBar.OnNavigationListener() {
            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {
                return false;
            }
        });

        if (page_count > 0) {
            page_adapter.clear(); // clear existing page item list

            for (int i = 1; i <= page_count; i++) {
                page_adapter.add("Page " + i);
            }

            // select the current page
            if (current_page != 1) {
                no_page_select = true;
                actionBar.setSelectedNavigationItem(current_page - 1);
                no_page_select = false;
            }
        }

        if (last_updated == 0 || thread == null) {
            getThread();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menu_info) {
        menu.setHeaderTitle(R.string.ctxmenu_post);

        //menu.add(Menu.NONE, 0, 0, "View in Browser");
        menu.add(Menu.NONE, 1, 1, getResources().getText(R.string.ctxmenu_reply_in_browser));
        menu.add(Menu.NONE, 2, 2, getResources().getText(R.string.ctxmenu_user_info));
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        int pos = info.position - getListView().getHeaderViewsCount();

        Post post = (Post) posts_adapter.getItem(pos);

        switch (item.getItemId()) {
			case 0: // open in browser
				String post_url = "http://forums.whirlpool.net.au/forum-replies.cfm?t=" + thread.getId() + "&p=" + current_page + "#r" + post.getId();
				Intent view_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(post_url));

                if (Build.VERSION.SDK_INT >= 18) {
                    final String EXTRA_CUSTOM_TABS_SESSION = "android.support.customtabs.extra.SESSION";
                    final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR";

                    Bundle extras = new Bundle();
                    extras.putBinder(EXTRA_CUSTOM_TABS_SESSION, null);
                    view_intent.putExtras(extras);
                    view_intent.putExtra(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR, Color.parseColor("#3A437B"));
                }

				startActivity(view_intent);
				return true;

            case 1: // reply in browser
                String reply_url = "http://forums.whirlpool.net.au/forum/index.cfm?action=reply&r=" + post.getId();
                Intent reply_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(reply_url));

                if (Build.VERSION.SDK_INT >= 18) {
                    final String EXTRA_CUSTOM_TABS_SESSION = "android.support.customtabs.extra.SESSION";
                    final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR";

                    Bundle extras = new Bundle();
                    extras.putBinder(EXTRA_CUSTOM_TABS_SESSION, null);
                    reply_intent.putExtras(extras);
                    reply_intent.putExtra(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR, Color.parseColor("#3A437B"));
                }

                startActivity(reply_intent);
                return true;

            case 2: // view user info
                Bundle bundle = new Bundle();
                bundle.putParcelable("user", post.getUser());
                ((MainActivity) getActivity()).switchFragment("UserInfo", true, bundle);
                return true;
        }

        return false;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater menuInflater) {
        MenuInflater inflater = getActivity().getMenuInflater();
        inflater.inflate(R.menu.thread, menu);
        super.onCreateOptionsMenu(menu, menuInflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (current_page == 1) {
            menu.findItem(R.id.menu_prev).setEnabled(false);
        }

        // if we came from the watched threads list
        if (from_forum != WhirlpoolApi.WATCHED_THREADS) {
            menu.findItem(R.id.menu_markread).setVisible(false);
        }
        else {
            menu.findItem(R.id.menu_watch).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                long now = System.currentTimeMillis() / 1000;
                // don't refresh too often
                if (now - last_updated > WhirlpoolApi.REFRESH_INTERVAL) {
                    getThread();
                }
                else {
                    Toast.makeText(getActivity(), "Wait " + WhirlpoolApi.REFRESH_INTERVAL + " seconds before refreshing", Toast.LENGTH_LONG).show();
                }
                return true;

            case R.id.menu_next:
                if (current_page < thread.getPageCount()) {
                    current_page++;
                    ((MainActivity) getActivity()).getSupportActionBar().setSelectedNavigationItem(current_page - 1);
                }
                getThread();
                return true;

			/*case R.id.menu_goto_page:
				final CharSequence[] pages = new CharSequence[thread.getPageCount()];
				for (int i = 0; i < pages.length; i++) {
					pages[i] = "" + (i + 1);
				}

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle("Jump to page...");
				builder.setItems(pages, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						current_page = Integer.parseInt((String) pages[item]);
						getThread();
					}
				});
				AlertDialog alert = builder.create();
				alert.show();
				return true;*/

            case R.id.menu_markread:
                try {
                    WatchedThreadTask markread_task = new WatchedThreadTask(thread.getId(), 0, 0);
                    markread_task.execute();
                    Toast.makeText(getActivity(), "Marking thread as read", Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    Toast.makeText(getActivity(), "Error marking thread as read", Toast.LENGTH_SHORT).show();
                }

                return true;

            case R.id.menu_open_browser:
                String thread_url = "http://forums.whirlpool.net.au/forum-replies.cfm?t=" + thread.getId();
                Intent thread_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(thread_url));
                startActivity(thread_intent);
                return true;

            case R.id.menu_prev:
                current_page--;
                getActivity().getActionBar().setSelectedNavigationItem(current_page - 1);
                getThread();
                return true;

            case R.id.menu_goto_last:
                current_page = thread.getPageCount();
                getActivity().getActionBar().setSelectedNavigationItem(current_page - 1);
                getThread();
                return true;

            case R.id.menu_watch:
                WatchedThreadTask watch_task = new WatchedThreadTask(0, 0, thread.getId());
                watch_task.execute();
                Toast.makeText(getActivity(), "Adding thread to watch list", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.menu_replythread:
                String replythread_url = WhirlpoolApi.REPLY_URL + thread.getId();
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

            case android.R.id.home:
                /*Intent dashboard_intent = new Intent(this, Dashboard.class);
                dashboard_intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(dashboard_intent);*/
                return true;
        }
        return false;
    }

    private void getThread() {
        task = new RetrieveThreadTask(); // start new thread to retrieve posts
        task.execute();
    }

    /**
     * Loads the whims into the list
     * @param posts Posts
     */
    private void setPosts(ArrayList<Post> posts) {
        if (posts == null || posts.size() == 0) { // no posts found
            return;
        }

        if (thread.getNotebar() != null && getListView().getHeaderViewsCount() == 0) {
            ListView lv = getListView();
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View header = inflater.inflate(R.layout.notebar, lv, false);
            TextView notebar = (TextView) header.findViewById(R.id.notebar_text);

            String content = thread.getNotebar();

            // links to other threads
            String url_replace = "whirldroid-thread://com.gregdev.whirldroid?threadid=";

            // wiki links
            content = content.replace("href=\"//whirlpool.net.au/wiki/", "href=\"https://whirlpool.net.au/wiki/");
            content = content.replace("href=\"/wiki/", "href=\"https://whirlpool.net.au/wiki/");

            content = content.replace("http://forums.whirlpool.net.au/forum-replies.cfm?t=", url_replace);
            content = content.replace("https://forums.whirlpool.net.au/forum-replies.cfm?t=", url_replace);
            content = content.replace("//forums.whirlpool.net.au/forum-replies.cfm?t=", url_replace);
            content = content.replace("href=\"/forum-replies.cfm?t=", "href=\"" + url_replace);

            notebar.setText(Html.fromHtml(content));

            notebar.setMovementMethod(LinkMovementMethod.getInstance());
            notebar.setLinksClickable(true);
            lv.addHeaderView(header, null, false);
        }

        posts_adapter = new PostsAdapter(getActivity(), R.layout.list_row_post, posts);
        setListAdapter(posts_adapter);

        // scroll to the last post?
        if (bottom) {
            getListView().setSelection(getListView().getCount() - 1);
            bottom = false; // we don't want new page loads to go to the bottom, so unset this
        }
        else if (goto_num != 0) {
            getListView().setSelection(goto_num + getListView().getHeaderViewsCount());
            goto_num = 0; // we don't want new page loads to go to this number, so unset this
        }
    }

    public boolean onNavigationItemSelected(int item_position, long item_id) {
        if (no_page_select) {
            no_page_select = false;
            return false;
        }

        int new_page = item_position + 1;

        // current page selected, no need to do anything
        if (new_page == current_page) {
            return false;
        }

        current_page = new_page;
        getThread();

        return true;
    }
}