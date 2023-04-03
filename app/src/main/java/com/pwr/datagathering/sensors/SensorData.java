package com.pwr.datagathering.sensors;

import androidx.annotation.NonNull;

public class SensorData
{
    private String flag = "0";
    private String acc_ax = "";
    private String acc_ay = "";
    private String acc_az = "";

    public SensorData(Boolean flag, String ax, String ay, String az)
    {
        this.flag = flag ? "1" : "0";
        acc_ax = ax;
        acc_ay = ay;
        acc_az = az;
    }

    @NonNull
    public String toString()
    {
        String separator = ";";
        return flag + separator + acc_ax + separator + acc_ay + separator + acc_az;
    }
}
