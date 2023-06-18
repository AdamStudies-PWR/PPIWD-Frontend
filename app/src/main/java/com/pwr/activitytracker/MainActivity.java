package com.pwr.activitytracker;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import es.dmoral.toasty.Toasty;

import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.pwr.activitytracker.data.model.ui.history.HistoryData;
import com.pwr.activitytracker.data.model.ui.history.HistoryViewModel;
import com.pwr.activitytracker.data.model.ui.train.login.LoginActivity;
import com.pwr.activitytracker.databinding.ActivityMainBinding;
import com.pwr.activitytracker.network.AsyncCallBack;
import com.pwr.activitytracker.network.models.History;
import com.pwr.activitytracker.network.models.Measurement;

import org.w3c.dom.Text;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity implements AsyncCallBack
{
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private HistoryViewModel historyViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();
        setUsername(intent.getStringExtra("username"));
        historyViewModel =
                new ViewModelProvider(this).get(HistoryViewModel.class);
        setSupportActionBar(binding.appBarMain.toolbar);
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(R.id.nav_train, R.id.nav_history,
                R.id.nav_settings).setOpenableLayout(drawer).build();
        NavController navController = Navigation.findNavController(this,
                R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController,
                mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    private void setUsername(String username)
    {
        NavigationView navigationView = binding.navView;
        TextView banner = navigationView.getHeaderView(0).findViewById(R.id.userBanner);
        banner.setText("Welcome, " + username);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onSupportNavigateUp()
    {
        NavController navController = Navigation.findNavController(this,
                R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    public void processRespond(String id, String respondData, Boolean isResponseSuccess)
    {
        if (Objects.equals(id, ""))
        {
            if (isResponseSuccess)
            {
                Gson gson = new Gson();

                Type listType = new TypeToken<ArrayList<History>>(){}.getType();
                List<History> measurement = gson.fromJson(respondData, listType);
                List<HistoryData> historyData = new ArrayList<>();
                for(History e : new ArrayList<>(measurement))
                {
                    String localDateTime = e.getDate().split("T")[0];
                    historyData.add(new HistoryData(String.valueOf(e.getDuration()), e.getJumpCount(), localDateTime));
                }

                historyViewModel.setHistoryData(historyData);
            }
        }
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture)
    {
        super.onPointerCaptureChanged(hasCapture);
    }
}