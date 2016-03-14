package com.gregdev.whirldroid.fragments;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.app.ListFragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.BadTokenException;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.gregdev.whirldroid.WhirlpoolApi;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.WhirlpoolApiException;
import com.gregdev.whirldroid.layout.SeparatedListAdapter;
import com.gregdev.whirldroid.models.Forum;
import com.gregdev.whirldroid.models.NewsArticle;

/**
 * Displays the latest Whirlpool news in a nice list format
 * @author Greg
 *
 */
public class NewsListFragment extends ListFragment {

    private SeparatedListAdapter sla;
    private ArrayList<NewsArticle> news_list;
    private ProgressDialog progress_dialog;
    private RetrieveNewsTask task;
    private View rootView;
    private ListView newsListView;

    /**
     * Private class to retrieve news in the background
     * @author Greg
     *
     */
    private class RetrieveNewsTask extends AsyncTask<String, Void, ArrayList<NewsArticle>> {

        private boolean clear_cache = false;
        private String error_message = "";

        public RetrieveNewsTask(boolean clear_cache) {
            this.clear_cache = clear_cache;
        }

        @Override
        protected ArrayList<NewsArticle> doInBackground(String... params) {
            if (clear_cache || Whirldroid.getApi().needToDownloadNews()) {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            progress_dialog = ProgressDialog.show(getActivity(), "Just a sec...", "Loading news...", true, true);
                            progress_dialog.setOnCancelListener(new CancelTaskOnCancelListener(task));
                        } catch (BadTokenException e) {
                        }
                    }
                });
                try {
                    Whirldroid.getApi().downloadNews();
                }
                catch (final WhirlpoolApiException e) {
                    error_message = e.getMessage();
                    return null;
                }
            }
            news_list = Whirldroid.getApi().getNewsArticles();
            return news_list;
        }

        @Override
        protected void onPostExecute(final ArrayList<NewsArticle> result) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    if (progress_dialog != null) {
                        try {
                            progress_dialog.dismiss(); // hide the progress dialog
                            progress_dialog = null;
                        } catch (Exception e) {
                        }

                        if (result != null) {
                            Toast.makeText(getActivity(), "News refreshed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    if (result != null) {
                        setNews(news_list); // display the news in the list
                    } else {
                        Toast.makeText(getActivity(), error_message, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    /**
     * A private class to format the news list items
     * @author Greg
     *
     */
    public class NewsAdapter extends ArrayAdapter<NewsArticle> {

        private ArrayList<NewsArticle> news_articles;

        public NewsAdapter(Context context, int textViewResourceId, ArrayList<NewsArticle> newsItems) {
            super(context, textViewResourceId, newsItems);
            this.news_articles = newsItems;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater vi = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = vi.inflate(R.layout.list_row, null);
            }

            final NewsArticle article = news_articles.get(position);

            if (article != null) {
                TextView tt = (TextView) convertView.findViewById(R.id.top_text);
                TextView bt = (TextView) convertView.findViewById(R.id.bottom_text);
                if (tt != null) {
                    tt.setText(article.getTitle());
                }
                if (bt != null){
                    bt.setText(article.getBlurb());
                }
            }

            return convertView;
        }

    }

    /**
     * Cancels the fetching of news if the back button is pressed
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
        rootView = inflater.inflate(R.layout.news_list, container, false);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getActivity().setTitle("News");
        newsListView = getListView();
        getNews(false);
    }


    private void getNews(boolean clear_cache) {
        task = new RetrieveNewsTask(clear_cache); // start new thread to retrieve the news
        task.execute();
    }

    /**
     * Loads the news items into the list
     * @param news_list News items
     */
    private void setNews(ArrayList<NewsArticle> news_list) {
        long last_updated = System.currentTimeMillis() / 1000 - Whirldroid.getApi().getNewsLastUpdated();

        if (last_updated < 10) { // updated less than 10 seconds ago
            //getActivity().getActionBar().setSubtitle("Updated just a moment ago");
        }
        else {
            String ago = Whirldroid.getTimeSince(last_updated);
            //getActivity().getActionBar().setSubtitle("Updated " + ago + " ago");
        }

        if (news_list == null || news_list.size() == 0) { // no news found
            return;
        }

        sla = new SeparatedListAdapter(getActivity());

        int current_day = -1;
        String current_day_name = null;
        ArrayList<NewsArticle> articles = new ArrayList<>();

        for (NewsArticle article : news_list) {
            Date article_date = article.getDate();
            Calendar cal = Calendar.getInstance();
            cal.setTime(article_date);
            int day = cal.get(Calendar.DAY_OF_WEEK);

            if (day != current_day) { // new day
                if (!articles.isEmpty()) {
                    NewsAdapter na = new NewsAdapter(getActivity(), android.R.layout.simple_list_item_1, articles);
                    sla.addSection(current_day_name, na);
                    articles = new ArrayList<>();
                }
                current_day = day;
                current_day_name = getDayName(day);
            }
            articles.add(article);
        }

        if (!articles.isEmpty()) {
            NewsAdapter na = new NewsAdapter(getActivity(), android.R.layout.simple_list_item_1, articles);
            sla.addSection(current_day_name, na);
        }

        ListView listView = (ListView) rootView.findViewById(android.R.id.list);
        listView.setAdapter(sla);
    }

    private String getDayName(int day) {
        switch (day) {
            case Calendar.SUNDAY:
                return "Sunday";
            case Calendar.MONDAY:
                return "Monday";
            case Calendar.TUESDAY:
                return "Tuesday";
            case Calendar.WEDNESDAY:
                return "Wednesday";
            case Calendar.THURSDAY:
                return "Thursday";
            case Calendar.FRIDAY:
                return "Friday";
            case Calendar.SATURDAY:
                return "Saturday";
            default:
                return null;
        }
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        NewsArticle article = (NewsArticle) sla.getItem(position);

        String news_url = "http://whirlpool.net.au/news/go.cfm?article=" + article.getId();

        Intent news_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(news_url));

        if (Build.VERSION.SDK_INT >= 18) {
            final String EXTRA_CUSTOM_TABS_SESSION = "android.support.customtabs.extra.SESSION";
            final String EXTRA_CUSTOM_TABS_TOOLBAR_COLOR = "android.support.customtabs.extra.TOOLBAR_COLOR";

            Bundle extras = new Bundle();
            extras.putBinder(EXTRA_CUSTOM_TABS_SESSION, null);
            news_intent.putExtras(extras);
            news_intent.putExtra(EXTRA_CUSTOM_TABS_TOOLBAR_COLOR, Color.parseColor("#3A437B"));
        }

        startActivity(news_intent);
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
                if (now - Whirldroid.getApi().getNewsLastUpdated() > WhirlpoolApi.REFRESH_INTERVAL) {
                    getNews(true);
                }
                else {
                    Toast.makeText(getActivity(), "Wait " + WhirlpoolApi.REFRESH_INTERVAL + " seconds before refreshing", Toast.LENGTH_SHORT).show();
                }
                return true;

            case android.R.id.home:
                /*Intent dashboard_intent = new Intent(this, Dashboard.class);
                dashboard_intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(dashboard_intent);*/
                return true;
        }
        return false;
    }
}