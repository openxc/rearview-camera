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
 * It receives two types of broadcasts:
 *
 * It listens for broadcasts of a USB device being detached. If this device is
 * the USB camera, the activity wanrs the users with a modal dialog, so they
 * don't mistakenly believe the camera is working when in fact it is detached.
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
            finish();
        }
    };

    BroadcastReceiver usbCloseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.w("USB ERROR", "USB Device Detached");
            // TODO check the class of device to see if it was a webcam, and put
            // that in the device filter if we can
            usbError();
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

        IntentFilter closeFilter = new IntentFilter();
        closeFilter.addAction("com.ford.openxc.VEHICLE_UNREVERSED");
        registerReceiver(vehicleUnreversedCloseReceiver, closeFilter);

        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        usbFilter.addAction("com.ford.openxc.NO_CAMERA_DETECTED");
        registerReceiver(usbCloseReceiver, usbFilter);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mRunning = false;
        unregisterReceiver(usbCloseReceiver);
        unregisterReceiver(vehicleUnreversedCloseReceiver);
    }

    public static boolean isRunning() {
        return mRunning;
    }

    private void usbError(){
        if (isRunning()) {
            Vibrator vibrator = (Vibrator) this.getSystemService(
                    Context.VIBRATOR_SERVICE);
            vibrator.vibrate(2000);
            new AlertDialog.Builder(this)
                .setTitle("USB Device Unplugged!")
                .setMessage("RearviewCamera is exiting because a USB device " +
                        "was detached - relaunch the app after plugging " +
                        "it in again")
                .setCancelable(false)
                .setNeutralButton("Close", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        mRunning = false;
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                }).show();
        } else if (!isRunning()) {
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }
}
