package com.example.drawerbottomnavigative.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.drawerbottomnavigative.R;
import com.example.drawerbottomnavigative.MqttHandler;
import org.apache.commons.lang3.math.NumberUtils;

public class HomeFragment extends Fragment {
    private MqttViewModel viewModel;
    public double ph;
    private double temperature;
    private double turbidity;
    private static final String OXYGEN_PUMP_TOPIC = "control-oxygen-pump";
    private static final String WATER_PUMP_TOPIC = "control-water-pump";
    private static final String RESTORE_TOPIC = "control-restore";
    private String status;
    private TextView phTextView;
    private TextView temperatureTextView;
    private TextView turbidityTextView;
    private TextView statusTextView;
    private MqttHandler mqttHandler;
    private static final String CLIENT_ID = "vovilak10202002+9999";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        initViews(view);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViewModelAndObservers();
        setupMqttConnectionAndSubscribe();
    }

    private void initViews(View view) {
        temperatureTextView = view.findViewById(R.id.temperature);
        turbidityTextView = view.findViewById(R.id.Turbidity);
        statusTextView = view.findViewById(R.id.status);
        phTextView = view.findViewById(R.id.PH);
    }

    private void initializeViewModelAndObservers() {
        viewModel = new ViewModelProvider(requireActivity()).get(MqttViewModel.class);

        viewModel.getPhValue().observe(getViewLifecycleOwner(), ph -> phTextView.setText(String.valueOf(ph)));
        viewModel.getTemperatureValue().observe(getViewLifecycleOwner(), temperature -> temperatureTextView.setText(temperature + " °C"));
        viewModel.getTurbidityValue().observe(getViewLifecycleOwner(), turbidity -> turbidityTextView.setText(turbidity + " NTU"));
        viewModel.getStatusValue().observe(getViewLifecycleOwner(), status -> statusTextView.setText(status));
    }

    private void setupMqttConnectionAndSubscribe() {
    mqttHandler = viewModel.getMqttHandler();

    if (mqttHandler == null) {
        mqttHandler = new MqttHandler();
        viewModel.setMqttHandler(mqttHandler);
        mqttHandler.connectToHiveMQ(CLIENT_ID);
        publishMessage(RESTORE_TOPIC, "ENABLE");

    }

    subscribeToTopic("collection-station");
    subscribeToTopic(OXYGEN_PUMP_TOPIC);
    subscribeToTopic(WATER_PUMP_TOPIC);
}

  private void publishMessage(String topic, String message) {
        Toast.makeText(requireContext(), "Publishing message: " + message, Toast.LENGTH_SHORT).show();
        viewModel.getMqttHandler().publish(topic, message);
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
        initializeViewModelAndObservers();
        // Update UI after reconnection
        updateUI();
        // Subscribe again after reconnecting
        setupMqttConnectionAndSubscribe();
    }

    private void updateUI() {
        requireActivity().runOnUiThread(() -> {
            viewModel.updateAllValues(ph, temperature, turbidity, status, "");
            phTextView.setText(String.valueOf(ph));
            temperatureTextView.setText(temperature + " °C");
            turbidityTextView.setText(turbidity + " NTU");
            statusTextView.setText(status);
        });
    }
}
