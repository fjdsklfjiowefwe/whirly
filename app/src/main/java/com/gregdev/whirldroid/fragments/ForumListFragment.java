package com.gregdev.whirldroid.fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.PopupMenu;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.WhirlpoolApiException;
import com.gregdev.whirldroid.layout.SeparatedListAdapter;
import com.gregdev.whirldroid.models.Forum;
import com.gregdev.whirldroid.services.DatabaseHandler;

import java.util.ArrayList;

public class ForumListFragment extends ListFragment {

    private SeparatedListAdapter forum_adapter;
    private ArrayList<Forum> forum_list;
    private ProgressDialog progress_dialog;
    private RetrieveForumsTask task;
    private int list_position;
    private ListView forum_listview;
    private View rootView;

    private int listIndex = -1;

    private class RetrieveForumsTask extends AsyncTask<String, Void, ArrayList<Forum>> {

        private boolean clear_cache = false;
        private String error_message = "";

        public RetrieveForumsTask(boolean clear_cache) {
            this.clear_cache = clear_cache;
        }

        @Override
        protected ArrayList<Forum> doInBackground(String... params) {
            if (clear_cache || Whirldroid.getApi().needToDownloadForums()) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        progress_dialog = ProgressDialog.show(getActivity(), "Just a sec...", "Loading forums...", true, true);
                    }
                });
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
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    if (progress_dialog != null) {
                        try {
                            progress_dialog.dismiss(); // hide the progress dialog
                            progress_dialog = null;
                        } catch (Exception e) {
                        }

                        if (result != null) {
                            Toast.makeText(getActivity(), "Forums refreshed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (result != null) {
                        setForums(forum_list); // display the forums in the list
                    } else {
                        Toast.makeText(getActivity(), error_message, Toast.LENGTH_LONG).show();
                    }
                }
            });
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

                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            switch (item.getItemId()) {
                                case R.id.menu_add_favourite:
                                    DatabaseHandler db = new DatabaseHandler(getActivity());
                                    db.addFavouriteForum(forum);
                                    getForums(false);
                                    return true;
                                case R.id.menu_new_thread:
                                    Toast.makeText(getActivity(), "New thread in " + forum.getId(), Toast.LENGTH_SHORT).show();
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.forum_list, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        forum_listview = getListView();
        getActivity().setTitle("Forums");
        if (forum_listview.getCount() == 0) {
            getForums(false);
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Forum forum = (Forum) forum_adapter.getItem(position);

        Fragment fragment = new ThreadListFragment();

        Bundle bundle = new Bundle();
        bundle.putInt("forum_id", forum.getId());
        bundle.putString("forum_name", forum.getTitle());

        fragment.setArguments(bundle);

        getFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out, R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.content_frame, fragment)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (listIndex != -1){
            forum_listview.setSelection(listIndex);
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        try {
            listIndex = forum_listview.getFirstVisiblePosition();
        } catch (Exception e) { }

    }

}