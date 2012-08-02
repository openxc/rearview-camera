package com.ford.openxc.backupcam;

/**Main Activity**/

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
     	Log.w(TAG, "Starting Service from BackupCameraActivity");
	}
	
	private void registerUsbDetachedCloseReceiver() {
		IntentFilter usbFilter = new IntentFilter();
		usbFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
		usbFilter.addAction("com.ford.openxc.NO_CAMERA_DETECTED");
        registerReceiver(usbCloseReceiver, usbFilter);
	}

	private void registerVehicleUnreversedCloseReceiver() {
		//create intent filter to listen for unreversing of vehicle to close activity
		IntentFilter closeFilter = new IntentFilter();
		closeFilter.addAction("com.ford.openxc.VEHICLE_UNREVERSED");
		registerReceiver(vehicleUnreversedCloseReceiver, closeFilter);
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
            Log.w("USB ERROR", "Usb device detached");
            usbError();   
        }
    };
	
	public void finish() {
		super.finish();
		activityRunning = false;
	}
	 
	public void usbError(){
		if (activityRunning == true) {
	        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
	        vibrator.vibrate(2000);
	        new AlertDialog.Builder(this)
	        .setTitle("USB Device Unplugged!")
	        .setMessage("FordBackupCam is closing. Please reconnect device(s) and relaunch app.")
	        .setCancelable(false)
	        .setNeutralButton("OK", new DialogInterface.OnClickListener() {
	        	public void onClick(DialogInterface dialog, int id) {
	        		activityRunning = false;
	        		android.os.Process.killProcess(android.os.Process.myPid());
	            }
	        }).show();
		}
		else if (activityRunning == false) {
	        android.os.Process.killProcess(android.os.Process.myPid());
		}
	}

	public static boolean isRunning() {
		return activityRunning;
	}
}