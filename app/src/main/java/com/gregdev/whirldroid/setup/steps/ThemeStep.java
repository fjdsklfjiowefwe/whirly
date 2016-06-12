package com.gregdev.whirldroid.setup.steps;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.RadioButton;
import android.widget.TextView;

import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;

public class ThemeStep extends SetupStep {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setLayoutResource(R.layout.setup_step_theme);
        setStepTitle("Theme");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        final View myView = view;

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean checked = ((RadioButton) v).isChecked();

                int selectedTheme               = 0;
                int backgroundColourResource    = 0;
                int primaryTextColourResource   = 0;

                // Check which radio button was clicked
                switch(v.getId()) {
                    case R.id.theme_light:
                        if (checked) {
                            selectedTheme               = Whirldroid.LIGHT_THEME;
                            backgroundColourResource    = R.color.background_colour_light;
                            primaryTextColourResource   = R.color.TextColourPrimaryLight;

                            break;
                        }
                    case R.id.theme_dark:
                        if (checked) {
                            selectedTheme               = Whirldroid.DARK_THEME;
                            backgroundColourResource    = R.color.background_colour_dark;
                            primaryTextColourResource   = R.color.TextColourPrimaryDark;

                            break;
                        }
                }

                myView.setBackgroundColor(getResources().getColor(backgroundColourResource));

                TextView textView = (TextView) myView.findViewById(R.id.textView);
                textView.setTextColor(getResources().getColor(primaryTextColourResource));

                View activityView = getActivity().findViewById(R.id.navigation);
                activityView.setBackgroundColor(getResources().getColor(backgroundColourResource));

                RadioButton radioLight  = (RadioButton) myView.findViewById(R.id.theme_light);
                RadioButton radioDark   = (RadioButton) myView.findViewById(R.id.theme_dark );

                radioLight.setTextColor(getResources().getColor(primaryTextColourResource));
                radioDark.setTextColor(getResources().getColor(primaryTextColourResource));

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor settingsEditor = settings.edit();
                settingsEditor.putString("pref_theme", selectedTheme + "");
                settingsEditor.apply();
                Whirldroid.log("pref_theme: " + selectedTheme);
            }
        };

        view.findViewById(R.id.theme_light  ).setOnClickListener(onClickListener);
        view.findViewById(R.id.theme_dark   ).setOnClickListener(onClickListener);
    }

}