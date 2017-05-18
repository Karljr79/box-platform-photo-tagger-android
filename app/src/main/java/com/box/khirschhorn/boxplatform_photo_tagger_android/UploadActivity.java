package com.box.khirschhorn.boxplatform_photo_tagger_android;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.box.androidsdk.content.BoxApiMetadata;
import com.box.androidsdk.content.models.BoxMetadata;
import com.box.androidsdk.content.requests.BoxRequestsMetadata;


import com.box.androidsdk.content.BoxApiFile;
import com.box.androidsdk.content.BoxException;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.requests.BoxRequestUpload;
import com.box.androidsdk.content.models.BoxFile;
import com.box.androidsdk.content.requests.BoxRequestsFile;
import com.clarifai.api.ClarifaiClient;
import com.clarifai.api.RecognitionRequest;
import com.clarifai.api.RecognitionResult;
import com.clarifai.api.Tag;
import com.clarifai.api.exception.ClarifaiException;

import com.box.androidsdk.content.auth.BoxAuthentication;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.LinkedHashMap;

public class UploadActivity extends AppCompatActivity implements BoxAuthentication.AuthenticationRefreshProvider {
    private static final int CODE_PICK = 1;
    private static final String TAG = UploadActivity.class.getSimpleName();
    private static final String PREFS = "myPrefs";

    Helpers helpers = new Helpers();

    //Box Client Setup
    BoxSession mSession = null;

    //Clarifai API Credentials
    private final String kClarifaiClientId = "ybnsuqot6fy_wIKEHGlpgGMz_YikSoIURHaUeJtZ";
    private final String kClarifaiClientSecret = "HKGI0NXio_lIMLLSD4vrdOzfoc5g7nUC7YtmICXV";
    private final ClarifaiClient client = new ClarifaiClient(kClarifaiClientId, kClarifaiClientSecret);

    //UI Components
    private Button btnUpload, btnTakePhoto, btnChoosePhoto;
    private TextView txtMetadata;
    private ImageView imageView;


    private Uri imageUri;
    private byte[] imageToUpload;
    private String mMetadataTags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        //UI
        txtMetadata = (TextView) findViewById(R.id.txtMetaData);
        btnChoosePhoto = (Button) findViewById(R.id.btnChoosePhoto);
        btnTakePhoto = (Button) findViewById(R.id.btnTakePhoto);
        btnUpload = (Button) findViewById(R.id.btnUpload);
        imageView = (ImageView) findViewById(R.id.image_view);

        //init Box Session
        initializeBoxClient();

        //set up button listeners
        btnChoosePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent intent = new Intent(Intent.ACTION_GET_CONTENT, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intent, CODE_PICK);
            }
        });

        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadImageToBox(imageToUpload);
            }
        });
    }

    private void initializeBoxClient() {
        BoxAuthentication.BoxAuthenticationInfo info = new BoxAuthentication.BoxAuthenticationInfo();

        SharedPreferences sharedpreferences = getSharedPreferences(PREFS, 0);
        String token = sharedpreferences.getString("access_token", null);
        info.setAccessToken(token);

        //initialize the Box Session
        mSession = new BoxSession(getApplicationContext(), info, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == CODE_PICK && resultCode == RESULT_OK) {
            // The user picked an image. Send it to Clarifai for recognition.
            Log.d(TAG, "User picked image: " + intent.getData());
            imageUri = intent.getData();
            //lets save the full res image for later upload
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                imageToUpload = convertImageToJpeg(bitmap);
            } catch (IOException e) {
                Log.e(TAG, "Error loading image: " + imageUri, e);
            }
            //Update the image view with a resized bitmap
            Bitmap bitmap = loadDisplayBitmapFromUri(intent.getData());
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                showToast("Recognizing Image...");
                btnChoosePhoto.setEnabled(false);
                btnTakePhoto.setEnabled(false);

                // Run recognition on a background thread since it makes a network call.
                new AsyncTask<Bitmap, Void, RecognitionResult>() {
                    @Override protected RecognitionResult doInBackground(Bitmap... bitmaps) {
                        return recognizeBitmap(bitmaps[0]);
                    }
                    @Override protected void onPostExecute(RecognitionResult result) {
                        updateUIForResult(result);
                    }
                }.execute(bitmap);
            } else {
                showToast("Unable to load selected image.");
            }
        }
    }

    @Override
    public boolean launchAuthUi(String userId, BoxSession session) {
        // return true if developer wishes to launch their own activity to interact with user for login.
        // Activity should call BoxAuthentication. BoxAuthentication.getInstance().onAuthenticated() or onAuthenticationFailure() as appropriate.
        return true;
    }

    @Override
    public BoxAuthentication.BoxAuthenticationInfo refreshAuthenticationInfo(BoxAuthentication.BoxAuthenticationInfo info) throws BoxException {
//        SharedPreferences sharedpreferences = getSharedPreferences(PREFS, 0);
//        String token = sharedpreferences.getString("access_token", null);
//        info.setAccessToken(token);
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

    /** Loads a Bitmap from a content URI to be displayed on screen. */
    private Bitmap loadDisplayBitmapFromUri(Uri uri) {
        try {
            // The image may be large. Load an image that is sized for display. This follows best
            // practices from http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, opts);
            int sampleSize = 1;
            while (opts.outWidth / (2 * sampleSize) >= imageView.getWidth() &&
                    opts.outHeight / (2 * sampleSize) >= imageView.getHeight()) {
                sampleSize *= 2;
            }

            opts = new BitmapFactory.Options();
            opts.inSampleSize = sampleSize;
            return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri), null, opts);
        } catch (IOException e) {
            Log.e(TAG, "Error loading image: " + uri, e);
        }
        return null;
    }

    //Converts the image to a full resolution Jpeg
    private byte[] convertImageToJpeg(Bitmap bitmap) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        byte[] jpeg = out.toByteArray();
        return jpeg;
    }

    //Scale down image for transport to Clarifai
    private Bitmap scaleDownBitmap(Bitmap bitmap) {
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 320,
                320 * bitmap.getHeight() / bitmap.getWidth(), true);
        return scaled;
    }

    /** Sends the given bitmap to Clarifai for recognition and returns the result. */
    private RecognitionResult recognizeBitmap(Bitmap bitmap) {
        try {
            // Scale down the image. This step is optional. However, sending large images over the
            // network is slow and  does not significantly improve recognition performance.
            Bitmap scaled = scaleDownBitmap(bitmap);

            byte[] jpeg = convertImageToJpeg(scaled);

            // Send the JPEG to Clarifai and return the result.
            return client.recognize(new RecognitionRequest(jpeg)).get(0);
        } catch (ClarifaiException e) {
            Log.e(TAG, "Clarifai error", e);
            return null;
        }
    }

    /** Updates the UI by displaying tags for the given result. */
    private void updateUIForResult(RecognitionResult result) {
        if (result != null) {
            if (result.getStatusCode() == RecognitionResult.StatusCode.OK) {
                // Display the list of tags in the UI.
                StringBuilder builder = new StringBuilder();
                for (Tag tag : result.getTags()) {
                    builder.append(builder.length() > 0 ? ", " : "").append(tag.getName());
                }
                mMetadataTags = builder.toString();
                txtMetadata.setText("Tags:\n" + mMetadataTags);
                btnUpload.setEnabled(true);
            } else {
                Log.e(TAG, "Clarifai: " + result.getStatusMessage());
                txtMetadata.setText("Sorry, there was an error recognizing your image.");
            }
        } else {
            txtMetadata.setText("Sorry, there was an error recognizing your image.");
        }
        btnChoosePhoto.setEnabled(true);
        btnTakePhoto.setEnabled(true);
    }

    // Uploads the photo to Box
    private void uploadImageToBox(byte[] image) {
        final byte[] uploadImage = image;
        new Thread() {
            @Override
            public void run() {
                try {
                    //add a timestamp to the filename to avoid duplicates
                    String prefix = "BoxPlatformUpload_";
                    String fileName = prefix + new Date().getTime() + ".jpg";

                    InputStream stream = new ByteArrayInputStream(uploadImage);

                    BoxApiFile fileApi = new BoxApiFile(mSession);
                    BoxRequestsFile.UploadFile request = fileApi.getUploadRequest(stream, fileName, "0");
                    final BoxFile uploadedFileInfo = request.send();
                    showToast("Uploaded: " + uploadedFileInfo.getName());
                    //Now that the file has been uploaded, set the metadata
                    setMetadataOnFile(uploadedFileInfo);
                } catch (BoxException e) {
                    e.printStackTrace();
                    showToast("Error Uploading File to Box!");
                }
            }
        }.start();
    }

    //Sets Metadata on image after uploading to Box
    private void setMetadataOnFile(BoxFile file) {
        final BoxFile metadataFile = file;
        //set up the map that we will use to set metadata values
        final LinkedHashMap map = new LinkedHashMap();
        map.put("tags", mMetadataTags);
        new Thread() {
            @Override
            public void run() {
                try {
                    BoxApiMetadata metadataApi = new BoxApiMetadata(mSession);
                    BoxRequestsMetadata.AddFileMetadata req = metadataApi.getAddMetadataRequest(metadataFile.getId(),map,"enterprise","photoUploads");
                    final BoxMetadata m = req.send();
                    showToast("Metadata set using template: " + m.getTemplate());
                } catch (BoxException e) {
                    //e.printStackTrace();
                    e.getAsBoxError();
                    showToast("Error Setting Metadata on File!" + e.toString());
                }
            }
        }.start();
    }

    private void showToast(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(UploadActivity.this, text, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
