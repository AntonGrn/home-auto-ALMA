package com.example.androidclient.fragments;

import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.example.androidclient.MainActivity;
import com.example.androidclient.R;
import com.example.androidclient.utilities.Updatable;


public class SettingsFragment extends Fragment implements Updatable {

    private MainActivity main;
    private View demoMode_box;
    private RadioGroup demoMode_radioGroup;
    private RadioButton demoMode_btn_on;
    private RadioButton demoMode_btn_off;
    private Button btnReset;
    private View.OnClickListener demoModeBtnListener;

    public  SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        main = (MainActivity)getActivity();
        main.currentFragment = this;

        // Set header data
        ((TextView)getView().findViewById(R.id.header_user)  ).setText(main.loggedInUser.getName());
        ((TextView)getView().findViewById(R.id.header_system)).setText(main.loggedInUser.getSystemName());

        // ====================== DEMO MODE SETTING ====================================

        demoMode_box = (View) getView().findViewById(R.id.demo_mode_settings_box);
        TextView title = (TextView) demoMode_box.findViewById(R.id.settings_name);
        title.setText("Demo Mode");
        demoMode_radioGroup = (RadioGroup) demoMode_box.findViewById(R.id.radioGroup);
        demoMode_btn_on = demoMode_radioGroup.findViewById(R.id.on);
        demoMode_btn_off = demoMode_radioGroup.findViewById(R.id.off);

        if (main.demoMode) {
            demoMode_btn_on.setBackgroundColor(getResources().getColor(R.color.colorRadioBtnOn));
            demoMode_btn_off.setBackgroundColor(Color.TRANSPARENT);
        } else {
            demoMode_btn_on.setBackgroundColor(Color.TRANSPARENT);
            demoMode_btn_off.setBackgroundColor(getResources().getColor(R.color.colorRadioBtnOff));
        }

        demoModeBtnListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onDemoModeRadioBtnClicked(v);
            }
        };

        demoMode_btn_on.setOnClickListener(demoModeBtnListener);
        demoMode_btn_off.setOnClickListener(demoModeBtnListener);

        // ====================== BUTTON RESET ====================================

        btnReset = (Button)getView().findViewById(R.id.btnReset);
        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //reset to demoMode = false
                demoMode_btn_off.performClick();
            }
        });


    }

    // Generate a server request (if input does not contain colon)
    public void onDemoModeRadioBtnClicked(View view) {

        switch (view.getId()) {
            case R.id.on: // Demo mode on
                if(!main.demoMode) {
                    main.demoMode = true;
                    demoMode_btn_on.setBackgroundColor(getResources().getColor(R.color.colorRadioBtnOn));
                    demoMode_btn_off.setBackgroundColor(Color.TRANSPARENT);
                    main.writeToast("Demo Mode: ON");
                }
                break;
            case R.id.off: // Demo mode off
                if(main.demoMode) {
                    main.demoMode = false;
                    demoMode_btn_on.setBackgroundColor(Color.TRANSPARENT);
                    demoMode_btn_off.setBackgroundColor(getResources().getColor(R.color.colorRadioBtnOff));
                    main.writeToast("Demo Mode: OFF");
                }
                break;
        }

    }


    // Generate a server request (if input does not contain colon)
    public void onResetBtnClicked(View view) {

    }

    @Override
    public void update(String command) {
        main.writeToast("Gadget update from cloud");
    }
}
