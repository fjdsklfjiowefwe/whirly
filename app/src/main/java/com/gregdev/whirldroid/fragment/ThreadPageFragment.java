package com.gregdev.whirldroid.fragment;

import java.util.ArrayList;

import android.app.ProgressDialog;
import android.support.v4.app.ListFragment;
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
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Pair;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;
import com.gregdev.whirldroid.model.Post;
import com.gregdev.whirldroid.model.Thread;

/**
 * Displays the latest Whirlpool whims in a nice list format
 * @author Greg
 *
 */
public class ThreadPageFragment extends ListFragment {

    private ArrayAdapter<Post> posts_adapter;
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
    private int filter = 0;
    private String filter_user = null;
    private String filter_user_id = null;
    private boolean pages_loaded = false;
    private boolean no_page_select = true;
    private String font_size_option = "0";
    private Tracker mTracker;
    private ProgressBar loading;
    ViewPager parent;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    boolean hasNotebar = false;

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
            try {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (!mSwipeRefreshLayout.isRefreshing()) {
                            loading.setVisibility(View.VISIBLE);
                        }
                    }
                });

                Thread thread = null;
                try {
                    thread = Whirldroid.getApi().downloadThread(thread_id, thread_title, current_page, filter, filter_user_id);
                } catch (final WhirlpoolApiException e) {
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

            } catch (NullPointerException e) { return null; }
        }

        @Override
        protected void onPostExecute(final Thread result) {
            try {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        loading.setVisibility(View.GONE);

                        if (mSwipeRefreshLayout.isRefreshing()) {
                            mSwipeRefreshLayout.setRefreshing(false);
                            Toast.makeText(getActivity(), "Page refreshed", Toast.LENGTH_SHORT).show();
                        }

                        if (result != null) {
                            last_updated = System.currentTimeMillis() / 1000;

                            page_count = result.getPageCount();
                            if (parent != null) {
                                ((ThreadViewFragment.ThreadPageFragmentPagerAdapter) parent.getAdapter()).setCount(page_count);
                            }

                            if (current_page == -1) { // -1 indicates we're on the last page
                                current_page = page_count;
                            }

                            thread_title = result.getTitle();

                            thread = result;
                            setPosts(result.getPosts()); // display the posts in the list
                        } else {
                            Toast.makeText(getActivity(), error_message, Toast.LENGTH_LONG).show();
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
                TextView top_left_text      = (TextView) convert_view.findViewById(R.id.top_left_text       );
                TextView top_right_text     = (TextView) convert_view.findViewById(R.id.top_right_text      );
                TextView middle_left_text   = (TextView) convert_view.findViewById(R.id.middle_left_text    );
                TextView middle_right_text  = (TextView) convert_view.findViewById(R.id.middle_right_text   );
                TextView bottom_text        = (TextView) convert_view.findViewById(R.id.bottom_text         );

                top_left_text.setText(post.getUser().getName());
                top_right_text.setText(post.getPostedTime());

                if (font_size_option.equals("0.5")) {
                    bottom_text.setTextSize(19);
                } else if (font_size_option.equals("1")) {
                    bottom_text.setTextSize(20);
                } else if (font_size_option.equals("2")) {
                    bottom_text.setTextSize(22);
                } else if (font_size_option.equals("-1")) {
                    bottom_text.setTextSize(16);
                } else if (font_size_option.equals("-2")) {
                    bottom_text.setTextSize(14);
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
                content = content.replaceAll("<p class=\"reference\"><a (.*?) title=\"(.*?)\">(.*?)</a></p>",
                        "<p><font color='" + user_quote_colour + "'><b><a href=\"whirldroid-reply://$2\">$3</a></b></font></p>");

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

                content = content.replace("<li>", "•");
                content = content.replace("â€¢", "");
                content = content.replace("</li>", "");

                CharSequence sequence = Html.fromHtml(content);
                SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
                URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);
                for (URLSpan span : urls) {
                    makeLinkClickable(strBuilder, span);
                }

                try {
                    bottom_text.setText(strBuilder);

                } catch (ArrayIndexOutOfBoundsException e) { // weird Jelly Bean bug
                    // just pull out a bunch of style tags and hope the problem goes away
                    content = content.replace("<b>", "");
                    content = content.replace("</b>", "");
                    content = content.replace("<strong>", "");
                    content = content.replace("</strong>", "");
                    content = content.replace("<i>", "");
                    content = content.replace("</i>", "");
                    content = content.replace("<em>", "");
                    content = content.replace("</em>", "");

                    bottom_text.setText(strBuilder);
                }

                bottom_text.setMovementMethod(LinkMovementMethod.getInstance());
                bottom_text.setLinksClickable(true);
            }
            return convert_view;
        }
    }

    // http://stackoverflow.com/a/19989677/602734
    protected void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span) {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);

        ClickableSpan clickable = new ClickableSpan() {
            public void onClick(View view) {
                final String replyId = span.getURL().replace("whirldroid-reply://", "");

                // either scroll to the reply on this page, or find which page the reply is on
                if (!scrollToReply(replyId)) {

                    // if we get here, then the reply is on a different page - BUT WHICH ONE?!

                    final ProgressDialog progressDialog = new ProgressDialog(getActivity());
                    progressDialog.setMessage("Finding reply...");
                    progressDialog.show();

                    java.lang.Thread thread = new java.lang.Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                final Pair<Integer, Integer> postLocation = Whirldroid.getApi().getPostLocation(replyId);

                                getActivity().runOnUiThread(new Runnable() {
                                    public void run() {
                                        progressDialog.hide();

                                        if (postLocation.first == thread_id || thread_id == 0) { // same thread
                                            ((ThreadViewFragment.ThreadPageFragmentPagerAdapter) parent.getAdapter()).setScrollToReply(replyId);
                                            parent.setCurrentItem(postLocation.second - 1);

                                        } else { // post is in a different thread
                                            Bundle bundle = new Bundle();

                                            bundle.putInt("thread_id"           , postLocation.first    );
                                            bundle.putInt("page_number"         , postLocation.second   );
                                            bundle.putString("scroll_to_post"   , replyId               );

                                            ((MainActivity) getActivity()).switchFragment("ThreadView", true, bundle);
                                        }
                                    }
                                });

                            } catch (final WhirlpoolApiException e) {
                                getActivity().runOnUiThread(new Runnable() {
                                    public void run() {
                                        progressDialog.hide();
                                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    });

                    thread.start();
                }
            }
        };

        // check if we need to handle this click
        if (span.getURL().contains("whirldroid-reply://")) {
            strBuilder.removeSpan(span);
            strBuilder.setSpan(clickable, start, end, flags);
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
        View rootView = inflater.inflate(R.layout.thread_list, container, false);

        try {
            parent = (ViewPager) container;
        } catch (ClassCastException e) {
            parent = null;
        }

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swiperefresh);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {

        Bundle bundle = getArguments();

        if (bundle != null) {
            thread_id       = bundle.getInt("thread_id");
            thread_title    = bundle.getString("thread_title");
            current_page    = bundle.getInt("page_number");
            bottom          = bundle.getBoolean("bottom");
            goto_num        = bundle.getInt("goto_num");
            from_forum      = bundle.getInt("from_forum");
            filter          = bundle.getInt("filter");
            filter_user     = bundle.getString("filter_user", null);
            filter_user_id  = bundle.getString("filter_user_id", null);

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(Whirldroid.getContext());
            font_size_option = settings.getString("pref_postfontsize", "0");
        }

        if (filter_user_id != null) {
            getListView().setPadding(0, 0, 0, 0);
        }

        registerForContextMenu(getListView());

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

        mTracker.setScreenName("ThreadView");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        if (filter_user_id != null) {
            MainActivity mainActivity = (MainActivity) getActivity();

            mainActivity.resetActionBar();

            mainActivity.setTitle("Posts by " + filter_user);
            mainActivity.getSupportActionBar().setSubtitle("");
        }

        if (last_updated == 0 || thread == null) {
            getThread();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menu_info) {
        menu.setHeaderTitle(R.string.ctxmenu_post);

        menu.add(Menu.NONE, 1, 1, getResources().getText(R.string.ctxmenu_reply_in_browser));
        menu.add(Menu.NONE, 2, 2, getResources().getText(R.string.ctxmenu_user_info));
        menu.add(Menu.NONE, 3, 3, getResources().getText(R.string.ctxmenu_user_posts));
    }

    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        if (getUserVisibleHint()) {
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

                case 3: // view user's posts
                    ((ThreadViewFragment) getParentFragment()).setFilterUser(post.getUser().getName(), post.getUser().getId());
                    return true;
            }
        }

        return false;
    }

    public void getThread() {
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

        try {
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
                hasNotebar = true;
            }
        } catch (Exception e) { }

        posts_adapter = new PostsAdapter(getActivity(), R.layout.list_row_post, posts);
        setListAdapter(posts_adapter);

        // scroll to the last post?
        if (bottom) {
            getListView().setSelection(getListView().getCount() - 1);
            bottom = false; // we don't want new page loads to go to the bottom, so unset this
        } else if (goto_num != 0) {
            getListView().setSelection(goto_num + getListView().getHeaderViewsCount());
            goto_num = 0; // we don't want new page loads to go to this number, so unset this
        }

        doScrollToReply();
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

    public boolean initiateRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        getThread();

        return true;
    }

    public boolean scrollToReply(String replyId) {
        return scrollToReply(replyId, false);
    }

    public boolean scrollToReply(String replyId, final boolean jump) {
        for (int i = 0; i < posts_adapter.getCount(); i++) {
            Post post = posts_adapter.getItem(i);
            if (post.getId().equals(replyId)) {
                if (hasNotebar) {
                    i++; // take notebar into account when scrolling to position
                }

                final int scrollTo = i;

                getListView().post(new Runnable() {
                    @Override
                    public void run() {
                        if (jump) {
                            getListView().setSelection(scrollTo);
                        } else {
                            getListView().smoothScrollToPosition(scrollTo);
                        }
                    }
                });

                return true;
            }
        }

        return false;
    }

    public void doScrollToReply() {
        try {
            ThreadViewFragment.ThreadPageFragmentPagerAdapter parentAdapter = ((ThreadViewFragment.ThreadPageFragmentPagerAdapter) parent.getAdapter());
            if (parentAdapter.getScrollToReply() != null && posts_adapter != null && posts_adapter.getCount() > 0) {
                scrollToReply(parentAdapter.getScrollToReply(), true);
                parentAdapter.setScrollToReply(null);
            }
        } catch (NullPointerException e) { }
    }
}