package com.ford.openxc.rearview;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Receive a broadcast on device startup and start the
 * VehicleMonitoringService.
 *
 * That service needs to be started to start watching for the transmission gear
 * position to change to "reverse".
*/
public class BootupReceiver extends BroadcastReceiver {
    private final static String TAG = "BootupReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
           Intent MonitoringServiceIntent = new Intent(context,
                   VehicleMonitoringService.class);
        Log.i(TAG, "Starting VehicleMonitoringService");
        context.startService(MonitoringServiceIntent);
    }
}
