package com.mobile.khewitt.myhike;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;//instance to map
    public Location mCurrentLocation;//marks current location
    public PolylineOptions mHikePath;//used to draw path
    public double mTotalDistance,mAltitudeGain, mCurrentBearing,mAveSpeed, mSumSpeed;//stats tracked
    public float mCurrentSpeed;//marks the most recent speed reading
    private Location mPreviousLocation;//keeps previous location  to compare between last 2 points
    private boolean mIsFirstReading = true;//flag to avoid issues from first readings
    public LocationManager locationManager;
    public LocationListener locationListener;
    private double distance, mAvePace,mPreviousAlt;
    private ImageView cameraButton;
    public TextView mDistanceView,mSpeedView,mAltView;
    public final double mToMPH = 2.23694;//converts from m/s to mph
    public final double mToFeet = 3.28084;//converts meters to feet
    public final double mToMile = 0.000621371;//converts meters to miles
    private int mUpdateTime = 1000;//the time between gps updates
    private int n = 1;//summation count to find average
    private int mZoomLevel =19;//map zoom level
    private int mAdjustZoom = 1;//how much to change the zoom level at a time
    private boolean mShowPace = true;//flag to tell the display to show speed or pace
    private boolean isDistHistory = false;//flag to tell distance display current or lifetime distance
    private boolean isAltHistory = false;//flag to tell altitude climb display current or lifetime climb
    private SharedPreferences preferences;//Shared Preferences for persistance
    private SharedPreferences.Editor edit;// ' ' ' ' ' ' ' ' ' ' ' ' ' ' ' ' '
    public static final String mSavePref = "Saved Preferences";//saved pref name
    public static final String distKey = "lifetime distance";//saved pref key for distance
    public static final String altKey = "lifetime climbed";//saved pref key for altitude climb
    float mLifetimeDist, mLifetimeClimbed;//lifetime stats saved in saved prefs
    public TextView reset, resetYes, resetNo, resetText;//used to reset lifetime stats
    private ArrayList<Bitmap> mImageList = new ArrayList<Bitmap>();//arraylist to be passed through savedInstance
    private ArrayList<LatLng> mLocationList = new ArrayList<LatLng>();//' ' ' ' ' ' ' ' ' ' ' ' ' ' ' ' ' ' ' ' '


/*-----------------------------------------------------------------------------------------------
*                          onCreate()
*------------------------------------------------------------------------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

//reading persisting data
        preferences = getSharedPreferences(mSavePref, Context.MODE_PRIVATE);
        edit = preferences.edit();
        getSavedData();

//map utilities
        mHikePath = new PolylineOptions();
        mHikePath.color(Color.YELLOW);
        mHikePath.geodesic(true);

//Initialization
        cameraButton = (ImageView)findViewById(R.id.cameraButton);
        mDistanceView = (TextView) findViewById(R.id.textView_distance);
        mSpeedView = (TextView) findViewById(R.id.speedView);
        mAltView = (TextView) findViewById(R.id.textView_altitude);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        reset = (TextView) findViewById(R.id.reset);
        resetYes = (TextView) findViewById(R.id.reset_yes);
        resetNo = (TextView) findViewById(R.id.reset_no);
        resetText = (TextView) findViewById(R.id.textView);

//create map fragment instance
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

//retain information from instance state change
        if (savedInstanceState != null) {
            mHikePath = savedInstanceState.getParcelable("polyLine");
            mImageList = savedInstanceState.getParcelableArrayList("image list");
            mLocationList = savedInstanceState.getParcelableArrayList("location list");

            for(int i =0;i<mLocationList.size();i++){
                mMap.addMarker(new MarkerOptions().position(mLocationList.get(i)).title("Picture!")
                        .icon(BitmapDescriptorFactory.fromBitmap(mImageList.get(i))));
            }
        }

//location listener
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                mHikePath.add(currentLatLng);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng));
                mCurrentLocation = location;
                mMap.addPolyline(mHikePath);
                calculateValues(location);

//shows the corresponding display according to display flags
                if (isDistHistory) {
                    if ((mLifetimeDist * mToFeet) < 528) {
                        mDistanceView.setText(String.format(" Lifetime: \n%.2f feet", (mLifetimeDist * mToFeet)));
                    } else {
                        mDistanceView.setText(String.format(" Lifetime: \n%.2f miles", (mLifetimeDist * mToMile)));
                    }
                } else {
                    if ((mTotalDistance * mToFeet) < 528) {
                        mDistanceView.setText(String.format("  Distance: \n%.2f feet", (mTotalDistance * mToFeet)));
                    } else {
                        mDistanceView.setText(String.format("  Distance: \n%.2f miles", (mTotalDistance * mToMile)));
                    }
                }
//sets the current location as the previous at the end
                mPreviousLocation = location;
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {/*not used*/}

            public void onProviderEnabled(String provider) {/*not used*/ }

            public void onProviderDisabled(String provider) {/*not used*/}
        };
//start updates
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                mUpdateTime, 0, locationListener);
        buttonFunctions();
    }

/*-----------------------------------------------------------------------------------------------
*                          buttonFunctions()
*------------------------------------------------------------------------------------------------*/

    public void buttonFunctions(){

//camera button
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 100);
            }
        });

//speed/pace display
        mSpeedView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mShowPace){
                    mShowPace = false;
                    makeToast("Showing Speed",false);
                }else{
                    mShowPace = true;
                    makeToast("Showing Pace",false);
                }
            }
        });

//distance current/lifetime display selection
        mDistanceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isDistHistory){
                    isDistHistory = false;
                    makeToast("Showing Trip Distance",false);
                }else{
                    isDistHistory = true;
                    makeToast("Showing Lifetime Distance",false);
                }
            }
        });

//climb current/lifetime display selection
        mAltView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isAltHistory){
                    isAltHistory = false;
                    makeToast("Showing Trip Climb",false);
                }else{
                    isAltHistory = true;
                    makeToast("Showing Lifetime Climb",false);
                }
            }
        });

//open reset option of lifetime stats
        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetText.setVisibility(View.VISIBLE);
                resetNo.setVisibility(View.VISIBLE);
                resetYes.setVisibility(View.VISIBLE);
                reset.setVisibility(View.INVISIBLE);
            }
        });

//choose no to resetting lifetime stats
        resetNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetText.setVisibility(View.GONE);
                resetNo.setVisibility(View.GONE);
                resetYes.setVisibility(View.GONE);
                reset.setVisibility(View.VISIBLE);
            }
        });

//choice to reset the lifetime stats
        resetYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edit.putFloat(distKey, 0);
                edit.putFloat(altKey, 0);
                edit.commit();
                mLifetimeDist = 0;
                mLifetimeClimbed = 0;
                resetText.setVisibility(View.GONE);
                resetNo.setVisibility(View.GONE);
                resetYes.setVisibility(View.GONE);
                reset.setVisibility(View.VISIBLE);
            }
        });
    }

/*-----------------------------------------------------------------------------------------------
*                          onPause()
*------------------------------------------------------------------------------------------------*/

    @Override
    protected void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

/*-----------------------------------------------------------------------------------------------
*                          onResume()
*------------------------------------------------------------------------------------------------*/

    @Override
    protected void onResume() {
        super.onResume();
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                mUpdateTime, 0, locationListener);
    }

/*-----------------------------------------------------------------------------------------------
*                          onSaveInstanceState()
*------------------------------------------------------------------------------------------------*/

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("polyLine", mHikePath);
        outState.putDouble("distance", mTotalDistance);
        outState.putDouble("pace", mAvePace);
        outState.putDouble("altitude", mAltitudeGain);
        outState.putDouble("lifetime distance", mLifetimeDist);
        outState.putDouble("lifetime climb",mLifetimeClimbed);
        outState.putParcelableArrayList("image list", mImageList);
        outState.putParcelableArrayList("location list",mLocationList);
    }

/*-----------------------------------------------------------------------------------------------
*                          onRestoreInstanceState()
*------------------------------------------------------------------------------------------------*/

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mTotalDistance = savedInstanceState.getDouble("distance");
        mAvePace = savedInstanceState.getDouble("pace");
        mAltitudeGain = savedInstanceState.getDouble("altitude");
        mLifetimeDist =(float)savedInstanceState.getDouble("lifetime distance");
        mLifetimeClimbed = (float)savedInstanceState.getDouble("lifetime climb");
    }

/*-----------------------------------------------------------------------------------------------
*                          onStop()
*------------------------------------------------------------------------------------------------*/

    @Override
    protected void onStop() {
        super.onStop();
//save lifetime stats
        btedit.putFloat(distKey, mLifetimeDist);
        edit.putFloat(altKey, mLifetimeClimbed);
        edit.commit();
    }

/*-----------------------------------------------------------------------------------------------
*                          getSavedData()
*------------------------------------------------------------------------------------------------*/

    public void getSavedData(){
        if(preferences.contains(distKey)) {
            mLifetimeDist = preferences.getFloat(distKey,-1);
        }else{//create pair
            edit.putFloat(distKey,0);
            edit.commit();
            mLifetimeDist =0;
        }
        if(preferences.contains(altKey)) {
            mLifetimeClimbed = preferences.getFloat(altKey,-1);
        }else{//create pair
            edit.putFloat(altKey,0);
            edit.commit();
            mLifetimeClimbed =0;
        }
    }

/*-----------------------------------------------------------------------------------------------
*                          onActivityResult()
*------------------------------------------------------------------------------------------------*/

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100 && resultCode == RESULT_OK){
            Bundle bundle = data.getExtras();
            Bitmap cameraImage = (Bitmap) bundle.get("data");
            LatLng current = new LatLng(mCurrentLocation.getLatitude(),mCurrentLocation.getLongitude());

//create marker on map for picture that was taken
            mMap.addMarker(new MarkerOptions().position(current).title("Picture!")
                    .icon(BitmapDescriptorFactory.fromBitmap(cameraImage)));
            mImageList.add(cameraImage);
            mLocationList.add(current);
        }
    }

/*-----------------------------------------------------------------------------------------------
*                          calculateValues()
*------------------------------------------------------------------------------------------------*/

    public void calculateValues(Location location){

//handles the first reading when there is no previous reading to pass
        if (mIsFirstReading) {
            mPreviousLocation = location;
            mPreviousAlt = location.getAltitude();
            mMap.addMarker(new MarkerOptions().
                    position(new LatLng(location.getLatitude(),location.getLongitude())).
                    title("Start").icon(BitmapDescriptorFactory.fromResource(R.drawable.start_marker)));
            mIsFirstReading = false;
        }

//calculate distance
            distance = location.distanceTo(mPreviousLocation);
            if(location.getSpeed()!=0) {//prevents calculating distance when device is still
                mTotalDistance += distance;
                mLifetimeDist += distance;
            }

//calculate speed and pace
        mSumSpeed += location.getSpeed()*mToMPH;
        mAveSpeed = mSumSpeed/n++;
            if(!mShowPace){
                mSpeedView.setText(String.format("  Speed: \n%.2f mph",(mCurrentSpeed*mToMPH)));
            }else {
                if(mAveSpeed!=0) {
                    mAvePace = 60 / mAveSpeed;
                    mSpeedView.setText(String.format("  Pace: \n%.2f min/mile", mAvePace));
                }else{
                    mSpeedView.setText("  Pace: \n--.--");
                }
       }
        if(location.hasSpeed()){
            mCurrentSpeed = location.getSpeed();
        }else{
            makeToast("Cannot get speed reading",false);
        }

//calculate altitude gain
        if(location.hasAltitude()){
            double currentAlt = location.getAltitude();
            if(currentAlt>mPreviousAlt){
                mAltitudeGain += (currentAlt-mPreviousAlt);
                mLifetimeClimbed += (currentAlt-mPreviousAlt);
            }
            if(isAltHistory){
                mAltView.setText(String.format(" Lifetime:\n%.2f feet",mLifetimeClimbed));
            }else{
                mAltView.setText(String.format(" Climbed: \n%.2f feet", mAltitudeGain));
            }
            mPreviousAlt = currentAlt;
        }
        if(location.hasBearing()){
            mCurrentBearing = location.getBearing();
        }

//handles zooming the map out every mile traveled. This prevents from having to load too many
//map pages, saving memory.
        if((mTotalDistance*mToMile) >= mAdjustZoom){//every mile traveled
            mMap.moveCamera(CameraUpdateFactory.zoomTo(--mZoomLevel));
            mAdjustZoom++;
        }
    }

/*-----------------------------------------------------------------------------------------------
*                          onMapReady()
*------------------------------------------------------------------------------------------------*/

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     */
     @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.zoomTo(mZoomLevel));
        //mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
    }

/*-----------------------------------------------------------------------------------------------
*                          makeToast()
*------------------------------------------------------------------------------------------------*/

    public void makeToast(String message, boolean isLong){
        if(isLong) {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        }
    }//end makeToast

}//end class
