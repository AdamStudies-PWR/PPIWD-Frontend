package com.pwr.activitytracker.data.model.ui.settings;

import static androidx.core.content.ContextCompat.getSystemService;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.pwr.activitytracker.R;
import com.pwr.activitytracker.databinding.FragmentSettingsBinding;

import java.util.Objects;

public class SettingsFragment extends Fragment
{
    private final String PREFERENCES_KEY = "user-prefs-key";
    private FragmentSettingsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
    {
        loadSettings();

        binding.logoutButton.setOnClickListener(this::logout);
        binding.ipSettingsText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable)
            {
                saveSettingString("IP", String.valueOf(binding.ipSettingsText.getText()));
            }
        });

        binding.portSettingsText.addTextChangedListener(new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable)
            {
                saveSettingString("PORT", String.valueOf(binding.portSettingsText.getText()));
            }
        });
    }

    private void saveSettingString(String key, String value)
    {
        SharedPreferences settings = requireContext().getSharedPreferences(PREFERENCES_KEY, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private void loadSettings()
    {
        SharedPreferences settings = requireContext().getSharedPreferences(PREFERENCES_KEY, 0);

        binding.ipSettingsText.setText(settings.getString("IP", "10.0.2.2"));
        binding.portSettingsText.setText(settings.getString("PORT", "5242"));
        binding.spinner.setText(settings.getString("DEVICE_ID","Unknown device"));
        binding.userNameText.setText(requireActivity().getIntent().getStringExtra("username"));
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        binding = null;
    }

    public void logout(View view)
    {
        requireActivity().finish();
    }
}