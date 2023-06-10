package com.pwr.activitytracker.data.model.ui.history;

public class HistoryData {
    String duration;
    int count;
    String data;

    public HistoryData() {
    }

    public HistoryData(String duration, int count, String data) {
        this.duration = duration;
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

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
