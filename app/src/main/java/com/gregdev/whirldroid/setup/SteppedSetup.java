package com.gregdev.whirldroid.setup;

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
import com.gregdev.whirldroid.setup.steps.ApiStep;
import com.gregdev.whirldroid.setup.steps.DoneStep;
import com.gregdev.whirldroid.setup.steps.IntroStep;
import com.gregdev.whirldroid.setup.steps.SetupStep;
import com.gregdev.whirldroid.setup.steps.NotificationsStep;
import com.gregdev.whirldroid.setup.steps.ThemeStep;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApi;

import java.util.ArrayList;

public class SteppedSetup extends DotStepper {

    private int i = 1;
    private boolean displayErrors = true;
    public ArrayList<AbstractStep> steps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setStateAdapter();

        steps = new ArrayList<>();

        steps.add(createFragment(new IntroStep()));
        steps.add(createFragment(new ApiStep()));
        steps.add(createFragment(new NotificationsStep()));
        steps.add(createFragment(new ThemeStep()));
        steps.add(createFragment(new DoneStep()));

        for (AbstractStep step : steps) {
            addStep(step);
        }

        setTitle("Whirldroid");
        super.onCreate(savedInstanceState);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                SetupStep currentStep = (SetupStep) mSteps.getCurrent();
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
        setTheme(Whirldroid.getCurrentTheme(this));
    }

    private AbstractStep createFragment(AbstractStep fragment) {
        Bundle b = new Bundle();
        b.putInt("position", i++);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    public void onComplete() {
        Intent intent = new Intent(SteppedSetup.this, MainActivity.class);
        Bundle bundle = new Bundle();
        bundle.putBoolean("showMenuShowcase", true);
        intent.putExtras(bundle);

        startActivity(intent);
        finish();
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
                Toast.makeText(this, "To get help, post your problem in the Whirldroid thread", Toast.LENGTH_LONG).show();
                Whirldroid.openInBrowser(WhirlpoolApi.THREAD_URL + Whirldroid.WHIRLDROID_THREAD_ID, getApplicationContext());

                return true;
        }

        return false;
    }

}
