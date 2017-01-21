package com.mobile.khewitt.myhike;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
//This is a the fragment inflated on the starting activity containing the UI
public class HikeActivityFragment extends Fragment {
    private Button mStartButton;//starts map activity
    public final static int START_BUTTON = 10;//code sent for this button pressed
    public final static int SAVED_DATA = 223;
    private OnFragmentButtonListener mListener;//interface

    public HikeActivityFragment() {}

    public interface OnFragmentButtonListener{
        public void onButtonPressed(int id);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_hike, container, false);
        mStartButton = (Button)view.findViewById(R.id.start_button);

        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //interface call
                mListener.onButtonPressed(START_BUTTON);
            }
        });

        return view;
    }
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentButtonListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }
}
