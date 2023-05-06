package com.pwr.activitytracker.data.model.ui.settings;

import static androidx.core.content.ContextCompat.getSystemService;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.os.Bundle;
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

public class SettingsFragment extends Fragment
{
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
        binding.showPasswordButton.setOnTouchListener(this::showHidePassword);
        binding.changePasswordButton.setOnClickListener(this::changePassword);
        binding.logoutButton.setOnClickListener(this::logout);
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        binding = null;
    }

    public boolean showHidePassword(View view, MotionEvent motionEvent)
    {
        EditText password = requireView().findViewById(R.id.passwordText);
        if (motionEvent.getAction() == MotionEvent.ACTION_DOWN)
        {
            password.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        }
        else if (motionEvent.getAction() == MotionEvent.ACTION_UP)
        {
            password.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }

        return false;
    }

    public void changePassword(View view)
    {
        Toast.makeText(requireContext(), "TODO: Not implemented", Toast.LENGTH_SHORT).show();
    }

    public void logout(View view)
    {
        Toast.makeText(requireContext(), "TODO: Not implemented", Toast.LENGTH_SHORT).show();
    }
}