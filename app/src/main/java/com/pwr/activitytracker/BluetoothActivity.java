package com.pwr.activitytracker;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

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
    private BtleService.LocalBinder serviceBinder;
    private MetaWearBoard sensorBoard;

    private Set<BluetoothDevice> pairedDevices = null;
    private BluetoothDevice selectedDevice;

    private ListView listView;

    public static <T> T nthElement(Iterable<T> data, int n)
    {
        int index = 0;
        for(T element : data)
        {
            if(index == n)
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

        if (!arePermissionsGranted())
        {
            requestBluetoothPermission();
        }

        setContentView(R.layout.activity_bluetooth);

        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, BIND_AUTO_CREATE);

        listView = findViewById(R.id.listview);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            selectedDevice = nthElement(pairedDevices, position);
            connectToSensor();
        });
    }

    private void startConnectionProcedure()
    {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        try
        {
            pairedDevices = bluetoothAdapter.getBondedDevices();
        } catch (SecurityException exception)
        {
            Log.e("BLUETOOTH", "Error granting permission: " + exception.toString());
            Toast.makeText(getApplicationContext(), R.string.permissionError, Toast.LENGTH_SHORT).show();
        }

        List<String> devicesNames = new ArrayList<>();
        if(pairedDevices !=null)
        {
            devicesNames = pairedDevices.stream().map(BluetoothDevice::getName).collect(Collectors.toList());
        }

        ArrayAdapter arrayAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, devicesNames);
        listView.setAdapter(arrayAdapter);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        getApplicationContext().unbindService(this);
    }

    public static Task<Void> reconnect(final MetaWearBoard board)
    {
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
        if(selectedDevice!=null)
        {
            connectToDevice(selectedDevice);
        }
        else
        {
            Log.e("BLUETOOTH", "Error, device is null");
            Toast.makeText(getApplicationContext(), R.string.permissionError, Toast.LENGTH_SHORT).show();
        }
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

    private void connectToDevice(BluetoothDevice device)
    {
        serviceBinder.removeMetaWearBoard(device); //tutaj jest blad // Jaki błąd?
        sensorBoard = serviceBinder.getMetaWearBoard(device);

        sensorBoard.connectAsync().continueWithTask(task -> {
            if (task.isCancelled()) return task;
            return task.isFaulted() ? reconnect(sensorBoard) : Task.forResult(null);
        }).continueWith(task -> {
            if (!task.isCancelled())
            {
                setConnInterval(sensorBoard.getModule(Settings.class));
                Intent incomingIntent = getIntent();
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("username", incomingIntent.getStringExtra("username"));
                intent.putExtra("sensor", device);
                this.startActivity(intent);

                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), R.string.connectedInfo, Toast.LENGTH_SHORT).show();
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
        startConnectionProcedure();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName)
    {
        Toast.makeText(getApplicationContext(), R.string.disconnectedInfo, Toast.LENGTH_SHORT).show();
    }

    private void proceedToActivity()
    {
        //TODO: Kwi
    }
}