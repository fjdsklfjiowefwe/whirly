package com.gregdev.whirldroid.login.steps;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.fcannizzaro.materialstepper.AbstractStep;

public abstract class LoginStep extends AbstractStep {

    private int layout;
    private String title;

    protected View view;

    protected void setLayoutResource(int resource) {
        layout = resource;
    }

    protected void setStepTitle(String title) {
        this.title = title;
    }

    public String getStepTitle() {
        return title;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        view = inflater.inflate(layout, container, false);
        return view;
    }

    @Override
    public String name() {
        return "Tab " + getArguments().getInt("position", 0);
    }

}