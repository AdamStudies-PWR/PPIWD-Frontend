package com.pwr.activitytracker.network.models;

import androidx.annotation.Nullable;

public class LoginCredentials {
    public String username;
    public String password;

    public LoginCredentials(String username, String password)
    {
        this.username = username;
        this.password = password;

    }

}
