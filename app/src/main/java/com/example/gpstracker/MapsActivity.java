package com.example.gpstracker;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    private FusedLocationProviderClient fusedLocationClient;

    private Button startBtn;
    private Button stopBtn;

    private SensorManager sensorManager;
    private Sensor sensor;
    private SensorEventListener sensorEventListener;

    private boolean isRunning;
    private Polyline polyline;
    private List<Polyline> polylines = new ArrayList<>();

    private long start;
    private long end = System.currentTimeMillis();
    private static final long SECOND = 1000;

    private LocationRequest locationRequest;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        this.startBtn = findViewById(R.id.startBtn);
        this.stopBtn = findViewById(R.id.stopBtn);
        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        this.sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.startBtn.setOnClickListener(this.onStartBtnClick());
        this.stopBtn.setOnClickListener(this.onStopBtnClick());
        this.sensorEventListener = onDeviceMoved();

        this.locationRequest = new LocationRequest();
        this.locationRequest.setInterval(1000);
        this.locationRequest.setFastestInterval(500);
        this.locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        this.locationCallback = locationCallback();
    }

    public LocationCallback locationCallback() {
        return new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                Location location1 = locationResult.getLastLocation();
                LatLng latLng = new LatLng(location1.getLatitude(), location1.getLongitude());
                showCoordinates(latLng);
                //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 21));
            }
        };
    }

    public View.OnClickListener onStartBtnClick() {
        return v -> {
            isRunning = true;
            startBtn.setEnabled(false);
            stopBtn.setEnabled(true);
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, onStartCallBack());
        };
    }

    public OnSuccessListener<Location> onStartCallBack() {
        return location -> {
            if (location != null) {
                LatLng coordinate = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.addMarker(new MarkerOptions().position(coordinate));
                polyline = mMap.addPolyline(new PolylineOptions().add(coordinate));
                showCoordinates(coordinate);
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinate, 21));
            }
        };
    }

    public OnSuccessListener<Location> onStopCallBack() {
        return location -> {
            if (location != null) {
                LatLng coordinate = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.addMarker(new MarkerOptions().position(coordinate));
                List<LatLng> points = polyline.getPoints();
                points.add(coordinate);
                polyline.setPoints(points);
                polyline = null;
                showCoordinates(coordinate);
            }
        };
    }

    public View.OnClickListener onStopBtnClick() {
        return v -> {
            isRunning = false;
            startBtn.setEnabled(true);
            stopBtn.setEnabled(false);
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, onStopCallBack());
        };
    }


    public SensorEventListener onDeviceMoved() {
        return new SensorEventListener() {

            @Override
            public void onSensorChanged(SensorEvent event) {
                start = System.currentTimeMillis();
                if ((start - end) >= SECOND) {
                    fusedLocationClient.getLastLocation().addOnSuccessListener(onMoved());
                    end = System.currentTimeMillis();
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };
    }

    public OnSuccessListener<Location> onMoved() {
        return location -> {
            if (location != null) {
                LatLng coordinate = new LatLng(location.getLatitude(), location.getLongitude());
                if (isRunning) {
                    List<LatLng> points = polyline.getPoints();
                    points.add(coordinate);
                    polyline.setPoints(points);
                }
                showCoordinates(coordinate);
                //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinate, 21));
            }
        };
    }

    public void showCoordinates(LatLng latLng) {
        String msg = String.format("(%.4f, %.4f)", latLng.latitude, latLng.longitude);
        Toast toast = Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG);
        toast.show();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;
        this.mMap.setMyLocationEnabled(true);
        this.mMap.getUiSettings().setMyLocationButtonEnabled(true);
        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper());
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            LatLng coordinate = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(coordinate, 21));
        });
    }



    @Override
    protected void onStop() {
        super.onStop();
        this.sensorManager.unregisterListener(sensorEventListener);
        this.fusedLocationClient.removeLocationUpdates(locationCallback());
        
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.sensorManager.registerListener(
                sensorEventListener,
                sensor,
                SensorManager.SENSOR_DELAY_FASTEST
        );
    }
}