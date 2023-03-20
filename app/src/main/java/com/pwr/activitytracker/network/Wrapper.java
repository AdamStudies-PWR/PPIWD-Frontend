package com.pwr.activitytracker.network;

class Wrapper {
    public String stringResponse;
    public boolean isResponseSuccess;

    public Wrapper(String stringResponse, boolean isOK) {
        this.stringResponse = stringResponse;
        this.isResponseSuccess = isOK;
    }
}
