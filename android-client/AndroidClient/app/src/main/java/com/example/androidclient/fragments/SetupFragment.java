package com.example.androidclient.fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.example.androidclient.LoggedInUser;
import com.example.androidclient.MainActivity;
import com.example.androidclient.R;
import com.example.androidclient.Updatable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;


/**
 * A simple {@link Fragment} subclass.
 */
public class SetupFragment extends Fragment implements Updatable {

    private MainActivity main;
    private ProgressBar spinner;

    public SetupFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        main = (MainActivity) getActivity();
        main.currentFragment = this;

        if(main.appInFocus) {
            spinner = (ProgressBar) getView().findViewById(R.id.setup_progressbar);
            spinner.setVisibility(View.GONE);

            readUserDataFromCache();
        }
    }
    //Reconnection with sessionKey
    private void readUserDataFromCache() {

        final Handler setupHandler = new Handler(Looper.getMainLooper());

        new Thread(new Runnable() {
            @Override
            public void run() {

                Log.d("NetworkActivity", "Read user from file   " + Thread.currentThread().getName());

                boolean successfulCacheRead = false;
                String message = "";

                try (ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(new File(main.getCacheDir() + "userdata")))) {

                    //Reads from "file" and casts to LoggedInUser-object.
                    main.loggedInUser = (LoggedInUser) objectInputStream.readObject();

                    //Retrieves information needed to reconnect.
                    String userName = main.loggedInUser.getName();
                    String sessionKey = main.loggedInUser.getSessionKey();
                    if(userName.equals("")) {
                        throw new FileNotFoundException("Cache content erased");
                    } else {
                        message = "4:" + userName + ":" + sessionKey;
                        successfulCacheRead = true;
                    }
                } catch (FileNotFoundException e) {
                    message = "Cache history empty";
                } catch (IOException e) {
                    message = "Failed Setup Stream";
                } catch (ClassNotFoundException e) {
                    message = "File empty, Log In";
                } finally {
                    postResult(successfulCacheRead, message);
                }
            }

            private void postResult(final boolean successfulCacheRead, final String message) {
                setupHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        setupServerConnection(successfulCacheRead, message);
                    }
                });
            }
        }).start();
    }

    private void setupServerConnection(boolean successfulCacheRead, String message) {
        //Precaution: Make sure connection threads are closed before new are launched
        if (main.isBound) {
            // If for some reason there is a cloud connection thread already running: Stop it so it can be re-started.
            main.networkService.stopConnectionToPublicServer();
        }
        // If user data was successfully read from cache: use it as log in criteria at cloud server
        if (successfulCacheRead) {
            main.loginAttemptWithSessionKey = true;
            spinner.setVisibility(View.VISIBLE);
            main.establishServerConnection(message);
        } else {
            // If user data was NOT successfully read from cache; direct to manual log in
            main.loggedInUser = null;
            main.loginAttemptWithSessionKey = false;
            main.writeToast(message);

            main.fragmentTransaction("login");
        }
    }

    @Override
    public void update(String command) {
        //Ignore here
    }
}
