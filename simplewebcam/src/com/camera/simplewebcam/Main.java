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
		
		IntentFilter filter = new IntentFilter();
		filter.addAction("com.ford.openxc.VEHICLE_UNREVERSED");
		registerReceiver(receiver, filter);
		
	}
	
	BroadcastReceiver receiver = new BroadcastReceiver() {
		
		@Override
		public void onReceive(Context context, Intent intent) {
			finish();
		}
	};
	public void finish() {
		super.finish();
	}
}
	 
	

