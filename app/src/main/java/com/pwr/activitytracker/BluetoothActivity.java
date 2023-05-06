package com.pwr.activitytracker;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
import android.widget.Toast;

import android.Manifest;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.module.Settings;

import java.util.Objects;
import java.util.Set;

import bolts.Task;

public class BluetoothActivity extends AppCompatActivity implements ServiceConnection
{

    private BtleService.LocalBinder serviceBinder;
    private String deviceName = "MetaWear";
    private MetaWearBoard sensorBoard;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, BIND_AUTO_CREATE);
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

    private void connectToSensor()
    {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!arePermissionsGranted())
        {
            requestBluetoothPermission();
            return;
        }

        try
        {
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

            for (BluetoothDevice device : pairedDevices)
            {
                // IMPORTANT TODO: This works, but will it work for all of them or just my specific one?
                if (Objects.equals(device.getName(), deviceName))
                {
                    connectToDevice(device);
                    break;
                }
            }
        }
        catch (SecurityException exception)
        {
            Log.e("BLUETOOTH", "Error granting permission: " + exception);
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
        serviceBinder.removeMetaWearBoard(device);
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
                    Toast.makeText(getApplicationContext(), R.string.connectedInfo,
                            Toast.LENGTH_SHORT).show();
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
        connectToSensor();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName)
    {
        Toast.makeText(getApplicationContext(), R.string.disconnectedInfo, Toast.LENGTH_SHORT).show();
    }
}