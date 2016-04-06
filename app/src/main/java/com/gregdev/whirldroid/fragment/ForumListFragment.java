package com.gregdev.whirldroid.fragment;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.ListFragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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
import com.gregdev.whirldroid.service.DatabaseHandler;

import java.util.ArrayList;

public class ForumListFragment extends ListFragment {

    private SeparatedListAdapter forum_adapter;
    private ArrayList<Forum> forum_list;
    private RetrieveForumsTask task;
    private int list_position;
    private ListView forum_listview;
    private View rootView;
    private SwipeRefreshLayout mSwipeRefreshLayout;

    private int listIndex = -1;

    private Tracker mTracker;

    private class RetrieveForumsTask extends AsyncTask<String, Void, ArrayList<Forum>> {

        private boolean clear_cache = false;
        private String error_message = "";

        public RetrieveForumsTask(boolean clear_cache) {
            this.clear_cache = clear_cache;
        }

        @Override
        protected ArrayList<Forum> doInBackground(String... params) {
            if (clear_cache || Whirldroid.getApi().needToDownloadForums()) {
                try {
                    Whirldroid.getApi().downloadForums();
                }
                catch (final WhirlpoolApiException e) {
                    error_message = e.getMessage();
                    return null;
                }
            }
            forum_list = Whirldroid.getApi().getForums();
            return forum_list;
        }

        @Override
        protected void onPostExecute(final ArrayList<Forum> result) {
            try {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (result != null) {
                            if (mSwipeRefreshLayout.isRefreshing()) {
                                Toast.makeText(getActivity(), "Forums refreshed", Toast.LENGTH_SHORT).show();
                            }

                            setForums(forum_list); // display the forums in the list
                        } else {
                            Toast.makeText(getActivity(), "Error: " + error_message, Toast.LENGTH_LONG).show();
                        }
                    }
                });
                mSwipeRefreshLayout.setRefreshing(false);
            } catch (NullPointerException e) { }
        }
    }

    private void getForums(boolean clear_cache) {
        task = new RetrieveForumsTask(clear_cache); // start new thread to retrieve forums
        task.execute();
    }

    private void setForums(ArrayList<Forum> forum_list) {
        if (forum_list == null || forum_list.size() == 0) { // no forums found
            return;
        }

        forum_adapter = new SeparatedListAdapter(getActivity());

        // get favourite forums
        DatabaseHandler db = new DatabaseHandler(getActivity());
        ArrayList<Forum> favourites = db.getFavouriteForums();

        if (favourites.size() > 0) {
            ForumAdapter fa = new ForumAdapter(getActivity(), android.R.layout.simple_list_item_1, favourites);
            forum_adapter.addSection("Favourites", fa);
        }

        // keep track of the current section (forums are divided into sections)
        String current_section = null;
        ArrayList<Forum> forums = new ArrayList<Forum>();

        for (Forum f : forum_list) {
            if (!f.getSection().equals(current_section)) { // we've reached a new section
                if (!forums.isEmpty()) { // there are items from a previous section
                    ForumAdapter fa = new ForumAdapter(getActivity(), android.R.layout.simple_list_item_1, forums);
                    forum_adapter.addSection(current_section, fa);
                    //articles.clear();
                    forums = new ArrayList<Forum>();
                }
                current_section = f.getSection();
            }
            forums.add(f);
        }
        // if we have forums to display
        if (!forums.isEmpty()) {
            ForumAdapter fa = new ForumAdapter(getActivity(), android.R.layout.simple_list_item_1, forums);
            forum_adapter.addSection(current_section, fa);
        }

        setListAdapter(forum_adapter); // display the forums
    }

    public class ForumAdapter extends ArrayAdapter<Forum> {

        private ArrayList<Forum> forum_items;

        public ForumAdapter(Context context, int textViewResourceId, ArrayList<Forum> forum_items) {
            super(context, textViewResourceId, forum_items);
            this.forum_items = forum_items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Forum forum = forum_items.get(position);
            View v = convertView;

            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.list_row_single, null);
            }

            if (forum != null) {
                TextView tt = (TextView) v.findViewById(R.id.top_text);
                if (tt != null) {
                    tt.setText(forum.getTitle());
                }
            }

            ImageButton btn = (ImageButton) v.findViewById(R.id.menu_button);
            registerForContextMenu(btn);

            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PopupMenu popupMenu = new PopupMenu(getContext(), v);
                    popupMenu.inflate(R.menu.forum_list_item);

                    DatabaseHandler db = new DatabaseHandler(getActivity());

                    if (db.isInFavourites(forum)) {
                        popupMenu.getMenu().findItem(R.id.menu_add_favourite).setVisible(false);
                        popupMenu.getMenu().findItem(R.id.menu_remove_favourite).setVisible(true);
                    }

                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            DatabaseHandler db = new DatabaseHandler(getActivity());
                            switch (item.getItemId()) {
                                case R.id.menu_add_favourite:
                                    db.addFavouriteForum(forum);
                                    getForums(false);
                                    return true;
                                case R.id.menu_remove_favourite:
                                    db.removeFavouriteForum(forum);
                                    getForums(false);
                                    return true;
                                case R.id.menu_new_thread:
                                    Intent newthread_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(WhirlpoolApi.NEWTHREAD_URL + forum.getId()));
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
                                default:
                                    return false;
                            }
                        }
                    });

                    popupMenu.show();
                }
            });

            return v;
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
        container.removeAllViews();
        rootView = inflater.inflate(R.layout.forum_list, container, false);
        ((MainActivity) getActivity()).selectMenuItem("ForumList");
        setHasOptionsMenu(true);

        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swiperefresh);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        forum_listview = getListView();

        if (forum_listview.getCount() == 0) {
            getForums(false);
        }

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initiateRefresh();
            }
        });
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Forum forum = (Forum) forum_adapter.getItem(position);

        Bundle bundle = new Bundle();
        bundle.putInt("forum_id", forum.getId());
        bundle.putString("forum_name", forum.getTitle());

        ((MainActivity) getActivity()).switchFragment("ThreadList", true, bundle);
    }

    @Override
    public void onResume() {
        super.onResume();

        mTracker.setScreenName("ForumList");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        MainActivity mainActivity = ((MainActivity) getActivity());

        mainActivity.resetActionBar();
        getActivity().setTitle("Forums");

        if (listIndex != -1){
            forum_listview.setSelection(listIndex);
        }

        mainActivity.setCurrentSearchType(mainActivity.SEARCH_FORUMS);

        mainActivity.selectMenuItem("ForumList");
    }

    @Override
    public void onPause() {
        super.onPause();

        try {
            listIndex = forum_listview.getFirstVisiblePosition();
        } catch (Exception e) { }

    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.refresh, menu);
        super.onCreateOptionsMenu(menu, inflater);
        //Create the search view
        SearchView search_view = new SearchView(((MainActivity) getActivity()).getSupportActionBar().getThemedContext());
        search_view.setQueryHint("Search for threads…");
        search_view.setOnQueryTextListener((MainActivity) getActivity());

        menu.add("Search")
                .setIcon(R.drawable.ic_search_white_24dp)
                .setActionView(search_view)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                return initiateRefresh();
        }
        return false;
    }

    public boolean initiateRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        getForums(true);

        return true;
    }

}