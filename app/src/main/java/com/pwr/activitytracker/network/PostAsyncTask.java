package com.pwr.activitytracker.network;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.widget.Toast;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import es.dmoral.toasty.Toasty;

public class PostAsyncTask extends AsyncTask<Void, Void, Wrapper> {
    private Activity activity;
    private AsyncCallBack asyncCallBack;

    private String requestName;
    private String urlDomainName;
    private String urlPath;
    private String body;
    private Toast toastLoading = null;
    private Toast toastResponse = null;
    private Boolean withAuthorization;

    public PostAsyncTask setInstance(String requestName, Context context, String urlDomainName, String urlPath, String body,Boolean withAuthorization) {
        this.activity = (Activity) context;
        asyncCallBack = (AsyncCallBack) context;
        this.urlDomainName = urlDomainName;
        this.urlPath = urlPath;
        this.body = body;
        this.requestName = requestName;
        this.withAuthorization = withAuthorization;
        return this;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        toastLoading = Toasty.info(activity, "Loading...", Toast.LENGTH_LONG, true);
        toastLoading.show();
    }


    @Override
    protected Wrapper doInBackground(Void... voids) {
        URL url;

        try {
            url = new URL(urlDomainName + urlPath);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            if (withAuthorization) {
                SharedPreferences settings = activity.getApplicationContext().getSharedPreferences("credentials", 0);
                String token = settings.getString("token", "");
                conn.setRequestProperty("Authorization", "Bearer " + token);
            }

            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            conn.setDoOutput(true);
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(body.getBytes());
            outputStream.flush();
            outputStream.close();

            // Read the response
            int statusCode = conn.getResponseCode();
            if (statusCode == 200) {
                InputStream inputStream = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                activity.runOnUiThread(() -> {
                    toastLoading.cancel();
                    toastResponse = Toasty.success(activity, "Success", Toast.LENGTH_SHORT, true);
                    toastResponse.show();
                });
                conn.disconnect();
                return new Wrapper(response.toString(), true);
            } else if (statusCode == 401) {
                SharedPreferences settings = activity.getApplicationContext().getSharedPreferences("credentials", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("login", "");
                editor.putString("password", "");
                editor.putString("token", "");
                editor.apply();

//                activity.finishAffinity();


                conn.disconnect();
                return new Wrapper("", false);
            } else {
                InputStream inputStream = conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                StringBuilder response = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                Gson gson = new Gson();
                ResponseFail responseFail = gson.fromJson(response.toString(), ResponseFail.class);


                activity.runOnUiThread(() -> {
                    toastLoading.cancel();
                    toastResponse = Toasty.error(activity, responseFail.message, Toast.LENGTH_SHORT, true);
                    toastResponse.show();
                });
                conn.disconnect();
                return new Wrapper(response.toString(), false);
            }

        } catch (IOException e) {
            activity.runOnUiThread(() -> {
                toastLoading.cancel();
                toastResponse = Toasty.error(activity, e.toString(), Toast.LENGTH_SHORT, true);
                toastResponse.show();
            });
            return new Wrapper("", false);
        }
    }

    @Override
    protected void onPostExecute(Wrapper w) {
        super.onPostExecute(w);
        asyncCallBack.processRespond(this.requestName, w.stringResponse, w.isResponseSuccess);
    }
}