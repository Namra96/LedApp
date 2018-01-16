package com.example.ansambassamabdulhamid.ledapp;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.MqttException;

import java.io.UnsupportedEncodingException;
import java.util.Random;

/**
 * Created by namragill on 2018-01-10.
 */

public class Connect {

    private static String topinIntiate = "INITIATE";
    private static String messageDisconnect = "DISCONNECT";

    private PahoMqttClient listener = null;
    private MqttAndroidClient client = null;
    private Random random = new Random();

    private String myTopic = null;
    private String arduinoTopic = "IoT/G15";

    private boolean connected = false;
    private boolean paired = false;


    public Connect(PahoMqttClient listener, MqttAndroidClient client) {
        this.listener = listener;
        this.client = client;
      /*  try {
            listener.subscribe(client,topinIntiate,1);
            this.myTopic = String.valueOf(random.nextLong());
        } catch (MqttException e) {
            e.printStackTrace();
        }*/
    }


    public void sendGesture(String gesture) {


            try {
                listener.publishMessage(client,gesture,1,arduinoTopic);

            } catch (MqttException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }





    }
}
