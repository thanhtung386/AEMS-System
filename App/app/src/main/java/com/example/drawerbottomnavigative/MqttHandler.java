package com.example.drawerbottomnavigative;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttHandler {

    private MqttClient client;
    private MessageCallback messageCallback;
    private static MqttHandler instance;

    public void connectToHiveMQ(String clientId) {
        try {
            // Set up the persistence layer
            MemoryPersistence persistence = new MemoryPersistence();

            // Initialize the MQTT client with the HiveMQ broker URL
            String brokerUrl = "tcp://broker.emqx.io:1883"; // Update this with the correct HiveMQ broker URL
            client = new MqttClient(brokerUrl, clientId, persistence);

            // Set up the connection options
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);

            // Connect to the HiveMQ broker
            client.connect(connectOptions);

            // Set up the callback for message handling
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    // Handle connection lost event
                    if (messageCallback != null) {
                        messageCallback.onConnectionLost(cause);
                    }
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    // Call the callback when a message is received
                    if (messageCallback != null) {
                        messageCallback.onMessageReceived(topic, new String(message.getPayload()));
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Handle message delivery complete event
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic, String message) {
        try {
            if (client != null && client.isConnected()) {
                MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                client.publish(topic, mqttMessage);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void subscribe(String topic, MessageCallback callback) {
        try {
            if (client != null && client.isConnected()) {
                client.subscribe(topic);
                this.messageCallback = callback;
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public interface MessageCallback {
        void onMessageReceived(String topic, String message);

        void onConnectionLost(Throwable cause);
    }
    public static synchronized MqttHandler getInstance() {
        if (instance == null) {
            instance = new MqttHandler();
        }
        return instance;
    }
}
