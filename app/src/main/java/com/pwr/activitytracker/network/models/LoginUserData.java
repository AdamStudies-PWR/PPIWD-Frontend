package com.pwr.activitytracker.network.models;

public class LoginUserData {
    public Number id;
    public String username;
    public String token;

    LoginUserData(Number  id, String  username,String password)
    {
        this.id = id;
        this.username = username;
        this.token = token;
    }

}
