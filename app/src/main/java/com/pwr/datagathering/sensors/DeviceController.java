package com.pwr.datagathering.sensors;

import android.util.Log;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.data.EulerAngles;
import com.mbientlab.metawear.data.Quaternion;
import com.mbientlab.metawear.module.SensorFusionBosch;

import java.util.ArrayList;

public class DeviceController
{
    private SensorFusionBosch sensorFusion;

    private boolean marker = false;
    private long startTime = 0;

    private final ArrayList<SensorData> sensorDataList = new ArrayList<>();

    public void startMeasurements()
    {
        sensorFusion.eulerAngles().addRouteAsync(source -> source.stream((data, env) -> {
            SensorData sensorData = new SensorData(marker);
            if (marker) marker = false;

            final EulerAngles angles = data.value(EulerAngles.class);
            sensorData.setTime(String.valueOf(System.currentTimeMillis() - startTime));
            sensorData.setPitch(String.valueOf(angles.pitch()));
            sensorData.setRoll(String.valueOf(angles.roll()));
            sensorData.setYaw(String.valueOf(angles.yaw()));

            Log.i("DeviceController", sensorData.toString());
            sensorDataList.add(sensorData);
        })).continueWith(task -> {
            sensorFusion.eulerAngles().start();
            sensorFusion.start();
            startTime = System.currentTimeMillis();
            return null;
        });
    }

    public ArrayList<SensorData> stopMeasurements()
    {
        sensorFusion.stop();
        return sensorDataList;
    }

    // For testing purposes not needed in final app
    public void setMarker()
    {
        marker = true;
    }

    public void setSensors(MetaWearBoard board) throws UnsupportedModuleException
    {
        sensorFusion = board.getModuleOrThrow(SensorFusionBosch.class);
        configureSensors();
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
