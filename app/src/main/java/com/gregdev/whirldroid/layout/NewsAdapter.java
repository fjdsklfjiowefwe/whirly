package com.gregdev.whirldroid.layout;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.model.NewsArticle;

import java.util.ArrayList;
import java.util.List;

public class NewsAdapter extends ArrayAdapter<NewsArticle> {

    private List<NewsArticle> newsArticles;
    private Context context;

    public NewsAdapter(Context context, List<NewsArticle> newsItems) {
        super(context, android.R.layout.simple_list_item_1, newsItems);
        this.newsArticles   = newsItems;
        this.context        = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.news_list_item, null);
        }

        final NewsArticle article = newsArticles.get(position);

        if (article != null) {
            TextView title      = (TextView) convertView.findViewById(R.id.top_text     );
            TextView subtitle   = (TextView) convertView.findViewById(R.id.subtitle_text);
            TextView content    = (TextView) convertView.findViewById(R.id.bottom_text  );

            if (title != null) {
                title.setText(article.getTitle());
            }

            if (subtitle != null) {
                subtitle.setText(article.getSource());
            }

            if (content != null){
                content.setText(article.getBlurb());
            }
        }

        return convertView;
    }

}