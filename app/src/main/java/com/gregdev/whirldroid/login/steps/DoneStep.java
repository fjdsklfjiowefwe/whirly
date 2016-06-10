package com.gregdev.whirldroid.login.steps;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.gregdev.whirldroid.R;

public class DoneStep extends LoginStep {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setLayoutResource(R.layout.login_step_intro);
        setStepTitle("Setup complete");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        TextView textView1 = (TextView) view.findViewById(R.id.textView);
        TextView textView2 = (TextView) view.findViewById(R.id.textView2);
        TextView textView3 = (TextView) view.findViewById(R.id.textView3);

        textView1.setText("All done!");
        textView2.setText("You're all set up and good to go. You can go into the app settings at any time to change any options or view advanced settings.");
        textView3.setText("Tap FINISH to go to the list of forums.");
    }

}