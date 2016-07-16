package com.gregdev.whirldroid.fragment;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;

public class FeedbackFragment extends Fragment {

    private Tracker mTracker;

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
        return inflater.inflate(R.layout.feedback, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Button openThread       = (Button) view.findViewById(R.id.open_thread);
        Button sendEmail        = (Button) view.findViewById(R.id.send_email);
        Button reviewInStore    = (Button) view.findViewById(R.id.review_in_store);

        openThread.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFeedbackButtonClick("Whirldroid Thread");

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("whirldroid-thread://com.gregdev.whirldroid?threadid=" + Whirldroid.WHIRLDROID_THREAD_ID));
                startActivity(intent);
            }
        });

        sendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFeedbackButtonClick("Email Greg");

                final Intent email_intent = new Intent(android.content.Intent.ACTION_SEND);

                // add email data to the intent
                email_intent.setType("plain/text");
                email_intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"greg@gregdev.com.au"});
                email_intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Whirldroid 2 feedback");
                email_intent.putExtra(android.content.Intent.EXTRA_TEXT, "");

                startActivity(Intent.createChooser(email_intent, "Send mail..."));
            }
        });

        reviewInStore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFeedbackButtonClick("Review in Play Store");

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + getActivity().getPackageName()));
                startActivity(intent);
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        mTracker.setScreenName("Feedback");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());

        MainActivity mainActivity = ((MainActivity) getActivity());
        mainActivity.resetActionBar();
        mainActivity.setTitle("Feedback");
        mainActivity.selectMenuItem("Feedback");
    }

    private void onFeedbackButtonClick(String label) {
        mTracker.send(new HitBuilders.EventBuilder()
                .setCategory("Feedback")
                .setAction("Click")
                .setLabel(label)
                .build());
    }

}