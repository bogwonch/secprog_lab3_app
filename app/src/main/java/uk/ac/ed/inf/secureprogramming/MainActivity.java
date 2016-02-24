package uk.ac.ed.inf.secureprogramming;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;

/**
 * Code running as part of the apps main screen.
 */
public class MainActivity extends AppCompatActivity {
    /**
     * File referencing the current photo
     * <p/>
     * Null when no-photo currently exists.
     */
    private File currentPhoto = null;

    /**
     * Address and protocol of the server to upload to
     */
    String serverAddr = "http://infr11098.space";

    /**
     * Highly secure username and password
     */
    final static String username = "user123";
    final static String password = "dummy";

    /** Used for debugging */
    final static String TAG = "SEC_PROG";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /** Callback for when the user wants to take a picture */
    public void onTakePicture(View view) {
        this.dispatchTakePictureIntent();
    }

    /** Creates an image file on external storage to save the photo into */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  // prefix
                ".jpg",         // suffix
                storageDir      // directory
        );

        this.currentPhoto = image;
        return image;
    }

    /** ID for request to take a photo */
    static final int REQUEST_TAKE_PHOTO = 1;

    /** Requests the camera takes a photo */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Toast.makeText(getApplicationContext(),
                        "Failed to take picture",
                        Toast.LENGTH_SHORT).show();
                Log.w(TAG, "error creating image file: "+ex);
            }
            // Continue only if the File was created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    /** Callback after returning from an activity */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO
                && resultCode == RESULT_OK
                && currentPhoto != null) {
            uploadPhoto(currentPhoto);
        }
    }

    /** Uploads the photo to the server */
    private void uploadPhoto(final File photo) {
        final EditText serverAddrText = (EditText) findViewById(R.id.serverAddr);
        serverAddr = serverAddrText.getText().toString();
        new SendServerPhotoTask().execute(photo);
    }

    /** Task to perform the upload to the server */
    private class SendServerPhotoTask extends AsyncTask<File, Void, String> {
        protected String doInBackground(File... files) {
            for (File file : files) {
                Log.d(TAG, "Uploading: "+file.getAbsolutePath());
                final URL url;
                HttpURLConnection conn = null;

                try {
                    url = new URL(serverAddr +"/upload");
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestProperty("Content-Type", "image/jpeg");
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Username", new String(Base64.encode(username.getBytes(), Base64.DEFAULT)));
                    conn.setRequestProperty("Password", new String(Base64.encode(password.getBytes(), Base64.DEFAULT)));

                    final OutputStream out;
                    final InputStream photo;
                    photo = new FileInputStream(file);
                    out = new BufferedOutputStream(conn.getOutputStream());

                    Log.d(TAG, "Uploading photo");
                    final byte[] buffer = new byte[4096];
                    while (true)
                    {
                        final int length = photo.read(buffer);
                        if (length < 0) break;
                        Log.d(TAG, "Uploaded "+length+" bytes");
                        out.write(buffer);
                    }
                    try {
                        photo.close();
                    } catch (IOException ex) {
                        Log.w(TAG, "Couldn't close photo");}
                    Log.d(TAG, "Finished uploading photo");

                    final Scanner resp = new Scanner(
                            new BufferedInputStream(conn.getInputStream()));
                    if (resp.hasNextLine())
                        return resp.nextLine();
                    else
                        return "<nothing>";

                } catch (MalformedURLException e) {
                    return "Looks like that server is wrong:\n" + e.toString();
                } catch (Exception e) {
                    return "Something went wrong:\n" + e.toString();
                } finally {
                    if (conn != null)
                        conn.disconnect();
                }
            }
            return "<error>";
        }

        protected void onPostExecute(String result) {
            final TextView view = (TextView) findViewById(R.id.resultView);
            view.setText(result);
        }
    }
}
