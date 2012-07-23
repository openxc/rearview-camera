package com.camera.simplewebcam;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

public class Main extends Activity {
	
	private final static String TAG = "MainCamera";
   
	CameraPreview cp;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		cp = new CameraPreview(this);
		setContentView(cp);

		Log.w(TAG, "In onCreate");
		
		//start monitoring service if not already started on boot
    	Intent MonitoringServiceIntent = new Intent(Main.this, VehicleMonitoringService.class);
    	startService(MonitoringServiceIntent);	
     	Log.w(TAG, "Starting Service from BootupReceiver");
		
		//create intent filter to listen for unreversing of vehicle to close activity
		IntentFilter closeFilter = new IntentFilter();
		closeFilter.addAction("com.ford.openxc.VEHICLE_UNREVERSED");
		//closeFilter.addAction("com.camera.simplewebcam.CLOSE_ACTIVITY");
		registerReceiver(closeReceiver, closeFilter);
		
	}
	
	BroadcastReceiver closeReceiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			finish();
		}
	};
	public void finish() {
		super.finish();
	}
}
	 
	

