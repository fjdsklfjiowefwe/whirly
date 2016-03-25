package com.gregdev.whirldroid.fragment;

import android.support.v4.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.WhirlpoolApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by Greg on 10/03/2016.
 */
public class LoginFragment extends Fragment {

    private ProgressDialog progress_dialog;
    private RetrieveDataTask task;
    private Tracker mTracker;

    private class RetrieveDataTask extends AsyncTask<String, Void, Boolean> {

        private String error_message = "";

        @Override
        protected Boolean doInBackground(String... params) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        progress_dialog = ProgressDialog.show(getActivity(), "Just a sec...", "Verifying your API key...", true, true);
                    } catch (WindowManager.BadTokenException e) {
                    }
                }
            });
            try {
                List<String> get = new ArrayList<String>();
                get.add("forum");
                get.add("whims");
                get.add("news");
                get.add("recent");
                get.add("watched");

                Whirldroid.getApi().downloadData(get, null);
            }
            catch (final WhirlpoolApiException e) {
                error_message = e.getMessage();
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    if (progress_dialog != null) {
                        try {
                            progress_dialog.dismiss(); // hide the progress dialog
                            progress_dialog = null;
                        } catch (Exception e) {
                        }
                    }

                    // got data, API key must be valid
                    if (result) {
                        // go to main dashboard
                        //Intent intent = new Intent(Login.this, Dashboard.class);
                        //finish();
                        //startActivity(intent);
                        ((MainActivity) getActivity()).switchFragment("ForumList", false);
                    }

                    // no data, API key is probably invalid (or error on Whirlpool side)
                    else {
                        // unset the API key setting
                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
                        SharedPreferences.Editor settingsEditor = settings.edit();
                        settingsEditor.putString("pref_apikey", null);
                        settingsEditor.commit();

                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        builder.setMessage("It looks like there might be a problem with your API key. Please check your key and remember to include dashes.")
                                .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });
                        AlertDialog alert = builder.create();
                        alert.show();
                    }
                }
            });
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Obtain the shared Tracker instance.
        Whirldroid application = (Whirldroid) getActivity().getApplication();
        mTracker = application.getDefaultTracker();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.login, container, false);

        getActivity().setTitle("Whirldroid");

        Button login_button  = (Button) rootView.findViewById(R.id.login_btn);
        final EditText api_key_edit = (EditText) rootView.findViewById(R.id.api_key_field);

        login_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // hide the keyboard
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(rootView.findViewById(R.id.home_root).getWindowToken(), 0);

                // store the API key
                String api_key = api_key_edit.getText().toString();

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity().getBaseContext());
                SharedPreferences.Editor settingsEditor = settings.edit();
                settingsEditor.putString("pref_apikey", api_key);
                settingsEditor.commit();

                task = new RetrieveDataTask(); // start new thread to retrieve data
                task.execute();
            }
        });

        TextView apiKeyWhere = (TextView) rootView.findViewById(R.id.api_key_where);

        Pattern p_url = Pattern.compile("http://whirlpool.net.au/profile/");
        Linkify.addLinks(apiKeyWhere, p_url, "http://");

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        mTracker.setScreenName("Login");
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
    }

}
