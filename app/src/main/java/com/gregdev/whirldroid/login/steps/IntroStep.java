package com.gregdev.whirldroid.login.steps;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.gregdev.whirldroid.R;

public class IntroStep extends LoginStep {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setLayoutResource(R.layout.login_step_intro);
        setStepTitle("Whirldroid");
    }

}