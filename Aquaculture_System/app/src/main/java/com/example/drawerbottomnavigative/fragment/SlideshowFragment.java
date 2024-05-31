package com.example.drawerbottomnavigative.fragment;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.drawerbottomnavigative.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.example.drawerbottomnavigative.MqttHandler;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Date;
public class SlideshowFragment extends Fragment {
    private static final String OXYGEN_PUMP_TOPIC = "control-oxygen-pump";
    private static final String WATER_PUMP_TOPIC = "control-water-pump";
    private static final String RESTORE_TOPIC = "control-restore";
    private LineChart lineChart1;
    private LineChart lineChart2;
    private LineChart lineChart3;
    private MqttHandler mqttHandler;
    private static final String CLIENT_ID = "vovilak10202002";
    private double ph = Double.NaN;
    private double temperature = Double.NaN;
    private double turbidity = Double.NaN;
    private MqttViewModel viewModel ;
    private String status;

    private float[] temperatureValues = new float[100];  // Adjust the size based on your needs
    private float[] pondusHydrogeniiValues = new float[100];
    private float[] turbidityValues = new float[100];
    private String[] timestamps = new String[100];  // Adjust the size based on your needs
    private int index = 0;
     private List<Double> latestPhValues = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sildeshow, container, false);

        // Initialize LineCharts

        lineChart1 = view.findViewById(R.id.chartViewTemp1);
        lineChart2 = view.findViewById(R.id.chartViewTemp2);
        lineChart3 = view.findViewById(R.id.chartViewTemp3);
        // Customize the appearance of the LineCharts

        customizeLineChart(lineChart1, "Time", "Value");
        customizeLineChart(lineChart2, "Time", "Value");
        customizeLineChart(lineChart3, "Time", "Value");

        viewModel = new ViewModelProvider(requireActivity()).get(MqttViewModel.class);

        // Hàm kiểm tra xem tất cả các giá trị trong mảng có đều bằng 0 hay không


        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupMqttConnectionAndSubscribe();


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

    private void updateCharts() {
        // Update the LineDataSets with the saved entries
        LineDataSet dataSet1 = new LineDataSet(getEntries(temperatureValues, index), "Temperature");
        LineDataSet dataSet2 = new LineDataSet(getEntries(pondusHydrogeniiValues, index), "Pondus Hydrogenii");
        LineDataSet dataSet3 = new LineDataSet(getEntries(turbidityValues, index), "Turbidity of Water");

        // Create LineData objects
        LineData lineData1 = new LineData(dataSet1);
        LineData lineData2 = new LineData(dataSet2);
        LineData lineData3 = new LineData(dataSet3);

        // Set data for each chart
        lineChart1.setData(lineData1);
        lineChart2.setData(lineData2);
        lineChart3.setData(lineData3);

        // Notify each chart to update its data
        lineChart1.invalidate();
        lineChart2.invalidate();
        lineChart3.invalidate();
    }
    private void processMqttMessage(String message) {
        String[] values = message.split(",");
        if (values.length >= 4) {
            ph = parseDouble(values[0]);
            temperature = parseDouble(values[1]);
            turbidity = parseDouble(values[2]);
            status = getStatus(values[3]);
          //  viewModel.updateValues(ph, temperature, turbidity, status);
            long currentTime = System.currentTimeMillis();

                    // Log timestamp
           logTimestamp(currentTime);

           SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
           String formattedDate = sdf.format(new Date(currentTime));
            // Log the variables
            Log.d("TAG1", "pH: " + ph);
            Log.d("TAG2", "Temperature: " + temperature);
            Log.d("TAG3", "Turbidity: " + turbidity);
            Log.d("TAG4", "Status: " + status);
            Log.d("TAG_TIME", "Timestamp: " + formattedDate);
            Log.d("TAG1  " + formattedDate, "pH: " + ph);
            Log.d("TAG2", "Temperature: " + temperature);
            Log.d("TAG3", "Turbidity: " + turbidity);
            addChartData(currentTime, temperature, ph, turbidity);
            updateCharts();
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

    private void customizeLineChart(LineChart chart, String xAxisLabel, String yAxisLabel) {
        // Hide right YAxis
        chart.getAxisRight().setEnabled(false);

        // Customize XAxis (x-axis labels)
        XAxis xAxis = chart.getXAxis();
        xAxis.setTextColor(ColorTemplate.getHoloBlue());
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(timestamps)); // Use timestamps as X-axis labels
        xAxis.setTextSize(13);
        chart.setVisibleXRangeMaximum(5); // Show only 5 entries at a time

        // Customize YAxis (y-axis labels)
        YAxis leftYAxis = chart.getAxisLeft();
        leftYAxis.setTextColor(ColorTemplate.getHoloBlue());
        leftYAxis.setTextSize(13);

        // Customize Legend (dataset labels)
        Legend legend = chart.getLegend();
        legend.setTextColor(ColorTemplate.getHoloBlue());
    }



    private void logTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String formattedDate = sdf.format(new Date(timestamp));
        Log.d("TAG_TIME", "Timestamp: " + formattedDate);
    }


    private void addChartData(long timestamp, double temperature, double ph, double turbidity) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
        String formattedDate = sdf.format(new Date(timestamp));

        if (index < temperatureValues.length && index < pondusHydrogeniiValues.length
                && index < turbidityValues.length && index < timestamps.length) {
            temperatureValues[index] = (float) temperature;
            pondusHydrogeniiValues[index] = (float) ph;
            turbidityValues[index] = (float) turbidity;
            timestamps[index] = formattedDate;

            // Update chart data with the new LineDataSets

            // Reset index to 0 when it reaches the array length
            index = (index + 1) % temperatureValues.length;
            updateCharts();

        } else {
            Log.e("TAG", "Index out of bounds: " + index);
        }
    }




     private List<Entry> getEntries(float[] values, int index) {
        List<Entry> entries = new ArrayList<>();
        int startIndex = Math.max(0, index - 5);
        for (int i = startIndex; i < index; i++) {
            entries.add(new Entry(i - startIndex, values[i]));
        }
        return entries;
    }

}