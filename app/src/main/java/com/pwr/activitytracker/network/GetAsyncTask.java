package com.pwr.activitytracker.network;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.pwr.activitytracker.MainActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import es.dmoral.toasty.Toasty;

public class GetAsyncTask extends AsyncTask<Void, Void, Wrapper> {
    private MainActivity activity;
    private AsyncCallBack asyncCallBack;

    private String urlDomainName;
    private String urlPath;
    private Toast toastLoading = null;
    private Toast toastResponse = null;

    public GetAsyncTask setInstance(Context context, String urlDomainName, String urlPath) {
        this.activity = (MainActivity) context;
        asyncCallBack = (AsyncCallBack) context;
        this.urlDomainName = urlDomainName;
        this.urlPath = urlPath;
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
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

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
