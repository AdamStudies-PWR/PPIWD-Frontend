package com.pwr.activitytracker.ui.train;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.pwr.activitytracker.databinding.FragmentTrainBinding;

public class TrainFragment extends Fragment {

    private FragmentTrainBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        TrainViewModel homeViewModel =
                new ViewModelProvider(this).get(TrainViewModel.class);

        binding = FragmentTrainBinding.inflate(inflater, container, false);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}