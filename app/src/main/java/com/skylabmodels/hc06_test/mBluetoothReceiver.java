package com.skylabmodels.hc06_test;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

class mBluetoothReceiver extends BroadcastReceiver {
    private static final String TAG = "HC-06 test receiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        Log.d(TAG, "Something append to " + device.toString());
        zBroadcastListener listener = (zBroadcastListener) context;

        if (action != null && action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))
            listener.updateSwitches(device);
    }
}
