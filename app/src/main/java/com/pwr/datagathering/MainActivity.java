package com.pwr.datagathering;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity
{
    private final String PREFERENCES_KEY = "appDataKey";
    ArrayList<Boolean> sensorPrefs = new ArrayList<>();

    private boolean trainingStarted = false;

    private int randomRange = 2000;
    private long soundInterval = 5000;

    private static MediaPlayer player;
    private static Random generator;
    private static Handler threadHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        generator = new Random();
        threadHandler = new Handler(Looper.getMainLooper());
        loadSettings();
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
    public void onClick(View view)
    {
        if (trainingStarted) endTraining();
        else startTraining();

        Button button = findViewById(R.id.actionButton);
        button.setText(trainingStarted ? R.string.stopButton : R.string.startButton);
    }

    private void endTraining()
    {
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
        trainingStarted = true;

        EditText randomTextView = findViewById(R.id.RandomnessText);
        EditText intervalTextView = findViewById(R.id.IntervalText);

        try
        {
            randomRange = Integer.parseInt(String.valueOf(randomTextView.getText()));
        }
        catch (NumberFormatException ignore) {}

        try
        {
            soundInterval = Integer.parseInt(String.valueOf(intervalTextView.getText()));
        }
        catch (NumberFormatException ignore) {}

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
}