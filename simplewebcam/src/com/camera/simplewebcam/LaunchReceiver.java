package com.camera.simplewebcam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class LaunchReceiver extends BroadcastReceiver{
	private final static String TAG = "LaunchReceiver";
	 int x;
	
	@Override
	   public void onReceive(final Context context, Intent intent) {
		
		
		
			
		Intent mainintent = new Intent(context, Main.class);
 		mainintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
 		context.startActivity(mainintent);
 		Log.w(TAG, "Camera Activity Launched");
	 
	 }
	 
}


