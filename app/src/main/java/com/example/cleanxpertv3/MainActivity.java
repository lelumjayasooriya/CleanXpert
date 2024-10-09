package com.example.cleanxpertv3;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "CleanXpertApp";
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    private InputStream inStream = null;

    private final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final String address = "00:22:12:01:4A:0E";  // HC-05 MAC address

    private TextView statusText, serialMonitor;
    private Handler handler;

    private static MainActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instance = this;

        Button startButton = findViewById(R.id.btn_start);
        Button stopButton = findViewById(R.id.btn_stop);
        Button scheduleButton = findViewById(R.id.btn_schedule);
        statusText = findViewById(R.id.status_text);
        serialMonitor = findViewById(R.id.serial_monitor);

        handler = new Handler();

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        checkBluetoothState();

        // Attempt to connect to Bluetooth device
        connectToDevice();

        // Start cleaning
        startButton.setOnClickListener(v -> sendCommand("S"));

        // Stop cleaning
        stopButton.setOnClickListener(v -> sendCommand("X"));

        // Schedule cleaning
        scheduleButton.setOnClickListener(v -> scheduleCleaning());

        // Start listening for incoming serial data
        new Thread(this::listenForData).start();
    }

    // Get the instance of MainActivity
    public static MainActivity getInstance() {
        return instance;
    }

    // Connect to the HC-05 Bluetooth device
    private void connectToDevice() {
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = device.createRfcommSocketToServiceRecord(uuid);
            btSocket.connect();
            statusText.setText("CleanXpert Connected");
            outStream = btSocket.getOutputStream();
            inStream = btSocket.getInputStream();

        } catch (IOException e) {
            Log.e(TAG, "Connection Failed", e);
            statusText.setText("CleanXpert Disconnected");
        }
    }

    // Method to listen for incoming serial data
    private void listenForData() {
        byte[] buffer = new byte[1024];
        int bytes;

        while (true) {
            try {
                bytes = inStream.read(buffer);
                String receivedData = new String(buffer, 0, bytes);
                handler.post(() -> serialMonitor.append(receivedData + "\n"));
            } catch (IOException e) {
                Log.e(TAG, "Data reception error", e);
                break;
            }
        }
    }

    // Send a command via Bluetooth
    public void sendCommand(String command) {
        try {
            if (outStream != null) {
                outStream.write(command.getBytes());
            }
        } catch (IOException e) {
            Log.e(TAG, "Command sending failed", e);
        }
    }

    // Method to check Bluetooth state
    private void checkBluetoothState() {
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            finish();
        } else if (!btAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
    }

    // Schedule cleaning using a TimePicker and AlarmManager
    private void scheduleCleaning() {
        Calendar currentTime = Calendar.getInstance();
        int hour = currentTime.get(Calendar.HOUR_OF_DAY);
        int minute = currentTime.get(Calendar.MINUTE);

        TimePickerDialog timePicker;
        timePicker = new TimePickerDialog(MainActivity.this, (TimePicker view, int selectedHour, int selectedMinute) -> {
            // Set calendar object to selected time
            Calendar scheduleTime = Calendar.getInstance();
            scheduleTime.set(Calendar.HOUR_OF_DAY, selectedHour);
            scheduleTime.set(Calendar.MINUTE, selectedMinute);
            scheduleTime.set(Calendar.SECOND, 0);

            // Schedule the task with AlarmManager
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(MainActivity.this, CleaningBroadcastReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);

            alarmManager.setExact(AlarmManager.RTC_WAKEUP, scheduleTime.getTimeInMillis(), pendingIntent);

            Toast.makeText(MainActivity.this, "Cleaning scheduled at " + selectedHour + ":" + selectedMinute, Toast.LENGTH_SHORT).show();
        }, hour, minute, true);  // Yes 24 hour time
        timePicker.setTitle("Select Time for Cleaning");
        timePicker.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (btSocket != null) btSocket.close();
        } catch (IOException e) {
            Log.e(TAG, "Error closing socket", e);
        }
    }
}
