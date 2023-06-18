package com.pwr.activitytracker.network.models;

import java.util.Collection;

public class History
{
    public String date;
    public int duration ;
    public int jumpCount = 0;

    public History() {}

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

    public int getJumpCount() {
        return jumpCount;
    }
    public void setJumpCount(int jumpCount) {
        this.jumpCount = jumpCount;
    }

}
