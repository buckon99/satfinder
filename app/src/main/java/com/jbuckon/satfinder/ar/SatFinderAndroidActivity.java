package com.jbuckon.satfinder.ar;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.menu.ActionMenuItemView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SizeF;
import android.view.Display;
import android.view.Menu;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.jbuckon.satfinder.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class SatFinderAndroidActivity extends AppCompatActivity implements
        SensorEventListener {

    SatFinderAndroidActivity activity;
    LocationManager locationManager = null;
    LocationListener locationGPSListener = null;
    LocationListener locationNetworkListener = null;
    double latitude = -100, longitude = 0;
    boolean goodLocation = false;
    TextView posSource, latTV, lngTV, magDecTV;
    RelativeLayout skyViewTab;
    SatImageOverlay skyViewOverlay;

    BiQuadraticFilter sinBF, cosBF;
    RadioButton satsUS, satsInt;
    String strSource;
    int posUpdateTimeInterval = 5000; // milliseconds
    int posUpdateDistanceInterval = 50; // meters
    ArrayList<SatDescription> satData = null;
    WebView webList = null;
    double magDec = 0;
    double useMagDec = 0;
    private Handler handler;

    private SensorManager mSensorManager;
    float sensorAzimuth = 0;
    float sensorPitch = 0;
    float sensorRoll = 0;
    Display defaultDisplay;

    int MY_GPS_REQ = 1234;

    float rToD = (float) (180 / Math.PI);
    float dToR = (float) (Math.PI / 180);

    boolean sensorsRegistered = false;

    int sensorSampleRate = 50; // milliseconds

    boolean fullScreen = false;
    boolean lightColorScheme = false;
    String declinationString = "";

    Sensor mRotationSensor = null;

    boolean badAccuracy = false;

    int cameraOrientation = 0;
    Camera2BasicFragment camProcess = null;
    float cameraXAngle = 60 * dToR, cameraYAngle = 45 * dToR;
    float camSensorX = 1, camSensorY = 1;

    protected DisplayMetrics metrics;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        activity = this;

        setContentView(R.layout.activity_sat_finder_android);

        Resources r = getResources();

        if (r != null) {
            metrics = r.getDisplayMetrics();
        }

        satData = new ArrayList<>();

        handler = new Handler();

        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayShowHomeEnabled(true);
            ab.setDisplayUseLogoEnabled(true);
        }
        double Q = .7;
        double rate = sensorSampleRate;
        double freq = 2;

        sinBF = new BiQuadraticFilter(BiQuadraticFilter.LOWPASS, freq, rate, Q);
        cosBF = new BiQuadraticFilter(BiQuadraticFilter.LOWPASS, freq, rate, Q);

        defaultDisplay = getWindowManager().getDefaultDisplay();
        layoutComplete();
    }

    protected void layoutComplete() {
        setupSkyView();
        if (webList == null) {
            readConfig();
            setupLinks();

            setupSkyView();

            getCameraAngles();

            View v = findViewById(R.id.texture);
            if (v != null) {
                v.setOnLongClickListener(
                        new View.OnLongClickListener() {
                            @Override
                            public boolean onLongClick(View v) {
                                toggleLightColors(null);
                                return true;
                            }
                        }
                );
                v.setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                toggleFullScreen(null);
                            }
                        }
                );
            }
        }
        setupLoc();
    }

    protected void setupSkyView() {
        if (camProcess == null) {
            camProcess = Camera2BasicFragment.newInstance();
            getFragmentManager().beginTransaction()
                    .replace(R.id.skyview_pane, camProcess)
                    .commit();
        }
    }


    protected void getCameraAngles() {
        boolean success = false;
        CameraManager cManager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        if (cManager != null) {
            try {
                for (final String cameraId : cManager.getCameraIdList()) {
                    CameraCharacteristics characteristics = cManager.getCameraCharacteristics(cameraId);
                    if (characteristics != null) {
                        int cOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                        if (cOrientation == CameraCharacteristics.LENS_FACING_BACK) {
                            float[] maxFocus = characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
                            SizeF size = characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
                            if (size != null) {
                                camSensorX = size.getWidth();
                                camSensorY = size.getHeight();
                                if (maxFocus != null && maxFocus.length > 0) {
                                    float fl = maxFocus[0];
                                    cameraXAngle = (float) (2 * Math.atan2(camSensorX, (fl * 2)));
                                    cameraYAngle = (float) (2 * Math.atan2(camSensorY, (fl * 2)));
                                    success = true;
                                }
                            }
                        }
                    }
                }
            } catch (CameraAccessException e) {
                Log.e("Error", "" + e.toString());
            }
        }
        if (!success) {
            cameraXAngle = 65 * dToR;
            cameraYAngle = 52 * dToR;
        }
    }

    protected void registerSensors() {
        if (!sensorsRegistered) {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            mRotationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            mSensorManager.registerListener(this, mRotationSensor,
                    sensorSampleRate * 1000);
            sensorsRegistered = true;
        }
    }

    protected void unregisterSensors() {
        mSensorManager.unregisterListener(this);
        sensorsRegistered = false;
    }


    private void setupLinks() {
        skyViewTab = findViewById(R.id.skyview_tab);
        skyViewOverlay = findViewById(R.id.sat_overlay);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if ((requestCode == MY_GPS_REQ) && grantResults.length > 0 && grantResults[0] == PERMISSION_GRANTED) {
            setupLoc();
        }
    }

    public void setupLoc() {
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_GPS_REQ);
                gotLocation(null);
            } else {
                // Acquire a reference to the system Location Manager
                locationManager = (LocationManager) this
                        .getSystemService(Context.LOCATION_SERVICE);

                // Define a listener that responds to location updates
                if (locationGPSListener == null) {
                    locationGPSListener = new LocationListener() {
                        public void onLocationChanged(Location location) {
                            gotLocation(location);
                        }

                        public void onStatusChanged(String provider, int status,
                                                    Bundle extras) {
                        }

                        public void onProviderEnabled(String provider) {
                        }

                        public void onProviderDisabled(String provider) {
                        }

                    };
                }
                if (locationNetworkListener == null) {
                    locationNetworkListener = new LocationListener() {
                        public void onLocationChanged(Location location) {
                            gotLocation(location);
                        }

                        public void onStatusChanged(String provider, int status,
                                                    Bundle extras) {
                        }

                        public void onProviderEnabled(String provider) {
                        }

                        public void onProviderDisabled(String provider) {
                        }

                    };
                }
                Location loc = null;
                loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null) {
                    loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                }
                if (loc != null) {
                    gotLocation(loc);
                }

                // request new fix
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, posUpdateTimeInterval, posUpdateDistanceInterval, locationNetworkListener);
            }
        } catch (Exception e) {
            Log.e("Error", "Location Setup: " + e.toString());
        }
    }


    private String toDegMin(double a, String ne, String sw) {
        String s = (a < 0) ? sw : ne;
        a = Math.abs(a);
        double d = Math.floor(a);
        double m = ((a - d) * 60);
        return String.format("%03.0f° %02.3f' %s", d, m, s);
    }

    private void gotLocation(Location loc) {
        if (loc != null) {
            latitude = loc.getLatitude();
            longitude = loc.getLongitude();
            goodLocation = true;
            updateCompassConvention();
            strSource = loc.getProvider();
            posSource.setText(strSource);
            String strLat = toDegMin(latitude, "N", "S");
            String strLng = toDegMin(longitude, "E", "W");
            latTV.setText(strLat);
            lngTV.setText(strLng);
        }
    }

    private void updateCompassConvention() {
        if (goodLocation) {
            long t = new Date().getTime();
            GeomagneticField gf = new GeomagneticField((float) latitude, (float) longitude, 0, t);
            magDec = gf.getDeclination() * dToR;
            useMagDec = magDec;
            declinationString = String.format("%4.1f°", magDec * rToD);
            if (magDecTV != null) {
                magDecTV.setText(declinationString);
            }
        }
    }


    private void writeConfig() {
    }

    private void readConfig() {
        updateCompassConvention();
    }

    protected void registerSensorManager(boolean activate) {
        if (activate) {
            registerSensors();
        } else {
            unregisterSensors();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        readConfig();
        registerSensorManager(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        registerSensorManager(false);
        writeConfig();
    }

    @Override
    public void onPause() {
        super.onPause();
        registerSensorManager(false);
        writeConfig();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        registerSensorManager(false);
        writeConfig();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            badAccuracy = true;
        }

        if (event.sensor == mRotationSensor) {
            if (badAccuracy) {
                badAccuracy = false;
            } else {
                updateOrientation(event.values);
            }
        }
    }


    @SuppressLint("WrongConstant")
    private void updateOrientation(float[] rotationVector) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);

        int worldAxisForDeviceAxisX = SensorManager.AXIS_X;
        int worldAxisForDeviceAxisY = SensorManager.AXIS_Y;

        cameraOrientation = defaultDisplay.getRotation();

        switch (cameraOrientation) {
            case Surface.ROTATION_0:
                worldAxisForDeviceAxisX = SensorManager.AXIS_X;
                worldAxisForDeviceAxisY = SensorManager.AXIS_Z;
                break;
            case Surface.ROTATION_90:
                worldAxisForDeviceAxisX = SensorManager.AXIS_Z;
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_X;
                break;
            case Surface.ROTATION_180:
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_X;
                worldAxisForDeviceAxisY = SensorManager.AXIS_MINUS_Z;
                break;
            case Surface.ROTATION_270:
                worldAxisForDeviceAxisX = SensorManager.AXIS_MINUS_Z;
                worldAxisForDeviceAxisY = SensorManager.AXIS_X;
                break;
            default:
                break;
        }

        float[] adjustedRotationMatrix = new float[9];
        SensorManager.remapCoordinateSystem(rotationMatrix, worldAxisForDeviceAxisX,
                worldAxisForDeviceAxisY, adjustedRotationMatrix);

        float[] orientation = new float[3];
        SensorManager.getOrientation(adjustedRotationMatrix, orientation);

        double localAzimuth = orientation[0];
        double azSin = Math.sin(localAzimuth);
        double azCos = Math.cos(localAzimuth);
        double rSin = sinBF.filter(azSin);
        double rCos = cosBF.filter(azCos);
        sensorAzimuth = (float) (Math.atan2(rSin, rCos));
        sensorPitch = (float) (orientation[1]);
        sensorRoll = (float) (orientation[2]);

    }

    public void toggleFullScreen(View v) {
        fullScreen = !fullScreen;
        if (fullScreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public void toggleLightColors(View v) {
        lightColorScheme = !lightColorScheme;
    }

}
