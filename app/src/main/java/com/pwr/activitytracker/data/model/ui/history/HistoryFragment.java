package com.pwr.activitytracker.data.model.ui.history;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;
import com.pwr.activitytracker.R;
import com.pwr.activitytracker.data.model.ui.train.login.LoginActivity;
import com.pwr.activitytracker.databinding.FragmentHistoryBinding;
import com.pwr.activitytracker.network.GetAsyncTask;
import com.pwr.activitytracker.network.models.LoginUserData;
import com.pwr.activitytracker.network.models.Measurement;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private String IP = "10.0.2.2";
    private String PORT = "5242";
    private HistoryViewModel historyViewModel;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        historyViewModel =
                new ViewModelProvider(this.getActivity()).get(HistoryViewModel.class);
        binding = FragmentHistoryBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        // ArrayList<HistoryData> list = new ArrayList<>();
        // list.add(new HistoryData("123",30000,"2022-22-02"));
        // list.add(new HistoryData("123",3,"2022-22-02"));
        // list.add(new HistoryData("123",3,"2022-22-02"));
        // list.add(new HistoryData("123",3,"2022-22-02"));
        // list.add(new HistoryData("123",3,"2022-22-02"));
        // list.add(new HistoryData("123",34444444,"2022-22-02"));
        SharedPreferences settings = this.getContext().getSharedPreferences("user-prefs-key", 0);
        PORT = settings.getString("PORT","");
        IP = settings.getString("IP","");
        new GetAsyncTask().setInstance("", this.getContext(),
                "http://" + IP + ":" + PORT,    "/Measurements", true).execute();
        historyViewModel.getHistoryData().observe(this.getViewLifecycleOwner(), historyData -> {
            ArrayAdapter arrayAdapter = new ArrayAdapter(this.getContext(),
                    R.layout.list_history_row, (ArrayList<HistoryData>) historyData);
            binding.listViewHistory.setAdapter(arrayAdapter);
        });
    }
    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        binding = null;
    }
}