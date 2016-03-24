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

import com.gregdev.whirldroid.R;

public class AboutFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.about, container, false);
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        getActivity().setTitle("About");

        TextView version_info = (TextView) view.findViewById(R.id.version_info);
        TextView gregdev_url = (TextView) view.findViewById(R.id.gregdev_web);

        Pattern p_url = Pattern.compile("http://gregdev.com.au");
        Linkify.addLinks(gregdev_url, p_url, "http://");

        String version = null;
        try {
            version = getActivity().getPackageManager().getPackageInfo("com.gregdev.whirldroid", 0).versionName;
            version_info.setText(String.format(getString(R.string.version_info), version));
        }
        catch (NameNotFoundException e) { }
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