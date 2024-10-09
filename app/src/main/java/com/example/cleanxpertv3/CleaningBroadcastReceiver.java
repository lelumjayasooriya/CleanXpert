package com.example.cleanxpertv3;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class CleaningBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d("CleaningBroadcastReceiver", "Cleaning task triggered.");
        // Send Bluetooth start command
        MainActivity activity = MainActivity.getInstance();
        if (activity != null) {
            activity.sendCommand("S"); // Send the same "Start Cleaning" command
        }
    }
}
