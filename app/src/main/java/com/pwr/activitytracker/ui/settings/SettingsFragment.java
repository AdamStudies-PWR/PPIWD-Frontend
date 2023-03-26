package com.pwr.activitytracker.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.pwr.activitytracker.R;
import com.pwr.activitytracker.databinding.FragmentSettingsBinding;

import java.util.Objects;

public class SettingsFragment extends Fragment
{
    private final String PREFERENCES_KEY = "appDataKey";

    private FragmentSettingsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        EditText uiddText = requireView().findViewById(R.id.UIDDDefault);
        uiddText.setInputType(InputType.TYPE_NULL);
        uiddText.setTextIsSelectable(true);

        SharedPreferences settings = requireActivity().getSharedPreferences(
                PREFERENCES_KEY, 0);

        uiddText.setText(settings.getString("UUID", String.valueOf(R.string.defaultUIDD)));
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        binding = null;
    }
}