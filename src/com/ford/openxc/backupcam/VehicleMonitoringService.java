package com.ford.openxc.backupcam;


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

/** Binds to VehicleManager.  Adds a listener for steering wheel angle and
 * transmission gear position.
 *
 * The purpose of this service is to bind with the VehicleManager, a service
 * performed by the OpenXC Enabler application. The service implements two
 * listeners, one for the steering wheel angle and one for the transmission gear
 * position.
 *
 * This service is launched both on bootup by BootupReceiver (see #2. and when
 * BackupCameraActivity (see #1. is launched.
 * The service also monitors the status of the activity (whether is is running or
 * not). By monitoring both the status of the transmission and the status of the
 * activity, the service can launch the activity appropriately. When the service
 * detects that the vehicle has been put into reverse, it checks to see if the
 * activity is running or not through the isRunning() method in
 * BackupCameraActivity. If the activity is not running, then both conditions are
 * satisfied for it to launch the activity. In addition, if the service detects
 * that the vehicle is no longer in reverse, it checks whether the activity is
 * running or not through the same method. If it is running and the car is not in
 * reverse, it sends an intent to BackupCameraActivity. When that intent is
 * received, BackupCameraActivity calls its finish() method, which closes the
 * application.
 */
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
