package com.gregdev.whirldroid.login.steps;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.fcannizzaro.materialstepper.AbstractStep;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;

public abstract class LoginStep extends AbstractStep {

    private int i = 1;
    private final static String CLICK = "click";
    private final static String NEXT_DATA = "next";

    private int layout;
    private String title;

    protected View view;

    protected void setLayoutResource(int resource) {
        layout = resource;
    }

    protected void setTitle(String title) {
        this.title = title;

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(layout, container, false);

        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        AppCompatActivity activity = (AppCompatActivity) getActivity();

        toolbar.setTitle(title);
        activity.setSupportActionBar(toolbar);
        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.help, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_help:
                Toast.makeText(getActivity(), "Post your problem in the Whirldroid thread to get help", Toast.LENGTH_LONG).show();
                Whirldroid.openInBrowser(this, WhirlpoolApi.THREAD_URL + Whirldroid.WHIRLDROID_THREAD_ID);

                return true;
        }

        return false;
    }

    @Override
    public String name() {
        return "Tab " + getArguments().getInt("position", 0);
    }

}