package com.gregdev.whirldroid.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.gregdev.whirldroid.R;

import java.util.Locale;

public class ForumListFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.forum_list, container, false);

        getActivity().setTitle("Forums");
        return rootView;
    }

}