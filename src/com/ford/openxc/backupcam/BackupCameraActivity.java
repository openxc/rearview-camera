package com.ford.openxc.backupcam;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;

/** Primary entry point and activity for the BackupCamera application.
 *
 * When created, it starts the VehicleMonitoringService. It contains two
 * receivers:
 *
 * Receiver that listens for a USB device being detached. When this intent is
 * received, it builds a dialog that informs the user of what has happened, and
 * forces them to close the activity. Receiver that listens for a closing intent
 * from the VehicleMonitoringService. When this intent is received, the app
 * closes by calling is finish() method.
 *
 * It includes a method that monitors whether the activity is active or not
 * (`isRunning()`). This method is accessed by the VehicleMonitoringService to
 * determine whether or not the activity needs to be launched/closed (see
 * VehicleMonitoringService).
*/
public class BackupCameraActivity extends Activity {

    private final static String TAG = "BackupCameraActivity";
    private static boolean activityRunning=false;
    CameraPreview cp;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activityRunning = true;
        cp = new CameraPreview(this);
        setContentView(cp);
        startVehicleMonitoringService();
        registerVehicleUnreversedCloseReceiver();
        registerUsbDetachedCloseReceiver();
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
            usbError();
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        activityRunning = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        activityRunning = true;
        registerVehicleUnreversedCloseReceiver();
        registerUsbDetachedCloseReceiver();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        activityRunning = false;
        unregisterReceiver(usbCloseReceiver);
        unregisterReceiver(vehicleUnreversedCloseReceiver);
    }

    public void startVehicleMonitoringService() {
        Intent VehicleMonitoringServiceIntent = new Intent(BackupCameraActivity.this, VehicleMonitoringService.class);
        startService(VehicleMonitoringServiceIntent);
         Log.i(TAG, "Starting Service from BackupCameraActivity");
    }

    private void registerUsbDetachedCloseReceiver() {
        IntentFilter usbFilter = new IntentFilter();
        usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        usbFilter.addAction("com.ford.openxc.NO_CAMERA_DETECTED");
        registerReceiver(usbCloseReceiver, usbFilter);
    }

    private void registerVehicleUnreversedCloseReceiver() {
        IntentFilter closeFilter = new IntentFilter();
        closeFilter.addAction("com.ford.openxc.VEHICLE_UNREVERSED");
        registerReceiver(vehicleUnreversedCloseReceiver, closeFilter);
    }

    public void finish() {
        super.finish();
        activityRunning = false;
    }

    public void usbError(){
        if (isRunning() == true) {
            Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.vibrate(2000);
            new AlertDialog.Builder(this)
            .setTitle("USB Device Unplugged!")
            .setMessage("FordBackupCam is closing. Please reconnect device(s) and relaunch app.")
            .setCancelable(false)
            .setNeutralButton("Close", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    activityRunning = false;
                    android.os.Process.killProcess(android.os.Process.myPid());
                }
            }).show();
        }
        else if (isRunning() == false) {
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    public static boolean isRunning() {
        return activityRunning;
    }
}
