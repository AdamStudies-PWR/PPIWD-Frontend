package com.pwr.datagathering;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.android.BtleService;
import com.mbientlab.metawear.module.Settings;

import bolts.Task;

public class MainActivity extends AppCompatActivity implements ServiceConnection
{


    private final String PREFERENCES_KEY = "appDataKey";

    private BtleService.LocalBinder serviceBinder;
    private MetaWearBoard sensorBoard;
    ArrayList<Boolean> sensorPrefs = new ArrayList<>();

    private boolean trainingStarted = false;
    private boolean deviceConnected = false;

    private int randomRange = 2000;
    private long soundInterval = 5000;

    private static MediaPlayer player;
    private static Random generator;
    private static Handler threadHandler;

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

    static void setConnInterval(Settings settings)
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
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        generator = new Random();
        threadHandler = new Handler(Looper.getMainLooper());
        loadSettings();

        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, BIND_AUTO_CREATE);
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        getApplicationContext().unbindService(this);
    }

    private void loadSettings()
    {
        SharedPreferences settings = getApplicationContext().getSharedPreferences(
                PREFERENCES_KEY, 0);

        sensorPrefs.add(settings.getBoolean("accelerometer", true));
        sensorPrefs.add(settings.getBoolean("gyroscope", true));
        sensorPrefs.add(settings.getBoolean("euler", true));
        sensorPrefs.add(settings.getBoolean("linear", true));
        sensorPrefs.add(settings.getBoolean("quaternion", true));
    }

    private void connectToSensor()
    {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            {
                requestBluetoothPermission();
            }
            return;
        }

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        for (BluetoothDevice device : pairedDevices)
        {
            // IMPORTANT TODO: This works but will it work for all of them or just my specific one?
            if (Objects.equals(device.getName(), "MetaWear"))
            {
                connectToDevice(device);
                break;
            }
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
                setConnInterval(sensorBoard.getModule(Settings.class));;
                deviceConnected = true;
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(getApplicationContext(), R.string.connectedInfo,
                                        Toast.LENGTH_SHORT).show();
                        TextView text = findViewById(R.id.deviceInfoText);
                        text.setText(R.string.deviceStatusConnected);

                        ImageView image = findViewById(R.id.deviceInfoImage);
                        image.setImageResource(R.drawable.baseline_bluetooth_connected_24);
                    }
                });
            }
            return null;
        });
    }

    private void requestBluetoothPermission()
    {
        this.requestPermissions(new String[] {Manifest.permission.BLUETOOTH_CONNECT}, 1);
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
    }

    private void playSound()
    {
        if (!trainingStarted) return;

        player.start();
        long playAfter = soundInterval + generator.nextInt(randomRange);
        threadHandler.postDelayed(this::playSound, playAfter);
    }

    private void startTraining()
    {
        Button button = findViewById(R.id.actionButton);
        button.setText(R.string.stopButton);
        trainingStarted = true;

        EditText randomTextView = findViewById(R.id.RandomnessText);
        EditText intervalTextView = findViewById(R.id.IntervalText);

        try
        {
            randomRange = Integer.parseInt(String.valueOf(randomTextView.getText()));
        } catch (NumberFormatException exception)
        {
            randomTextView.setText(randomRange);
        }

        try
        {
            soundInterval = Integer.parseInt(String.valueOf(intervalTextView.getText()));
        } catch (NumberFormatException exception)
        {
            intervalTextView.setText((int) soundInterval);
        }

        player = MediaPlayer.create(this, R.raw.miau);
        long playAfter = soundInterval + generator.nextInt(randomRange);
        threadHandler.postDelayed(this::playSound, playAfter);
    }

    public void openSettings(View view)
    {
        if (trainingStarted) endTraining();

        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
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
        Toast.makeText(getApplicationContext(), R.string.disconnectedInfo, Toast.LENGTH_SHORT)
                .show();
        if (trainingStarted) endTraining();
    }
}