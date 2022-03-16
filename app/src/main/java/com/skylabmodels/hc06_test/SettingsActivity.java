package com.skylabmodels.hc06_test;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Set;



public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "settings";


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Log.d("MainActivity", "Settings activity Permission approved");
                    appendLog(this, "in onCreatePreferences(): Permission Granted");
                } else {
                    Log.d("MainActivity", "Error getting BT permission");
                    appendLog(this, "in onCreatePreferences(): Permission Not Granted");
                }
                return;
            default:
                throw new IllegalStateException("Unexpected value: " + requestCode);
        }

    }

    public static void appendLog(Context context, String text) {

        File logFile = new File(context.getExternalFilesDir(null), "log.file");
        Log.d(TAG, "writing to" + logFile.getAbsolutePath());
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } try {
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(String.valueOf(Calendar.getInstance().getTime())).append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            Log.d(TAG, "preference changed");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onStop() {
        super.onStop();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(listener);
    }



    public static class SettingsFragment extends PreferenceFragmentCompat {

        @RequiresApi(api = Build.VERSION_CODES.R)
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            //appendLog(getContext(),"in onCreatePreferences():");
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            //appendLog(getContext(), "in onCreatePreferences(): preferences loaded");


            //Query for paired devices
            BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Log.d(TAG, "in onCreatePreferences(): requesting permission");
                appendLog(getContext(), "in onCreatePreferences(): requesting permission");
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.BLUETOOTH},1);
            }
            //Log.d(TAG, "in onCreatePreferences(): BT Permission Requested, we now query the list of paired devices");
            //appendLog(getContext(), "in onCreatePreferences(): we now query the list of paired devices");
            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

            //Log.d(TAG, "in onCreatePreferences(): Found " + pairedDevices.size() + " devices");
            //appendLog(getContext(), "in onCreatePreferences(): Found " + pairedDevices.size() + " devices");

            ListPreference device_wing_list = (ListPreference) findPreference("wing");
            ListPreference device_elevator_list = (ListPreference) findPreference("elevator");

            if (pairedDevices.size() > 0) {
                List<String> deviceNames = new ArrayList<String>();
                List<String> deviceHardwareAddresses = new ArrayList<String>();
                for(BluetoothDevice bt : pairedDevices) {
                    //appendLog(getContext(), "in onCreatePreferences(): ========= ");
                    //Log.d(TAG, "in onCreatePreferences(): ========= ");
                    String alias;
                    alias = bt.getAlias();
                    //appendLog(getContext(), "in onCreatePreferences(): Found Alias " + alias);
                    //Log.d(TAG, "in onCreatePreferences(): Found Alias " + alias);
                    if (alias == null) {
                        //Log.d(TAG, "Alias not valid, we use the name");
                        //appendLog(getContext(), "in onCreatePreferences(): alias is not valid, we use name");
                        alias = bt.getName();
                        //appendLog(getContext(), "in onCreatePreferences(): Found Name " + alias);
                        //Log.d(TAG, "in onCreatePreferences(): Found Name " + alias);
                    }

                    if (alias == null)
                        break;


                    deviceNames.add(alias);
                    deviceHardwareAddresses.add(bt.getAddress());
                    //Log.d(TAG , "Found " +
                    //        deviceNames.get(deviceNames.size() -1) +
                    //        "//" + bt.getName() +
                    //        " MAC " +
                    //        deviceHardwareAddresses.get(deviceHardwareAddresses.size() - 1));
                    //appendLog(getContext(), "in onCreatePreferences(): " + alias + "MAC is " +
                    //        deviceHardwareAddresses.get(deviceHardwareAddresses.size() - 1));
                    //appendLog(getContext(), "in onCreatePreferences(): ==============");
                    //Log.d(TAG, "in onCreatePreferences(): ==============");
                }
                //appendLog(getContext(), "in onCreatePreferences(): No other devices found. We can now populate the ListPreference");
                //Log.d(TAG, "in onCreatePreferences(): No other devices found. We can now populate the ListPreference");
                // Modify the following code to run it only if the preference is updated by the user.

                CharSequence[] device_list_names = deviceNames.toArray(new CharSequence[0]);
                CharSequence[] device_list_mac = deviceHardwareAddresses.toArray(new CharSequence[0]);

                //Log.d(TAG, Arrays.toString(device_list_names));
                //Toast.makeText(getContext(), "Dev: " + Arrays.toString(device_list_names), Toast.LENGTH_LONG).show();
                //appendLog(getContext(), "Dev: " + Arrays.toString(device_list_names));
                //Context context = getPreferenceManager().getContext();

                //assert device_wing_list != null;
                //appendLog(getContext(), "in onCreatePreferences(): Populating Wing Sensor");
                device_wing_list.setEntries(device_list_names);
                device_wing_list.setEntryValues(device_list_mac);


                //assert device_elevator_list != null;
                ///appendLog(getContext(), "in onCreatePreferences(): Populating Elevator Sensor");
                device_elevator_list.setEntries(device_list_names);
                device_elevator_list.setEntryValues(device_list_mac);
            }

            //appendLog(getContext(), "in onCreatePreferences(): We now reister the preference selector listener");
            //assert device_wing_list != null;
            //device_wing_list.setOnPreferenceClickListener(preference -> {
            //    Log.d(TAG, "Wing preference selected");
            //    return true;
            //});
        }



    }



}