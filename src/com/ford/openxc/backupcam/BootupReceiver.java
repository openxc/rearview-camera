package com.ford.openxc.backupcam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Receive the BOOT_COMPLETED signal and start the VehicleMonitoringService.
 *
 * This is a receiver whose purpose is to listen for an intent sent by the
 * Android system that the device has been booted. When received, the receiver
 * launches the VehicleMonitoringService. The purpose of this is that it gives
 * the app the ability to monitor the status of the transmission without the
 * need for the application to be manually launched. The user can simply turn on
 * the tablet and it's ready to go.
*/
public class BootupReceiver extends BroadcastReceiver {
    private final static String TAG = "BootupReceiver";

    @Override
    public void onReceive(final Context context, Intent intent) {
           Intent MonitoringServiceIntent = new Intent(context, VehicleMonitoringService.class);
         Log.i(TAG, "Starting VehicleMonitoringService");
        context.startService(MonitoringServiceIntent);
    }
}




