package com.gregdev.whirldroid.setup.steps;

import android.os.Bundle;

import com.gregdev.whirldroid.R;

public class IntroStep extends SetupStep {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setLayoutResource(R.layout.setup_step_intro);
        setStepTitle("Whirldroid");
    }

}