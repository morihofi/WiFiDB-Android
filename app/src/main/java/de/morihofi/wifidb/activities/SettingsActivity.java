package de.morihofi.wifidb.activities;

import static android.os.Environment.getExternalStorageDirectory;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentManager;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

import de.morihofi.wifidb.BuildConfig;
import de.morihofi.wifidb.Config;
import de.morihofi.wifidb.R;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SettingsActivity extends AppCompatActivity {

    TextView lblVersion = null;
    Button btnSearchForUpdates = null;




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);


        Bundle b = getIntent().getExtras();
        boolean forceUpdate = false; // or other values
        if(b != null) {
            forceUpdate = b.getBoolean("forceupdate");
        }

        if(!forceUpdate){
            //Normal open
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            //Display settings
            if (savedInstanceState == null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.settings, new SettingsFragment())
                        .commit();
            }

        }







        lblVersion = (TextView) findViewById(R.id.lblVersion);
        btnSearchForUpdates = (Button) findViewById(R.id.btnSearchForUpdates);

        lblVersion.setText(String.format(getString(R.string.version), BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")"));
        btnSearchForUpdates.setOnClickListener((e) -> {
            btnSearchForUpdates.setEnabled(false);
            checkForUpdates(SettingsActivity.this, () -> {
                btnSearchForUpdates.setEnabled(true);
            });
        });

    }

    public void checkForUpdates(Context context, Runnable doAfter){
        ProgressDialog progressDialog = new ProgressDialog(context);
        OkHttpClient client = new OkHttpClient();
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL); // Progress Dialog Style
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);

        ProgressDialog updateSearchProgressDialog = new ProgressDialog(context);
        updateSearchProgressDialog.setMessage(context.getApplicationContext().getResources().getString(R.string.msg_searchforupdates_text)); // Setting Message
        updateSearchProgressDialog.setTitle(R.string.msg_searchforupdates_title); // Setting Title
        updateSearchProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style
        updateSearchProgressDialog.setMax(100);
        updateSearchProgressDialog.setCancelable(false);
        updateSearchProgressDialog.show();


        Thread updaterThread = new Thread() {
            @Override
            public void run() {
                String filename;
                Request request = new Request.Builder()
                        .url("http" + Config.getSecureCharIfNeeded() + "://data.morihofi.de/updatecheck/de.morihofi.wifidb/versioncheck.php")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    JSONObject o = new JSONObject(response.body().string());

                    runOnUiThread(() -> {
                        updateSearchProgressDialog.dismiss();

                    });

                    if (o.getInt("serverstatus") == 1) {
                        //Server is available


                        if (o.getInt("version") != BuildConfig.VERSION_CODE) {


                            Log.i("Settings", "Server is available");
                            String updateUrl = o.getString("downloadurl");
                            filename = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + BuildConfig.APPLICATION_ID + ".apk";

                            File filenameFile = new File(filename);
                            if (filenameFile.exists()) {
                                filenameFile.delete();
                            }

                            runOnUiThread(() -> {
                                progressDialog.setIcon(R.drawable.ic_baseline_browser_updated_24);
                                progressDialog.setTitle(R.string.msg_update_downloading_title);
                                progressDialog.setMessage(context.getString(R.string.msg_update_downloading_text));
                                progressDialog.show(); // Display Progress Dialog
                            });

                            try {
                                Log.i("Settings", "Downloading Update");

                                URL url = new URL(updateUrl);
                                HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
                                long completeFileSize = httpConnection.getContentLength();

                                java.io.BufferedInputStream in = new java.io.BufferedInputStream(httpConnection.getInputStream());
                                java.io.FileOutputStream fos = new java.io.FileOutputStream(filename);
                                java.io.BufferedOutputStream bout = new BufferedOutputStream(
                                        fos, 1024);
                                byte[] data = new byte[1024];
                                long downloadedFileSize = 0;
                                int x = 0;
                                while ((x = in.read(data, 0, 1024)) >= 0) {
                                    downloadedFileSize += x;

                                    // calculate progress
                                    final int currentProgress = (int) ((((double) downloadedFileSize) / ((double) completeFileSize)) * 100000d);

                                    runOnUiThread(() -> {
                                        progressDialog.setProgress(currentProgress);
                                    });

                                    bout.write(data, 0, x);
                                }
                                bout.close();
                                in.close();
                            } catch (Exception e) {
                                Log.e("Settings", "Update exception", e);
                                runOnUiThread(() -> {
                                    AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                                    alertDialog.setTitle(R.string.msg_update_exception_title);
                                    alertDialog.setMessage(String.format(context.getString(R.string.msg_update_exception_text), e.getMessage()));
                                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                            new DialogInterface.OnClickListener() {
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.dismiss();
                                                }
                                            });
                                    doAfter.run();
                                    progressDialog.dismiss();
                                    alertDialog.show();
                                });
                            }


                            if (Build.VERSION.SDK_INT >= 24) {
                                installAPK(filename,context);
                            } else {
                                // Android 4 to Android 7

                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setDataAndType(Uri.fromFile(new File(filename)), "application/vnd.android.package-archive");
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // without this flag android returned a intent error!
                                context.startActivity(intent);
                            }

                        }else{
                            runOnUiThread(() -> {
                                AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                                alertDialog.setTitle(R.string.msg_update_uptodate_title);
                                alertDialog.setMessage(context.getString(R.string.msg_update_uptodate_text));
                                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        });
                                doAfter.run();
                                progressDialog.dismiss();
                                alertDialog.show();
                            });
                        }

                    }
                    if (o.getInt("serverstatus") == 2) {
                        //Server is under maintenance
                        Log.i("Settings", "Server is under maintenance");


                        runOnUiThread(() -> {
                            AlertDialog alertDialog = new AlertDialog.Builder(context).create();
                            alertDialog.setTitle(R.string.msg_update_maintenance_title);
                            alertDialog.setMessage(context.getString(R.string.msg_update_maintenance_text));
                            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    });
                            doAfter.run();
                            progressDialog.dismiss();
                            alertDialog.show();
                        });


                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }


                runOnUiThread(() -> {
                    doAfter.run();
                    progressDialog.dismiss();
                });
            }
        };
        updaterThread.start();


    }

    private void installAPK(String PATH,Context context) {


        File file = new File(PATH);
        if (file.exists()) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uriFromFile(getApplicationContext(), new File(PATH)), "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                context.getApplicationContext().startActivity(intent);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
                Log.e("TAG", "Error in opening the file!");
            }
        } else {
            Toast.makeText(getApplicationContext(), "installing", Toast.LENGTH_LONG).show();
        }
    }

    private Uri uriFromFile(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
        } else {
            return Uri.fromFile(file);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);


            EditTextPreference general_rescan_interval = findPreference("general_rescan_interval");
            if (general_rescan_interval != null) {
                general_rescan_interval.setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                    @Override
                    public void onBindEditText(@NonNull EditText editText) {
                        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                    }
                });
            }

        }
    }
}