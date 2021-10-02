package com.skylabmodels.hc06_test;

import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.sqrt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    Button buttonCalibrateXY, buttonCalibrateZ;
    Switch switchConnectElevator, switchConnectWing;
    TextView textSensorElevator, textSensorWing;
    TextView textViewResult;
    ConstraintLayout layout;
    Handler h;

    private static final String TAG = "bluetooth2";
    final int RECEIVE_ELEVATOR_MESSAGE = 1;        // Status  for Handler
    final int RECEIVE_WING_MESSAGE = 2;
    final int ELEVATOR = 0;
    final int WING =1;

    private BluetoothAdapter btAdapter = null;

    private ConnectedThread mConnectedThreadWing;
    private ConnectedThread mConnectedThreadElevator;

    // SPP UUID service
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // MAC-address of Bluetooth module (you must edit this line)
    private static final String addressSensorElevator = "20:21:01:12:32:11";
    private static final String addressSensorWing     = "20:21:01:12:27:63";

    private static final byte[] msgCalXY = {(byte) 0xFF, (byte) 0xAA, (byte) 0x67};
    private static final byte[] msgCalZ = {(byte) 0xFF, (byte) 0xAA, (byte) 0x52};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Create interface
        setContentView(R.layout.activity_main);

        buttonCalibrateXY       = findViewById(R.id.buttonCalibrateXY);
        buttonCalibrateZ        = findViewById(R.id.buttonCalibrateZ);
        switchConnectElevator   = findViewById(R.id.switchConnectElevator);
        switchConnectWing       = findViewById(R.id.switchConnectWing);

        textSensorElevator      = findViewById(R.id.textSensorElevator);
        textSensorWing          = findViewById(R.id.textSensorWing);

        textViewResult          = findViewById(R.id.textViewResult);



        layout = findViewById(R.id.layout);

        // Assign callbacks to all the switches/buttons
        buttonCalibrateXY.setOnClickListener(v -> {
            if (mConnectedThreadWing != null) {
                Toast.makeText(getBaseContext(), "Wait for calibration", Toast.LENGTH_SHORT).show();
                mConnectedThreadWing.write(msgCalXY);
            }
            if (mConnectedThreadElevator != null) {
                Toast.makeText(getBaseContext(), "Wait for calibration", Toast.LENGTH_SHORT).show();
                mConnectedThreadElevator.write(msgCalXY);
            }

        });

        buttonCalibrateZ.setOnClickListener(v -> Toast.makeText(getBaseContext(), "TODO: change poll rate", Toast.LENGTH_SHORT).show());

        switchConnectElevator.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b)
                connectDevice(btAdapter.getRemoteDevice(addressSensorElevator),
                        RECEIVE_ELEVATOR_MESSAGE, "Elevator");
            else if (mConnectedThreadElevator != null)
                mConnectedThreadElevator.cancel();

        });

        switchConnectWing.setOnCheckedChangeListener((compoundButton, b) -> {
            if (b)
                connectDevice(btAdapter.getRemoteDevice(addressSensorWing),
                        RECEIVE_WING_MESSAGE, "Wing");
            else if (mConnectedThreadWing != null)
                mConnectedThreadWing.cancel();
        });

        // Create processing data objects
        createDataProcessingObjects();

        // Create an handle to process the data received by the sensors
        h = new Handler(Looper.getMainLooper(), msg -> {
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
            textViewResult.setText(String.format(Locale.ITALIAN,"%3.2f", getData(WING) - getData(ELEVATOR)));
            return true;
        });

        // Ask to turn on phone's Bkuetooth if needed
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBTState();

        // Done

    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        try {
            final Method m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", UUID.class);
            return (BluetoothSocket) m.invoke(device, MY_UUID);
        } catch (Exception e) {
            Log.e(TAG, "Could not create Insecure RFComm Connection", e);
        }
        return device.createRfcommSocketToServiceRecord(MY_UUID);
    }


    public void connectDevice(BluetoothDevice device, int m, String s) {
        Log.d(TAG, "...onResume - try connect to " + s);
        if (s.equals("Elevator")) {
            mConnectedThreadElevator = new ConnectedThread(device, m, s);
            mConnectedThreadElevator.start();
        } else {
            mConnectedThreadWing = new ConnectedThread(device, m, s);
            mConnectedThreadWing.start();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "...In onPause()...");

        if (mConnectedThreadElevator != null){
            Log.d(TAG, "...closing Elevator Socket");
            mConnectedThreadElevator.connect();
        }

        if (mConnectedThreadWing != null) {
            Log.d(TAG, "...closing Wing Socket");
            mConnectedThreadWing.connect();
        }

    }

    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "...In onPause()...");

        if (mConnectedThreadElevator != null){
            Log.d(TAG, "...closing Elevator Socket");
            mConnectedThreadElevator.start();
        }

        if (mConnectedThreadWing != null) {
            Log.d(TAG, "...closing Wing Socket");
            mConnectedThreadWing.start();
        }
    }

    private void checkBTState() {
        // Check for Bluetooth support and then check to make sure it is turned on
        // Emulator doesn't support Bluetooth and will return null
        if(btAdapter==null) {
            errorExit("Bluetooth not support");
        } else {
            if (btAdapter.isEnabled()) {
                Log.d(TAG, "...Bluetooth ON...");
            } else {
                //Prompt user to turn on Bluetooth

                ActivityResultLauncher<Intent> enableBtResultLauncher = registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == Activity.RESULT_CANCELED){
                                errorExit("BT must be activated.");
                            }
                        }
                );

                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                enableBtResultLauncher.launch(enableBtIntent);
            }
        }
    }

    private void errorExit(String message){
        Toast.makeText(getBaseContext(), "Fatal Error" + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }


    private class ConnectedThread extends Thread {


        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final int MESSAGE;
        private BluetoothSocket btSocket = null;
        private final String position;

        public ConnectedThread(BluetoothDevice device, int message, String p) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            MESSAGE = message;
            position = p;

            //Create Bluetooth Socket

            try {
                btSocket = createBluetoothSocket(device);
            } catch (IOException e) {
                errorExit("In onResume() and socket create failed: " + e.getMessage() + ".");
            }

            // Discovery is resource intensive.  Make sure it isn't going on
            // when you attempt to connect and pass your message.
            btAdapter.cancelDiscovery();

            connect();

            // Create a data stream so we can talk to server.
            Log.d(TAG, "...Create " + position + " Socket...");

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = btSocket.getInputStream();
                tmpOut = btSocket.getOutputStream();
            } catch (IOException e) {
                errorExit("In ConnectedThread()" + e.getMessage() + ".");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void connect(){

            // Establish the connection.  This will block until it connects.
            Log.d(TAG, "...Connecting...");
            try {
                btSocket.connect();
                Log.d(TAG, ".... Wing Connection ok...");
                Toast.makeText(getBaseContext(), "Connected to" + position, Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    errorExit("In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
                }
            }


        }

        public void run() {
            byte[] buffer = new byte[512];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    //Log.d(TAG, "in ConnectedThread, " + MESSAGE + ", reading buffer ");
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
        public void write(byte[] msgBuffer) {
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException e) {
                Log.d(TAG, "...Error data send: " + e.getMessage() + "...");
            }
        }

        public void cancel () {
            try {
                btSocket.close();
            } catch (IOException e2) {
                errorExit("From onPause() and failed to close " + position + " socket." + e2.getMessage() + ".");
            }
        }
    }



    private void processBuffer (int id, byte[] buffer, TextView tv){

        int bufferLen = buffer.length;

        while (bufferLen >= 11) {
            //Log.d(TAG, "BufLen: " + bufferLen);
            if (buffer[0] != 0x55){
                //Log.d(TAG, "Start byte not found, going ahead");
                bufferLen -= 1;
                System.arraycopy(buffer, 1, buffer, 0, bufferLen);
                continue;
            }
            //Log.d(TAG, "Start byte found");
            if (buffer[1] == 0x53) {
                Log.d(TAG, "In processData2: Angles value found");
                handleData(id, Arrays.copyOfRange(buffer, 0, 11));
                String processed = String.format(Locale.ITALIAN, "%3.2f +/- %3.2f", getData(id), getStdDev(id));
                tv.setText(processed);
            }

            //Log.d(TAG, "Moving ahead by 11");
            bufferLen -= 11;
            System.arraycopy(buffer, 11, buffer, 0, bufferLen);
        }
    }

    private static class smoothData {

        private float[] angleBuffer;
        private int pos;
        private int k;
        private float avg;
        private float sum;
        private float dev;
        private int avgLen;

        public smoothData (int L) {
            pos = 0;
            k = 0;
            avg = 0;
            sum = 0;
            dev = 0;
            avgLen = L;

        }

        public void mobile_average (float d) {
            if (k > avgLen && (abs(avg - d) > 8*dev))
                Log.d(TAG, "Ops " + d + "/" + avg + "/" + dev);

            if (k < avgLen)
                avgLen = k;

            sum += sum - angleBuffer[pos] + d;
            angleBuffer[pos] = d;
            avg = sum / avgLen;
            dev = standardDev(angleBuffer, avg, avgLen);

            pos++;
            if (pos > avgLen)
                pos = 0;
        }

        public float standardDev(float[] x, float xm, int avgLen){
            float sDev = 0;
            int i = 0;

            for (i = 0; i < avgLen; i++)
                sDev += (x[i] - xm) * (x[i] - xm);

            sDev = (float) sqrt(sDev / avgLen);
            sDev = (sDev > 0.001) ? sDev : (float) 0.001;
            return sDev;
        }

    }

    //C functions

    static {
       System.loadLibrary("hc06_test");
    }
    public native String processData(byte [] data);
    public native void createDataProcessingObjects();
    public native void handleData(int id, byte[] data);
    public native float getData(int id);
    public native float getStdDev(int id);
    public native void resetData(int id, int N);
    public native void deleteDataProcessingObjects();
}


