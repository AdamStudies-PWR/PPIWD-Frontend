package com.pwr.activitytracker.sensors;

import androidx.annotation.NonNull;

public class SensorData
{
    // </testing>
    private String time = "";
    private String heading = "";
    private String pitch = "";
    private String roll = "";
    private String yaw = "";

    public String getTime() {
        return time;
    }

    public String getHeading() {
        return heading;
    }

    public String getPitch() {
        return pitch;
    }

    public String getRoll() {
        return roll;
    }

    public String getYaw() {
        return yaw;
    }

    public SensorData()
    {}

    public void setTime(String time) { this.time = time; }
    public void setHeading(String heading) { this.heading = heading; }
    public void setPitch(String pitch) { this.pitch = pitch; }
    public void setRoll(String roll) { this.roll = roll; }
    public void setYaw(String yaw) { this.yaw = yaw; }

    @NonNull
    public String toString()
    {
        String separator = ";";
        return time + separator + heading + separator + pitch + separator + roll + separator + yaw;
    }
}
