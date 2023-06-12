package com.pwr.activitytracker.data.model.ui.train;

import static android.content.Context.BIND_AUTO_CREATE;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.gson.Gson;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.android.BtleService;
import com.pwr.activitytracker.MainActivity;
import com.pwr.activitytracker.R;
import com.pwr.activitytracker.databinding.FragmentTrainBinding;
import com.pwr.activitytracker.network.AsyncCallBack;
import com.pwr.activitytracker.network.PostAsyncTask;
import com.pwr.activitytracker.network.models.Measurement;
import com.pwr.activitytracker.network.models.SensorData;
import com.pwr.activitytracker.sensors.DeviceController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class TrainFragment extends Fragment implements ServiceConnection, AsyncCallBack
{
    private BtleService.LocalBinder serviceBinder;
    private long startTime = 0;
    private long elapsed = 0;
    private final int timerInterval = 1000;

    private boolean trainingStarted = false;
    private boolean isPaused = false;
    private static boolean bluetoothError = false;

    private Handler timerHandler;

    private FragmentTrainBinding binding;

    private DeviceController deviceController;

    private String IP = "10.0.2.2";
    private String PORT = "5242";

    private final BroadcastReceiver btReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF)
                {
                    bluetoothError = true;
                }
        }
    }};

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        timerHandler = new Handler(Looper.getMainLooper());

        binding = FragmentTrainBinding.inflate(inflater, container, false);

        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        requireActivity().registerReceiver(btReceiver, filter);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        deviceController = new DeviceController();

        requireContext().bindService(new Intent(requireActivity(), BtleService.class),
                this, BIND_AUTO_CREATE);

        binding.playButton.setOnClickListener(trainingButton -> {
            if (!trainingStarted) startTraining();
            else
            {
                unpauseTraining();
            }
        });

        binding.pauseButton.setOnClickListener(pauseButton -> {
            pauseTraining();
        });

        binding.stopButton.setOnClickListener(stopButton -> {
            if (trainingStarted) stopTraining();
        });

        ImageButton button = requireView().findViewById(R.id.stopButton);
        button.setEnabled(false);
        button.setClickable(false);
        button.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.grey));
    }

    private void getDevice()
    {
        Intent intent = requireActivity().getIntent();
        BluetoothDevice device = intent.getParcelableExtra("sensor");
        MetaWearBoard sensorBoard = serviceBinder.getMetaWearBoard(device);

        sensorBoard.onUnexpectedDisconnect(status -> {
            bluetoothError = true;
        });

        try
        {
            deviceController.setSensors(sensorBoard);
        }
        catch (UnsupportedModuleException exception)
        {
            Toast.makeText(requireContext(), R.string.connectedFailure, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPause()
    {
        if (trainingStarted)
        {
            Toast.makeText(requireContext(), R.string.training_stopped, Toast.LENGTH_SHORT).show();
            stopTraining();
        }
        super.onPause();
    }

    private void startTraining()
    {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null)
        {
            Toast.makeText(requireContext(), R.string.connectivty_issue, Toast.LENGTH_SHORT).show();
            return;
        }
        else if (!adapter.isEnabled())
        {
            Toast.makeText(requireContext(), R.string.bt_disabled, Toast.LENGTH_SHORT).show();
            return;
        }


        TextView headingView = requireView().findViewById(R.id.headingData);
        TextView pitchView = requireView().findViewById(R.id.pitchData);
        TextView rollView = requireView().findViewById(R.id.rollData);
        TextView yawView = requireView().findViewById(R.id.yawData);

        startTime = System.currentTimeMillis();
        if (deviceController.startMeasurements(startTime, headingView, pitchView, rollView, yawView))
        {
            ImageButton stopButton = requireView().findViewById(R.id.stopButton);
            stopButton.setEnabled(true);
            stopButton.setClickable(true);
            stopButton.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.purple_200));

            ImageButton playButton = requireView().findViewById(R.id.playButton);
            playButton.setVisibility(View.GONE);

            ImageButton pauseButton = requireView().findViewById(R.id.pauseButton);
            pauseButton.setVisibility(View.VISIBLE);

            trainingStarted = true;
            isPaused = false;
            timerHandler.postDelayed(this::updateTimer, timerInterval);
        }
        else
        {
            Toast.makeText(requireContext(), R.string.training_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void stopTraining()
    {
        ImageButton button = requireView().findViewById(R.id.stopButton);
        button.setEnabled(false);
        button.setClickable(false);
        button.setBackgroundTintList(ContextCompat.getColorStateList(requireContext(), R.color.grey));

        ImageButton playButton = requireView().findViewById(R.id.playButton);
        playButton.setVisibility(View.VISIBLE);

        ImageButton pauseButton = requireView().findViewById(R.id.pauseButton);
        pauseButton.setVisibility(View.GONE);

        SharedPreferences settings = this.getContext().getSharedPreferences("user-prefs-key", 0);
        PORT = settings.getString("PORT","");
        IP = settings.getString("IP","");
        List<com.pwr.activitytracker.sensors.SensorData> measurements = deviceController.stopMeasurements();
        Gson gson = new Gson();
        Measurement measurement = new Measurement();
        measurement.setDate(LocalDateTime.now().toString());
        AtomicLong duration = new AtomicLong();
        List list =new ArrayList();
        for(com.pwr.activitytracker.sensors.SensorData e : new ArrayList<>(measurements))
        {
            duration.addAndGet(Long.parseLong(e.getTime()));
            SensorData s =new SensorData("sensor",Float.valueOf(e.getPitch()),Float.valueOf(e.getRoll()),Float.valueOf(e.getYaw()));
            list.add(s);
        }
        measurement.setDuration((int) duration.get());
        measurement.setSensorDatas(list);
        String requestBody = gson.toJson(measurement);
        new PostAsyncTask().setInstance("PostMeasurements", TrainFragment.this.getContext(), "http://" + IP + ":" + PORT, "/Measurements/", requestBody, true).execute();

        trainingStarted = false;
        isPaused = false;
        elapsed = 0;
        resetTimer();
    }

    private void pauseTraining()
    {
        long current = System.currentTimeMillis() - startTime;
        elapsed = elapsed + current;
        deviceController.pause();
        isPaused = true;

        ImageButton playButton = requireView().findViewById(R.id.playButton);
        playButton.setVisibility(View.VISIBLE);
        ImageButton pauseButton = requireView().findViewById(R.id.pauseButton);
        pauseButton.setVisibility(View.GONE);
    }

    private void unpauseTraining()
    {
        startTime = System.currentTimeMillis();
        deviceController.unPause(startTime, elapsed);
        isPaused = false;

        ImageButton playButton = requireView().findViewById(R.id.playButton);
        playButton.setVisibility(View.GONE);
        ImageButton pauseButton = requireView().findViewById(R.id.pauseButton);
        pauseButton.setVisibility(View.VISIBLE);
    }

    private void updateTimer()
    {
        if (bluetoothError)
        {
            bluetoothError = false;
            Toast.makeText(requireContext(), "Lost connection to sensor!", Toast.LENGTH_SHORT)
                    .show();
            stopTraining();
            return;
        }

        if (!trainingStarted)
        {
            return;
        }

        if (isPaused)
        {
            timerHandler.postDelayed(this::updateTimer, timerInterval);
            return;
        }

        long millis = System.currentTimeMillis() - startTime;
        long duration = elapsed + millis;
        int seconds = (int)(duration / 1000);
        int minutes = seconds / 60;
        int hours = minutes / 60;
        seconds = seconds % 60;
        minutes = minutes % 60;

        String timerText = "" + (((hours / 10) == 0) ? "0" + hours : hours);
        timerText = timerText + ":" + (((minutes / 10) == 0) ? "0" + minutes : minutes);
        timerText = timerText + ":" + (((seconds / 10) == 0) ? "0" + seconds : seconds);

        TextView timer = requireView().findViewById(R.id.timerText);
        timer.setText(timerText);

        timerHandler.postDelayed(this::updateTimer, timerInterval);
    }

    private void resetTimer()
    {
        TextView timer = requireView().findViewById(R.id.timerText);
        timer.setText("00:00:00");
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service)
    {
        serviceBinder = (BtleService.LocalBinder) service;
        // Comment this to not use bt device
        getDevice();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName)
    {
        Toast.makeText(requireContext(), R.string.disconnectedInfo, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void processRespond(String id, String respondData, Boolean isResponseSuccess) {

    }
}