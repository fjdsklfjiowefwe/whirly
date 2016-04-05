package com.gregdev.whirldroid.fragment;

import java.util.ArrayList;
import java.util.Date;

import android.support.v4.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
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

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.WhirlpoolApi;
import com.gregdev.whirldroid.WhirlpoolApiException;
import com.gregdev.whirldroid.model.Whim;

/**
 * Displays the latest Whirlpool whims in a nice list format
 * @author Greg
 *
 */
public class WhimListFragment extends ListFragment {

    private ArrayAdapter<Whim> whim_adapter;
    private ArrayList<Whim> whim_list;
    private ProgressDialog progress_dialog;
    private RetrieveWhimsTask task;
    private TextView no_whims;
    private Tracker mTracker;

    private class MarkWhimAsReadTask extends AsyncTask<String, Void, Boolean> {
        private Whim whim;

        public MarkWhimAsReadTask(Whim whim) {
            this.whim = whim;
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                Whirldroid.getApi().downloadWhims(whim.getId());
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
                        Toast.makeText(getActivity(), "Marked whim as read", Toast.LENGTH_SHORT).show();
                        WhimListFragment.this.getWhims(false);
                    }
                }
            });
        }
    }

    /**
     * Private class to retrieve whims in the background
     * @author Greg
     *
     */
    private class RetrieveWhimsTask extends AsyncTask<String, Void, ArrayList<Whim>> {

        private boolean clear_cache = false;
        private String error_message = "";

        public RetrieveWhimsTask(boolean clear_cache) {
            this.clear_cache = clear_cache;
        }

        @Override
        protected ArrayList<Whim> doInBackground(String... params) {
            if (clear_cache || Whirldroid.getApi().needToDownloadWhims()) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            progress_dialog = ProgressDialog.show(getActivity(), "Just a sec...", "Loading whims...", true, true);
                            progress_dialog.setOnCancelListener(new CancelTaskOnCancelListener(task));
                        } catch (BadTokenException e) {
                        }
                    }
                });
                try {
                    Whirldroid.getApi().downloadWhims(0);
                }
                catch (final WhirlpoolApiException e) {
                    error_message = e.getMessage();
                    return null;
                }
            }
            whim_list = Whirldroid.getApi().getWhims();
            return whim_list;
        }

        @Override
        protected void onPostExecute(final ArrayList<Whim> result) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    if (progress_dialog != null) {
                        try {
                            progress_dialog.dismiss(); // hide the progress dialog
                            progress_dialog = null;
                        } catch (Exception e) {
                        }

                        if (result != null) {
                            Toast.makeText(getActivity(), "Whims refreshed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (result != null) {
                        if (whim_list.size() == 0) {
                            no_whims.setVisibility(View.VISIBLE);
                        } else {
                            no_whims.setVisibility(View.GONE);
                            setWhims(whim_list); // display the news in the list
                        }
                    } else {
                        Toast.makeText(getActivity(), error_message, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    /**
     * A private class to format the whim list items
     * @author Greg
     *
     */
    public class WhimAdapter extends ArrayAdapter<Whim> {

        private ArrayList<Whim> whim_items;

        public WhimAdapter(Context context, int textViewResourceId, ArrayList<Whim> whim_items) {
            super(context, textViewResourceId, whim_items);
            this.whim_items = whim_items;
        }

        /**
         * The next two methods are here to avoid issues caused by the system recycling views.
         * This method returns an integer which identifies the view we should use for the
         * corresponding list item
         */
        public int getItemViewType(int position) {
            Whim item = whim_list.get(position);
            if (!item.isRead()) {
                return 1; // highlight as unread
            }

            return 0; // normal, no highlighting
        }

        /**
         * This method needs to return the number of different item view layouts we have
         * eg. sticky + unread + normal = 3, so return 3
         */
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(int position, View convert_view, ViewGroup parent) {
            Whim whim = whim_items.get(position);
            int type = getItemViewType(position);

            if (convert_view == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convert_view = vi.inflate(R.layout.list_row, null);
            }

            if (whim != null) {
                TextView tt = (TextView) convert_view.findViewById(R.id.top_text);
                TextView bt = (TextView) convert_view.findViewById(R.id.bottom_text);
                if (bt == null) {
                    bt = (TextView) convert_view.findViewById(R.id.bottom_left_text);
                }

                if (tt != null) {
                    tt.setText(whim.getFromName());
                }
                if (bt != null){
                    Date date = whim.getDate();
                    String timeText = Whirldroid.getTimeSince(date);

                    bt.setText(timeText + " ago");
                }
            }

            return convert_view;
        }

    }

    /**
     * Cancels the fetching of whims if the back button is pressed
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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Obtain the shared Tracker instance.
        Whirldroid application = (Whirldroid) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        container.removeAllViews();
        View rootView = inflater.inflate(R.layout.whim_list, container, false);

        setHasOptionsMenu(true);

        no_whims = (TextView) rootView.findViewById(R.id.no_whims);
        getWhims(false);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        registerForContextMenu(getListView());
    }

    @Override
    public void onResume() {
        super.onResume();

        mTracker.setScreenName("WhimList");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        ((MainActivity) getActivity()).resetActionBar();
        getActivity().setTitle("Whims");

        ((MainActivity) getActivity()).selectMenuItem("WhimList");
    }

    /**
     * View single whim
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        Whim whim = whim_adapter.getItem(position);

        Bundle bundle = new Bundle();
        bundle.putParcelable("whim", whim);

        ((MainActivity) getActivity()).switchFragment("WhimView", true, bundle);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menu_info) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) menu_info;
        int pos = info.position;

        Whim w = whim_list.get(pos);

        menu.setHeaderTitle(R.string.ctxmenu_whim);

        if (!w.isRead()) {
            menu.add(Menu.NONE, 0, 0, getResources().getText(R.string.ctxmenu_mark_as_read));
        }

        menu.add(Menu.NONE, 1, 1, getResources().getText(R.string.ctxmenu_open_in_browser));
        menu.add(Menu.NONE, 2, 2, getResources().getText(R.string.ctxmenu_reply_in_browser));
    }

    /**
     * Context menu item selection
     */
    @Override
    public boolean onContextItemSelected(android.view.MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        int pos = info.position;

        Whim w = whim_list.get(pos);

        switch (item.getItemId()) {
            case 0: // mark as read
                progress_dialog = ProgressDialog.show(getActivity(), "Just a sec...", "Marking whim as read...", true, true);
                progress_dialog.setOnCancelListener(new CancelTaskOnCancelListener(task));
                MarkWhimAsReadTask task = new MarkWhimAsReadTask(w); // start new thread to mark whim as read
                task.execute();
                return true;

            case 1: // open in browser
                String whim_url = "http://whirlpool.net.au/whim/?action=read&m=" + w.getId();
                Intent whim_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(whim_url));
                startActivity(whim_intent);
                return true;

            case 2: // reply in browser
                String reply_url = "http://whirlpool.net.au/whim/?action=write&rt=" + w.getId();
                Intent reply_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(reply_url));
                startActivity(reply_intent);
                return true;
        }

        return false;
    }


    private void getWhims(boolean clear_cache) {
        task = new RetrieveWhimsTask(clear_cache); // start new thread to retrieve whims
        task.execute();
    }

    /**
     * Loads the whims into the list
     * @param whim_list Whims
     */
    private void setWhims(ArrayList<Whim> whim_list) {
        long last_updated = System.currentTimeMillis() / 1000 - Whirldroid.getApi().getWhimsLastUpdated();

        if (last_updated < 10) { // updated less than 10 seconds ago
            ((MainActivity) getActivity()).getSupportActionBar().setSubtitle("Updated just a moment ago");
        }
        else {
            String ago = Whirldroid.getTimeSince(last_updated);
            ((MainActivity) getActivity()).getSupportActionBar().setSubtitle("Updated " + ago + " ago");
        }


        if (whim_list == null || whim_list.size() == 0) { // no whims found
            return;
        }

        whim_adapter = new WhimAdapter(getActivity(), R.layout.list_row, whim_list);
        setListAdapter(whim_adapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.refresh, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                long now = System.currentTimeMillis() / 1000;
                // don't refresh too often
                if (now - Whirldroid.getApi().getWhimsLastUpdated() > WhirlpoolApi.REFRESH_INTERVAL) {
                    getWhims(true);
                }
                else {
                    Toast.makeText(getActivity(), "Wait " + WhirlpoolApi.REFRESH_INTERVAL + " seconds before refreshing", Toast.LENGTH_LONG).show();
                }
                return true;
        }
        return false;
    }
}