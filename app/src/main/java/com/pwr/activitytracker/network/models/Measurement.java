package com.pwr.activitytracker.network.models;


import java.util.Collection;

public class Measurement
{
    public String date;
    public int duration ;
    public Collection<SensorData> sensorDatas;

    public Measurement(String date, int duration) {
        this.date = date;
        this.duration = duration;
    }

    public Measurement() {
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public Collection<SensorData> getSensorDatas() {
        return sensorDatas;
    }

    public void setSensorDatas(Collection<SensorData> sensorDatas) {
        this.sensorDatas = sensorDatas;
    }
}
