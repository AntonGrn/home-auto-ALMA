package com.example.androidclient.fragments;


import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.androidclient.MainActivity;
import com.example.androidclient.R;
import com.example.androidclient.utilities.Updatable;


/**
 * A simple {@link Fragment} subclass.
 */
public class AboutFragment extends Fragment implements Updatable {


    public AboutFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_about, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ((MainActivity)getActivity()).currentFragment = this;

        // Set header data
        ((TextView)getView().findViewById(R.id.header_user)  ).setText(((MainActivity)getActivity()).loggedInUser.getName());
        ((TextView)getView().findViewById(R.id.header_system)).setText(((MainActivity)getActivity()).loggedInUser.getSystemName());
    }

    @Override
    public void update(String command) {
        // Ignore here
    }
}
