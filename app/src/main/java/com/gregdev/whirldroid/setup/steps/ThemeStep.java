package com.gregdev.whirldroid.setup.steps;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.CompoundButtonCompat;
import android.support.v7.widget.AppCompatRadioButton;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import com.github.fcannizzaro.materialstepper.AbstractStep;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.setup.SteppedSetup;

import java.util.ArrayList;

public class ThemeStep extends SetupStep {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setLayoutResource(R.layout.setup_step_theme);
        setStepTitle("Theme");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());

        if (Integer.parseInt(settings.getString("pref_theme", "0")) == Whirldroid.DARK_THEME) {
            RadioButton light   = (RadioButton) view.findViewById(R.id.theme_light);
            RadioButton dark    = (RadioButton) view.findViewById(R.id.theme_dark);

            light.setChecked(false);
            dark.setChecked(true);
        }

        View.OnClickListener onClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean checked     = ((RadioButton) v).isChecked();
                int selectedTheme   = 0;

                // Check which radio button was clicked
                switch (v.getId()) {
                    case R.id.theme_light:
                        if (checked) {
                            selectedTheme = Whirldroid.LIGHT_THEME;
                            break;
                        }
                    case R.id.theme_dark:
                        if (checked) {
                            selectedTheme = Whirldroid.DARK_THEME;
                            break;
                        }
                }

                setThemeColours(selectedTheme);

                SharedPreferences.Editor settingsEditor = settings.edit();
                settingsEditor.putString("pref_theme", selectedTheme + "");
                settingsEditor.apply();
            }
        };

        view.findViewById(R.id.theme_light  ).setOnClickListener(onClickListener);
        view.findViewById(R.id.theme_dark   ).setOnClickListener(onClickListener);
    }

}