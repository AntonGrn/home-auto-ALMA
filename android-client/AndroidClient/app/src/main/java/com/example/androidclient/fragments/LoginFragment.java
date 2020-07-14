package com.example.androidclient.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.widget.ImageButton;
import android.widget.ProgressBar;
import com.example.androidclient.MainActivity;
import com.example.androidclient.R;
import com.example.androidclient.utilities.ServerSpec;
import com.example.androidclient.utilities.Updatable;

/**
 * A simple {@link Fragment} subclass.
 */
public class LoginFragment extends Fragment implements Updatable {

    private MainActivity main;
    private Button btnLogin;
    private ImageButton btnConfig;
    private EditText usernameInput, passwordInput;
    private ProgressBar spinner;
    private boolean userNameInputEmpty;
    private boolean passwordInputEmpty;
    private boolean validServerSpecFormat;

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
        main = (MainActivity) getActivity();

        main.currentFragment = this;

        btnLogin = (Button) getView().findViewById(R.id.btn_login);
        btnConfig = (ImageButton) getView().findViewById(R.id.btn_config);
        //animationHandler();
        userNameInputEmpty = true;
        passwordInputEmpty = true;
        validServerSpecFormat = main.serverDefined;
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
        btnConfig.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openServerDialog();
            }
        });

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
        } else if (!validServerSpecFormat) {
            main.writeToast("Invalid server specs.");
            openServerDialog();
        } else {
            btnLogin.setEnabled(false);
            usernameInput.setFocusable(false);
            passwordInput.setFocusable(false);
            spinner.setVisibility(View.VISIBLE);
            hideKeyboardFrom(main, passwordInput);

            main.establishServerConnection(String.format("%s:%s:%s", "1", usernameEntered, passwordEntered));
        }
    }

    // ============================ PUBLIC SERVER DIALOG ==========================================

    private void openServerDialog() {
        // Create an alert builder
        AlertDialog.Builder builder = new AlertDialog.Builder(main);
        //builder.setTitle("Public Server");

        // Set custom dialog layout
        final View customLayout = getLayoutInflater().inflate(R.layout.dialog_server, null);
        builder.setView(customLayout);

        // Initiate custom layout text fields
        final EditText editIP = customLayout.findViewById(R.id.server_ip);
        final EditText editPort = customLayout.findViewById(R.id.server_port);

        if (main.serverDefined) {
            editIP.setText(main.server.IP.toString());
            editPort.setText(String.valueOf(main.server.port));
        }

        // Add dialog buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String IP = editIP.getText().toString().trim();
                String port = editPort.getText().toString().trim();
                if(verifyIPv4(IP) && verifyPort(port)) {
                    main.server = new ServerSpec(IP, Integer.parseInt(port));
                    validServerSpecFormat = true;
                } else {
                    validServerSpecFormat = false;
                }
            }
        });
        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
            }
        });

        // Create and show alert dialog
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.dialog_background);
        dialog.show();
    }

    // ================================ UTILITIES =================================================

    private static void hideKeyboardFrom(Context context, View view) {
        try {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (NullPointerException e) {
            //Ignore
        }
    }

    private boolean verifyIPv4(String ipv4) {
        if(ipv4.isEmpty() || ipv4.endsWith(".")) {
            return false;
        }
        String[] ipSplit = ipv4.split("\\.");
        if (ipSplit.length == 4) {
            for (int i = 0; i < 4; i++) {
                try {
                    int ipByte = Integer.parseInt(ipSplit[i]);
                    if(ipByte < 0 || ipByte > 255) {
                        return false;
                    }
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean verifyPort(String portString) {
        try {
            int port = Integer.parseInt(portString);
            return port >= 0 && port <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public void update(String command) {
        //Ignore here
    }
}