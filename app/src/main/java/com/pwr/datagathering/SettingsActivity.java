package com.pwr.datagathering;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity
{
    private final String PREFERENCES_KEY = "appDataKey";

    private SharedPreferences settings;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Objects.requireNonNull(getSupportActionBar()).setTitle(R.string.settingsActivityName);
        setSwitches();
    }

    private void setSwitches()
    {
        settings = getApplicationContext().getSharedPreferences(
                PREFERENCES_KEY, 0);

        setSwitch(R.id.accelerometerSwitch, "accelerometer");
        setSwitch(R.id.gyroscopeSwitch, "gyroscope");
        setSwitch(R.id.barometerSwitch, "barometer");
        setSwitch(R.id.magnetoSwitch, "magneto");
    }

    private void setSwitch(int id, String name)
    {
        SwitchCompat switchCompat = findViewById(id);
        switchCompat.setChecked(settings.getBoolean(name, true));
        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean(name, isChecked);
                editor.apply();
            }
        });
    }

    public void onBack(View view)
    {
        finishAndRemoveTask();
    }
}