package com.pwr.activitytracker.data.model.ui.history;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.pwr.activitytracker.R;
import com.pwr.activitytracker.data.model.ui.train.login.LoginActivity;
import com.pwr.activitytracker.databinding.FragmentHistoryBinding;
import com.pwr.activitytracker.network.GetAsyncTask;

import java.util.ArrayList;
import java.util.List;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private String IP = "10.0.2.2";
    private String PORT = "5242";
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HistoryViewModel historyViewModel =
                new ViewModelProvider(this).get(HistoryViewModel.class);

        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        ArrayList<HistoryData> list = new ArrayList<>();

        list.add(new HistoryData("123",30000,"2022-22-02"));
        list.add(new HistoryData("123",3,"2022-22-02"));
        list.add(new HistoryData("123",3,"2022-22-02"));
        list.add(new HistoryData("123",3,"2022-22-02"));
        list.add(new HistoryData("123",3,"2022-22-02"));
        list.add(new HistoryData("123",34444444,"2022-22-02"));
        AsyncTask asyncTask = new GetAsyncTask().setInstance("", this.getContext(), "http://" + IP + ":" + PORT, "/Measurements", true).execute();

        ArrayAdapter arrayAdapter = new ArrayAdapter(this.getContext(), R.layout.list_history_row,list);
        binding.listViewHistory.setAdapter(arrayAdapter);
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}