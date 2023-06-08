package com.pwr.activitytracker.data.model.ui.train.login;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;
import com.pwr.activitytracker.BluetoothActivity;
import com.pwr.activitytracker.R;
import com.pwr.activitytracker.databinding.ActivityLoginBinding;
import com.pwr.activitytracker.network.AsyncCallBack;
import com.pwr.activitytracker.network.GetAsyncTask;
import com.pwr.activitytracker.network.PostAsyncTask;
import com.pwr.activitytracker.network.models.LoginCredentials;
import com.pwr.activitytracker.network.models.LoginUserData;

import java.util.Objects;

import es.dmoral.toasty.Toasty;

public class LoginActivity extends AppCompatActivity implements AsyncCallBack
{
    private final String PREFERENCES_KEY = "user-prefs-key";
    private LoginViewModel loginViewModel;
    private ActivityLoginBinding binding;

    private String IP = "10.0.2.2";
    private String PORT = "5242";

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //loadUserPrefs();
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loadServerSettings();

        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory()).get(LoginViewModel.class);

        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        final Button loginButton = binding.login;
        final Button registerButton = binding.register;
        final ProgressBar loadingProgressBar = binding.loading;

        loginViewModel.getLoginFormState().observe(this, loginFormState -> {
            if (loginFormState == null)
            {
                return;
            }
            loginButton.setEnabled(loginFormState.isDataValid());
            registerButton.setEnabled(loginFormState.isDataValid());
            if (loginFormState.getUsernameError() != null)
            {
                usernameEditText.setError(getString(loginFormState.getUsernameError()));
            }
            if (loginFormState.getPasswordError() != null)
            {
                passwordEditText.setError(getString(loginFormState.getPasswordError()));
            }
        });

        loginViewModel.getLoginResult().observe(this, loginResult -> {
            if (loginResult == null)
            {
                return;
            }
            loadingProgressBar.setVisibility(View.GONE);

            if (loginResult.getError() != null)
            {
                showLoginFailed(loginResult.getError());
            }

            if (loginResult.getSuccess() != null)
            {
                updateUiWithUser(loginResult.getSuccess());
            }

            setResult(Activity.RESULT_OK);
            getBtDevice(usernameEditText.getText().toString());
            finish();
        });

        TextWatcher afterTextChangedListener = new TextWatcher()
        {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s)
            {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(), passwordEditText.getText().toString());
            }
        };

        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE)
            {
                loginViewModel.login(usernameEditText.getText().toString(), passwordEditText.getText().toString());
            }
            return false;
        });

        loginButton.setOnClickListener(v -> {
            saveServerSettings();
            LoginCredentials newUserCredentials = new LoginCredentials(usernameEditText.getText().toString(), passwordEditText.getText().toString());
            Gson gson = new Gson();
            String requestBody = gson.toJson(newUserCredentials);

            new PostAsyncTask().setInstance("login", LoginActivity.this, "http://" + IP + ":" + PORT, "/User/Authorize", requestBody, false).execute();
        });

        registerButton.setOnClickListener(v -> {
            saveServerSettings();
            LoginCredentials newUserCredentials = new LoginCredentials(usernameEditText.getText().toString(), passwordEditText.getText().toString());

            Gson gson = new Gson();
            String requestBody = gson.toJson(newUserCredentials);

            new PostAsyncTask().setInstance("register", LoginActivity.this, "http://" + IP + ":" + PORT, "/User/Register", requestBody, false).execute();
        });

        autoLogin();
    }

    private void loadServerSettings()
    {
        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFERENCES_KEY, 0);

        EditText ipText = requireViewById(R.id.quickIp);
        EditText portText = requireViewById(R.id.quickPort);

        IP = settings.getString("IP", IP);
        PORT = settings.getString("PORT", PORT);

        ipText.setText(IP);
        portText.setText(PORT);
    }

    private void saveServerSettings()
    {
        SharedPreferences settings = getApplicationContext().getSharedPreferences(PREFERENCES_KEY, 0);
        SharedPreferences.Editor editor = settings.edit();

        EditText ipText = requireViewById(R.id.quickIp);
        IP = String.valueOf(ipText.getText());

        EditText portText = requireViewById(R.id.quickPort);
        PORT = String.valueOf(portText.getText());

        editor.putString("IP", IP);
        editor.putString("PORT", PORT);

        editor.apply();
    }

    private void autoLogin()
    {
        SharedPreferences settings = getApplicationContext().getSharedPreferences("credentials", 0);
        String token = settings.getString("token", "");

        if (!token.equals(""))
        {
            new GetAsyncTask().setInstance("auto-login", LoginActivity.this,
                    "http://" + IP + ":" + PORT, "/User", true).execute();
        }
    }

    private void getBtDevice(String username)
    {
        Intent intent = new Intent(this, BluetoothActivity.class);
        intent.putExtra("username", username);
        this.startActivity(intent);
    }

    private void updateUiWithUser(LoggedInUserView model)
    {
        String welcome = getString(R.string.welcome) + model.getDisplayName();
        // TODO : initiate successful logged in experience
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString)
    {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void processRespond(String requestName, String respondData, Boolean isResponseSuccess)
    {
        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;

        if (Objects.equals(requestName, "register"))
        {
            if (isResponseSuccess)
            {
                usernameEditText.setText("");
                passwordEditText.setText("");
                Toasty.success(LoginActivity.this, "New account added successfully", Toast.LENGTH_SHORT, true).show();
            }
        }
        else if (Objects.equals(requestName, "login"))
        {
            if (isResponseSuccess)
            {
                Toasty.success(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT, true).show();

                Gson gson = new Gson();
                LoginUserData userData = gson.fromJson(respondData, LoginUserData.class);

                // Save credentials to SharedPreferences
                SharedPreferences settings = getApplicationContext().getSharedPreferences("credentials", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("token", String.valueOf(userData.token));
                editor.apply();

                // Uncomment this to use bluetooth device connection
                Intent intent = new Intent(this, BluetoothActivity.class);

                // Uncomment this to skip bluetooth device connection
                // Intent intent = new Intent(this, MainActivity.class);

                intent.putExtra("username", userData.username);
                this.startActivity(intent);

            }
        }
        else if (Objects.equals(requestName, "auto-login"))
        {
            if (isResponseSuccess)
            {
                Gson gson = new Gson();
                LoginUserData userData = gson.fromJson(respondData, LoginUserData.class);

                // Uncomment this to use bluetooth device connection
                Intent intent = new Intent(this, BluetoothActivity.class);

                // Uncomment this to skip bluetooth device connection
                // Intent intent = new Intent(this, MainActivity.class);

                intent.putExtra("username", userData.username);
                this.startActivity(intent);
            }
            else
            {
                SharedPreferences settings = getApplicationContext().getSharedPreferences("credentials", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("token", "");
                editor.apply();
            }
        }
    }
}