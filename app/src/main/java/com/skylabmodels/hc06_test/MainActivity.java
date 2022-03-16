package com.skylabmodels.hc06_test;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;


//public class MainActivity extends AppCompatActivity
//        implements ActivityCompat.OnRequestPermissionsResultCallback {

interface zBroadcastListener{

    void updateSwitches(BluetoothDevice d);

}


public class MainActivity extends AppCompatActivity implements zBroadcastListener {

    Button buttonCalibrateXY, buttonCalibrateZ;
    SwitchCompat switchConnectElevator, switchConnectWing;
    TextView textSensorElevator, textSensorWing;
    TextView textViewResult;

    ConstraintLayout layout;
    Handler queueMessageHandler;

    private static final String TAG = "HC-06 Test";
    private static final String TAG_SP = "Preferences";
    final int RECEIVE_ELEVATOR_MESSAGE = 1;        // Status  for Handler
    final int RECEIVE_WING_MESSAGE = 2;
    final int ELEVATOR = 0;
    final int WING = 1;
    final String SKYLAB_MODEL_VERSION = BuildConfig.VERSION_NAME;

    final static int PERMISSION_BLUETOOTH = 1;
    final static int PERMISSION_BLUETOOTH_CONNECT = 2;

    boolean bt_permissions_granted;

    private BluetoothAdapter btAdapter = null;
    private SharedPreferences settings;
    private ConnectedThread mWing;
    private ConnectedThread mElevator;

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");


    private static String addressSensorWing;
    private static String addressSensorElevator;

    private static final byte[] msgCalXY     = {(byte) 0xFF, (byte) 0xAA, (byte) 0x67};
    private static final byte[] msgSetRate   = {(byte) 0xFF, (byte) 0xAA, (byte) 0x03, (byte) 0x07};
    private static final byte[] msgSendCmd   = {(byte) 0xFF, (byte) 0xAA, (byte) 0x69, (byte) 0x88, (byte) 0xb5};
    private static final byte[] prefixBW     = {(byte) 0xFF, (byte) 0xAA, (byte) 0x1f};
    private static final byte[] prefixRrate  = {(byte) 0xFF, (byte) 0xAA, (byte) 0x03};
    private static final byte[] msgSave      = {(byte) 0xFF, (byte) 0xAA, (byte) 0x00, (byte) 0x00, (byte) 0x00};
    private static final byte suffix = (byte) 0x00;
    //private static final byte[] msgCalZ = {(byte) 0xFF, (byte) 0xAA, (byte) 0x52};


    //@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_select_sensors) {
            // User chose the "Settings" item, show the app settings UI...
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            Log.d(TAG, "Pressed Settings");
        }

        if (item.getItemId() == R.id.action_select_version) {
            Log.d(TAG, "Menu Pressed Select Version");
            Toast.makeText(getBaseContext(), "Ramingo version: " + BuildConfig.VERSION_NAME, Toast.LENGTH_LONG).show();
        }

        return super.onOptionsItemSelected(item);
    }

    BroadcastReceiver btReceiver = new mBluetoothReceiver();


    IntentFilter filter1 = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
    IntentFilter filter2 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
    IntentFilter filter3 = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_BLUETOOTH) {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission is granted. Continue the action or workflow
                    // in your app.
                    Log.d(TAG, "in onRequestPermissionResults() " + "BLUETOOTH" +
                            " permission granted");
                }  else {
                    // Explain to the user that the feature is unavailable because
                    // the features requires a permission that the user has denied.
                    // At the same time, respect the user's decision. Don't link to
                    // system settings in an effort to convince the user to change
                    // their decision.
                    Log.d(TAG, "in onRequestPermissionResults() " + "BLUETOOTH" +
                            " permission NOT granted");
                }
        }
        // Other 'case' lines to check for other
        // permissions this app might request.
    }



    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted. Continue the action or workflow in your
                    // app.

                    bt_permissions_granted = true;
                } else {
                    // Explain to the user that the feature is unavailable because the
                    // features requires a permission that the user has denied. At the
                    // same time, respect the user's decision. Don't link to system
                    // settings in an effort to convince the user to change their
                    // decision.
                    bt_permissions_granted = false;
                    Toast.makeText(getBaseContext(), "permissions not granted, app cannot " +
                            "continue", Toast.LENGTH_LONG).show();
                }
            });



    public void updateSwitches(BluetoothDevice d) {
        String address =  d.getAddress();
        Log.d(TAG, "in updateSwitches(): called by device " + address);

        if (address.equals(addressSensorElevator) && switchConnectElevator.isChecked()){
            Toast.makeText(getBaseContext(), "Elevator Sensor Connection Lost",
                    Toast.LENGTH_LONG).show();
            mElevator.isConnected = false;
            mElevator.cancel();
            switchConnectElevator.setChecked(false);
        }

        if (address.equals(addressSensorWing) && switchConnectWing.isChecked()){
            Toast.makeText(getBaseContext(), "Wing Sensor Connection Lost",
                    Toast.LENGTH_LONG).show();
            mWing.isConnected = false;
            mWing.cancel();
            switchConnectWing.setChecked(false);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        registerReceiver(btReceiver, filter1);
        registerReceiver(btReceiver, filter2);
        registerReceiver(btReceiver, filter3);

        // Use this check to determine whether Bluetooth classic is supported on the device.
        // Then you can selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            Toast.makeText(this, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }


        // Register the permissions callback, which handles the user's response to the
        // system permissions dialog. Save the return value, an instance of
        // ActivityResultLauncher, as an instance variable.

        settings = PreferenceManager.getDefaultSharedPreferences(this);


        addressSensorWing = settings.getString("wing", "0");
        Log.d(TAG_SP, "In onCreate()... Wing Sensor address: " + addressSensorWing);
        addressSensorElevator = settings.getString("elevator", "0");
        Log.d(TAG_SP, "In onCreate()... Elevator Sensor address: " + addressSensorElevator);

        android.content.Context c = getBaseContext();
        Log.d(TAG, "context is" + c.toString());

        // Create interface
        setContentView(R.layout.activity_main);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        buttonCalibrateXY = findViewById(R.id.buttonCalibrateXY);
        buttonCalibrateZ = findViewById(R.id.buttonCalibrateZ);
        switchConnectElevator = findViewById(R.id.switchConnectElevator);
        switchConnectWing = findViewById(R.id.switchConnectWing);

        textSensorElevator = findViewById(R.id.textSensorElevator);
        textSensorWing = findViewById(R.id.textSensorWing);

        textViewResult = findViewById(R.id.textViewResult);


        layout = findViewById(R.id.layout);

        // Assign callbacks to all the switches/buttons
        buttonCalibrateXY.setOnClickListener(v -> {
            if (!switchConnectElevator.isChecked() && !switchConnectWing.isChecked()) {
                Toast.makeText(getBaseContext(), "No sensor selected. Please connect at least" +
                        "a sensor first", Toast.LENGTH_LONG).show();
                return;
            }
            if (mWing != null) {
                Toast.makeText(getBaseContext(), "Wait for Wing sensor calibration", Toast.LENGTH_SHORT).show();
                mWing.write(msgCalXY);
            }
            if (mElevator != null) {
                Toast.makeText(getBaseContext(), "Wait for Elevator calibration", Toast.LENGTH_SHORT).show();
                mElevator.write(msgCalXY);
            }
        });

        buttonCalibrateZ.setOnClickListener(v -> Toast.makeText(getBaseContext(), "TODO: change poll rate", Toast.LENGTH_SHORT).show());

        switchConnectElevator.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                addressSensorElevator = settings.getString("elevator", "0");
                try {
                    Log.d(TAG, "Elevator Switch activated. Connecting to " +
                            "//" + btAdapter.getRemoteDevice(addressSensorElevator).toString());
                    connectDevice(btAdapter.getRemoteDevice(addressSensorElevator), RECEIVE_ELEVATOR_MESSAGE, "Elevator");
                } catch (IllegalArgumentException e) {
                    switchConnectElevator.setChecked(false);
                    Toast.makeText(getBaseContext(), "No Sensor Associated. Please set one in the settings menu", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "no sensor associated");
                }
            } else if (mElevator != null)
                mElevator.cancel();
                //mElevator = null;
        });

        switchConnectWing.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (isChecked) {
                addressSensorWing = settings.getString("wing", "0");
                try {
                    Log.d(TAG, "Wing Switch activated. Connecting to " +
                            "//" + btAdapter.getRemoteDevice(addressSensorWing).toString());
                    connectDevice(btAdapter.getRemoteDevice(addressSensorWing), RECEIVE_WING_MESSAGE, "Wing");

                } catch (IllegalArgumentException e) {
                    switchConnectWing.setChecked(false);
                    Toast.makeText(getBaseContext(), "No Sensor Associated. Please set one in the settings menu", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "no sensor associated");
                }
            } else if (mWing != null)
                mWing.cancel();
                //mWing = null;
        });

        // Create processing data objects
        createDataProcessingObjects(16);

        // Create an handle to process the data received by the sensors

        // In each Connection Thread, when data are read, the data are sent to the following handler
        queueMessageHandler = new Handler(Looper.getMainLooper(), msg -> {
            switch (msg.what) {
                case RECEIVE_ELEVATOR_MESSAGE:
                    byte[] readElevatorBuf = (byte[]) msg.obj;
                    processBuffer(ELEVATOR, readElevatorBuf, textSensorElevator);
                    break;
                case RECEIVE_WING_MESSAGE:
                    byte[] readWingBuf = (byte[]) msg.obj;
                    processBuffer(WING, readWingBuf, textSensorWing);
                    break;
                default:
                    break;
            }
            textViewResult.setText(String.format(Locale.ITALIAN, "%3.1f", getAngle(ELEVATOR) - getAngle(WING)));
            return true;
        });

        // Ask to turn on phone's Bluetooth if needed
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        // Done

    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) {


        try {
            final Method m = device.getClass().getMethod("createRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, MY_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create RFComm Connection", e);
            return null;
        }

    }


    @RequiresApi(api = Build.VERSION_CODES.S)
    public void connectDevice(BluetoothDevice device, int m, String s) {
        Log.d(TAG, "...trying connection to " + s);
        String bwPreference = settings.getString("bandwidth", "0x07");
        String rrate = settings.getString("return_rate", "0x06");

        if (s.equals("Elevator")) {
            try {
                mElevator = new ConnectedThread(device, m, s);
                mElevator.start();


                Log.d(TAG, "Setting Bandwidth " + bwPreference);


                mElevator.write(msgSendCmd);

                byte command = (byte) Integer.decode(bwPreference).byteValue();
                byte [] msgBW = new byte[5];
                try {
                    msgBW = (byte[]) mElevator.create_command(prefixBW, command, suffix);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mElevator.write(msgBW);
                mElevator.write(msgSave);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                byte [] msgRR = new byte[5];
                //mElevator.write(msgSendCmd);
                command = Integer.decode(rrate).byteValue();
                try {
                    msgRR = mElevator.create_command(prefixRrate, command, suffix);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mElevator.write(msgRR);
                mElevator.write(msgSave);


            } catch (IOException e) {
                Log.d(TAG, "... in connectDevice(), thread creation failed");
                switchConnectElevator.setChecked(false);
            } catch (Exception e) {
                Log.d(TAG, "... BLUETOOTH_SCAN permission not granted");
            }

        } else if (s.equals("Wing")) {
            try {
                mWing = new ConnectedThread(device, m, s);

                mWing.start();

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mWing.write(msgSendCmd);
                byte command = (byte) Integer.decode(bwPreference).byteValue();
                byte [] msg = new byte[5];
                try {
                    msg = (byte[]) mWing.create_command(prefixBW, command, suffix);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                mWing.write(msg);
                mWing.write(msgSave);

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mWing.write(msgSendCmd);
                command = Integer.decode(rrate).byteValue();
                try {
                    msg = mWing.create_command(prefixRrate, command, suffix);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                mWing.write(msg);
                mWing.write(msgSave);



            } catch (IOException e) {
                Log.d(TAG, "... in connectDevice(), thread creation failed");
                switchConnectWing.setChecked(false);
            } catch (Exception e) {
                Log.d(TAG, "in connectDevice(): " + e);
            }


        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }


    @RequiresApi(api = Build.VERSION_CODES.S)
    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "...In onResume()...");

        addressSensorWing = settings.getString("wing", "0");
        Log.d(TAG_SP, "In onResume()... Wing Sensor address: " + addressSensorWing);
        addressSensorElevator = settings.getString("elevator", "0");
        Log.d(TAG_SP, "In onResume()... Elevator Sensor address: " + addressSensorElevator);

        resetData(WING, 16);
        resetData(ELEVATOR, 16);

        // TODO : Modify and reconnect only if the devices were connected
        if (mElevator != null) {
            Log.d(TAG, "...reconnecting Elevator Socket");
            mElevator.connect();
        }

        if (mWing != null) {
            Log.d(TAG, "...reconnecting Wing Socket");
            //mWing.connect();



        }

    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        if (mElevator != null) {
            Log.d(TAG, "...closing Elevator Socket");
            mElevator.cancel();
            switchConnectWing.setChecked(false);
        }

        /*
        if (mWing != null) {
            Log.d(TAG, "...closing Wing Socket");
            mWing.cancel();
            switchConnectWing.setChecked(false);
        }
        */

    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if (btAdapter == null) {
            errorExit("Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) Log.d(TAG, "...Bluetooth ON...");
            else {
                //Prompt user to turn on Bluetooth

                ActivityResultLauncher<Intent> enableBtResultLauncher = registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_CANCELED) {
                                errorExit("BT must be activated.");
                            }
                        }
                );

                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBtResultLauncher.launch(enableBtIntent);
            }
        }
    }

    private void errorExit(String message) {
        Toast.makeText(getBaseContext(), "Fatal Error" + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }


    private class ConnectedThread extends Thread {

        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final int MESSAGE;
        private BluetoothSocket btSocket = null;
        private final String position;
        boolean isConnected, stop;


        @RequiresApi(api = Build.VERSION_CODES.S)
        public ConnectedThread(BluetoothDevice device, int message, String p) throws Exception {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            MESSAGE = message;
            position = p;


            //Create Bluetooth Socket


            try {
                Log.d(TAG, "in ConnectionThread(): trying to create BT Socket");
                btSocket = createBluetoothSocket(device);
            } catch (Exception e) {
                errorExit("in Connected Thread: " + e.getMessage() + ".");
                Log.d(TAG, "in Connected Thread:" + e);
            }

            // Discovery is resource intensive.  Make sure it isn't going on
            // when you attempt to connect and pass your message.


            //btAdapter.cancelDiscovery();

            if (!connect()) {
                throw new IOException("Connect failed");
            }
            // Create a data stream so we can talk to server.
            Log.d(TAG, "...Create " + position + " Socket...");

            mmInStream = btSocket.getInputStream();
            mmOutStream = btSocket.getOutputStream();
            isConnected = true;

        }


        @RequiresApi(api = Build.VERSION_CODES.S)
        public boolean connect() {

            bt_permissions_granted = true;
            if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                Log.d(TAG, "in ConnectedThread.connect(): BLUETOOTH permission is not " +
                        "granted, so we launch the permission");
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH);

            } else {
                Log.d(TAG, "permission BLUETOOTH is granted, we can proceed!");
            }
            // Establish the connection.  This will block until it connects.
            Log.d(TAG, "...Connecting...");

            if (!bt_permissions_granted)
                return false;

            try {
                btSocket.connect();
                Log.d(TAG, ".... BT Connection to " + position + " ok...");
                Toast.makeText(getBaseContext(), "Connected to" + position, Toast.LENGTH_LONG).show();


                stop = false;
                return true;
            } catch (IOException e) {
                try {
                    btSocket.close();
                    Log.d(TAG, ".... BT Connection " + position + " failed...");
                    return false;

                } catch (IOException e2) {
                    errorExit("In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }

        public void run() {

            int bytes_available, bytes; // bytes returned from read()

            stop = false;
            // Keep listening to the InputStream until an exception occurs
            while (!stop) {

                try {

                    this.sleep(50);
                    //Log.d(TAG, "in ConnectedThread, " + MESSAGE + ", reading buffer ");
                    // Read from the InputStream
                    bytes_available = mmInStream.available();
                    if (bytes_available > 0) {
                        byte[] buffer = new byte[bytes_available];  // buffer store for the stream
                        //bytes = mmInStream.read(buffer, 0, 200);        // Get number of bytes and message in "buffer"
                        bytes = mmInStream.read(buffer);
                        //Log.d(TAG, "Bytes Read: " + bytes);

                        queueMessageHandler.obtainMessage(MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler in main thread
                    }

                } catch (IOException | InterruptedException e) {
                    Log.d(TAG, "Reading from BT exception: " + e.getMessage());
                    break;
                }
            }
            cancel();
            isConnected = false;
        }

        /* Call this from the main activity to send data to the remote device */
        @RequiresApi(api = Build.VERSION_CODES.O)
        public void write(byte[] msgBuffer) {
            try {
                for (byte b : msgBuffer){
                    Log.d(TAG, "Byte: " + Byte.toUnsignedInt(b));
                }
                Log.d(TAG, "Msg: " + msgBuffer.length);
                mmOutStream.write(msgBuffer, 0, msgBuffer.length);

            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }

        public void cancel() {
            try {
                btSocket.close();
                isConnected = false;
            } catch (IOException e2) {
                errorExit("From onPause() and failed to close " + position + " socket." + e2.getMessage() + ".");
            }
        }

        private byte[] create_command (byte[] prefix, byte command, byte suffix) throws IOException {
            ByteArrayOutputStream stream = new ByteArrayOutputStream( );
            stream.write(prefix);
            stream.write(command);
            stream.write(suffix);

            return stream.toByteArray();

        }

    }


    private void processBuffer(int id, byte[] buffer, TextView tv) {

        int bufferLen = buffer.length;

        while (bufferLen >= 11) {
            //Log.d(TAG, "BufLen: " + bufferLen);
            if (buffer[0] != 0x55) {
                //Log.d(TAG, "Start byte not found, going ahead");
                //The first element of the buffer is discarded
                bufferLen -= 1;
                System.arraycopy(buffer, 1, buffer, 0, bufferLen);
                continue;
            }
            //Log.d(TAG, "Start byte found");
            if (buffer[1] == 0x53) {
                //Log.d(TAG, "In processData2: Angles value found");
                handleData(id, Arrays.copyOfRange(buffer, 0, 11));
                String processed = String.format(Locale.ITALIAN, "%3.1f", getAngle(id));
                tv.setText(processed);
            }
            //TODO: the following lines should be moved into the above if structure.
            //Log.d(TAG, "Moving ahead by 11");
            bufferLen -= 11;
            System.arraycopy(buffer, 11, buffer, 0, bufferLen);
        }
    }


    //C functions

    static {
        System.loadLibrary("hc06_test");
    }

    //public native String processData(byte[] data);

    public native void createDataProcessingObjects(int avgLen);

    public native void handleData(int id, byte[] data);

    public native float getData(int id);

    public native float getStdDev(int id);

    public native float getAngle(int id);

    public native void resetData(int id, int N);

    public native void deleteDataProcessingObjects();
}


