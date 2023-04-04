package com.pwr.datagathering.sensors;

import android.util.Log;

import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.UnsupportedModuleException;
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
        sensorFusion.quaternion().addRouteAsync(source -> source.stream((data, env) -> {
            SensorData sensorData = new SensorData(marker);
            if (marker) marker = false;

            final Quaternion quaternion = data.value(Quaternion.class);
            sensorData.setTime(String.valueOf(System.currentTimeMillis() - startTime));
            sensorData.setQw(String.valueOf(quaternion.w()));
            sensorData.setQx(String.valueOf(quaternion.x()));
            sensorData.setQy(String.valueOf(quaternion.y()));
            sensorData.setQz(String.valueOf(quaternion.z()));

            Log.i("DeviceController", sensorData.toString());
            sensorDataList.add(sensorData);
        })).continueWith(task -> {
            sensorFusion.quaternion().start();
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
