package com.pwr.activitytracker.data.model.ui.login;

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
import com.pwr.activitytracker.MainActivity;
import com.pwr.activitytracker.R;
import com.pwr.activitytracker.databinding.ActivityLoginBinding;
import com.pwr.activitytracker.network.AsyncCallBack;
import com.pwr.activitytracker.network.GetAsyncTask;
import com.pwr.activitytracker.network.PostAsyncTask;
import com.pwr.activitytracker.network.models.LoginCredentials;
import com.pwr.activitytracker.network.models.LoginUserData;

import es.dmoral.toasty.Toasty;

public class LoginActivity extends AppCompatActivity implements AsyncCallBack {

    private LoginViewModel loginViewModel;
    private ActivityLoginBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        loginViewModel = new ViewModelProvider(this, new LoginViewModelFactory()).get(LoginViewModel.class);

        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;
        final Button loginButton = binding.login;
        final Button registerButton = binding.register;
        final ProgressBar loadingProgressBar = binding.loading;

        loginViewModel.getLoginFormState().observe(this, loginFormState -> {
            if (loginFormState == null) {
                return;
            }
            loginButton.setEnabled(loginFormState.isDataValid());
            registerButton.setEnabled(loginFormState.isDataValid());
            if (loginFormState.getUsernameError() != null) {
                usernameEditText.setError(getString(loginFormState.getUsernameError()));
            }
            if (loginFormState.getPasswordError() != null) {
                passwordEditText.setError(getString(loginFormState.getPasswordError()));
            }
        });

        loginViewModel.getLoginResult().observe(this, loginResult -> {
            if (loginResult == null) {
                return;
            }
            loadingProgressBar.setVisibility(View.GONE);
            if (loginResult.getError() != null) {
                showLoginFailed(loginResult.getError());
            }
            if (loginResult.getSuccess() != null) {
                updateUiWithUser(loginResult.getSuccess());
            }
            setResult(Activity.RESULT_OK);
            get_bt_device(usernameEditText.getText().toString());
            finish();
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                loginViewModel.loginDataChanged(usernameEditText.getText().toString(), passwordEditText.getText().toString());
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                loginViewModel.login(usernameEditText.getText().toString(), passwordEditText.getText().toString());
            }
            return false;
        });

        loginButton.setOnClickListener(v -> {
            LoginCredentials newUserCredentials = new LoginCredentials(usernameEditText.getText().toString(), passwordEditText.getText().toString());
            Gson gson = new Gson();
            String requestBody = gson.toJson(newUserCredentials);

            new PostAsyncTask().setInstance("login", LoginActivity.this, "http://10.0.2.2:5242", "/User/Authorize", requestBody, false).execute();
        });

        registerButton.setOnClickListener(v -> {
            LoginCredentials newUserCredentials = new LoginCredentials(usernameEditText.getText().toString(), passwordEditText.getText().toString());

            Gson gson = new Gson();
            String requestBody = gson.toJson(newUserCredentials);

            new PostAsyncTask().setInstance("register", LoginActivity.this, "http://10.0.2.2:5242", "/User/Register", requestBody, false).execute();
        });

        auto_login();
    }

    private void auto_login() {
        SharedPreferences settings = getApplicationContext().getSharedPreferences("credentials", 0);
        String token = settings.getString("token", "");

        if (!token.equals("")) {
            new GetAsyncTask().setInstance("auto-login", LoginActivity.this, "http://10.0.2.2:5242", "/User", true).execute();
        }
    }

    private void get_bt_device(String username) {
        Intent intent = new Intent(this, BluetoothActivity.class);
        intent.putExtra("username", username);
        this.startActivity(intent);
    }

    private void updateUiWithUser(LoggedInUserView model) {
        String welcome = getString(R.string.welcome) + model.getDisplayName();
        // TODO : initiate successful logged in experience
        Toast.makeText(getApplicationContext(), welcome, Toast.LENGTH_LONG).show();
    }

    private void showLoginFailed(@StringRes Integer errorString) {
        Toast.makeText(getApplicationContext(), errorString, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void processRespond(String requestName, String respondData, Boolean isResponseSuccess) {
        final EditText usernameEditText = binding.username;
        final EditText passwordEditText = binding.password;

        if (requestName == "register") {
            if (isResponseSuccess) {
                usernameEditText.setText("");
                passwordEditText.setText("");
                Toasty.success(LoginActivity.this, "New account added successfully", Toast.LENGTH_SHORT, true).show();
            }
        } else if (requestName == "login") {
            if (isResponseSuccess) {
                Toasty.success(LoginActivity.this, "Login successful", Toast.LENGTH_SHORT, true).show();


                Gson gson = new Gson();
                LoginUserData userData = gson.fromJson(respondData, LoginUserData.class);

                // Save credentials to SharedPreferences
                SharedPreferences settings = getApplicationContext().getSharedPreferences("credentials", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("token", String.valueOf(userData.token));
                editor.apply();


                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("username", userData.username);
//                intent.putExtra("sensor", device);
                this.startActivity(intent);

            }
        } else if (requestName == "auto-login") {
            if (isResponseSuccess) {
                Gson gson = new Gson();
                LoginUserData userData = gson.fromJson(respondData, LoginUserData.class);

                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("username", userData.username);
//                intent.putExtra("sensor", device);
                this.startActivity(intent);
            } else {
                SharedPreferences settings = getApplicationContext().getSharedPreferences("credentials", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("token", "");
                editor.apply();
            }
        }
    }
}

