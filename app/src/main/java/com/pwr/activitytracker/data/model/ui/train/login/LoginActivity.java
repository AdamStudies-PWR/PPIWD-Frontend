package com.pwr.activitytracker.data.model.ui.train.login;

import static com.pwr.activitytracker.BluetoothActivity.reconnect;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.gson.Gson;
import com.mbientlab.metawear.MetaWearBoard;
import com.mbientlab.metawear.UnsupportedModuleException;
import com.mbientlab.metawear.android.BtleService;
import com.pwr.activitytracker.BluetoothActivity;
import com.pwr.activitytracker.MainActivity;
import com.pwr.activitytracker.R;
import com.pwr.activitytracker.databinding.ActivityLoginBinding;
import com.pwr.activitytracker.network.AsyncCallBack;
import com.pwr.activitytracker.network.GetAsyncTask;
import com.pwr.activitytracker.network.PostAsyncTask;
import com.pwr.activitytracker.network.models.LoginCredentials;
import com.pwr.activitytracker.network.models.LoginUserData;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import bolts.Task;
import es.dmoral.toasty.Toasty;
import com.mbientlab.metawear.module.Settings;
import com.pwr.activitytracker.sensors.DeviceController;
import android.Manifest;

public class LoginActivity extends AppCompatActivity implements ServiceConnection, AsyncCallBack {

    private final String PREFERENCES_KEY = "user-prefs-key";
    private LoginViewModel loginViewModel;
    private ActivityLoginBinding binding;

    private String IP = "10.0.2.2";
    private String PORT = "5242";

    private Set<BluetoothDevice> pairedDevices = null;

    private BluetoothDevice selectedDevice;

    private boolean deviceConnected = false;

    private BtleService.LocalBinder serviceBinder;

    private MetaWearBoard sensorBoard;

    private static DeviceController deviceController;

    public static Task<Void> reconnect(final MetaWearBoard board)
    {
        return board.connectAsync()
                .continueWithTask(task -> {
                    if (task.isFaulted())
                    {
                        return reconnect(board);
                    } else if (task.isCancelled())
                    {
                        return task;
                    }
                    return Task.forResult(null);
                });
    }

    void setConnInterval(Settings settings)
    {
        if (settings != null)
        {
            Settings.BleConnectionParametersEditor editor = settings.editBleConnParams();
            if (editor != null)
            {
                editor.maxConnectionInterval(11.25f).commit();
            }
        }
    }

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder service)
    {
        serviceBinder = (BtleService.LocalBinder) service;
        //connectToSensor(); TODO: Panie Adamie, mogę to usunąć? @mkalina
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {

    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        deviceController = new DeviceController();
        super.onCreate(savedInstanceState);
        getApplicationContext().bindService(new Intent(this, BtleService.class),
                this, BIND_AUTO_CREATE);

        //tloadUserPrefs();
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
            if (loginFormState == null) {
                return;
            }
            loginButton.setEnabled(loginFormState.isDataValid() && deviceConnected);
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

        auto_login();
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




        ListView listView = findViewById(R.id.listview);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        try {
            pairedDevices = bluetoothAdapter.getBondedDevices();
        } catch (SecurityException exception) {
            Log.e("BLUETOOTH", "Error granting permission: " + exception.toString());
            Toast.makeText(getApplicationContext(), R.string.permissionError, Toast.LENGTH_SHORT).show();
        }
        List<String> devicesNames = new ArrayList<>();
        if(pairedDevices !=null){
            devicesNames = pairedDevices.stream().map(BluetoothDevice::getName).collect(Collectors.toList());
        }

        ArrayAdapter arrayAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, devicesNames);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (!arePermissionsGranted()) {
                    requestBluetoothPermission();
                }
                 selectedDevice = nthElement(pairedDevices, position);
                 connectToSensor();
            }
        });
    }

    public static final <T> T nthElement(Iterable<T> data, int n){
        int index = 0;
        for(T element : data){
            if(index == n){
                return element;
            }
            index++;
        }
        return null;
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

    private void connectToSensor()
    {
        if(selectedDevice!=null) {
            connectToDevice(selectedDevice);
        }
        else {
            Log.e("BLUETOOTH", "Error, device is null");
            Toast.makeText(getApplicationContext(), R.string.permissionError, Toast.LENGTH_SHORT).show();
        }
    }

    private void connectToDevice(BluetoothDevice device)
    {
        serviceBinder.removeMetaWearBoard(device);//tutaj jest blad
        sensorBoard = serviceBinder.getMetaWearBoard(device);

        sensorBoard.connectAsync().continueWithTask(task -> {
            if (task.isCancelled()) return task;
            return task.isFaulted() ? reconnect(sensorBoard) : Task.forResult(null);
        }).continueWith(task -> {
            if (!task.isCancelled())
            {
                setConnInterval(sensorBoard.getModule(Settings.class));
                try
                {
                    deviceController.setSensors(sensorBoard);
                    deviceConnected = true;
                }
                catch (UnsupportedModuleException exception)
                {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), R.string.connectedFailure,
                            Toast.LENGTH_SHORT).show());
                }
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), R.string.connectedInfo,
                            Toast.LENGTH_SHORT).show();
                    //To zakomentowane jest z data gettera. Zostawiam, moze sie tu potem przyda
                    //TextView text = findViewById(R.id.deviceInfoText);
                    //text.setText(R.string.deviceStatusConnected);
                    //ImageView image = findViewById(R.id.deviceInfoImage);
                    //image.setImageResource(R.drawable.baseline_bluetooth_connected_24);
                });
                ListView listView = findViewById(R.id.listview);
                listView.setVisibility(View.GONE);//to nie dziala chyba dlatego ze jest tu linear view.
                //Ale w sumie to nie musi dzialac, moze poprawie gui aby to sie jakos fajnie prezentowalo
            }
            return null;
        });
    }

    private void auto_login() {
        SharedPreferences settings = getApplicationContext().getSharedPreferences("credentials", 0);
        String token = settings.getString("token", "");

        if (!token.equals("")) {
            new GetAsyncTask().setInstance("auto-login", LoginActivity.this, "http://" + IP + ":" + PORT, "/User", true).execute();
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


                // Uncomment this to use bluetooth device connection
                //Intent intent = new Intent(this, BluetoothActivity.class);

                // Uncomment this to skip bluetooth device connection
                Intent intent = new Intent(this, MainActivity.class);

                intent.putExtra("username", userData.username);
                this.startActivity(intent);

            }
        } else if (Objects.equals(requestName, "auto-login")) {
            if (isResponseSuccess) {
                Gson gson = new Gson();
                LoginUserData userData = gson.fromJson(respondData, LoginUserData.class);

                // Uncomment this to use bluetooth device connection
                Intent intent = new Intent(this, BluetoothActivity.class);

                // Uncomment this to skip bluetooth device connection
                // Intent intent = new Intent(this, MainActivity.class);

                intent.putExtra("username", userData.username);
                this.startActivity(intent);
            } else {
                SharedPreferences settings = getApplicationContext().getSharedPreferences("credentials", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("token", "");
                editor.apply();
            }
        }
    }
    private void requestBluetoothPermission()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            this.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 1);
        }
        else
        {
            this.requestPermissions(new String[]{Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }
    private boolean arePermissionsGranted()
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
                    == PackageManager.PERMISSION_GRANTED;
        }
        else
        {
            return ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        }
    }
}