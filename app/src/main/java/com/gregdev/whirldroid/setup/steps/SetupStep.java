package com.gregdev.whirldroid.setup.steps;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.CompoundButtonCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

import com.github.fcannizzaro.materialstepper.AbstractStep;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.setup.SteppedSetup;

import java.util.ArrayList;

public abstract class SetupStep extends AbstractStep {

    private int layout;
    private String title;

    public View view;

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
    public void onResume() {
        super.onResume();

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
        setThemeColours(Integer.parseInt(settings.getString("pref_theme", "0")));
    }

    @Override
    public String name() {
        return "Tab " + getArguments().getInt("position", 0);
    }

    // http://stackoverflow.com/a/18668935/602734
    private ArrayList<View> getAllChildren(View v) {
        if (!(v instanceof ViewGroup)) {
            ArrayList<View> viewArrayList = new ArrayList<View>();
            viewArrayList.add(v);
            return viewArrayList;
        }

        ArrayList<View> result = new ArrayList<>();

        ViewGroup viewGroup = (ViewGroup) v;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {

            View child = viewGroup.getChildAt(i);

            ArrayList<View> viewArrayList = new ArrayList<>();
            viewArrayList.add(v);
            viewArrayList.addAll(getAllChildren(child));

            result.addAll(viewArrayList);
        }
        return result;
    }

    public void setThemeColours(int selectedTheme) {
        int backgroundColourResource    = 0;
        int primaryTextColourResource   = 0;
        int highlightColourResource     = 0;

        int highlightColourLight    = getResources().getColor(R.color.colorPrimary);
        int highlightColourDark     = getResources().getColor(R.color.colorAccentDark);


        if (selectedTheme == Whirldroid.LIGHT_THEME) {
            backgroundColourResource    = R.color.background_colour_light;
            primaryTextColourResource   = R.color.TextColourPrimaryLight;
            highlightColourResource     = R.color.colorPrimary;

        } else if (selectedTheme == Whirldroid.DARK_THEME) {
            backgroundColourResource    = R.color.background_colour_dark;
            primaryTextColourResource   = R.color.TextColourPrimaryDark;
            highlightColourResource     = R.color.colorAccentDark;
        }

        int primaryTextColour   = getResources().getColor(primaryTextColourResource );
        int backgroundColour    = getResources().getColor(backgroundColourResource  );
        int highlightColour     = getResources().getColor(highlightColourResource   );

        View activityView = getActivity().findViewById(R.id.navigation);
        activityView.setBackgroundColor(backgroundColour);

        for (AbstractStep step : ((SteppedSetup) getActivity()).steps) {
            View stepView = ((SetupStep) step).view;

            if (stepView != null) {
                ((SetupStep) step).view.setBackgroundColor(backgroundColour);

                for (View child : getAllChildren(((SetupStep) step).view)) {
                    if (child instanceof RadioButton) {
                        RadioButton radioButton = (RadioButton) child;

                        ColorStateList colorStateList = new ColorStateList(
                                new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}},
                                new int[]{primaryTextColour, highlightColour}
                        );

                        radioButton.setTextColor(primaryTextColour);
                        CompoundButtonCompat.setButtonTintList(radioButton, colorStateList);

                    } else if (child instanceof CheckBox) {
                        CheckBox checkBox = (CheckBox) child;

                        ColorStateList colorStateList = new ColorStateList(
                                new int[][]{new int[]{-android.R.attr.state_checked}, new int[]{android.R.attr.state_checked}},
                                new int[]{primaryTextColour, highlightColour}
                        );

                        checkBox.setTextColor(primaryTextColour);
                        CompoundButtonCompat.setButtonTintList(checkBox, colorStateList);

                    } else if (child instanceof TextView) {
                        TextView textView = (TextView) child;

                        if (textView.getCurrentTextColor() == highlightColourLight || textView.getCurrentTextColor() == highlightColourDark) {
                            textView.setTextColor(highlightColour);
                        } else {
                            textView.setTextColor(primaryTextColour);
                        }
                    }
                }
            }
        }
    }

}