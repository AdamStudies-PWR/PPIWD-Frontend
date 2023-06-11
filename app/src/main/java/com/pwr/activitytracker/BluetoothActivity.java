package com.pwr.activitytracker;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.module.Settings;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import bolts.Task;

public class BluetoothActivity extends AppCompatActivity implements ServiceConnection
{
    private final String TAG = "BluetoothActivity";
    private final String PREFERENCES_KEY = "user-prefs-key";

    private BtleService.LocalBinder serviceBinder;
    private MetaWearBoard sensorBoard;

    private Set<BluetoothDevice> pairedDevices = null;
    private BluetoothDevice selectedDevice;
    private String deviceName = "";

    private static int retryCount = 0;

    private static boolean clickable = true;

    private ProgressBar progress;

    private ListView listView;

    public static <T> T nthElement(Iterable<T> data, int n)
    {
        if (!clickable) return null;

        int index = 0;
        for (T element : data)
        {
            if (index == n)
            {
                return element;
            }
            index++;
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bluetooth);

        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, BIND_AUTO_CREATE);

        progress = findViewById(R.id.progressBar);
        listView = findViewById(R.id.listview);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            selectedDevice = nthElement(pairedDevices, position);
            if (selectedDevice != null)
            {
                saveDeviceName(selectedDevice.getName());
                connectToSensor();
            }
            else if (!clickable)
            {
                Toast.makeText(getApplicationContext(), R.string.deviceUnreachable,
                        Toast.LENGTH_SHORT).show();
            }
        });

        deviceName = loadPreviousDevice();

        if (!arePermissionsGranted())
        {
            requestBluetoothPermission();
        }
    }

    private void saveDeviceName(String deviceName)
    {
        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFERENCES_KEY, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("DEVICE_ID", deviceName);
        editor.apply();
    }

    private void startConnectionProcedure(String savedName)
    {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        try
        {
            pairedDevices = bluetoothAdapter.getBondedDevices();
        } catch (SecurityException exception)
        {
            Log.e(TAG, "Error granting permission: " + exception.toString());
            Toast.makeText(getApplicationContext(), R.string.permissionError, Toast.LENGTH_SHORT).show();
            progress.setVisibility(View.GONE);
            return;
        }

        List<String> devicesNames = new ArrayList<>();
        if (pairedDevices != null)
        {
            devicesNames = pairedDevices.stream().map(BluetoothDevice::getName).collect(Collectors.toList());
        }

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getApplicationContext(),
                android.R.layout.simple_list_item_1, devicesNames);
        listView.setAdapter(arrayAdapter);

        if (devicesNames.contains(savedName))
        {
            int deviceId = devicesNames.indexOf(savedName);
            selectedDevice = nthElement(pairedDevices, deviceId);
            connectToSensor();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        getApplicationContext().unbindService(this);
    }

    public Task<Void> reconnect(final MetaWearBoard board)
    {
        if (retryCount < 5)
        {
            retryCount++;

            return board.connectAsync().continueWithTask(task -> {
                if (task.isFaulted())
                {
                    return reconnect(board);
                }
                else if (task.isCancelled())
                {
                    return task;
                }
                return Task.forResult(null);
            });
        }
        else
        {
            return board.disconnectAsync().continueWith(task ->  {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), R.string.deviceUnreachable,
                                    Toast.LENGTH_SHORT).show();
                });
                Log.e(TAG, "Connection aborted");
                return null;
            });
        }
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

    private void connectToSensor()
    {
        if (selectedDevice != null)
        {
            progress.setVisibility(View.VISIBLE);
            clickable = false;
            connectToDevice(selectedDevice);
        }
        else
        {
            Log.e(TAG, "Error, device is null");
            progress.setVisibility(View.GONE);
            clickable = true;
        }
    }

    private boolean arePermissionsGranted()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        } else
        {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }

    private String loadPreviousDevice()
    {
        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFERENCES_KEY, 0);
        return settings.getString("DEVICE_ID", "");
    }

    private void connectToDevice(BluetoothDevice device)
    {
        serviceBinder.removeMetaWearBoard(device); //tutaj jest blad // Jaki błąd?
        sensorBoard = serviceBinder.getMetaWearBoard(device);

        sensorBoard.connectAsync().continueWithTask(task -> {
            if (task.isCancelled())
            {
                progress.setVisibility(View.GONE);
                return task;
            }

            if (task.isFaulted())
            {
                return reconnect(sensorBoard);
            }
            else
            {
                return Task.forResult(null);
            }
        }).continueWith(task -> {
            if (retryCount == 5)
            {
                runOnUiThread(() -> {
                    retryCount = 0;
                    progress.setVisibility(View.GONE);
                    clickable = true;
                });
                return null;
            }

            if (!task.isCancelled())
            {
                setConnInterval(sensorBoard.getModule(Settings.class));
                Intent incomingIntent = getIntent();
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("username", incomingIntent.getStringExtra("username"));
                intent.putExtra("sensor", device);
                this.startActivity(intent);

                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    clickable = true;
                    Toast.makeText(getApplicationContext(), R.string.connectedInfo, Toast.LENGTH_SHORT).show();
                });

                finish();
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
    public void onServiceConnected(ComponentName componentName, IBinder service)
    {
        serviceBinder = (BtleService.LocalBinder) service;

        if (arePermissionsGranted())
        {
            startConnectionProcedure(deviceName);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName)
    {
        Toast.makeText(getApplicationContext(), R.string.disconnectedInfo, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1 && (grantResults.length > 0))
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                startConnectionProcedure(deviceName);
            }
        }
    }

    public void refreshView(View view)
    {
        Log.i(TAG, "Refreshing");
        startConnectionProcedure("");
    }
}