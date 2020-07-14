package com.example.androidclient.fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.example.androidclient.utilities.LoggedInUser;
import com.example.androidclient.MainActivity;
import com.example.androidclient.R;
import com.example.androidclient.utilities.Updatable;


/**
 * A simple {@link Fragment} subclass.
 */
public class LogoutFragment extends Fragment implements Updatable {
    //access modifiers, private/public/package protected?
    private Button logoutBtn;
    //LoggedInUser loggedInUser;
    private MainActivity main;

    public LogoutFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_logout, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        main = (MainActivity) getActivity();
        main.currentFragment = this;

        // Set header data
        ((TextView) getView().findViewById(R.id.header_user)).setText(main.loggedInUser.getName());
        ((TextView) getView().findViewById(R.id.header_system)).setText(main.loggedInUser.getSystemName());


        logoutBtn = getView().findViewById(R.id.logoutButton);
        //get loggedIn user and set to instance variable
        //loggedInUser = ((MainActivity) getActivity()).loggedInUser;


        // Other implementation?
        logoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Overwrite cached user data with un-usable data.
                main.loggedInUser = new LoggedInUser("", false, "", "");
                main.writeUserToCache();

                if (main.isBound) {
                    //if (main.networkService.inputThreadRunning) {
                    try {
                        // Send logout request to cloud server.
                        // Only done so that the cloud server can destroy the sessionKey,
                        // preventing the session key to be used for log in attempts
                        main.networkService.requestsToServer.put("3");
                    } catch (InterruptedException e) {
                        main.writeToast("Unable to perform safe log out");
                    }

                    // Start new thread to gain some wait time before requesting the
                    // cloud connection to terminate, so the logout request ("3") has time
                    // to be sent to the cloud server.

                    final Handler logOutHandler = new Handler(Looper.getMainLooper());

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                Thread.sleep(500);
                            } catch (InterruptedException e) {
                                // Ignore
                            }
                            logOutHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (main.isBound && main.appInFocus)
                                        main.networkService.stopConnectionToPublicServer();
                                }
                            });
                        }
                    }).start();
                }
            }
        });
    }

    @Override
    public void update(String command) {
        // Ignore here
    }
}
