package com.camera.simplewebcam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CloseReceiver extends BroadcastReceiver{
	private final static String TAG = "CloseReceiver";

	
	@Override
	   public void onReceive(final Context context, Intent intent) {
		 
		//Intent mainintent = new Intent(context, Main.class);
 		//mainintent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
 		//context.startActivity(mainintent);
		Log.w(TAG, "Camera Activity Closed");
	 
	 }
}