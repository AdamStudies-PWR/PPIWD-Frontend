package com.pwr.activitytracker.data.model.ui.train;

import static android.content.Context.BIND_AUTO_CREATE;

import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.android.BtleService;
import com.pwr.activitytracker.R;
import com.pwr.activitytracker.databinding.FragmentTrainBinding;
import com.pwr.activitytracker.sensors.DeviceController;

public class TrainFragment extends Fragment implements ServiceConnection
{
    private BtleService.LocalBinder serviceBinder;
    private long startTime = 0;
    private long elapsed = 0;
    private final int timerInterval = 1000;

    private boolean trainingStarted = false;
    private boolean isPaused = false;

    private Handler timerHandler;

    private FragmentTrainBinding binding;

    private DeviceController deviceController;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        timerHandler = new Handler(Looper.getMainLooper());

        binding = FragmentTrainBinding.inflate(inflater, container, false);
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

        deviceController.stopMeasurements();
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
        // getDevice();
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName)
    {
        Toast.makeText(requireContext(), R.string.disconnectedInfo, Toast.LENGTH_SHORT).show();
    }
}