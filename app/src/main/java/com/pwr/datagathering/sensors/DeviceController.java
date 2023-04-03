package com.pwr.datagathering.sensors;

import android.os.Handler;
import android.util.Log;

import com.mbientlab.metawear.AsyncDataProducer;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.data.Acceleration;
import com.mbientlab.metawear.module.Accelerometer;
import com.mbientlab.metawear.module.BarometerBosch;
import com.mbientlab.metawear.module.Gyro;
import com.mbientlab.metawear.module.MagnetometerBmm150;

import java.util.ArrayList;

public class DeviceController
{
    private static Accelerometer accelerometer;
    private static BarometerBosch barometer;
    private static Gyro gyro;
    private static MagnetometerBmm150 magneto;

    private static Handler handler;

    // For testing purposes not needed in final app
    private ArrayList<Boolean> sensorPrefs;

    // For testing purposes not needed in final app
    private static boolean marker = false;
    private static boolean start = false;

    private ArrayList<SensorData> sensorDataList = new ArrayList<>();

    public void getSensors(MetaWearBoard board)
    {
        accelerometer = board.getModule(Accelerometer.class);
        barometer = board.getModule(BarometerBosch.class);
        gyro = board.getModule(Gyro.class);
        magneto = board.getModule(MagnetometerBmm150.class);

        setUpSensors();
    }

    private void setUpSensors()
    {
        accelerometer.configure()
                .range(8.f)
                .odr(50.f)
                .commit();

        barometer.configure()
                .pressureOversampling(BarometerBosch.OversamplingMode.ULTRA_HIGH)
                .filterCoeff(BarometerBosch.FilterCoeff.OFF)
                .standbyTime(0.5f)
                .commit();

        gyro.configure()
                .odr(Gyro.OutputDataRate.ODR_25_HZ)
                .range(Gyro.Range.FSR_500)
                .commit();

        magneto.configure()
                .outputDataRate(MagnetometerBmm150.OutputDataRate.ODR_25_HZ)
                .commit();
    }

    public void setAllowedSensors(ArrayList<Boolean> allowed)
    {
        sensorPrefs = allowed;
    }

    private void setLabels()
    {
        SensorData labels = new SensorData(marker, "Accelerometer AX", "Accelerometer AY", "Accelerometer AZ");
        sensorDataList.add(labels);
    }

    public void startMeasurements()
    {
        start = true;
        setLabels();
        final AsyncDataProducer producer = accelerometer.packedAcceleration() == null ?
                accelerometer.packedAcceleration() :
                accelerometer.acceleration();

        producer.addRouteAsync(source -> source.stream((data, env) -> {
            SensorData sensorData = new SensorData(marker,
                    String.valueOf(data.value(Acceleration.class).x()),
                    String.valueOf(data.value(Acceleration.class).y()),
                    String.valueOf(data.value(Acceleration.class).z()));

            // autism moment
            marker = marker ? !marker : marker;

            Log.i("UWU", sensorData.toString());
            sensorDataList.add(sensorData);
        })).continueWith(task -> {
            producer.start();
            accelerometer.start();
            return null;
        });

        accelerometer.acceleration().start();
        accelerometer.start();
    }

    public ArrayList<SensorData> stopMeasurements()
    {
        accelerometer.stop();

        (accelerometer.packedAcceleration() == null ?
                accelerometer.packedAcceleration() :
                accelerometer.acceleration()
        ).stop();

        return sensorDataList;
    }

    // For testing purposes not needed in final app
    public void addMarker()
    {
        marker = true;
    }
}
