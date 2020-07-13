package com.example.androidclient.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.example.androidclient.MainActivity;
import com.example.androidclient.R;
import com.example.androidclient.Updatable;

/**
 * A simple {@link Fragment} subclass.
 */
public class LoginFragment extends Fragment implements Updatable {

    private MainActivity main;
    private Button btnLogin;
    private EditText usernameInput;
    private EditText passwordInput;
    private ProgressBar spinner;
    private boolean userNameInputEmpty;
    private boolean passwordInputEmpty;

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        main = (MainActivity)getActivity();

        main.currentFragment = this;

        btnLogin = (Button) getView().findViewById(R.id.btnLogIn);
        //animationHandler();
        userNameInputEmpty = true;
        passwordInputEmpty = true;
        btnLogin.setEnabled(false);
        usernameInput = (EditText) getView().findViewById(R.id.usernameText);
        passwordInput = (EditText) getView().findViewById(R.id.passwordText);
        spinner = (ProgressBar) getView().findViewById(R.id.login_progressbar);
        spinner.setVisibility(View.GONE); // Hide spinner
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onLoginBtnClicked();
                }
            });
        //requestToServer.put(command);

        // Add listeners on input fields, to detect any character input changes
        // Enable button if both fields contain some input
        usernameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                userNameInputEmpty = usernameInput.getText().toString().trim().isEmpty();
                setButtonState();
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        passwordInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                passwordInputEmpty = passwordInput.getText().toString().trim().isEmpty();
                setButtonState();
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setButtonState() {
        if (userNameInputEmpty || passwordInputEmpty) {
            btnLogin.setEnabled(false);
        } else {
            btnLogin.setEnabled(true);
        }
    }

    private void onLoginBtnClicked() {
        String usernameEntered = usernameInput.getText().toString().trim();
        String passwordEntered = passwordInput.getText().toString().trim();

        if (usernameEntered.contains(":") || passwordEntered.contains(":")) {
            main.writeToast("Invalid symbols entered. Try again!");
        } else {
            btnLogin.setEnabled(false);
            usernameInput.setFocusable(false);
            passwordInput.setFocusable(false);
            spinner.setVisibility(View.VISIBLE);
            hideKeyboardFrom(main, passwordInput);

            main.establishServerConnection(String.format("%s:%s:%s", "1", usernameEntered, passwordEntered));
        }
    }

    private static void hideKeyboardFrom(Context context, View view) {
        try {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (NullPointerException e) {
            //Ignore
        }
    }

    @Override
    public void update(String command) {
        //Ignore here
    }
}