package com.camera.simplewebcam;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Receive the BOOT_COMPLETED signal and start the VehicleManager.

 */
public class BootupReceiver extends BroadcastReceiver {
    private final static String TAG = "CameraBootupReceiver";

    
    @Override
    public void onReceive(final Context context, Intent intent) {
       
   
    	Intent MonitoringServiceIntent = new Intent(context, VehicleMonitoringService.class);
    	context.startService(MonitoringServiceIntent);	
     	Log.w(TAG, "Starting Service from BootupReceiver");
    	
  	
}
}



	
