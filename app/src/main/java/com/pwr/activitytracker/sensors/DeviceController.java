package com.pwr.activitytracker.sensors;

import android.util.Log;
import android.widget.TextView;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.EulerAngles;
import com.mbientlab.metawear.module.SensorFusionBosch;

import java.util.ArrayList;

public class DeviceController
{
    private SensorFusionBosch sensorFusion;
    private boolean sensorConnected = false;
    private boolean isPaused = false;
    private long offset = 0;
    private long startTime = 0;

    private final ArrayList<SensorData> sensorDataList = new ArrayList<>();

    public boolean startMeasurements(long startTime, TextView headingView, TextView pitchView,
                                     TextView rollView, TextView yawView)
    {
        if (!sensorConnected)
        {
            return false;
        }

        sensorFusion.eulerAngles().addRouteAsync(source -> source.stream((data, env) -> {
            if (isPaused)
            {
                return;
            }

            SensorData sensorData = new SensorData();

            final EulerAngles angles = data.value(EulerAngles.class);
            long duration = offset + System.currentTimeMillis() - this.startTime;
            sensorData.setTime(String.valueOf(duration));
            String heading = String.valueOf(angles.heading());
            sensorData.setHeading(heading);
            headingView.setText(heading);
            String pitch = String.valueOf(angles.pitch());
            sensorData.setPitch(pitch);
            pitchView.setText(pitch);
            String roll = String.valueOf(angles.roll());
            sensorData.setRoll(roll);
            rollView.setText(roll);
            String yaw = String.valueOf(angles.yaw());
            sensorData.setYaw(yaw);
            yawView.setText(yaw);

            Log.i("DeviceController", sensorData.toString());
            sensorDataList.add(sensorData);
        })).continueWith(task -> {
            sensorFusion.eulerAngles().start();
            this.startTime = startTime;
            sensorFusion.start();
            return null;
        });

        return true;
    }

    public ArrayList<SensorData> stopMeasurements()
    {
        isPaused = false;
        offset = 0;
        sensorFusion.stop();
        return sensorDataList;
    }

    public void pause()
    {
        isPaused = true;
    }

    public void unPause(long startTime, long offset)
    {
        this.startTime = startTime;
        this.offset = offset;
        isPaused = false;
    }

    public void setSensors(MetaWearBoard board) throws UnsupportedModuleException
    {
        sensorFusion = board.getModuleOrThrow(SensorFusionBosch.class);
        configureSensors();
        sensorConnected = true;
    }

    private void configureSensors()
    {
        sensorFusion.configure()
                .mode(SensorFusionBosch.Mode.NDOF)
                .accRange(SensorFusionBosch.AccRange.AR_16G)
                .gyroRange(SensorFusionBosch.GyroRange.GR_2000DPS)
                .commit();
    }
}
