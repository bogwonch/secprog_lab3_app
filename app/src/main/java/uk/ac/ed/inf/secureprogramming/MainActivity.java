package uk.ac.ed.inf.secureprogramming;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;

public class MainActivity extends AppCompatActivity {
    private File currentPhoto = null;
    String serverAddr = "https://infr11098.space";

    final static String username = "user123";
    final static String password = "dummy";

    final static String TAG = "SEC_PROG";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Set up the trust manager */
        /*
        try {
            final TrustManager tm = new SecureProgrammingTrustManager();
            final TrustManager[] tms = new TrustManager[]{
                    new SecureProgrammingTrustManager(),
            };

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tms, null);
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(
                    new HostnameVerifier() {
                        @Override
                        public boolean verify(String hostname, SSLSession session) {
                            return true;
                        }
                    }
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        */
    }

    public void onPushMe(View view) {
        final EditText serverAddrText = (EditText) findViewById(R.id.serverAddr);
        final String serverAddr = serverAddrText.getText().toString();

        new SendServerTask().execute(serverAddr);
    }

    public void onTakePicture(View view) {
        this.dispatchTakePictureIntent();
    }

    private class SendServerTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... addrs) {
            final StringBuilder builder = new StringBuilder();

            try {
                for (String addr : addrs) {
                    final URL url = new URL(addr);
                    final HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
                    final InputStream in = new BufferedInputStream(conn.getInputStream());
                    final Scanner scanner = new Scanner(in);

                    while (scanner.hasNextLine()) {
                        final String str = scanner.nextLine();
                        builder.append(str);
                        builder.append("\n");
                    }
                }
            } catch (MalformedURLException e) {
                return e.toString();
            } catch (IOException e) {
                return e.toString();
            }

            return builder.toString();
        }

        protected void onPostExecute(String result) {
            final TextView view = (TextView) findViewById(R.id.resultView);
            view.setText(result);
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        this.currentPhoto = image;
        return image;
    }

    static final int REQUEST_TAKE_PHOTO = 1;

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
            }
            // Continue only if the File was created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO
                && resultCode == RESULT_OK
                && currentPhoto != null) {
            uploadPhoto(currentPhoto);
        }
    }

    private void uploadPhoto(final File photo) {
        final EditText serverAddrText = (EditText) findViewById(R.id.serverAddr);
        serverAddr = serverAddrText.getText().toString();
        new SendServerPhotoTask().execute(photo);
    }

    private class SendServerPhotoTask extends AsyncTask<File, Void, String> {
        protected String doInBackground(File... files) {
            for (File file : files) {
                Log.d(TAG, "Uploading: "+file.getAbsolutePath());
                final URL url;
                HttpURLConnection conn = null;

                try {
                    url = new URL(serverAddr +"/upload");
                    conn = (HttpsURLConnection) url.openConnection();
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
                    try { photo.close(); } catch(IOException ex) {}
                    Log.d(TAG, "Finished uploading photo");

                    final Scanner resp = new Scanner(
                            new BufferedInputStream(conn.getInputStream()));
                    if (resp.hasNextLine())
                        return resp.nextLine();
                    else
                        return "<nothing>";

                } catch (MalformedURLException e) {
                    return e.toString();
                } catch (IOException e) {
                    return e.toString();
                } catch (Exception e) {
                    return e.toString();
                } finally {
                    if (conn != null)
                        conn.disconnect();
                }
            }
            return "<error>";
        }

        protected void onPostExecute(String result) {
            Log.d(TAG, "Got result: "+result);
            final TextView view = (TextView) findViewById(R.id.resultView);
            view.setText(result);
        }
    }
}
