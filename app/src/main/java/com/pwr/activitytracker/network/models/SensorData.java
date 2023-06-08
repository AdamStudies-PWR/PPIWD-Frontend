package com.pwr.activitytracker.network.models;


public class SensorData
{
//    public Measurement Measurement;
    public String Sensor;
    public float XAxis = 0;
    public float YAxis = 0;
    public float ZAxis = 0;

    public SensorData(String sensor, float XAxis, float YAxis, float ZAxis) {
//        Measurement = measurement;
        Sensor = sensor;
        this.XAxis = XAxis;
        this.YAxis = YAxis;
        this.ZAxis = ZAxis;
    }

    public SensorData() {
    }
}