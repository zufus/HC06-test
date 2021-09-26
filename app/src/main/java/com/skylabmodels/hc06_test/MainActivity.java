package com.skylabmodels.hc06_test;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.bluetooth.BluetoothAdapter;
//import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    Button buttonCalibrate, buttonPollRate;
    TextView textSensorElevator, textSensorWing;
    ConstraintLayout layout;
    Handler h;

    private static final String TAG = "bluetooth2";
    final int RECEIVE_ELEVATOR_MESSAGE = 1;        // Status  for Handler
    final int RECEIVE_WING_MESSAGE = 2;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocketElevator = null;
    private BluetoothSocket btSocketWing = null;


    private final StringBuilder sb = new StringBuilder();
    private static int flag = 0;

    private ConnectedThread mConnectedThreadWing, mConnectedThreadElevator;

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Bluetooth module (you must edit this line)
    private static final String addressSensorElevator = "20:21:01:12:32:11";
    private static final String addressSensorWing     = "20:21:01:12:27:63";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonCalibrate = findViewById(R.id.buttonCalibrate);

        buttonPollRate = findViewById(R.id.buttonPollRate);

        textSensorElevator = findViewById(R.id.textSensorElevator);
        textSensorWing = findViewById(R.id.textSensorWing);
        layout = findViewById(R.id.layout);

        h = new Handler() {
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case RECEIVE_ELEVATOR_MESSAGE:
                        byte[] readElevatorBuf = (byte[]) msg.obj;
                        String strEleIncom = new String(readElevatorBuf, 0, msg.arg1);
                        Log.d(TAG, "Received Elevator Message");
                        textSensorElevator.setText(strEleIncom);
                        break;
                    case RECEIVE_WING_MESSAGE:
                        byte[] readWingBuf = (byte[]) msg.obj;
                        String strWingIncom = new String(readWingBuf, 0, msg.arg1);
                        Log.d(TAG, "Received Wing Message");
                        textSensorWing.setText(strWingIncom);
                        break;
                    default:
                        break;
                }
            }

        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        buttonCalibrate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getBaseContext(), "Wait for calibration", Toast.LENGTH_SHORT).show();
            }
        });

        buttonPollRate.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Toast.makeText(getBaseContext(), "TODO: change poll rate", Toast.LENGTH_SHORT).show();
            }
        });

    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        if (Build.VERSION.SDK_INT >= 10) {
            try {
                final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[]{UUID.class});
                return (BluetoothSocket) m.invoke(device, MY_UUID);
            } catch (Exception e) {
                Log.e(TAG, "Could not create Insecure RFComm Connection", e);
            }
        }
        return device.createRfcommSocketToServiceRecord(MY_UUID);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "...onResume - try connect to Elevator");

        // Set up a pointer to the remote node using it's address.
        BluetoothDevice deviceElevator = btAdapter.getRemoteDevice(addressSensorElevator);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocketElevator = createBluetoothSocket(deviceElevator);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting to Elevator...");
        try {
            btSocketElevator.connect();
            Log.d(TAG, "....Connection ok...");
            Toast.makeText(getBaseContext(), "Connected", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            try {
                btSocketElevator.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Elevator Socket...");

        mConnectedThreadElevator = new ConnectedThread(btSocketElevator, RECEIVE_ELEVATOR_MESSAGE);
        mConnectedThreadElevator.start();


        Log.d(TAG, "...onResume - try connect to Wing");


        // Set up a pointer to the remote node using it's address.
        BluetoothDevice deviceWing = btAdapter.getRemoteDevice(addressSensorWing);

        // Two things are needed to make a connection:
        //   A MAC address, which we got above.
        //   A Service ID or UUID.  In this case we are using the
        //     UUID for SPP.

        try {
            btSocketWing = createBluetoothSocket(deviceWing);
        } catch (IOException e) {
            errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
        }

        // Discovery is resource intensive.  Make sure it isn't going on
        // when you attempt to connect and pass your message.
        btAdapter.cancelDiscovery();

        // Establish the connection.  This will block until it connects.
        Log.d(TAG, "...Connecting...");
        try {
            btSocketWing.connect();
            Log.d(TAG, ".... Wing Connection ok...");
            Toast.makeText(getBaseContext(), "Connected to Wing", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            try {
                btSocketWing.close();
            } catch (IOException e2) {
                errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
            }
        }

        // Create a data stream so we can talk to server.
        Log.d(TAG, "...Create Wing Socket...");

        mConnectedThreadWing = new ConnectedThread(btSocketWing, RECEIVE_WING_MESSAGE);
        mConnectedThreadWing.start();

    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        try     {
            btSocketElevator.close();
        } catch (IOException e2) {
            errorExit("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
        }
    }



    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Fatal Error", "Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    private void errorExit(String title, String message){
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final int MESSAGE;

        public ConnectedThread(BluetoothSocket socket, int message) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            MESSAGE = message;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);        // Get number of bytes and message in "buffer"
                    //Log.d(TAG, "Bytes Read: " + bytes);
                    h.obtainMessage(MESSAGE, bytes, -1, buffer).sendToTarget();     // Send to message queue Handler
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(String message) {
            Log.d(TAG, "...Data to send: " + message + "...");
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }
    }

}


