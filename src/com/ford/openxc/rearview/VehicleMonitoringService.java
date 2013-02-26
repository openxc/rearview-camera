package com.ford.openxc.rearview;

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

/** Listens for changes in gear position from the vehicle.
 *
 * This services binds with OpenXC's VehicleManager and takes care of monitoring
 * the state of the vehicle for the RearviewCamera application.
 *
 * The service also monitors the RearviewCameraActivity itself, to avoid
 * re-launching if it's already open. When the vehicle shifts out of reverse,
 * the service makes sure the video activity is stopped.
 */
public class VehicleMonitoringService extends Service {
    private final static String TAG = "VehicleMonitoringService";
    public static final String ACTION_VEHICLE_UNREVERSED =
            "com.ford.openxc.VEHICLE_UNREVERSED";

    private final Handler mHandler = new Handler();
    private VehicleManager mVehicleManager;

    TransmissionGearPosition.Listener mTransmissionListener =
        new TransmissionGearPosition.Listener() {
            public void receive(Measurement measurement) {
                final TransmissionGearPosition status =
                        (TransmissionGearPosition) measurement;

                mHandler.post(new Runnable() {
                    public void run() {
                        if(status.getValue().enumValue() ==
                                TransmissionGearPosition.GearPosition.REVERSE &&
                                !RearviewCameraActivity.isRunning()){
                            startRearviewCameraActivity();
                        } else if(status.getValue().enumValue() !=
                                TransmissionGearPosition.GearPosition.REVERSE &&
                                RearviewCameraActivity.isRunning()) {
                            sendVehicleUnreversedBroadcast();
                        }
                    }

                    private void startRearviewCameraActivity() {
                        Intent intent = new Intent(
                                VehicleMonitoringService.this,
                                RearviewCameraActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        VehicleMonitoringService.this.startActivity(intent);
                    }

                    private void sendVehicleUnreversedBroadcast() {
                        Intent unreversedIntent = new Intent(
                                ACTION_VEHICLE_UNREVERSED);
                        sendBroadcast(unreversedIntent);
                    }
                });
            }
        };

    ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className,
                IBinder service) {
            Log.i(TAG, "Bound to VehicleManager");
            mVehicleManager = ((VehicleManager.VehicleBinder)service
                    ).getService();

            try {
                mVehicleManager.addListener(TransmissionGearPosition.class,
                        mTransmissionListener);
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
        bindService(new Intent(this, VehicleManager.class), mConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unbindService(mConnection);
    }
}
