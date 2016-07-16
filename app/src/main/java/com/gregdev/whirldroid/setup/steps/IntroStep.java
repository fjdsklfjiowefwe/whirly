package com.gregdev.whirldroid.setup.steps;

import android.os.Bundle;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;

public class IntroStep extends SetupStep {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setLayoutResource(R.layout.setup_step_intro);
        setStepTitle("Whirldroid");

        // Obtain the shared Tracker instance.
        Whirldroid application = (Whirldroid) getActivity().getApplication();
        Tracker mTracker = application.getDefaultTracker();
        mTracker.setScreenName("Setup: Start");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

}