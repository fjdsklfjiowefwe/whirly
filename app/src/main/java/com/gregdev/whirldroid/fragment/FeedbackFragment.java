package com.gregdev.whirldroid.fragment;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;

public class FeedbackFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("whirldroid-thread://com.gregdev.whirldroid?threadid=" + Whirldroid.WHIRLDROID_THREAD_ID));
                startActivity(intent);
            }
        });

        sendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + v.getContext().getPackageName()));
                startActivity(intent);
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();

        Whirldroid.getTracker().setCurrentScreen(getActivity(), "Feedback", null);
        Whirldroid.logScreenView("Feedback");

        MainActivity mainActivity = ((MainActivity) getActivity());
        mainActivity.resetActionBar();
        mainActivity.setTitle("Feedback");
        mainActivity.selectMenuItem("Feedback");
    }

}