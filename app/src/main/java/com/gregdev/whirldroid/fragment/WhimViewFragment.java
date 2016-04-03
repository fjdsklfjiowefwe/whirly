package com.gregdev.whirldroid.fragment;

import android.support.v4.app.Fragment;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.WhirlpoolApiException;
import com.gregdev.whirldroid.model.Whim;

public class WhimViewFragment extends Fragment {
    private TextView whimContent;
    private Whim whim = null;
    private Tracker mTracker;

    private class MarkWhimAsReadTask extends AsyncTask<Whim, Integer, Integer> {

        @Override
        protected Integer doInBackground(Whim... whims) {
            try {
                Whirldroid.getApi().downloadWhims(whims[0].getId());
            }
            catch (WhirlpoolApiException e) {
                // error marking whim as read, meh
            }

            return null;
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
        View rootView = inflater.inflate(R.layout.whim_view, container, false);

        Bundle bundle = getArguments();
        if (bundle != null) {
            whim = bundle.getParcelable("whim");
        }

        whimContent = (TextView) rootView.findViewById(R.id.whim_content);
        whimContent.setText(whim.getContent());

        ((MainActivity) getActivity()).getSupportActionBar().setSubtitle("From " + whim.getFromName());

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());

        // if then whim is unread and the option to auto mark as read is selected
        if (!whim.isRead() && settings.getBoolean("pref_whimautomarkasread", true)) {
            new MarkWhimAsReadTask().execute(whim);
        }

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
    }

    @Override
    public void onResume() {
        super.onResume();

        mTracker.setScreenName("WhimView");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.whim, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        // hide the Mark as Read option if auto mark as read is enabled
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
        if (settings.getBoolean("pref_whimautomarkasread", true)) {
            menu.findItem(R.id.menu_whimmarkread).setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_whimmarkread:
                new MarkWhimAsReadTask().execute(whim);
                Toast.makeText(getActivity(), "Marking whim as read", Toast.LENGTH_SHORT).show();
                return true;

            case R.id.menu_whimopenbrowser:
                String whim_url = "http://whirlpool.net.au/whim/?action=read&m=" + whim.getId();
                Intent whim_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(whim_url));
                startActivity(whim_intent);
                return true;

            case R.id.menu_whimreply:
                String reply_url = "http://whirlpool.net.au/whim/?action=write&rt=" + whim.getId();
                Intent reply_intent = new Intent(Intent.ACTION_VIEW, Uri.parse(reply_url));
                startActivity(reply_intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
}