package com.pwr.activitytracker.ui.train;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.pwr.activitytracker.R;
import com.pwr.activitytracker.databinding.FragmentTrainBinding;

public class TrainFragment extends Fragment
{
    private long startTime = 0;
    private final int timerInterval = 1000;

    private boolean trainingStarted = false;

    private Handler timerHandler;

    private FragmentTrainBinding binding;

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
        binding.trainingButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (trainingStarted) stopTraining();
                else startTraining();
            }
        });
    }

    @Override
    public void onPause()
    {
        Context context = requireView().getContext();
        Toast toast = Toast.makeText(context, R.string.training_stopped, Toast.LENGTH_SHORT);
        toast.show();

        stopTraining();
        super.onPause();
    }

    private void startTraining()
    {
        Button button = requireView().findViewById(R.id.trainingButton);
        button.setText(R.string.button_stop);

        trainingStarted = true;
        startTime = System.currentTimeMillis();
        timerHandler.postDelayed(this::updateTimer, timerInterval);
    }

    private void stopTraining()
    {
        Button button = requireView().findViewById(R.id.trainingButton);
        button.setText(R.string.button_start);

        trainingStarted = false;
        resetTimer();
    }

    private void updateTimer()
    {
        if (!trainingStarted)
        {
            return;
        }

        long millis = System.currentTimeMillis() - startTime;
        int seconds = (int)(millis / 1000);
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
}