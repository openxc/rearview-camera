package com.ford.openxc.rearview;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

/** Primary entry point and activity for the RearviewCamera application.
 *
 * If it's not already started (as a result of the BootupReceiver), this
 * activity starts the VehicleMonitoringService.
 *
 * It listenes for a broadcast sent when the vehicle shifts out of "reverse"
 * into another gear, and stops displaying the video feed. The activity will
 * exit.
 */
public class RearviewCameraActivity extends Activity {
    private final static String TAG = "RearviewCameraActivity";

    private static boolean mRunning = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Intent VehicleMonitoringServiceIntent = new Intent(
                RearviewCameraActivity.this, VehicleMonitoringService.class);
        startService(VehicleMonitoringServiceIntent);
        Log.i(TAG, "Starting Service from RearviewCameraActivity");
    }

    BroadcastReceiver vehicleUnreversedCloseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Leaving rearview camera, received unreversed signal");
            finish();
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        mRunning = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        mRunning = true;

        IntentFilter filter = new IntentFilter();
        filter.addAction(VehicleMonitoringService.ACTION_VEHICLE_UNREVERSED);
        registerReceiver(vehicleUnreversedCloseReceiver, filter);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mRunning = false;
        unregisterReceiver(vehicleUnreversedCloseReceiver);
    }

    public static boolean isRunning() {
        return mRunning;
    }
}
