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
import java.util.UUID;

import weka.classifiers.trees.J48;
import weka.core.DenseInstance;
import weka.core.Instances;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private MqttAndroidClient client;
    private String TAG = "MainActivity";
    private PahoMqttClient pahoMqttClient;
    static String  inputString = null;

    private EditText textMessage, subscribeTopic, unSubscribeTopic;
    private Button btnConnect, subscribe, unSubscribe, receive;

    TextView txtGesture;
    Handler bluetoothIn;

    final int handlerState = 1;        				 //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private String[] liveDataString = new String[120];
    private static Instances tempTrain;

    private ConnectedThread mConnectedThread;

    int n = 0;
    int counter =0;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;

    private double[] liveData = new double[121];
    static Instances train;
    static J48 tree;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialise();
        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        BufferedReader breader = null;
        try {
            breader = new BufferedReader(
                    new InputStreamReader(getAssets().open("TEST1IOT.arff")));
                      train = new Instances (breader);
            tempTrain = new Instances(train);
            train.setClassIndex(train.numAttributes() -1);
            tempTrain.setClassIndex(tempTrain.numAttributes() -1);
            tempTrain.clear();
            tree = new J48();         // new instance of tree
            try {
                tree.buildClassifier(train);
            } catch (Exception e) {
                e.printStackTrace();
            }


            tree = new J48();         // new instance of tree
            try {
                tree.buildClassifier(train);
            } catch (Exception e) {
                e.printStackTrace();
            }
            bluetoothReader();

    } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initialise() {
        btnConnect = (Button) findViewById(R.id.connect);
        subscribe = (Button) findViewById(R.id.subscribe);
        unSubscribe = (Button) findViewById(R.id.unSubscribe);

        subscribeTopic = (EditText) findViewById(R.id.subscribeTopic);
        unSubscribeTopic = (EditText) findViewById(R.id.unSubscribeTopic);
        txtGesture = (TextView)findViewById(R.id.tv_receivedGesture);

        btnConnect.setOnClickListener(this);
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    public void sendGesture(String gesture) throws MqttException, UnsupportedEncodingException {
        switch (gesture){
            case "right":
                    pahoMqttClient.publishMessage(client, "2", 1, Constants.PUBLISH_TOPIC);
                break;
            case "left":
                pahoMqttClient.publishMessage(client, "4", 1, Constants.PUBLISH_TOPIC);
                break;
            case "up":
                pahoMqttClient.publishMessage(client, "1", 1, Constants.PUBLISH_TOPIC);//tiltright = 5
                break;
            case "down":
                pahoMqttClient.publishMessage(client, "3", 1, Constants.PUBLISH_TOPIC); //titltleft = 6
                break;
        }
    }

    private class BluetoothReader extends Thread{

    }
    private void NewDataSet(String str){

    }

    private void bluetoothReader(){

        bluetoothIn = new Handler() {
            public void handleMessage(Message msg) {

                if (msg.what == handlerState) {//if message is what we want
                    // load training data
                        //Load live data
                   // inputString = msg.obj.toString();

                   byte[] writeBuff = (byte[]) msg.obj;
                    int begin = (int)msg.arg1;
                    int end = (int)msg.arg2;

                    String s = new String(writeBuff);
                    inputString = s.substring(begin,end);

                        Log.d("INPUTSTRING",inputString);
                    String [] nydataArray = inputString.split(",");
                    if (nydataArray.length>= 7 && nydataArray[0].equals("h")){
                        liveDataString[6 * counter]= nydataArray[1];
                        liveDataString[6 * counter+1]= nydataArray[2];
                        liveDataString[6 * counter+2]= nydataArray[3];
                        liveDataString[6 * counter+3]= nydataArray[4];
                        liveDataString[6 * counter+4]= nydataArray[5];
                        liveDataString[6 * counter+5]= nydataArray[6];
                        counter++;
                    }

                    if (counter> 19){
                        Log.d("In-forloop","------------------------------------------------------------20 values");
                        for (int i = 0; i <liveDataString.length; i++){
                            liveData[i] = Integer.parseInt(liveDataString[i]);
                        }
                        counter = 0;
                        tempTrain.add(new DenseInstance(1.0,liveData));
                        int classIndex = tempTrain.numAttributes()-1;
                        int numInstances = tempTrain.numInstances()-1;
                        double classLabel = 0;
                        try {
                            classLabel = tree.classifyInstance(tempTrain.instance(numInstances));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        tempTrain.instance(numInstances).setClassValue(classLabel);
                        String gesture = tempTrain.instance(numInstances).attribute(classIndex).value((int)classLabel);
                        Log.d("GESTURE",gesture);
                        txtGesture.setText(gesture);
                     /*   try {
                            sendGesture(gesture);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        } */

                        Log.d("BREAK","-------------------------------------------------------------------------------------------------------");
                    }


                      /*  if (inputString != null && inputString.length() > 0) {
                            StringBuilder sb = new StringBuilder(inputString);
                            String res = "";
                            for(int i = 0; i < sb.length(); i++){

                                if(sb.charAt(i) != ',' && sb.charAt(i) != 'h' && sb.charAt(i) != ' '){
                                    res += sb.charAt(i);
                                }
                                else {
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
                        } */
                 /*   if (n > 119){

                        tempTrain.add(new DenseInstance(1.0,liveData));
                        int classIndex = tempTrain.numAttributes()-1;
                        int numInstances = tempTrain.numInstances()-1;
                        double classLabel = 0;
                        try {
                            classLabel = tree.classifyInstance(tempTrain.instance(numInstances));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        tempTrain.instance(numInstances).setClassValue(classLabel);
                        String gesture = tempTrain.instance(numInstances).attribute(classIndex).value((int)classLabel);
                        Log.d("GESTURE",gesture);
                        txtGesture.setText(gesture);
                        try {
                            sendGesture(gesture);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        Log.d("BREAK","-------------------------------------------------------------------------------------------------------");

                        n = 0;
                    } */


                }
                }

        };


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

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.connect:
                pahoMqttClient = new PahoMqttClient();
                client = pahoMqttClient.getMqttClient(getApplicationContext(), Constants.MQTT_BROKER_URL, Constants.CLIENT_ID);
                Intent intent = new Intent(MainActivity.this, MqttMessageService.class);
                startService(intent);
                break;

            case R.id.subscribe:
                String topic = subscribeTopic.getText().toString().trim();
                if (!topic.isEmpty()) {
                    try {
                        pahoMqttClient.subscribe(client, topic, 1);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                break;

            case R.id.unSubscribe:
                String topic1 = unSubscribeTopic.getText().toString().trim();
                if (!topic1.isEmpty()) {
                    try {
                        pahoMqttClient.unSubscribe(client, topic1);
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
                break;
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
        //    byte[] buffer = new byte[256];
          //  int bytes;
            byte[] buffer = new byte[1024];
            int begin =0;
            int bytes = 0;

            while (true){
                try{
                    bytes+=mmInStream.read(buffer,bytes,buffer.length-bytes);
                    for (int i = begin; i <bytes; i++){
                        if (buffer[i] == '\n'){
                            bluetoothIn.obtainMessage(handlerState, begin, i, buffer).sendToTarget();
                            begin = i +1;
                            if (i == bytes-1){
                                bytes = 0;
                                begin = 0;

                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Keep looping to listen for received messages
           /* while (true) {
                try {
                    bytes = mmInStream.read(buffer);        	//read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }*/
        }
    }
}
