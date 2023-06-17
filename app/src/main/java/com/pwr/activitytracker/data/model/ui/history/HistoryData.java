package com.pwr.activitytracker.data.model.ui.history;

import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class HistoryData {
    String duration;
    int count;
    String data;

    public HistoryData() {}

    public HistoryData(String duration, int count, String data)
    {
        this.duration = convertDurationToTimeString(duration);
        this.count = count;
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration)
    {
        this.duration = convertDurationToTimeString(duration);;
    }

    public int getCount() {
        return count;
    }


    public void setCount(int count) {
        this.count = count;
    }

    private String convertDurationToTimeString(String duration)
    {
        int dur = Integer.parseInt(duration);

        int seconds = dur / 1000;
        int minutes = seconds / 60;
        seconds = seconds - (minutes * 60);
        int hours = minutes / 60;
        minutes = minutes - (hours * 60);

        String hour = hours > 0 ? (hours < 10 ? "0" + hours + ":" : hours + ":") : "00:";
        String minute = minutes > 0 ? (minutes < 10 ? "0" + minutes + ":" : minutes + ":") : "00:";
        String second = seconds > 0 ? (seconds < 10 ? "0" + seconds : String.valueOf(seconds)) : "00";

        return hour + minute + second;
    }
}
