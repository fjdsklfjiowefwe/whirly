package com.gregdev.whirldroid.fragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.support.v4.app.ListFragment;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Refresher;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.layout.NewsAdapter;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;
import com.gregdev.whirldroid.layout.SeparatedListAdapter;
import com.gregdev.whirldroid.model.NewsArticle;
import com.gregdev.whirldroid.whirlpool.manager.NewsManager;

/**
 * Displays the latest Whirlpool news in a nice list format
 * @author Greg
 *
 */
public class NewsListFragment extends ListFragment implements Refresher {

    private SeparatedListAdapter sla;
    private ArrayList<NewsArticle> newsList;
    private View rootView;
    private ProgressBar loading;
    private SwipeRefreshLayout mSwipeRefreshLayout;

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
            NewsManager newsManager = Whirldroid.getApi().getNewsManager();

            if (clear_cache || newsManager.needToDownload()) {
                try {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            if (!mSwipeRefreshLayout.isRefreshing()) {
                                loading.setVisibility(View.VISIBLE);
                            }
                        }
                    });
                } catch (NullPointerException e) { }

                try {
                    newsManager.download();

                } catch (final WhirlpoolApiException e) {
                    error_message = e.getMessage();
                    return null;
                }
            }

            newsList = newsManager.getItems();
            return newsList;
        }

        @Override
        protected void onPostExecute(final ArrayList<NewsArticle> result) {
            try {
                getActivity().runOnUiThread(new Runnable() {
                    public void run() {
                        if (mSwipeRefreshLayout.isRefreshing()) {
                            mSwipeRefreshLayout.setRefreshing(false);

                            if (result != null) {
                                Toast.makeText(getActivity(), "News refreshed", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            loading.setVisibility(View.GONE);
                        }

                        if (result != null) {
                            setNews(newsList); // display the news in the list
                        } else {
                            Toast.makeText(getActivity(), error_message, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            } catch (NullPointerException e) { }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        container.removeAllViews();
        rootView = inflater.inflate(R.layout.news_list, container, false);
        setHasOptionsMenu(true);

        loading = (ProgressBar) rootView.findViewById(R.id.loading);
        mSwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swiperefresh);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getNews(false);

        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                initiateRefresh();
            }
        });

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                NewsArticle article = (NewsArticle) sla.getItem(position);

                String news_url = "https://whirlpool.net.au/news/go.cfm?article=" + article.getId();

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
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        Whirldroid.getTracker().setCurrentScreen(getActivity(), "NewsList", null);
        Whirldroid.logScreenView("NewsList");

        MainActivity mainActivity = ((MainActivity) getActivity());
        mainActivity.resetActionBar();
        mainActivity.setTitle("News");

        mainActivity.selectMenuItem("NewsList");
    }


    private void getNews(boolean clear_cache) {
        RetrieveNewsTask task = new RetrieveNewsTask(clear_cache); // start new thread to retrieve the news
        task.execute();
    }

    /**
     * Loads the news items into the list
     * @param news_list News items
     */
    private void setNews(ArrayList<NewsArticle> news_list) {
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
                    NewsAdapter na = new NewsAdapter(getActivity(), articles);
                    sla.addSection(current_day_name, na);
                    articles = new ArrayList<>();
                }
                current_day = day;
                current_day_name = getDayName(day);
            }
            articles.add(article);
        }

        if (!articles.isEmpty()) {
            NewsAdapter na = new NewsAdapter(getActivity(), articles);
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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.refresh, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                initiateRefresh();
                return true;
        }
        return false;
    }

    public boolean initiateRefresh() {
        mSwipeRefreshLayout.setRefreshing(true);
        getNews(true);

        return true;
    }
}