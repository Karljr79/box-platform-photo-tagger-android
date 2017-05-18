package com.box.khirschhorn.boxplatform_photo_tagger_android;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxApiSearch;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.auth.BoxAuthentication;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.box.androidsdk.content.requests.BoxRequestsSearch;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;

public class SearchActivity extends AppCompatActivity implements BoxAuthentication.AuthenticationRefreshProvider {

    //UI
    private EditText mSearchText;
    private Button mSearchButton;
    private BoxSession mSession = null;

    Helpers helpers = new Helpers();
    private static final String PREFS = "myPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mSearchText = (EditText) findViewById(R.id.txtSearch);
        mSearchButton = (Button) findViewById(R.id.btnSearch);

        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mSearchText.getText().length() < 1) {
                    showToast("Please enter text to search");
                }
                else {

                }
            }
        });

        initializeBoxClient();
    }

    private void initializeBoxClient() {
        BoxAuthentication.BoxAuthenticationInfo info = new BoxAuthentication.BoxAuthenticationInfo();

        SharedPreferences sharedpreferences = getSharedPreferences(PREFS, 0);
        String token = sharedpreferences.getString("access_token", null);
        info.setAccessToken(token);

        //initialize the Box Session
        mSession = new BoxSession(getApplicationContext(), info, this);
    }

    private void performBoxSearch(String searchText) {
        final String text = searchText;
        new Thread() {
            @Override
            public void run() {
                try {
                    BoxApiSearch searchApi = new BoxApiSearch(mSession);
                    BoxRequestsSearch.Search search = searchApi.getSearchRequest(text);
                    search.send();
                } catch (BoxException e) {
                    e.printStackTrace();
                    showToast("Error Uploading File to Box!");
                }
            }
        }.start();
    }

    private void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(SearchActivity.this, text, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public boolean launchAuthUi(String userId, BoxSession session) {
        // return true if developer wishes to launch their own activity to interact with user for login.
        // Activity should call BoxAuthentication. BoxAuthentication.getInstance().onAuthenticated() or onAuthenticationFailure() as appropriate.
        return true;
    }

    @Override
    public BoxAuthentication.BoxAuthenticationInfo refreshAuthenticationInfo(BoxAuthentication.BoxAuthenticationInfo info) throws BoxException {
        JSONObject res = helpers.getAccessTokenForAppUser();
        String token = null;

        try {
            token = res.getString("access_token");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        info.setAccessToken(token);
        return info;
    }
}
