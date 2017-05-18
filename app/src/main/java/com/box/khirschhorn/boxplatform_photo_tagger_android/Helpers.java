package com.box.khirschhorn.boxplatform_photo_tagger_android;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.OkHttpClient;
import okhttp3.Response;

/**
 * Created by khirschhorn on 8/18/16.
 */
public class Helpers {
    private static final String kBaseURL = "https://box-node-photo-tagging-karljr791.c9users.io/api/accesstoken/";
    private static final String kBoxAppUserId = "283032981";
    JSONObject mJson;

    public JSONObject getAccessTokenForAppUser() {
        String url = kBaseURL + kBoxAppUserId;

        OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder()
                .url(url)
                .get()
                .build();

        //send the call
        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                JSONObject json = new JSONObject();
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                } else {
                    try {
                        //convert response body to JSON
                        mJson = new JSONObject(response.body().string());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        return mJson;
    }
}
