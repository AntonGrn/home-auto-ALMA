package com.example.androidclient.fragments;


import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
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
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.example.androidclient.Gadget;
import com.example.androidclient.GadgetType;
import com.example.androidclient.MainActivity;
import com.example.androidclient.R;
import com.example.androidclient.Updatable;

import java.util.ArrayList;

public class HomeFragment extends Fragment implements Updatable {

    private MainActivity main;

    // Access data_boxes.
    private View dataBox1;
    private View dataBox2;
    private View dataBox3;
    private View dataBox4;

    // Access body of data_boxes.
    private LinearLayout body1;
    private LinearLayout body2;
    private LinearLayout body3;
    private LinearLayout body4;

    private View.OnClickListener radioBtnListener;
    private View.OnClickListener submitValueBtnListener;

    private TextView statusText;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        radioBtnListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRadioBtnClicked(v);
            }
        };

        submitValueBtnListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSubmitValueBtnClicked(v);
            }
        };

        main = (MainActivity) getActivity();
        main.currentFragment = this;

        statusText = (TextView) getView().findViewById(R.id.status_field);
        statusText.setVisibility(View.GONE);

        //Identify data_boxes
        dataBox1 = getView().findViewById(R.id.data_box_control_onoff);
        dataBox2 = getView().findViewById(R.id.data_box_control_values);
        dataBox3 = getView().findViewById(R.id.data_box_sensor_onoff);
        dataBox4 = getView().findViewById(R.id.data_box_sensor_values);

        //Set title of data_boxes
        ((TextView) dataBox1.findViewById(R.id.data_box_header)).setText("ON/OFF");
        ((TextView) dataBox2.findViewById(R.id.data_box_header)).setText("VALUES");
        ((TextView) dataBox3.findViewById(R.id.data_box_header)).setText("ON/OFF");
        ((TextView) dataBox4.findViewById(R.id.data_box_header)).setText("VALUES");

        // Access body of data_boxes
        body1 = (LinearLayout) dataBox1.findViewById(R.id.data_box_body);
        body2 = (LinearLayout) dataBox2.findViewById(R.id.data_box_body);
        body3 = (LinearLayout) dataBox3.findViewById(R.id.data_box_body);
        body4 = (LinearLayout) dataBox4.findViewById(R.id.data_box_body);

        update("");
    }

    // ========================= UPDATE GADGET PRESENTATION ====================================

    // Update GUI from gadgetlist (parameters not used, just conforming to interface Updatable)
    // Will be called when a gadget update from public server arrives,
    // if this fragment is currently in focus.

    public void update(String arg) {
        // demoMode == true:  Display demo-gadgets from a fake gadget list.
        // demoMode == false: Display a user's (real) physical gadgets. (Updated from home network via public server)

        //Clear GUI gadget box bodies
        body1.removeAllViews(); // control_onoff
        body2.removeAllViews(); // control_value
        body3.removeAllViews(); // sensor_onoff
        body4.removeAllViews(); // sensor_onoff

        if (main.demoMode) {
            loadDemoGadgets();
            statusText.setText("DEMO MODE: ON");
            statusText.setVisibility(View.VISIBLE);
        } else {

            for (int i = 0; i < main.gadgetList.size(); i++) {
                Gadget gadget = main.gadgetList.get(i);

                switch (gadget.getType()) {
                    case CONTROL_ONOFF: // E.g: Lamp
                        addGadgetToBody1(gadget);
                        break;
                    case CONTROL_VALUE: // E.g: Set servo
                        addGadgetToBody2(gadget);
                        break;
                    case SENSOR_ONOFF: // E.g: Boolean sensor
                        addGadgetToBody3(gadget);
                        break;
                    case SENSOR_VALUE: // E.g: Thermometer
                        addGadgetToBody4(gadget);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    // =========================== CONTROL_ONOFF ===================================================

    private void addGadgetToBody1(Gadget gadget) {
        // Create a gadget_box view
        View gadgetBox = (View) getLayoutInflater().inflate(R.layout.gadget_box_control_onoff, null);
        TextView gadgetNameView = (TextView) gadgetBox.findViewById(R.id.gadget_name);
        gadgetNameView.setText(gadget.getName());

        // Assign the RadioButtons of the gadget_box view
        RadioGroup radioGroup = (RadioGroup) gadgetBox.findViewById(R.id.radioGroup);
        RadioButton btn_on = radioGroup.findViewById(R.id.on);
        RadioButton btn_off = radioGroup.findViewById(R.id.off);

        // Add listener to the RadioButtons
        btn_on.setOnClickListener(radioBtnListener);
        btn_off.setOnClickListener(radioBtnListener);

        // Attach gadget id to the RadioButtons (To extract gadget id when pressed)
        btn_on.setTag(gadget.getGadgetID());
        btn_off.setTag(gadget.getGadgetID());

        // Set the RadioButtons according to current gadget state
        switch (gadget.getState()) {
            case 1:
                btn_on.setBackgroundColor(getResources().getColor(R.color.colorRadioBtnOn));
                btn_off.setBackgroundColor(Color.TRANSPARENT);
                break;
            case 0:
                btn_on.setBackgroundColor(Color.TRANSPARENT);
                btn_off.setBackgroundColor(getResources().getColor(R.color.colorRadioBtnOff));
                break;
        }
        body1.addView(gadgetBox);
    }

    // =========================== CONTROL_VALUE ===================================================

    private void addGadgetToBody2(final Gadget gadget) {
        // Create a gadget_box view
        View gadgetBox = (View) getLayoutInflater().inflate(R.layout.gadget_box_control_value, null);
        TextView gadgetNameView = (TextView) gadgetBox.findViewById(R.id.gadget_name);
        gadgetNameView.setText(gadget.getName());

        // Initiate EditText for user to request new gadget value
        final EditText gadgetValueField = (EditText) gadgetBox.findViewById(R.id.value);
        gadgetValueField.setText(String.valueOf(gadget.getState())); // ???

        //Assign the Button for submitting a new value
        final Button submitValueBtn = (Button) gadgetBox.findViewById(R.id.btn_submit_value);

        //Add listener to the Button
        submitValueBtn.setOnClickListener(submitValueBtnListener);

        // Attach gadget id to the Button (To extract gadget id when pressed)
        submitValueBtn.setTag(gadget.getGadgetID());

        // Set default visibility to false
        submitValueBtn.setEnabled(false);

        // Enable the button only when a valid integer value has been typed
        gadgetValueField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String inputValue = gadgetValueField.getText().toString().trim();
                if (!inputValue.isEmpty() && !(String.valueOf(gadget.getState()).equals(inputValue))) {
                    try {
                        Integer.parseInt(gadgetValueField.getText().toString().trim());
                        submitValueBtn.setEnabled(true);
                    } catch (NumberFormatException e) {
                        submitValueBtn.setEnabled(false);

                    }
                } else {
                    submitValueBtn.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        body2.addView(gadgetBox);
    }

    // =========================== SENSOR_ONOFF ===================================================

    private void addGadgetToBody3(Gadget gadget) {
        // Create a gadget_box view
        View gadgetBox = (View) getLayoutInflater().inflate(R.layout.gadget_box_sensor_onoff, null);
        TextView gadgetNameView = (TextView) gadgetBox.findViewById(R.id.gadget_name);
        gadgetNameView.setText(gadget.getName());

        // Assign the RadioButtons of the gadget_box view
        RadioGroup radioGroup = (RadioGroup) gadgetBox.findViewById(R.id.radioGroup);
        RadioButton btn_on = radioGroup.findViewById(R.id.on);
        RadioButton btn_off = radioGroup.findViewById(R.id.off);

        // Set the RadioButtons according to current gadget state
        switch (gadget.getState()) {
            case 1:
                btn_on.setBackgroundColor(getResources().getColor(R.color.colorRadioBtnOn));
                btn_off.setBackgroundColor(Color.TRANSPARENT);
                break;
            case 0:
                btn_on.setBackgroundColor(Color.TRANSPARENT);
                btn_off.setBackgroundColor(getResources().getColor(R.color.colorRadioBtnOff));
                break;
        }
        btn_on.setClickable(false);
        btn_off.setClickable(false);
        body3.addView(gadgetBox);
    }

    // =========================== SENSOR_VALUE ===================================================

    private void addGadgetToBody4(Gadget gadget) {
        View gadgetBox = (View) getLayoutInflater().inflate(R.layout.gadget_box_sensor_value, null);
        TextView gadgetNameView = (TextView) gadgetBox.findViewById(R.id.gadget_name);
        TextView gadgetValue = (TextView) gadgetBox.findViewById(R.id.value);
        gadgetNameView.setText(gadget.getName());
        gadgetValue.setText(String.valueOf(gadget.getState()));
        body4.addView(gadgetBox);
    }

    // ======================== HANDLE USER REQUESTS =============================================

    public void onRadioBtnClicked(View view) {

        int gadgetId = (Integer) view.getTag();
        String requestedState = "0";

        RadioButton on = ((RadioGroup) view.getParent()).findViewById(R.id.on);
        RadioButton off = ((RadioGroup) view.getParent()).findViewById(R.id.off);

        switch (view.getId()) {
            case R.id.on:
                on.setBackgroundColor(getResources().getColor(R.color.colorRadioBtnChecked));
                off.setBackgroundColor(Color.TRANSPARENT);
                requestedState = "1";
                break;
            case R.id.off:
                on.setBackgroundColor(Color.TRANSPARENT);
                off.setBackgroundColor(getResources().getColor(R.color.colorRadioBtnChecked));
                requestedState = "0";
                break;
        }
        // To avoid repeated commands
        on.setClickable(false);
        off.setClickable(false);
        // Add gadget state request
        if (main.isBound && !main.demoMode) {
            try {
                main.networkService.requestsToServer.put(String.format("%s%s%s%s", "8:", gadgetId, ":", requestedState));
            } catch (InterruptedException e) {
                main.writeToast("Unable to add gadget request");
            }
        }
    }

    public void onSubmitValueBtnClicked(View view) {

        // Get the gadget id attached to the button
        String gadgetId = (String) view.getTag();

        // Identify the button and the associated EditText view
        Button btn = (Button)view;
        ViewGroup group = (ViewGroup) view.getParent();
        EditText textInputField = (EditText) group.findViewById(R.id.value);

        // Derive the new requested gadget state from the EditText view
        String requestedState = textInputField.getText().toString().trim();

        // Alter appearance to indicate that the request is being handled
        btn.setEnabled(false);
        btn.setTextColor(getResources().getColor(R.color.colorRadioBtnOn));
        textInputField.setTextColor(getResources().getColor(R.color.colorRadioBtnChecked));
        textInputField.setFocusable(false);
        hideKeyboard(main, textInputField);

        // Add gadget state request
        if(main.isBound && !main.demoMode) {
            try {
                main.networkService.requestsToServer.put(String.format("%s%s%s%s", "8:", gadgetId, ":", requestedState));
            } catch (InterruptedException e) {
                main.writeToast("Unable to add gadget request");
            }
        }
    }

    private void loadDemoGadgets() {

        ArrayList<Gadget> demoModeGadgetList = new ArrayList<>();

        demoModeGadgetList.add(new Gadget(0, "Kitchen lamp", 1, GadgetType.CONTROL_ONOFF));
        demoModeGadgetList.add(new Gadget(0, "Raspberry Pi", 0, GadgetType.CONTROL_ONOFF));
        demoModeGadgetList.add(new Gadget(0, "Camera stream", 0, GadgetType.CONTROL_ONOFF));
        demoModeGadgetList.add(new Gadget(0, "Camera servo", 100, GadgetType.CONTROL_VALUE));
        demoModeGadgetList.add(new Gadget(0, "Temp threshold", 21, GadgetType.CONTROL_VALUE));
        demoModeGadgetList.add(new Gadget(0, "Front door lock", 0, GadgetType.SENSOR_ONOFF));
        demoModeGadgetList.add(new Gadget(0, "Temperature  (C)", 22, GadgetType.SENSOR_VALUE));
        demoModeGadgetList.add(new Gadget(0, "Solar pwr today  (kWh)", 11, GadgetType.SENSOR_VALUE));
        demoModeGadgetList.add(new Gadget(0, "System Pi CPU temp (C)", 48, GadgetType.SENSOR_VALUE));

        for (int i = 0; i < demoModeGadgetList.size(); i++) {
            Gadget gadget = demoModeGadgetList.get(i);

            switch (gadget.getType()) {
                case CONTROL_ONOFF: // E.g: Lamp
                    addGadgetToBody1(gadget);
                    break;
                case CONTROL_VALUE: // E.g: Set servo
                    addGadgetToBody2(gadget);
                    break;
                case SENSOR_ONOFF: // E.g: Boolean sensor
                    addGadgetToBody3(gadget);
                    break;
                case SENSOR_VALUE: // E.g: Thermometer
                    addGadgetToBody4(gadget);
                    break;
                default:
                    break;
            }
        }
    }

    private static void hideKeyboard(Context context, View view) {
        try {
            InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (NullPointerException e) {
            //Ignore
        }
    }
}
