package com.box.khirschhorn.boxplatform_photo_tagger_android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    //constants
    private static final String kBaseURL = "https://box-node-photo-tagging-karljr791.c9users.io/api/accesstoken/";
    private static final String kBoxAppUserId = "283032981";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String PREFS = "myPrefs";

    private Handler mHandler;

    TextView mTextAppUserId, mTextAppUserName, mTextStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextAppUserId = (TextView) findViewById(R.id.txtAppUserId);
        mTextAppUserName = (TextView) findViewById(R.id.txtAppUserName);
        mTextStatus = (TextView) findViewById(R.id.txtStatus);
        final Button mButtonSearchPhotos = (Button) findViewById(R.id.btnSearchPhotos);
        final Button mButtonUploadPhotos = (Button) findViewById(R.id.btnUpload);

        //set up Handler for populating UI on separate thread
        mHandler = new Handler(Looper.getMainLooper());

        // setup search button's click listener
        mButtonSearchPhotos.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                launchSearchActivity();
            }

        });

        // setup upload button's click listener
        mButtonUploadPhotos.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                launchUploadActivity();
            }

        });

        //call sample server to get access token for a given App User
        getAccessTokenforUser(kBoxAppUserId);

    }

    //function to launch search activity
    private void launchSearchActivity()
    {
        Intent intent = new Intent(MainActivity.this, SearchActivity.class);
        startActivity(intent);
        Log.i(TAG, "Launching Search Activity");
    }

    //function to launch upload activity
    private void launchUploadActivity()
    {
        Intent intent = new Intent(MainActivity.this, UploadActivity.class);
        startActivity(intent);
        Log.i(TAG, "Launching Upload Activity");
    }

    //function to get Access Token for user
    private void getAccessTokenforUser(String userId) {
        //update status text
        mTextStatus.setText("Logging in.......");

        //create the url from base + hardcoded userID
        String url = kBaseURL + userId;

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
                if (!response.isSuccessful()){
                    showToast("Unable to Login");
                    throw new IOException("Unexpected code " + response);
                }
                else {
                    try {
                        //convert response body to JSON
                        final JSONObject json = new JSONObject(response.body().string());
                        final String userId = json.getString("user_id");
                        final String userName = json.getString("name");
                        //save the token
                        saveAccessToken(json.getString("access_token"));
                        //update UI
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                mTextAppUserId.setText(userId);
                                mTextAppUserName.setText(userName);
                                mTextStatus.setText("Successfully Logged In");
                                showToast("Logged In");
                            }
                        });
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    //helper function for storing access token in shared prefs
    private void saveAccessToken(String token) {
        SharedPreferences sharedpreferences = getSharedPreferences(PREFS, 0);
        SharedPreferences.Editor edit = sharedpreferences.edit();
        edit.putString("access_token", token);
        edit.apply();
    }

    //helper function for updating UI fields
    private void updateUI(String userName, String userId) {
        mTextAppUserName.setText(userName);
        mTextAppUserId.setText(userId);
    }

    private void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, text, Toast.LENGTH_LONG).show();
            }
        });
    }
}
