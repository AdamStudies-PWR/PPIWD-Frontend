package com.pwr.activitytracker.network;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;


import com.pwr.activitytracker.MainActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import es.dmoral.toasty.Toasty;

public class PostAsyncTask extends AsyncTask<Void, Void, Wrapper> {
    private MainActivity activity;
    private AsyncCallBack asyncCallBack;

    private String urlDomainName;
    private String urlPath;
    private String body;
    private Toast toastLoading = null;
    private Toast toastResponse = null;

    public PostAsyncTask setInstance(Context context, String urlDomainName, String urlPath, String body) {
        this.activity = (MainActivity) context;
        asyncCallBack = (AsyncCallBack) context;
        this.urlDomainName = urlDomainName;
        this.urlPath = urlPath;
        this.body = body;
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
            } else {
                activity.runOnUiThread(() -> {
                    toastLoading.cancel();
                    toastResponse = Toasty.error(activity, "Error status code: " + statusCode, Toast.LENGTH_SHORT, true);
                    toastResponse.show();
                });
                return new Wrapper("", false);
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
        asyncCallBack.processRespond(w.stringResponse, w.isResponseSuccess);
    }
}