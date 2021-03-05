package com.gregdev.whirldroid.setup.steps;

import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;

public class IntroStep extends SetupStep {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setLayoutResource(R.layout.setup_step_intro);
        setStepTitle("Whirldroid");

        // Obtain the shared Tracker instance.
        try {
            Whirldroid.getTracker().setCurrentScreen(getActivity(), "Setup: Start", null);
        } catch (NullPointerException e) {
            Crashlytics.logException(e);
        }
    }

}