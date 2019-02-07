package com.gregdev.whirldroid.fragment;

import java.util.Map;

import android.support.v4.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager.BadTokenException;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.gregdev.whirldroid.MainActivity;
import com.gregdev.whirldroid.R;
import com.gregdev.whirldroid.Whirldroid;
import com.gregdev.whirldroid.model.User;

public class UserInfoFragment extends Fragment {

    private TextView user_details;
    private User user;
    private ProgressDialog progress_dialog;
    private GetUserInfoTask task;

    private class GetUserInfoTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            user.downloadInfo();

            return null;
        }

        @Override
        protected void onPostExecute(final Void result) {
            if (progress_dialog != null) {
                try {
                    progress_dialog.dismiss(); // hide the progress dialog
                    progress_dialog = null;
                } catch (Exception e) {
                }
            }

            setUserInfo();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        container.removeAllViews();
        View rootView = inflater.inflate(R.layout.user_info, container, false);

        user_details = (TextView) rootView.findViewById(R.id.user_details);

        Bundle bundle = getArguments();
        if (bundle != null) {
            user = bundle.getParcelable("user");
        }

        try {
            progress_dialog = ProgressDialog.show(getActivity(), "Just a sec...", "Loading user info...", true, true);
            progress_dialog.setOnCancelListener(new CancelTaskOnCancelListener(task));
        } catch (BadTokenException e) {
            Crashlytics.logException(e);
        }

        task = new GetUserInfoTask(); // start new thread to retrieve user info
        task.execute();

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        Whirldroid.getTracker().setCurrentScreen(getActivity(), "UserInfo", null);
        Whirldroid.logScreenView("UserInfo");

        ((MainActivity) getActivity()).resetActionBar();
        ((MainActivity) getActivity()).getSupportActionBar().setSubtitle("#" + user.getId());
        getActivity().setTitle(user.getName());
    }

    private void setUserInfo() {
        String info = "";

        for (Map.Entry<String, String> entry : user.getInfo().entrySet()) {
            info += entry.getKey() + " " + entry.getValue() + "\n\n";
        }

        user_details.setText(info);
    }

    /**
     * Cancels the fetching of user info if the back button is pressed
     */
    private class CancelTaskOnCancelListener implements OnCancelListener {
        private AsyncTask<?, ?, ?> task;
        public CancelTaskOnCancelListener(AsyncTask<?, ?, ?> task) {
            this.task = task;
        }

        public void onCancel(DialogInterface dialog) {
            if (task != null) {
                task.cancel(true);
            }
        }
    }

}