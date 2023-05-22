package de.morihofi.wifidb.activities;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.preference.EditTextPreference;
import androidx.preference.PreferenceFragmentCompat;

import org.json.JSONObject;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

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
        if (b != null) {
            forceUpdate = b.getBoolean("forceupdate");
        }

        if (!forceUpdate) {
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


            checkForUpdates(SettingsActivity.this, () -> {
                //Run After
                btnSearchForUpdates.setEnabled(true);
            }, () -> {
                //Run Before
                btnSearchForUpdates.setEnabled(false);
            });

        });

    }


    void checkForUpdates(Context context, Runnable doAfter, Runnable doBefore) {

        if(doBefore != null){
            doBefore.run();
        }

        AtomicBoolean updateSearchCanceled = new AtomicBoolean(false);

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
        updateSearchProgressDialog.setCancelable(true);
        updateSearchProgressDialog.setOnCancelListener(dialogInterface -> {
            dialogInterface.dismiss();
            updateSearchCanceled.set(true);
        });
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

                    if(updateSearchCanceled.get()){
                        return;
                    }

                    runOnUiThread(updateSearchProgressDialog::dismiss);

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

                            DownloadManager.Request dlRequest = new DownloadManager.Request(Uri.parse(updateUrl));
                            dlRequest.setTitle(getString(R.string.dlman_update_title));
                            dlRequest.setDescription(getString(R.string.dlman_update_text));
                            String apkPath = "wifidb.apk";
                            dlRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apkPath);

                            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                            long downloadId = downloadManager.enqueue(dlRequest);

                            // Download abgeschlossen abwarten
                            boolean downloading = true;
                            boolean failed = false;
                            while (downloading) {
                                DownloadManager.Query query = new DownloadManager.Query();
                                query.setFilterById(downloadId);
                                android.database.Cursor cursor = downloadManager.query(query);
                                if (cursor.moveToFirst()) {

                                    int statusColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                                    int status = cursor.getInt(statusColumnIndex);
                                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                        downloading = false;
                                    }
                                    if (status == DownloadManager.STATUS_FAILED) {
                                        downloading = false;
                                        failed = true;
                                    }

                                    int totalBytesColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
                                    int downloadedBytesColumnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);

                                    int totalBytes = cursor.getInt(totalBytesColumnIndex);
                                    int downloadedBytes = cursor.getInt(downloadedBytesColumnIndex);

                                    progressDialog.setMax(totalBytes);
                                    progressDialog.setProgress(downloadedBytes);



                                }
                                cursor.close();
                            }

                            if(!failed){
                                // APK-Datei installieren
                                String apkFilePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + apkPath;
                                installAPK(apkFilePath, context);

                            }else{
                                AlertDialog alert = new AlertDialog.Builder(SettingsActivity.this)
                                        .setTitle(getString(R.string.msg_updatedlfailed_title))
                                        .setMessage(getString(R.string.msg_updatedlfailed_text))
                                        .setNeutralButton("OK", (dialogInterface, i) -> {
                                            dialogInterface.dismiss();
                                        })
                                        .create();
                                alert.show();

                            }

                        } else {
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

    private void installAPK(String PATH, Context context) {

        if (Build.VERSION.SDK_INT >= 24) {


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


        } else {
            // Android 4 to Android 7

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(new File(PATH)), "application/vnd.android.package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // without this flag android returned a intent error!
            context.startActivity(intent);
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