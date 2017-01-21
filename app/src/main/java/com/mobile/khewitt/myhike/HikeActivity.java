package com.mobile.khewitt.myhike;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class HikeActivity extends AppCompatActivity implements HikeActivityFragment.OnFragmentButtonListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hike);
    }
    public void onButtonPressed(int id){
        switch (id){
//starts maps activity
            case HikeActivityFragment.START_BUTTON:
                Intent intent = new Intent(this, MapsActivity.class);
                startActivity(intent);
                break;
            case HikeActivityFragment.SAVED_DATA:
                //TODO
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_hike, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }
}