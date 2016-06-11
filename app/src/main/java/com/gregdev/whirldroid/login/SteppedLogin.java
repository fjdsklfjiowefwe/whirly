package com.gregdev.whirldroid.login;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.github.fcannizzaro.materialstepper.AbstractStep;
import com.github.fcannizzaro.materialstepper.style.DotStepper;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.login.steps.ApiStep;
import com.gregdev.whirldroid.login.steps.DoneStep;
import com.gregdev.whirldroid.login.steps.IntroStep;
import com.gregdev.whirldroid.login.steps.LoginStep;
import com.gregdev.whirldroid.login.steps.NotificationsStep;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;

public class SteppedLogin extends DotStepper {

    private int i = 1;
    private boolean displayErrors = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        addStep(createFragment(new IntroStep        ()));
        addStep(createFragment(new ApiStep          ()));
        addStep(createFragment(new NotificationsStep()));
        addStep(createFragment(new DoneStep         ()));

        super.onCreate(savedInstanceState);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                LoginStep currentStep = (LoginStep) mSteps.getCurrent();
                toolbar.setTitle(currentStep.getStepTitle());
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        setTheme(Whirldroid.getCurrentTheme(Whirldroid.LIGHT_THEME));
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.help, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_help:
                Toast.makeText(this, "Post your problem in the Whirldroid thread to get help", Toast.LENGTH_LONG).show();
                Whirldroid.openInBrowser(WhirlpoolApi.THREAD_URL + Whirldroid.WHIRLDROID_THREAD_ID, getBaseContext());

                return true;
        }

        return false;
    }

}
