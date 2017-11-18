package com.a132room.kp0hyc.videoeditor;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IllegalFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class TabActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tab);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }

    public static class PlaceholderFragment extends Fragment {

        private static final String ARG_SECTION_NUMBER = "section_number";
        private static final int MY_PERMISSIONS_REQUEST_CAMERA = 0;

        private String currentCam;
        private CameraManager cameraManager;
        private Surface surface = null;
        private CameraDevice cameraDevice = null;
        private View rootView;
        private boolean windowOpened = false;
        private boolean cameraStarted = false;

        public PlaceholderFragment() {
        }

        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            switch (getArguments().getInt(ARG_SECTION_NUMBER))
            {
                case 2:
                    return inflater.inflate(R.layout.third_window, container, false);
                case 0:
                    rootView = inflater.inflate(R.layout.fragment_tab, container, false);
                    TextView textView = (TextView) rootView.findViewById(R.id.section_label);
                    textView.setText(getString(R.string.section_format, 1));
                    View surface = rootView.findViewById(R.id.surfaceView);
                    surface.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            reOpenCamera();
                        }
                    });
                    rootView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                        @Override
                        public void onViewAttachedToWindow(View view) {
                            Log.d("kp0hyc_debug", "view attached");
                            windowOpened = true;
                            /*if (cameraDevice == null)
                                reOpenCamera();*/
                        }

                        @Override
                        public void onViewDetachedFromWindow(View view) {
                            Log.d("kp0hyc_debug", "view detached");
                            windowOpened = false;
                        }
                    });
                    return rootView;
                case 1:
                    return inflater.inflate(R.layout.second_window, container, false);
                default:
                    return null;
            }
        }

        @Override
        public void onResume()
        {
            super.onResume();
            Log.d("kp0hyc_debug", "on resume");
            if (windowOpened && cameraDevice == null) {
                Log.d("kp0hyc_debug", "on resume open camera");
                reOpenCamera();
            }
        }

        public void reOpenCamera()
        {
            if (Build.VERSION.SDK_INT >= 21)
            {
                Log.d("kp0hyc_debug", "enough version");
                cameraManager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);

                if (Build.VERSION.SDK_INT >= 23)
                    if (getContext().checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
                        tryOpenCamera();
                    else
                    {
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
                    }
                else {
                    tryOpenCamera();
                }
            } else {
                Log.d("kp0hyc_debug", "this device in progress");
            }
        }

        @TargetApi(21)
        private void startCameraSession()
        {
            try {
                if (surface == null) {
                    Log.d("kp0hyc_debug", "surface not ready");
                    return;
                }
                if (cameraDevice == null) {
                    Log.d("kp0hyc_debug", "camera not ready");
                    return;
                }

                List<Surface> l = new ArrayList<>();

                l.add(surface);

                Log.d("kp0hyc_debug", "creating capture session");

                cameraDevice.createCaptureSession(l, new MyStateCallback(), null);
            } catch (CameraAccessException e) {
            }
        }

        private void tryOpenCamera() {
            if (Build.VERSION.SDK_INT >= 21) {
                try {
                    //setting up surface
                    if (rootView.findViewById(R.id.surfaceView) == null)
                    {
                        Log.d("kp0hyc_debug", "surface not opened");
                        return;
                    }
                    SurfaceHolder sh = ((SurfaceView) rootView.findViewById(R.id.surfaceView)).getHolder();
                    currentCam = cameraManager.getCameraIdList()[0];
                    StreamConfigurationMap config = cameraManager.getCameraCharacteristics(currentCam).
                            get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    sh.addCallback(new MySurfaceHolderCallback());
                    sh.setFixedSize(config.getHighSpeedVideoSizes()[0].getHeight(), config.getHighSpeedVideoSizes()[0].getWidth());
                    Log.d("kp0hyc_debug", "started setting up surface");

                    cameraManager.openCamera(currentCam, new CameraCallback(), null);
                    Log.d("kp0hyc_debug", "started setting up camera");
                } catch (SecurityException e) {
                    Log.d("kp0hyc_debug", "Security Exception");
                } catch (CameraAccessException e) {
                    Log.d("kp0hyc_debug", "Camera Access Exception");
                }
            } else
                Log.d("kp0hyc_debug", "Low version of android");
        }

        @Override
        public void onRequestPermissionsResult(int requestCode,
                                               String permissions[], int[] grantResults) {
            switch (requestCode) {
                case MY_PERMISSIONS_REQUEST_CAMERA: {
                    if (grantResults.length > 0
                            && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        tryOpenCamera();
                    }
                    return;
                }
            }
        }

        @TargetApi(21)
        private class CameraCallback extends CameraDevice.StateCallback {

            @Override
            public void onOpened(@NonNull CameraDevice cd) {
                Log.d("kp0hyc_debug", "camera onOpened");
                cameraDevice = cd;
                startCameraSession();
            }

            @Override
            public void onDisconnected(@NonNull CameraDevice cd) {
                Log.d("kp0hyc_debug", "camera onDisconnected");
                cameraDevice.close();
                cameraDevice = null;
            }

            @Override
            public void onError(@NonNull CameraDevice cd, int i) {
                Log.d("kp0hyc_debug", " camera onError");
                cameraDevice.close();
                cameraDevice = null;
            }
        }

        @TargetApi(21)
        class MyStateCallback extends CameraCaptureSession.StateCallback
        {

            @Override
            public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                Log.d("kp0hyc_debug", " camera onConfigured " + Integer.toString(cameraCaptureSession.hashCode()));
                try {
                    cameraCaptureSession.abortCaptures();
                    CaptureRequest.Builder previewRequestBuilder = cameraDevice
                            .createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    previewRequestBuilder.addTarget(surface);
                    cameraCaptureSession.setRepeatingRequest(previewRequestBuilder.build(),
                            null, null);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                Log.d("kp0hyc_debug", " camera onConfigureFailed");
            }
        }

        class MySurfaceHolderCallback implements SurfaceHolder.Callback
        {

            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                Log.d("kp0hyc_debug", "surface configured");
                //surface = surfaceHolder.getSurface();
                startCameraSession();
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
                Log.d("kp0hyc_debug", "surface changed");
                surface = surfaceHolder.getSurface();
                startCameraSession();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
                Log.d("kp0hyc_debug", "surface destroyed");
                cameraDevice.close();
                surface = null;
                cameraDevice = null;
            }
        }
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return PlaceholderFragment.newInstance(position);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }
    }
}
