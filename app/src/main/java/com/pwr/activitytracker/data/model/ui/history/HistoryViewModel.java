package com.pwr.activitytracker.data.model.ui.history;

import com.pwr.activitytracker.network.models.Measurement;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HistoryViewModel extends ViewModel {
    private final MutableLiveData<String> mText;
    private MutableLiveData<List<HistoryData>> historyData;
    public HistoryViewModel() {
        mText = new MutableLiveData<>();
        historyData = new MutableLiveData<>();
        mText.setValue("This is slideshow fragment");
    }
    public void setHistoryData(List<HistoryData> measurements) {
        this.historyData.setValue(measurements);
    }
    public LiveData<List<HistoryData>> getHistoryData(){return historyData;}
    public LiveData<String> getText() {
        return mText;
    }
}