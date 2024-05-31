
package com.example.drawerbottomnavigative.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.drawerbottomnavigative.MqttHandler;
import com.example.drawerbottomnavigative.R;
import android.os.Handler;

import org.apache.commons.lang3.math.NumberUtils;

public class FavoriteFragment extends Fragment {
    public double ph;
    private double temperature;
    private double turbidity;
    private String status;
    private static final String CLIENT_ID = "vovilak10202002";
    private static final String OXYGEN_PUMP_TOPIC = "control-oxygen-pump";
    private static final String WATER_PUMP_TOPIC = "control-water-pump";
    private static final String RESTORE_TOPIC = "control-restore";
    private Switch switchOxy;
    private Switch switchWater;
    private MqttViewModel viewModel;
    MqttHandler mqttHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_favorite, container, false);
        switchOxy = view.findViewById(R.id.switchoxy);
        switchWater = view.findViewById(R.id.switchwater);
        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(requireActivity()).get(MqttViewModel.class);
        setupMqttConnectionAndSubscribe();


        // Set up listeners for user-initiated changes to the switches
        setupSwitchListeners();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Lấy thông tin từ ViewModel
        viewModel.getOxygenSwitchState().observe(getViewLifecycleOwner(), isChecked -> switchOxy.setChecked(isChecked));
        viewModel.getWaterSwitchState().observe(getViewLifecycleOwner(), isChecked -> switchWater.setChecked(isChecked));

        // Khởi tạo hoặc lấy lại kết nối MQTT từ ViewModel
         mqttHandler = viewModel.getMqttHandler();


        if (mqttHandler == null) {
            mqttHandler = new MqttHandler();
            mqttHandler.connectToHiveMQ(CLIENT_ID);
            viewModel.setMqttHandler(mqttHandler);
            publishMessage(RESTORE_TOPIC, "ENABLE");
        }
    }

    @Override
    public void onDestroyView() {
        // Hủy đăng ký lắng nghe khi Fragment bị hủy
        viewModel.getOxygenSwitchState().removeObservers(getViewLifecycleOwner());
        viewModel.getWaterSwitchState().removeObservers(getViewLifecycleOwner());

        super.onDestroyView();
    }

    private void setupSwitchListeners() {
        // Set up listener for the Oxygen Pump switch
        switchOxy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle the state of the switch
                // Publish the first message based on the new state
                publishMessage(OXYGEN_PUMP_TOPIC, switchOxy.isChecked() ? "ON" : "OFF");

                // Delay for 1 second and then publish the second message
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        publishMessage(OXYGEN_PUMP_TOPIC, switchOxy.isChecked() ? "ON" : "OFF");

                        // Delay for 1 second and then publish the third message
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                publishMessage(OXYGEN_PUMP_TOPIC, switchOxy.isChecked() ? "ON" : "OFF");
                            }
                        }, 1000); // 1000 milliseconds = 1 second
                    }
                }, 1000); // 1000 milliseconds = 1 second
            }
        });

        // Set up listener for the Water Pump switch
        switchWater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Toggle the state of the switch
                // Publish the first message based on the new state
                publishMessage(WATER_PUMP_TOPIC, switchWater.isChecked() ? "ON" : "OFF");

                // Delay for 1 second and then publish the second message
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        publishMessage(WATER_PUMP_TOPIC, switchWater.isChecked() ? "ON" : "OFF");

                        // Delay for 1 second and then publish the third message
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                publishMessage(WATER_PUMP_TOPIC, switchWater.isChecked() ? "ON" : "OFF");
                            }
                        }, 1000); // 1000 milliseconds = 1 second
                    }
                }, 1000); // 1000 milliseconds = 1 second
            }
        });
    }



    private void publishMessage(String topic, String message) {
        Toast.makeText(requireContext(), "Publishing message: " + message, Toast.LENGTH_SHORT).show();
        viewModel.getMqttHandler().publish(topic, message);
    }

    private void setupMqttConnectionAndSubscribe() {
    mqttHandler = viewModel.getMqttHandler();

    if (mqttHandler == null) {
        mqttHandler = new MqttHandler();
        viewModel.setMqttHandler(mqttHandler);
        mqttHandler.connectToHiveMQ(CLIENT_ID);
    }

    subscribeToTopic("collection-station");
    subscribeToTopic(OXYGEN_PUMP_TOPIC);
    subscribeToTopic(WATER_PUMP_TOPIC);
}

    private void subscribeToTopic(String topic) {
        mqttHandler.subscribe(topic, new MqttHandler.MessageCallback() {
            @Override
            public void onMessageReceived(String receivedTopic, String message) {
                processMqttMessage(message);

                if (OXYGEN_PUMP_TOPIC.equals(receivedTopic)) {
                    boolean isChecked = "ON".equals(message);
                    viewModel.getOxygenSwitchState().postValue(isChecked);
                    Log.e("oxy", isChecked + "");
                } else if (WATER_PUMP_TOPIC.equals(receivedTopic)) {
                    boolean isChecked = "ON".equals(message);
                    viewModel.getWaterSwitchState().postValue(isChecked);
                    Log.e("oxy", isChecked + "");
                }
            }

            @Override
            public void onConnectionLost(Throwable cause) {
                handleMqttConnectionLost();
            }
        });
    }


    private void processMqttMessage(String message) {
        String[] values = message.split(",");
        if (values.length >= 4) {
            ph = parseDouble(values[0]);
            temperature = parseDouble(values[1]);
            turbidity = parseDouble(values[2]);
            status = getStatus(values[3]);
          //  viewModel.updateValues(ph, temperature, turbidity, status);

            // Log the variables
            Log.d("TAG1", "pH: " + ph);
            Log.d("TAG2", "Temperature: " + temperature);
            Log.d("TAG3", "Turbidity: " + turbidity);
            Log.d("TAG4", "Status: " + status);

            updateUI();
        }
    }

    private double parseDouble(String value) {
        return NumberUtils.isCreatable(value) ? Double.parseDouble(value) : Double.NaN;
    }

    private String getStatus(String value) {
        return value.equals("Stable") ? "Stable" : value.equals("Unstable") ? "Unstable" : "N/A";
    }

    private void handleMqttConnectionLost() {
        Log.e("TAG", "Connection lost");

        // Reconnect to MQTT
        viewModel.setMqttHandler(mqttHandler);
        mqttHandler.connectToHiveMQ(CLIENT_ID);

        // Observe LiveData and update UI after reconnection

        // Update UI after reconnection
        updateUI();

        // Subscribe again after reconnecting
        setupMqttConnectionAndSubscribe();
    }
    private void updateUI() {
        requireActivity().runOnUiThread(() -> {
            viewModel.updateAllValues(ph, temperature, turbidity, status, "");

        });
    }
}