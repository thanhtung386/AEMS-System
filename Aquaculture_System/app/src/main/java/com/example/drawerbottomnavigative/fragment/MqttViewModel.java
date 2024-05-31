package com.example.drawerbottomnavigative.fragment;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.drawerbottomnavigative.MqttHandler;

public class MqttViewModel extends ViewModel {
    private final MutableLiveData<Boolean> oxygenSwitchState = new MutableLiveData<>();
    private final MutableLiveData<Boolean> waterSwitchState = new MutableLiveData<>();
    private MqttHandler mqttHandler;
    private final MutableLiveData<Double> phValue = new MutableLiveData<>();
    private final MutableLiveData<Double> temperatureValue = new MutableLiveData<>();
    private final MutableLiveData<Double> turbidityValue = new MutableLiveData<>();
    private final MutableLiveData<String> statusValue = new MutableLiveData<>();
    private final MutableLiveData<String> timestampValue = new MutableLiveData<>();
    private final MutableLiveData<String> mqttMessage = new MutableLiveData<>();

    // New MutableLiveData for saved chart data
    private final MutableLiveData<float[]> savedTemperatureValues = new MutableLiveData<>(new float[100]);
    private final MutableLiveData<float[]> savedPondusHydrogeniiValues = new MutableLiveData<>(new float[100]);
    private final MutableLiveData<float[]> savedTurbidityValues = new MutableLiveData<>(new float[100]);
    private final MutableLiveData<String[]> savedTimestamps = new MutableLiveData<>(new String[100]);
    private int savedIndex = 0;

    public LiveData<String> getMqttMessage() {
        return mqttMessage;
    }

    public void updateValues(double ph, double temperature, double turbidity, String status) {
        phValue.setValue(ph);
        temperatureValue.setValue(temperature);
        turbidityValue.setValue(turbidity);
        statusValue.setValue(status);
    }

    public void setMqttMessage(String message) {
        mqttMessage.postValue(message);
    }

    public void updateSensorValues(double ph, double temperature, double turbidity, String timestamp) {
        phValue.setValue(ph);
        temperatureValue.setValue(temperature);
        turbidityValue.setValue(turbidity);
        timestampValue.setValue(timestamp);
    }

    public LiveData<Double> getPhValue() {
        return phValue;
    }

    public LiveData<Double> getTemperatureValue() {
        return temperatureValue;
    }

    public LiveData<Double> getTurbidityValue() {
        return turbidityValue;
    }

    public LiveData<String> getTimestampValue() {
        return timestampValue;
    }
    private final MutableLiveData<float[]> dataArray = new MutableLiveData<>();

    public LiveData<String> getStatusValue() {
        return statusValue;
    }

    public void updateStatusValue(double ph, double temperature, double turbidity, String status) {
        phValue.setValue(ph);
        temperatureValue.setValue(temperature);
        turbidityValue.setValue(turbidity);
        statusValue.setValue(status);
    }

    public void updateAllValues(double ph, double temperature, double turbidity, String status, String timestamp) {
        updateSensorValues(ph, temperature, turbidity, timestamp);
        updateStatusValue(ph, temperature, turbidity, status);
    }

    public MutableLiveData<Boolean> getOxygenSwitchState() {
        return oxygenSwitchState;
    }

    public MutableLiveData<Boolean> getWaterSwitchState() {
        return waterSwitchState;
    }

    public MqttHandler getMqttHandler() {
        return mqttHandler;
    }

    public void setMqttHandler(MqttHandler handler) {
        mqttHandler = handler;
    }

    // Methods for saved chart data
    public void saveChartData(float[] temperatureValues, float[] pondusHydrogeniiValues, float[] turbidityValues, String[] timestamps, int index) {
        savedTemperatureValues.setValue(temperatureValues);
        savedPondusHydrogeniiValues.setValue(pondusHydrogeniiValues);
        savedTurbidityValues.setValue(turbidityValues);
        savedTimestamps.setValue(timestamps);
        savedIndex = index;
    }

    public LiveData<float[]> getSavedTemperatureValues() {
        return savedTemperatureValues;
    }

    public LiveData<float[]> getSavedPondusHydrogeniiValues() {
        return savedPondusHydrogeniiValues;
    }

    public LiveData<float[]> getSavedTurbidityValues() {
        return savedTurbidityValues;
    }

    public LiveData<String[]> getSavedTimestamps() {
        return savedTimestamps;
    }

    public LiveData<Integer> getSavedIndex() {
        return new MutableLiveData<>(savedIndex);
    }

}
