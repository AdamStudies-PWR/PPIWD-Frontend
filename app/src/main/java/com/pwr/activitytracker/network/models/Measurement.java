package com.pwr.activitytracker.network.models;


import java.util.Collection;

public class Measurement
{
    public String Date;
    public int Duration ;
    public Collection<SensorData> SensorDatas;

    public Measurement(String date, int duration) {
        Date = date;
        Duration = duration;
    }

    public Measurement() {
    }

    public String getDate() {
        return Date;
    }

    public void setDate(String date) {
        Date = date;
    }

    public int getDuration() {
        return Duration;
    }

    public void setDuration(int duration) {
        Duration = duration;
    }

    public Collection<SensorData> getSensorDatas() {
        return SensorDatas;
    }

    public void setSensorDatas(Collection<SensorData> sensorDatas) {
        SensorDatas = sensorDatas;
    }
}
