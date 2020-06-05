package com.example.lab7;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
    GoogleMap.OnMapLoadedCallback,
    GoogleMap.OnMarkerClickListener,
    GoogleMap.OnMapLongClickListener,
        SensorEventListener {

    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    Marker gpsMarker = null;
    private SensorManager sensorManager;
    private Sensor sensor;
    private TextView textView;
    private ArrayList<Marker> markerList = new ArrayList<>();
    private Animation AnimationIn;
    private Animation AnimationOut;
    double x;
    double y;
    private FloatingActionButton AccButton;
    private FloatingActionButton DelButton;
   // private final String JSON_FILE = "markers.json";
    //List<Double> latitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        animation();
        AccButton = findViewById(R.id.ButtonAcceleration);
        DelButton = findViewById(R.id.ButtonDelete);
        textView = findViewById(R.id.AccelerationText);
        textView.setVisibility(View.INVISIBLE);
        AccButton.setVisibility(View.INVISIBLE);
        DelButton.setVisibility(View.INVISIBLE);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        markerList = new ArrayList<>();
       // latitude = new ArrayList<>();
        setSensor();

        AccButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.setVisibility(View.VISIBLE);
                textView.startAnimation(AnimationIn);

            }
        });


        DelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textView.setText("");
                textView.startAnimation(AnimationOut);
                textView.setVisibility(View.INVISIBLE);
                AccButton.startAnimation(AnimationOut);
                AccButton.setVisibility(View.INVISIBLE);
                DelButton.startAnimation(AnimationOut);
                DelButton.setVisibility(View.INVISIBLE);

            }
        });

    }

    public void setSensor() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        assert sensorManager != null;
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_NORMAL);

    }

    public void animation() {
        AnimationIn = AnimationUtils.loadAnimation(this, R.anim.bounce);
        AnimationOut = AnimationUtils.loadAnimation(this, R.anim.bounce_out);
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);
    }


    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if(locationResult != null) {
                    if(gpsMarker  != null)
                        gpsMarker.remove();
                    Location location = locationResult.getLastLocation();
                    //BitmapDescriptor icon = BitmapDescriptorFactory.fromResource(R.drawable.circle_shape);
                    /*
                    gpsMarker = mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.circle_shape))
                            .alpha(0.8f)
                            .title("Current Location"));

                     */
                }
            }
        };
    }

    @Override
    public void onMapLoaded() {
        Log.i(MapsActivity.class.getSimpleName(), "MapLoaded");
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
        Task<Location> lastLocation = fusedLocationClient.getLastLocation();
        lastLocation.addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if(location != null && mMap != null) {
                    mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude()))
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                            .title(getString(R.string.last_known_loc_msg)));
                }
            }
        });
        createLocationRequest();
        createLocationCallback();
        startLocationUpdates();

    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
        //if(sensor != null)
            //sensorManager.unregisterListener(this,sensor);
    }
/*
    @Override
    protected void onDestroy() {

        try {
            saveMarkersToJson();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        super.onDestroy();
    }

 */

    private void stopLocationUpdates() {
        if(locationCallback != null)
            fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        Marker marker = mMap.addMarker(new MarkerOptions()
            .position(new LatLng(latLng.latitude, latLng.longitude))
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE))
            .alpha(0.8f)
            .title(String.format("Position:(%.2f, %.2f) ",latLng.latitude, latLng.longitude)));
        markerList.add(marker);
   // latitude.add(latLng.latitude);
   // latitude.add(latLng.longitude);


    }

    @Override
    public boolean onMarkerClick(Marker marker) {

        AccButton.setVisibility(View.VISIBLE);
        AccButton.startAnimation(AnimationIn);
        DelButton.setVisibility(View.VISIBLE);
        DelButton.startAnimation(AnimationIn);

        return false;
    }

    public void zoomInClick(View v) {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View v) {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        x = event.values[0];
        y = event.values[1];
        textView.setText("Acceleration " + " \n" + "x: " + x + " \n " + "y: " + y);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void Clear(View view){
        if(view.getId() == R.id.butClearMemory){
            mMap.clear();
            textView.setText("");
            textView.startAnimation(AnimationOut);
            textView.setVisibility(View.INVISIBLE);
            AccButton.startAnimation(AnimationOut);
            AccButton.setVisibility(View.INVISIBLE);
            DelButton.startAnimation(AnimationOut);
            DelButton.setVisibility(View.INVISIBLE);
            Toast.makeText(this, "Memory cleared!", Toast.LENGTH_SHORT).show();
        }
    }
    /*
    private void saveMarkersToJson() throws JSONException {
        Gson gson = new Gson();
        String listJson = gson.toJson(latitude);
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(JSON_FILE, MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(listJson);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void restoreFromJson() {
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 1000;
        Gson gson = new Gson();
        String readJson;

        try {
            inputStream = openFileInput(JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();

            while ((n = reader.read(buf)) >= 0) {
                String tmp = String.valueOf(buf);
                String substring = (n < DEFAULT_BUFFER_SIZE) ? tmp.substring(0, n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collecionType = new TypeToken<List<Double>>() {
            }.getType();
            List<Double> o = gson.fromJson(readJson, collecionType);
            latitude.clear();
            if (o != null) {
                latitude.addAll(o);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        markerList.clear();

    }

     */
}