package com.pwr.datagathering.sensors;

import androidx.annotation.NonNull;

public class SensorData
{
    // For testing purposes not needed in final app
    private String flag = "0";
    private String time = "";
    private String qw = "";
    private String qx = "";
    private String qy = "";
    private String qz = "";

    private String heading = "";
    private String pitch = "";
    private String roll = "";
    private String yaw = "";

    public SensorData(Boolean flag)
    {
        this.flag = flag ? "1" : "0";
    }

    public void setTime(String time) { this.time = time; }
    public void setQw(String qw) { this.qw = qw; }

    public void setQx(String qx) { this.qx = qx; }
    public void setQy(String qy) { this.qy = qy; }
    public void setQz(String qz) { this.qz = qz; }
    public void setHeading(String heading) { this.heading = heading; }
    public void setPitch(String pitch) { this.pitch = pitch; }
    public void setRoll(String roll) { this.roll = roll; }
    public void setYaw(String yaw) { this.yaw = yaw; }

    @NonNull
    public String toString(boolean mode)
    {
        String separator = ";";

        return mode ? flag + separator + time + separator + qw + separator + qx + separator + qy
                + separator + qz : flag + separator + heading + separator + pitch + separator + roll
                + separator + yaw;
    }
}
