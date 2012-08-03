package com.ford.openxc.backupcam;

/**Binds to vehiclemanager.  Adds a listener for steering wheel angle and transmission gear position**/

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
import com.openxc.measurements.SteeringWheelAngle;
import com.openxc.measurements.TransmissionGearPosition;
import com.openxc.measurements.UnrecognizedMeasurementTypeException;
import com.openxc.remote.VehicleServiceException;

public class VehicleMonitoringService extends Service {
	
	private final static String TAG = "VehicleMonitoringService";
    public static final String ACTION_VEHICLE_REVERSED = "com.ford.openxc.VEHICLE_REVERSED";
    public static final String ACTION_VEHICLE_UNREVERSED = "com.ford.openxc.VEHICLE_UNREVERSED";
    
    private final Handler mHandler = new Handler();
    private VehicleManager mVehicleManager;
    public static double SteeringWheelAngle;

    TransmissionGearPosition.Listener mTransmissionGearPos =
    		new TransmissionGearPosition.Listener() {
    	public void receive(Measurement measurement) {
    		final TransmissionGearPosition status = (TransmissionGearPosition) measurement;
    		mHandler.post(new Runnable() {
    			public void run() {
    				Log.w(TAG, ""+status.getValue().enumValue().toString());

    				if (status.getValue().enumValue() == TransmissionGearPosition.GearPosition.REVERSE
    						&& !BackupCameraActivity.isRunning()){
    					
    					startBackupCameraActivity();
    				}
    				else if (status.getValue().enumValue() != TransmissionGearPosition.GearPosition.REVERSE
    						&& BackupCameraActivity.isRunning()) {
    					
    					sendVehicleUnreversedBroadcast();
    				}
    			}
    			
    			private void startBackupCameraActivity() {
					Intent launchIntent = new Intent(VehicleMonitoringService.this, BackupCameraActivity.class);
					launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					VehicleMonitoringService.this.startActivity(launchIntent);
					Log.i(TAG, "Activity Launched from Vehicle Monitoring Service");
				}
    			
				private void sendVehicleUnreversedBroadcast() {
					
					Intent unreversedIntent = new Intent(ACTION_VEHICLE_UNREVERSED);
					sendBroadcast(unreversedIntent);
					Log.i(TAG, "Vehicle UNREVERSED Broadcast Intent Sent");
				}
    		});
    	}
    };
      
    SteeringWheelAngle.Listener mSteeringWheelListener =
    		new SteeringWheelAngle.Listener() {
    	public void receive(Measurement measurement) {
    		final SteeringWheelAngle angle = (SteeringWheelAngle) measurement;
    		mHandler.post(new Runnable() {
    			public void run() {
    				SteeringWheelAngle = angle.getValue().doubleValue();
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
    			mVehicleManager.addListener(SteeringWheelAngle.class,
    					mSteeringWheelListener);
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

	@Override
	public void onDestroy() {
		super.onDestroy();
		unbindService(mConnection);
	}
}