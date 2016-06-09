package com.gregdev.whirldroid.login;

import android.content.Intent;
import android.os.Bundle;

import com.github.fcannizzaro.materialstepper.AbstractStep;
import com.github.fcannizzaro.materialstepper.style.DotStepper;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.login.steps.ApiStep;
import com.gregdev.whirldroid.login.steps.DoneStep;
import com.gregdev.whirldroid.login.steps.IntroStep;

public class SteppedLogin extends DotStepper {

    private int i = 1;
    private boolean displayErrors = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        addStep(createFragment(new IntroStep()));
        addStep(createFragment(new ApiStep  ()));
        addStep(createFragment(new DoneStep()));

        super.onCreate(savedInstanceState);
    }

    private AbstractStep createFragment(AbstractStep fragment) {
        Bundle b = new Bundle();
        b.putInt("position", i++);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onComplete() {
        Intent intent = new Intent(SteppedLogin.this, MainActivity.class);
        finish();
        startActivity(intent);
    }

    public void setDisplayErrors(boolean displayErrors) {
        this.displayErrors = displayErrors;
    }

    @Override
    public void onError() {
        if (displayErrors) {
            super.onError();
        }
    }

}
