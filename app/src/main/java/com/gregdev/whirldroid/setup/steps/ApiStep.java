package com.gregdev.whirldroid.setup.steps;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteConstraintException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.setup.SteppedSetup;
import com.gregdev.whirldroid.model.Forum;
import com.gregdev.whirldroid.service.DatabaseHandler;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiException;
import com.gregdev.whirldroid.whirlpool.WhirlpoolApiFactory;

import java.util.ArrayList;
import java.util.List;

public class ApiStep extends SetupStep {

    private boolean haveValidApiKey = false;
    private EditText apiKeyEdit;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setLayoutResource(R.layout.setup_step_api);
        setStepTitle("Log in");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        apiKeyEdit = view.findViewById(R.id.api_key_field);

        apiKeyEdit.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onNext();
                    return true;
                }

                return false;
            }
        });

        TextView apiKeyWhere = view.findViewById(R.id.textView3);
        CharSequence sequence = Html.fromHtml(getText(R.string.login_api_desc) + "");
        SpannableStringBuilder strBuilder = new SpannableStringBuilder(sequence);
        URLSpan[] urls = strBuilder.getSpans(0, sequence.length(), URLSpan.class);

        for (URLSpan span : urls) {
            makeLinkClickable(strBuilder, span);
        }

        apiKeyWhere.setText(strBuilder);
        apiKeyWhere.setMovementMethod(LinkMovementMethod.getInstance());
    }

    protected void makeLinkClickable(SpannableStringBuilder strBuilder, final URLSpan span) {
        int start = strBuilder.getSpanStart(span);
        int end = strBuilder.getSpanEnd(span);
        int flags = strBuilder.getSpanFlags(span);

        ClickableSpan clickable = new ClickableSpan() {
            public void onClick(View view) {
                Whirldroid.openInBrowser(span.getURL(), getContext());
            }
        };

        strBuilder.setSpan(clickable, start, end, flags);
        strBuilder.removeSpan(span);
    }

    @Override
    public void onNext() {
        if (apiKeyEdit == null) {
            apiKeyEdit = (EditText) view.findViewById(R.id.api_key_field);
        }

        String apiKey = apiKeyEdit.getText().toString();

        if (!haveValidApiKey && apiKey.length() > 0) {
            try {
                ((SteppedSetup) getActivity()).setDisplayErrors(false);
            } catch (NullPointerException e) {
                Crashlytics.logException(e);
            }

            // hide the keyboard
            InputMethodManager imm = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.findViewById(R.id.scroller).getWindowToken(), 0);

            // store the API key
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
            SharedPreferences.Editor settingsEditor = settings.edit();
            settingsEditor.putString("pref_apikey", apiKey);
            settingsEditor.apply();

            progress_dialog = ProgressDialog.show(getContext(), "Just a sec...", "Verifying your API key...", true, true);
            RetrieveDataTask task = new RetrieveDataTask(); // start new thread to retrieve data
            task.execute();
        }
    }

    @Override
    public boolean nextIf() {
        EditText apiKeyField = (EditText) view.findViewById(R.id.api_key_field);

        return haveValidApiKey && apiKeyField.getText().length() > 0;
    }

    public String error() {
        return "Please enter your API key";
    }

    private ProgressDialog progress_dialog;

    private class RetrieveDataTask extends AsyncTask<String, Void, Boolean> {

        private String error_message = "";

        @Override
        protected Boolean doInBackground(String... params) {
            try {
                List<String> get = new ArrayList<String>();
                get.add("user"      );
                get.add("forum"     );
                get.add("whims"     );
                get.add("news"      );
                get.add("recent"    );
                get.add("watched"   );

                WhirlpoolApiFactory.getFactory().getApi(getContext()).downloadData(get, null);

            } catch (final WhirlpoolApiException e) {
                error_message = e.getMessage();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            try {
                ((SteppedSetup) getActivity()).setDisplayErrors(false);
            } catch (NullPointerException e) {
                Crashlytics.logException(e);
            }

            if (progress_dialog != null) {
                try {
                    progress_dialog.dismiss(); // hide the progress dialog
                    progress_dialog = null;
                } catch (Exception e) {
                    Crashlytics.logException(e);
                }
            }

            // got data, API key must be valid
            if (result) {
                if (Whirldroid.isGreg(getContext())) { // restore Greg's settings, because he's sick of doing it over and over and over again
                    Toast.makeText(getContext(), "Hi, Greg!", Toast.LENGTH_SHORT).show();

                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
                    SharedPreferences.Editor settingsEditor = settings.edit();
                    settingsEditor.putBoolean("pref_watchedbacktolist"      , true      );
                    settingsEditor.putBoolean("pref_watchedautomarkasread"  , true      );
                    settingsEditor.putBoolean("pref_ignoreownreplies"       , true      );
                    settingsEditor.putBoolean("pref_whimnotify"             , true      );
                    settingsEditor.putBoolean("pref_watchednotify", true);
                    settingsEditor.putString("pref_notifyfreq", "15");
                    //settingsEditor.putString("pref_theme", "2");
                    settingsEditor.putString ("pref_nightmodestart"         , "21:30"   );
                    settingsEditor.putString("pref_nightmodeend", "07:30");
                    settingsEditor.apply();

                    try {
                        DatabaseHandler db = new DatabaseHandler(getContext());
                        db.addFavouriteForum(new Forum(138, "Home", "Lounges"));
                        db.addFavouriteForum(new Forum(126, "Home theatre", "Entertainment"));
                        db.addFavouriteForum(new Forum(71, "Lifestyle", "Life"));
                        db.addFavouriteForum(new Forum(63, "Web development", "IT Industry"));

                    } catch (SQLiteConstraintException e) { }
                }

                haveValidApiKey = true;

                TextView nextButton = (TextView) view.getRootView().findViewById(R.id.stepNext);
                nextButton.performClick();
            }

            // no data, API key is probably invalid (or error on Whirlpool side)
            else {
                // unset the API key setting
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor settingsEditor = settings.edit();
                settingsEditor.putString("pref_apikey", null);
                settingsEditor.apply();

                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage("It looks like there's a problem with your API key. Double-check your key and remember to include dashes.")
                        .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        }
    }

}