package com.example.ansambassamabdulhamid.ledapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.UUID;

import weka.classifiers.trees.J48;
import weka.core.DenseInstance;
import weka.core.Instances;

public class MainActivity extends AppCompatActivity {

    private MqttAndroidClient client;
    private String TAG = "MainActivity";
    private PahoMqttClient pahoMqttClient;
    static String  inputString = null;

    private EditText textMessage, subscribeTopic, unSubscribeTopic;
    private Button publishMessage, subscribe, unSubscribe, receive;
    TextView myLabel;
    int counterg = 0;
    int insNum = 20; //length of sliding window




    TextView txtArduino, txtString, txtStringLength, sensorView0, sensorView1, sensorView2, sensorView3;
    Handler bluetoothIn;

    final int handlerState = 0;        				 //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private ArrayList<String> recDataString = new ArrayList<>();
    private String[] liveDataString = new String[120];
    private Instances tempTrain;

    private ConnectedThread mConnectedThread;

    int n = 0;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;

    private double[] liveData = new double[121];
     Instances train;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pahoMqttClient = new PahoMqttClient();

        myLabel = (TextView) findViewById(R.id.tv_receivedData);

        textMessage = (EditText) findViewById(R.id.textMessage);
        publishMessage = (Button) findViewById(R.id.publishMessage);

        subscribe = (Button) findViewById(R.id.subscribe);
        unSubscribe = (Button) findViewById(R.id.unSubscribe);
        receive = (Button) findViewById(R.id.receive);

        subscribeTopic = (EditText) findViewById(R.id.subscribeTopic);
        unSubscribeTopic = (EditText) findViewById(R.id.unSubscribeTopic);
        client = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);

        publishMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = textMessage.getText().toString().trim();
                if (!msg.isEmpty()) {
                    try {
                        pahoMqttClient.publishMessage(client, msg, 1, Constants.PUBLISH_TOPIC);

                    } catch (MqttException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        receive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        subscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String topic = subscribeTopic.getText().toString().trim();
                if (!topic.isEmpty()) {
                    try {
                        pahoMqttClient.subscribe(client, topic, 1);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        unSubscribe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String topic = unSubscribeTopic.getText().toString().trim();
                if (!topic.isEmpty()) {
                    try {
                        pahoMqttClient.unSubscribe(client, topic);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        Intent intent = new Intent(MainActivity.this, MqttMessageService.class);
        startService(intent);



        //Link the buttons and textViews to respective views
        txtString = (TextView) findViewById(R.id.txtString);
        txtStringLength = (TextView) findViewById(R.id.testView1);
        sensorView0 = (TextView) findViewById(R.id.sensorView0);
        sensorView1 = (TextView) findViewById(R.id.sensorView1);
        sensorView2 = (TextView) findViewById(R.id.sensorView2);
        sensorView3 = (TextView) findViewById(R.id.sensorView3);

        bluetoothIn = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == handlerState) {//if message is what we want

                    // load training data
                    BufferedReader breader = null;

                        try {

                            breader = new BufferedReader(
                                    new InputStreamReader(getAssets().open("all_train_data.arff")));
                            train = new Instances (breader);

                            tempTrain = new Instances(train);

                            train.setClassIndex(train.numAttributes() -1);
                            tempTrain.setClassIndex(tempTrain.numAttributes() -1);
                            tempTrain.clear();

                            J48 tree = new J48();         // new instance of tree
                            tree.buildClassifier(train);

                            //Load live data
                            inputString = (String) msg.obj;

                            if (inputString != null && inputString.length() > 0) {
                                StringBuilder sb = new StringBuilder(inputString);
                                String res = "";
                                for(int i = 0; i < sb.length(); i++){
                                    if(sb.charAt(i) != ',' && sb.charAt(i) != 'h' && sb.charAt(i) != ' '){
                                        res += sb.charAt(i);
                                    }else {
                                        if(n <= 120){
                                            if(!res.equals("")){
                                                Log.d("RES", res);
                                                liveDataString[n] = res;
                                                liveData[n] = Double.parseDouble(liveDataString[n]); // Another common reason for NumberFormatException is the alphanumeric input. No non-numeric letter other than + and - is not permitted in the input string.
                                                Log.i("CONTENT", ""+liveData[n]);
                                                n++;
                                                res = "";
                                                Log.d("INDEX", ""+n);
                                            }


                                        }
                                    }
                                }

                            }

                            if (n >= 100){
                                n = 0;
                                tempTrain.add(new DenseInstance(1.0,liveData));
                                int classIndex = tempTrain.numAttributes()-1;
                                int numInstances = tempTrain.numInstances()-1;
                                double classLabel = 0;
                                classLabel = tree.classifyInstance(tempTrain.instance(numInstances));
                                tempTrain.instance(numInstances).setClassValue(classLabel);
                                String gesture = tempTrain.instance(numInstances).attribute(classIndex).value((int)classLabel);
                                Log.d("GESTURE",gesture);
                                sendGesture(gesture);
                                Log.d("BREAK","-------------------------------------------------------------------------------------------------------");

                            }

                        }catch (IOException e){

                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                }

                }

        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();


    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    public void sendGesture(String gesture) throws MqttException, UnsupportedEncodingException {
        switch (gesture){
            case "right":
                    pahoMqttClient.publishMessage(client, "right", 1, Constants.PUBLISH_TOPIC);
                break;
            case "left":
                pahoMqttClient.publishMessage(client, "left", 1, Constants.PUBLISH_TOPIC);
                break;
            case "up":
                pahoMqttClient.publishMessage(client, "up", 1, Constants.PUBLISH_TOPIC);
                break;
            case "down":
                pahoMqttClient.publishMessage(client, "down", 1, Constants.PUBLISH_TOPIC);
                break;
        }
    }



    @Override
    public void onResume() {
        super.onResume();

        //Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();

        //Get the MAC address from the DeviceListActivty via EXTRA
        address = intent.getStringExtra(DeviceListActivity.EXTRA_DEVICE_ADDRESS);

        //create device and set the MAC address
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }
        // Establish the Bluetooth socket connection.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try
            {
                btSocket.close();
            } catch (IOException e2)
            {
                //insert code to deal with this
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }


        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);        	//read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
    }
}
