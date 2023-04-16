package com.pwr.datagathering.sensors;

import androidx.annotation.NonNull;

public class SensorData
{
    // For testing purposes not needed in final app
    private final String flag;
    // </testing>
    private String time = "";
    private String pitch = "";
    private String roll = "";
    private String yaw = "";

    public SensorData(Boolean flag)
    {
        this.flag = flag ? "1" : "0";
    }

    public void setTime(String time) { this.time = time; }
    public void setPitch(String pitch) { this.pitch = pitch; }
    public void setRoll(String roll) { this.roll = roll; }
    public void setYaw(String yaw) { this.yaw = yaw; }

    @NonNull
    public String toString()
    {
        String separator = ";";

        return flag + separator + time + separator + pitch + separator + roll + separator + yaw;
    }
}
