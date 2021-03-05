package com.gregdev.whirldroid.fragment;

import java.util.regex.Pattern;

import android.support.v4.app.Fragment;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;

public class AboutFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        container.removeAllViews();
        View rootView = inflater.inflate(R.layout.about, container, false);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        TextView version_info   = view.findViewById(R.id.version_info);
        TextView gregdev_url    = view.findViewById(R.id.gregdev_web);

        Pattern p_url = Pattern.compile("https://gregdev.com.au");
        Linkify.addLinks(gregdev_url, p_url, "https://");

        String version = null;
        try {
            version = view.getContext().getPackageManager().getPackageInfo("com.gregdev.whirldroid", 0).versionName;
            version_info.setText(String.format(getString(R.string.version_info), version));
        }
        catch (NameNotFoundException e) { }
    }

    @Override
    public void onResume() {
        super.onResume();

        Whirldroid.getTracker().setCurrentScreen(getActivity(), "About", null);
        Whirldroid.logScreenView("About");

        MainActivity mainActivity = ((MainActivity) getActivity());
        mainActivity.resetActionBar();
        mainActivity.setTitle("About");
        mainActivity.selectMenuItem("About");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

}