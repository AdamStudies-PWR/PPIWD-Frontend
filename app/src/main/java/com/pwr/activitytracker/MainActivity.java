package com.pwr.activitytracker;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.pwr.activitytracker.databinding.ActivityMainBinding;

import java.util.Locale;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
{
    private final String PREFERENCES_KEY = "appDataKey";
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

        SharedPreferences settings = getApplicationContext().getSharedPreferences(
                PREFERENCES_KEY, 0);

        if (settings.getString("UUID", "").equals(""))
        {
            setUserUIDD(settings);
        }

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

    private void setUserUIDD(SharedPreferences settings)
    {
        String defaultUIDD = String.valueOf(R.string.defaultUIDD);

        try
        {
            defaultUIDD = createUserUIDD();
        }
        catch (Exception ignore) {}

        SharedPreferences.Editor editor = settings.edit();
        editor.putString("UUID", defaultUIDD);
        editor.apply();
    }

    private String createUserUIDD() throws Exception
    {
        return UUID.randomUUID().toString().replaceAll("-", "")
                .toUpperCase(Locale.ROOT);
    }
}