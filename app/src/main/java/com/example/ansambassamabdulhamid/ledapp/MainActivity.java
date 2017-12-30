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
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.UUID;

import weka.classifiers.trees.J48;
import weka.core.Instances;
import wekaizing.WekaClassifier;
import wekaizing.WekaData;


public class MainActivity extends AppCompatActivity {

    private MqttAndroidClient client;
    private String TAG = "MainActivity";
    private PahoMqttClient pahoMqttClient;
    static String  inputString = null;

    private EditText textMessage, subscribeTopic, unSubscribeTopic;
    private Button publishMessage, subscribe, unSubscribe, receive;
    TextView myLabel;
    WekaData mydata;
    WekaClassifier classifier;
    int counterg = 0;
    int insNum = 20; //length of sliding window
    String gesture = "none";




    TextView txtArduino, txtString, txtStringLength, sensorView0, sensorView1, sensorView2, sensorView3;
    Handler bluetoothIn;

    final int handlerState = 0;        				 //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private ArrayList<String> recDataString = new ArrayList<>();
    private String[] liveDataString = new String[120];

    private ConnectedThread mConnectedThread;

    int n = 0;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;

   // String liveData[] = new String[120];
    Object[] liveData = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20};
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

                    //"/Users/namragill/Desktop/TEST.rtf"

                    // load training data
                    BufferedReader breader = null;
                    //File yourFile = getFileStreamPath("Users/namragill/Desktop/smoothed_train_data.arff");

                        //breader = new BufferedReader(new FileReader(("/Users//namragill//Downloads//LedApp//app//src//main//res//smoothed_train_data.arff")));

                        try {

                            breader = new BufferedReader(
                                    new InputStreamReader(getAssets().open("smoothed_train_data.arff")));
                             train = new Instances (breader);
                            train.setClassIndex(train.numAttributes() -1);


                            if ((breader.readLine()) != null) {
                              //  Log.d("MSG", mLine);
                            }
                            inputString = (String) msg.obj;

                           // recDataString.add(inputString);


                           liveDataString[n] = inputString;
                            Log.d("MSG", liveDataString[n]);
                            n++;

                            /*
                            breader = new BufferedReader (new FileReader(fileDir.toString()));
                            Instances test = new Instances (breader);
                            test.setClassIndex(test.numAttributes() -1);
                            */




                         //   recDataString.append(inputString);
                            Log.d("RECIEVED", String.valueOf(inputString));//keep appending to string until ~


                        }catch (IOException e){

                        }finally{
                            if(breader != null){
                                try{

                                    File file = new File("myFile.txt");
                                    FileWriter writer = new FileWriter(file, true);
                                    PrintWriter output = new PrintWriter(writer);
                                    Log.d("File","declared");

                                    for(int i = 0; i < liveDataString.length; i++)
                                        output.println(liveDataString[i]);
                                    output.close();
                                    Log.d("File","CREATED");

                                    BufferedReader bf = new BufferedReader (new FileReader(file.getAbsolutePath()));
                                    Log.d("After","BufferReader");
                                    Instances testInstance = new Instances (bf);
                                    testInstance.setClassIndex(testInstance.numAttributes() -1);
                                    bf.close();
                                    Log.d("CLOSED","BufferReader");

                                    breader.close();
                                        Log.d("CLOSED","breader");
                                    J48 tree = new J48();         // new instance of tree
                                    tree.buildClassifier(train);

                                    int classIndex = train.numAttributes() -1;
                                    Instances labeled = new Instances(testInstance);
                                                 Log.d("Before","Forloop");
                                    for (int i = 0; i < testInstance.numInstances(); i++){
                                        double clsLabel = tree.classifyInstance(testInstance.instance(i)) ;
                                        labeled.instance(i).setClassValue(clsLabel);
                                        Log.d("GESTURE",labeled.instance(i).attribute(classIndex).value((int) clsLabel));
                                    }

                                }catch (IOException e){

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }





                 /*   if (inputString != null && inputString.length() > 0) {
                        String [] inputStringArr = split(inputString, ",");
                        if (inputStringArr.length >= 7 && inputStringArr[0].equals("h")) { //  we have 7 elements
                            int xAccel = Integer.parseInt(inputStringArr[1]);
                            int yAccel = Integer.parseInt(inputStringArr[2]);
                            int zAccel = Integer.parseInt(inputStringArr[3]);
                            int vRef = Integer.parseInt(inputStringArr[4]);
                            int xRate = Integer.parseInt(inputStringArr[5]);
                            int yRate = Integer.parseInt(inputStringArr[6]);
                                //Add Acc and gyr attributes to the object
                                liveData[6*(counterg)] = xAccel;
                                liveData[6*(counterg)+1] = yAccel;
                                liveData[6*(counterg)+2] = zAccel;
                                liveData[6*(counterg)+3] = vRef;
                                liveData[6*(counterg)+4] = xRate;
                                liveData[6*(counterg)+5] = yRate;
                                counterg++;
                                //Predict the class (the gestures "up", "right","left","down" ... are assigned to numbers 1,2,3,4 ...)
                                String []gesture = {"up", "down","left","right","tilt_left","tilt_right"};
                                if (counterg == insNum) {
                                    liveData[insNum] = 9;
                                    counterg = 0;
                                    int pred = classifier.Classify(liveData);
                                    Toast.makeText(getApplicationContext(), "Detected Gesture:"+gesture[pred], Toast.LENGTH_SHORT).show();
                                    Log.i("detected Gesture:", gesture[pred]);
                                }
                            }


                    } */
                    /*
                    mydata = new WekaData("/Users/namragill/Desktop/live_data.arff"); //Initialize a WekaData with empty attributes and dataset
                    // upload trainig data
                    classifier = new WekaClassifier(WekaClassifier.LOGISTIC);//Initialize a new classifier with KStar algorithm
                    classifier.Build(mydata);

                    try {
                        breader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    J48 tree = new J48();         // new instance of tree
                    try {
                        tree.buildClassifier(train);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    //now use the readmessage and label each gesture read*/

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

  /*  public void sendGesture(){
        switch (gesture){
            case "right":
                pahoMqttClient.publishMessage(client, msg, 1, Constants.PUBLISH_TOPIC);
                break;
            case "left":
                pahoMqttClient.publishMessage(client, msg, 1, Constants.PUBLISH_TOPIC);
                break;
            case "up":
                pahoMqttClient.publishMessage(client, msg, 1, Constants.PUBLISH_TOPIC);
                break;
            case "down":
                pahoMqttClient.publishMessage(client, msg, 1, Constants.PUBLISH_TOPIC);
                break;
        }
    }
    */


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
