package com.pwr.datagathering;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.DefaultLifecycleObserver;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.module.Settings;
import com.pwr.datagathering.sensors.DeviceController;
import com.pwr.datagathering.sensors.SensorData;

import bolts.Task;

public class MainActivity extends AppCompatActivity implements ServiceConnection,
        DefaultLifecycleObserver
{
    private final String PREFERENCES_KEY = "user-prefs-key";
    private BtleService.LocalBinder serviceBinder;
    private MetaWearBoard sensorBoard;

    private boolean trainingStarted = false;
    private boolean deviceConnected = false;

    private int randomRange = 2000;
    private long soundInterval = 5000;
    private String filename = "training_data";

    private static MediaPlayer player;
    private static Random generator;
    private static Handler threadHandler;

    private static DeviceController deviceController;
    private SharedPreferences settings;
    private String deviceName = "MetaWear";

    private Set<BluetoothDevice> pairedDevices = null;

    private BluetoothDevice selectedDevice;

    public static Task<Void> reconnect(final MetaWearBoard board)
    {
        return board.connectAsync()
                .continueWithTask(task -> {
                    if (task.isFaulted())
                    {
                        return reconnect(board);
                    } else if (task.isCancelled())
                    {
                        return task;
                    }
                    return Task.forResult(null);
                });
    }

    void setConnInterval(Settings settings)
    {
        if (settings != null)
        {
            Settings.BleConnectionParametersEditor editor = settings.editBleConnParams();
            if (editor != null)
            {
                editor.maxConnectionInterval(11.25f).commit();
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        generator = new Random();
        threadHandler = new Handler(Looper.getMainLooper());
        deviceController = new DeviceController();

        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, BIND_AUTO_CREATE);
        loadUserPrefs();

        ListView listView = findViewById(R.id.listview);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!arePermissionsGranted()) {
            requestBluetoothPermission();
            return;
        }

        try {
            pairedDevices = bluetoothAdapter.getBondedDevices();
        } catch (SecurityException exception) {
            Log.e("BLUETOOTH", "Error granting permission: " + exception.toString());
            Toast.makeText(getApplicationContext(), R.string.permissionError, Toast.LENGTH_SHORT).show();
        }
        List<String> devicesNames = new ArrayList<>();
        if(pairedDevices !=null){
            devicesNames = pairedDevices.stream().map(BluetoothDevice::getName).collect(Collectors.toList());
        }

        ArrayAdapter arrayAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, devicesNames);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedDevice = nthElement(pairedDevices, position);
                connectToSensor();
            }
        });


    }

    private void loadUserPrefs()
    {
        settings = getApplicationContext().getSharedPreferences(
                PREFERENCES_KEY, 0);

        EditText randomTextView = findViewById(R.id.RandomnessText);
        randomRange = settings.getInt("random", randomRange);
        randomTextView.setText(String.valueOf(randomRange));

        EditText intervalTextView = findViewById(R.id.IntervalText);
        soundInterval = settings.getInt("interval", (int) soundInterval);
        intervalTextView.setText(String.valueOf(soundInterval));

        EditText csvTextView = findViewById(R.id.CsvText);
        filename = settings.getString("csv", filename);
        csvTextView.setText(filename);

        EditText deviceTextView = findViewById(R.id.DeviceNameText);
        deviceName = settings.getString("sensor", deviceName);
        deviceTextView.setText(deviceName);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        getApplicationContext().unbindService(this);
    }

    private boolean arePermissionsGranted()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        else
        {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void connectToSensor()
    {
        //TODO: Adam Kizar sprawdź plis czy to jest git, bo  ie do konca ogarniam ta metode @mkalina
        if(selectedDevice!=null) {
            connectToDevice(selectedDevice);
        }
        else {
            Log.e("BLUETOOTH", "Error, device is null");
            Toast.makeText(getApplicationContext(), R.string.permissionError, Toast.LENGTH_SHORT).show();
        }
    }

    public void applySettings(View view)
    {
        SharedPreferences.Editor editor = settings.edit();
        EditText randomTextView = findViewById(R.id.RandomnessText);
        try
        {
            randomRange = Integer.parseInt(String.valueOf(randomTextView.getText()));
            editor.putInt("random", randomRange);
        } catch (NumberFormatException exception)
        {
            randomTextView.setText(String.valueOf(randomRange));
        }

        EditText intervalTextView = findViewById(R.id.IntervalText);
        try
        {
            soundInterval = Integer.parseInt(String.valueOf(intervalTextView.getText()));
            editor.putInt("interval", (int) soundInterval);
        } catch (NumberFormatException exception)
        {
            intervalTextView.setText(String.valueOf(soundInterval));
        }

        EditText csvTextView = findViewById(R.id.CsvText);
        filename = String.valueOf(csvTextView.getText());
        editor.putString("csv", filename);

        EditText deviceTextView = findViewById(R.id.DeviceNameText);
        deviceName = String.valueOf(deviceTextView.getText());
        editor.putString("sensor", deviceName);

        editor.apply();
    }

    private void connectToDevice(BluetoothDevice device)
    {
        serviceBinder.removeMetaWearBoard(device);
        sensorBoard = serviceBinder.getMetaWearBoard(device);

        sensorBoard.connectAsync().continueWithTask(task -> {
            if (task.isCancelled()) return task;
            return task.isFaulted() ? reconnect(sensorBoard) : Task.forResult(null);
        }).continueWith(task -> {
            if (!task.isCancelled())
            {
                setConnInterval(sensorBoard.getModule(Settings.class));
                try
                {
                    deviceController.setSensors(sensorBoard);
                    deviceConnected = true;
                }
                catch (UnsupportedModuleException exception)
                {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), R.string.connectedFailure,
                            Toast.LENGTH_SHORT).show());
                }
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), R.string.connectedInfo,
                                    Toast.LENGTH_SHORT).show();
                    TextView text = findViewById(R.id.deviceInfoText);
                    text.setText(R.string.deviceStatusConnected);
                    ImageView image = findViewById(R.id.deviceInfoImage);
                    image.setImageResource(R.drawable.baseline_bluetooth_connected_24);
                });
            }
            return null;
        });
    }

    private void requestBluetoothPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            this.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
        else
        {
            this.requestPermissions(new String[]{Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                          @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                connectToSensor();
            }
            else
            {
                Toast.makeText(this,  R.string.btNoPermission, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onClick(View view)
    {
        if (!deviceConnected)
        {
            Toast.makeText(getApplicationContext(), R.string.connectDevice, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        if (trainingStarted) endTraining();
        else startTraining();
    }

    private void endTraining()
    {
        Button button = findViewById(R.id.actionButton);
        button.setText(R.string.startButton);
        trainingStarted = false;
        player.stop();
        writeToFile(deviceController.stopMeasurements());
    }

    private void playSound()
    {
        if (!trainingStarted) return;

        player.start();
        long playAfter = soundInterval + generator.nextInt(randomRange);
        deviceController.setMarker();
        threadHandler.postDelayed(this::playSound, playAfter);
    }

    private void startTraining()
    {
        Button button = findViewById(R.id.actionButton);
        button.setText(R.string.stopButton);
        trainingStarted = true;

        player = MediaPlayer.create(this, R.raw.miau);
        long playAfter = soundInterval + generator.nextInt(randomRange);
        threadHandler.postDelayed(this::playSound, playAfter);
        deviceController.startMeasurements();
    }

    // For testing purposes not needed in final app
    private void writeToFile(ArrayList<SensorData> data)
    {
        String headerEuler = "FLAG;TIME;PITCH;ROLL;YAW";

        File documents = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS);

        File file = new File(documents, filename + ".csv");

        int counter = 1;
        while (file.isFile() || file.isDirectory() || file.exists())
        {
            file = new File(documents, filename + "_" + counter + ".csv");
            counter++;
        }

        FileOutputStream stream;

        try
        {
            if (!file.createNewFile())
            {
                Toast.makeText(this,  R.string.saveFailure + file.getPath(),
                        Toast.LENGTH_SHORT).show();
                return;
            }

            stream = new FileOutputStream(file, true);
            OutputStreamWriter writer = new OutputStreamWriter(stream);

            writer.append(headerEuler);
            writer.append("\n");

            for (SensorData sensor: data)
            {
                writer.append(sensor.toString());
                writer.append("\n");
            }

            writer.close();
            stream.flush();
            stream.close();
            Toast.makeText(this,  R.string.dataSaved + file.getPath(),
                    Toast.LENGTH_SHORT).show();
        }
        catch (Exception exception)
        {
            Toast.makeText(getApplicationContext(), R.string.writeFailure, Toast.LENGTH_SHORT)
                    .show();
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service)
    {
        serviceBinder = (BtleService.LocalBinder) service;
        //connectToSensor(); TODO: Panie Adamie, mogę to usunąć? @mkalina
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName)
    {
        Toast.makeText(getApplicationContext(), R.string.disconnectedInfo, Toast.LENGTH_SHORT)
                .show();
    }

    /**
     * Return an element selected by position in iteration order.
     * @param data The source from which an element is to be selected
     * @param n The index of the required element. If it is not in the
     * range of elements of the iterable, the method returns null.
     * @return The selected element.
     */
    public static final <T> T nthElement(Iterable<T> data, int n){
        int index = 0;
        for(T element : data){
            if(index == n){
                return element;
            }
            index++;
        }
        return null;
    }
}