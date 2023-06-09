package com.pwr.activitytracker.network.models;


public class SensorData
{
//    public Measurement Measurement;
    public String sensor;
    public float xAxis = 0;
    public float yAxis = 0;
    public float zAxis = 0;
    public SensorData(String sensor, float XAxis, float YAxis, float ZAxis) {
//        Measurement = measurement;
        this.sensor = sensor;
        this.xAxis = XAxis;
        this.yAxis = YAxis;
        this.zAxis = ZAxis;
    }
    public SensorData() {
    }
}