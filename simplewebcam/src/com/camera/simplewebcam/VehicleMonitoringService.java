package com.camera.simplewebcam;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.openxc.VehicleManager;
import com.openxc.measurements.Measurement;
import com.openxc.measurements.TransmissionGearPosition;
import com.openxc.measurements.UnrecognizedMeasurementTypeException;
import com.openxc.remote.VehicleServiceException;

public class VehicleMonitoringService extends Service {
	
	private final static String TAG = "VehicleMonitoringService";
    private final Handler mHandler = new Handler();
    private VehicleManager mVehicleManager;
    private String TransmissionGearString;
    public static final String ACTION_VEHICLE_REVERSED = "com.ford.openxc.VEHICLE_REVERSED";
    public static final String ACTION_VEHICLE_UNREVERSED = "com.ford.openxc.VEHICLE_UNREVERSED";
    
      TransmissionGearPosition.Listener mTransmissionGearPos =
			new TransmissionGearPosition.Listener() {
	        	public void receive(Measurement measurement) {
	        		final TransmissionGearPosition status = (TransmissionGearPosition) measurement;
	        		mHandler.post(new Runnable() {
	        			public void run() {
	        				
	        				Log.w(TAG, status.getValue().enumValue().toString());
	                	
	        				TransmissionGearString = status.getValue().enumValue().toString();
	        				
	        				if (TransmissionGearString =="REVERSE" && Main.activityRunning == false){
	        					Intent launchIntent = new Intent(VehicleMonitoringService.this, Main.class);
	        	         		launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        	                VehicleMonitoringService.this.startActivity(launchIntent);
	        	                Log.i(TAG, "Activity Launched");
	        				}
	        				else if (TransmissionGearString != "REVERSE") {
	        					Intent unreversedIntent = new Intent(ACTION_VEHICLE_UNREVERSED);
	        	                sendBroadcast(unreversedIntent);
	        	                Log.i(TAG, "Vehicle UNREVERSED Broadcast Intent Sent");
	        				}
	                	}
	        		});
	        	}
      };
		
      ServiceConnection mConnection = new ServiceConnection() {
    	  public void onServiceConnected(ComponentName className,
	                IBinder service) {
	            		Log.i(TAG, "Bound to VehicleManager");
	            		mVehicleManager = ((VehicleManager.VehicleBinder)service).getService();

	            		try {
	            			mVehicleManager.addListener(TransmissionGearPosition.class,
	            					mTransmissionGearPos);
	                
	            		} catch(VehicleServiceException e) {
	            			Log.w(TAG, "Couldn't add listeners for measurements", e);
	            		} catch(UnrecognizedMeasurementTypeException e) {
	            			Log.w(TAG, "Couldn't add listeners for measurements", e);
	            		}
    	  }

    	  public void onServiceDisconnected(ComponentName className) {
    		  Log.w(TAG, "VehicleService disconnected unexpectedly");
    		  mVehicleManager = null;
    	  }
      };

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		bindService(new Intent(this, VehicleManager.class),
	                mConnection, Context.BIND_AUTO_CREATE);
	}
}
