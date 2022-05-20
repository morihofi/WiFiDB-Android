package net.wifidb.android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Process;
import android.os.StrictMode;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity{

    private TextView lblofflinerecords;
    Button btnstart = null;
    Button btnstop = null;
    Button btnkill = null;
    WebView webview = null;
    Button btnuploadofflinerecs = null;
    ProgressDialog progdlg_upload;
    BroadcastReceiver updateUIReciver;
    OkHttpClient client = new OkHttpClient();
    SharedPreferences preferences;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.



                } else {


                    AlertDialog alertDialog = new AlertDialog.Builder(this)
                            .setIcon(android.R.drawable.ic_dialog_alert) //set icon
                            .setTitle(R.string.msg_energysafing_title) //set title
                            .setMessage(R.string.msg_energysafing_text) //set message
                            .setPositiveButton(R.string.btn_close, new DialogInterface.OnClickListener() { //set positive button
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //set what would happen when positive button is clicked
                                    finish();
                                }
                            })
                            .show();

                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public static void runJavascriptOnWebView(WebView webview, String js) {
        // before Kitkat, the only way to run javascript was to load a url that starts with "javascript:".
        // Starting in Kitkat, the "javascript:" method still works, but it expects the rest of the string
        // to be URL encoded, unlike previous versions. Rather than URL encode for Kitkat and above,
        // use the new evaluateJavascript method.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            webview.loadUrl("javascript:" + js);
        } else {/* from wwwjava2s.com */
            webview.evaluateJavascript(js, null);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch(item.getItemId()){
            case R.id.men_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return true;

        }

    }

    public void InstallApp() {


//installtion permission

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(!getPackageManager().canRequestPackageInstalls()){
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse(String.format("package:%s", getPackageName()))), 1);
            }
        }

    }

    void installAPK(){

        File file = new File(MainActivity.this.getFilesDir(),"update.apk");
        if(file.exists()) {
            Uri fileProvider = FileProvider.getUriForFile(getApplicationContext(),getApplicationContext().getPackageName() + ".provider",file);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(fileProvider, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                getApplicationContext().startActivity(intent);
            } catch (ActivityNotFoundException e) {
                e.printStackTrace();
            }
        }else{
            Toast.makeText(getApplicationContext(),"Unable to install update",Toast.LENGTH_LONG).show();
        }


    }
    Uri uriFromFile(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
        } else {
            return Uri.fromFile(file);
        }
    }



    public void checkForUpdate() {
        Response response;

        try {
            response = client.newCall(new Request.Builder().url("http://data.morihofi.de/updatecheck/net.wifidb.android/versioncheck.php").build()).execute();
            JSONObject respobj = new JSONObject(response.body().string());
            String update_version = respobj.getString("version");
            int update_serverstatus = respobj.getInt("serverstatus");
            final String downloadurl = respobj.getString("downloadurl");
            if (update_serverstatus == 1 && !update_version.equals(BuildConfig.VERSION_NAME)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("An update is available. Please update this app to continue.")
                        .setCancelable(false)
                        .setTitle("Update available")
                        .setNegativeButton("Exit app", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                System.exit(0);
                            }
                        }).setPositiveButton("Update now", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);

                                Thread t = new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {


                                            runOnUiThread(() -> {
                                                progressDialog.setTitle("Software Update");
                                                progressDialog.setMessage("Downloading newest version... Please wait");
                                                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                                                progressDialog.setMax(100);
                                                progressDialog.setCancelable(false);
                                                progressDialog.show();
                                            });


                                            URL url = new URL(downloadurl);
                                            HttpURLConnection httpConnection = (HttpURLConnection) (url.openConnection());
                                            long completeFileSize = httpConnection.getContentLength();

                                            java.io.BufferedInputStream in = new java.io.BufferedInputStream(httpConnection.getInputStream());
                                            java.io.FileOutputStream fos = new java.io.FileOutputStream(new File(MainActivity.this.getFilesDir(),"update.apk"));
                                            java.io.BufferedOutputStream bout = new BufferedOutputStream(
                                                    fos, 1024);
                                            byte[] data = new byte[1024];
                                            long downloadedFileSize = 0;
                                            int x = 0;
                                            while ((x = in.read(data, 0, 1024)) >= 0) {
                                                downloadedFileSize += x;

                                                // calculate progress
                                                final double currentProgress = (Double) (((((double) downloadedFileSize)
                                                        / ((double) completeFileSize)) * 100000d) / 1000);
                                                int currentProgressInt = (int) currentProgress;
                                                // update progress bar
                                                runOnUiThread(() -> {
                                                    progressDialog.setProgress(currentProgressInt);
                                                });

                                                bout.write(data, 0, x);
                                            }
                                            bout.close();
                                            in.close();

                                            runOnUiThread(() -> {
                                                installAPK();
                                                progressDialog.dismiss();
                                            });

                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                                t.start();



                            }});
                builder.create().show();
            }
            if (response != null) {
                response.close();
                return;
            }
            return;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
            return;


        }

    }


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Ignore android.os.NetworkOnMainThreadException
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getApplicationContext().getPackageName();
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);


            if (pm.isIgnoringBatteryOptimizations(packageName)){
                System.out.println("App ignores Battery optimations");
             //   intent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);




        } else {
                System.out.println("App does not ignore Battery optimations");
             //   intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
             //   intent.setData(Uri.parse("package:" + packageName));

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder
                        .setMessage(R.string.msg_energysafing_text)
                        .setTitle(R.string.msg_energysafing_title)
                        .setPositiveButton(R.string.btn_opensettings, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                Intent myIntent = new Intent();
                                myIntent.setAction(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                                startActivity(myIntent);
                            }
                        })
                        .setNegativeButton(R.string.btn_maybelater, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                // Create the AlertDialog object and return it
                builder.create().show();


            }

        }

        // checkForUpdate();

        lblofflinerecords = (TextView) findViewById(R.id.lblofflinerecords);
        btnstart = (Button) findViewById(R.id.btnstart);
        btnstop = (Button) findViewById(R.id.btnstop);
        btnkill = (Button) findViewById(R.id.btnkill);
        webview = (WebView)  findViewById(R.id.webView);
        btnuploadofflinerecs = (Button) findViewById(R.id.btnuploadofflinerecs);



        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                1);




        webview.getSettings().setJavaScriptEnabled(true);
        webview.loadUrl("file:///android_asset/status.html");

        updateofflinerecordsnumber();


        IntentFilter filter = new IntentFilter();
        filter.addAction("wificollectorservice.to.activity.transfer");
        updateUIReciver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //UI update here
                if (intent != null){
                    //Toast.makeText(context, , Toast.LENGTH_LONG).show();
                    int wifi_networks = intent.getIntExtra("wifis", -1);
                    String gps_state = intent.getStringExtra("gps_state");
                    String wsstate = intent.getStringExtra("wsstate");
                    Double loc_lat = intent.getDoubleExtra("gps_lat", 0);
                    Double loc_lon = intent.getDoubleExtra("gps_lon", 0);
                    Boolean is_scan_running = intent.getBooleanExtra("is_scan_running", false);
                    int rescan_interval = intent.getIntExtra("rescan_interval", -1);
                    Boolean stopflag = intent.getBooleanExtra("stopflag", false);
                    Boolean offlinemode = intent.getBooleanExtra("offlinemode", false);

                    if(stopflag){
                        btnstart.setEnabled(true);
                        btnstop.setEnabled(false);


                        runJavascriptOnWebView(webview, "updateValue(\"val_title_scaninterval\", \"" + "()" + "\")");

                        runJavascriptOnWebView(webview, "updateValue(\"val_status\", \"" + "..." + "\")");
                        runJavascriptOnWebView(webview, "updateValue(\"val_gps\", \"" + "..." + "\")");
                        runJavascriptOnWebView(webview, "updateValue(\"val_wifi\", \"" + "..." + "\")");
                    }else{
                        btnstop.setEnabled(true);
                        btnstart.setEnabled(false);

                        runJavascriptOnWebView(webview, "updateValue(\"val_title_scaninterval\", \"" + "(Rescan every " + rescan_interval + " sec.)" + "\")");

                        runJavascriptOnWebView(webview, "updateValue(\"val_wifi\", \"" + wifi_networks + "\")");

                        if(gps_state.equals("searching")){
                            runJavascriptOnWebView(webview, "updateValue(\"val_gps\", \"" + gps_state + "\")");
                        }else{
                            runJavascriptOnWebView(webview, "updateValue(\"val_gps\", \"" + libgeo.CoordinateString(loc_lat, loc_lon).replace("\"","\\" + "\"") + "\")");
                        }

                        if(wsstate.equals("disconnected")){

                            if(offlinemode){
                                runJavascriptOnWebView(webview, "updateValue(\"val_status\", \"" + "Offline WiFi-Recording" + "\")");
                            }else{
                                runJavascriptOnWebView(webview, "updateValue(\"val_status\", \"" + "Disconnected" + "\")");
                            }


                        }else{

                            if(is_scan_running){
                                runJavascriptOnWebView(webview, "updateValue(\"val_status\", \"" + "Scanning Wifi networks" + "\")");
                            }else{
                                runJavascriptOnWebView(webview, "updateValue(\"val_status\", \"" + "Connected to WebSocket" + "\")");
                            }

                        }

                    }









                }

            }
        };
        registerReceiver(updateUIReciver, filter);




        btnstart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // TODO Auto-generated method stub

                btnstart.setEnabled(false);
                btnstop.setEnabled(true);

                Intent serviceIntent = new Intent(v.getContext(), WiFiCollectorService.class);
                serviceIntent.putExtra("action","start");
                serviceIntent.putExtra("offlinemode", preferences.getBoolean("offline_mode",false));
                serviceIntent.putExtra("rescan_interval", preferences.getString("general_rescan_interval","1"));
                serviceIntent.putExtra("contributor", preferences.getString("general_contributorname",""));


                startService(serviceIntent);

                // \n is for new line
                //  Toast.makeText(getApplicationContext(), "Your Location is - \nLat: " + latitude + "\nLong: " + longitude, Toast.LENGTH_LONG).show();

            }
        });


        btnstop.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                btnstart.setEnabled(true);
                btnstop.setEnabled(false);


                Intent serviceIntent = new Intent(v.getContext(), WiFiCollectorService.class);
                serviceIntent.putExtra("action","stop");
                startService(serviceIntent);

               // Intent serviceIntent = new Intent(v.getContext(), WiFiCollectorService.class);

               // stopService(serviceIntent);

            }
        });

        btnkill.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {




                int id= Process.myPid();
                Process.killProcess(id);

            }
        });

        lblofflinerecords.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                updateofflinerecordsnumber();
            }
        });


        btnuploadofflinerecs.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                progdlg_upload = new ProgressDialog(MainActivity.this);
                progdlg_upload.setMessage("Please wait until all records are uploaded"); // Setting Message
                progdlg_upload.setTitle("Uploading records"); // Setting Title
                progdlg_upload.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style
                progdlg_upload.show(); // Display Progress Dialog
                progdlg_upload.setCancelable(false);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONArray recordsarr = libfile.getWifiRecords(getApplicationContext());



/*
                            for (int i=0; i < recordsarr.length(); i++) {
                                JSONObject record = null;

                                try {
                                    record = recordsarr.getJSONObject(i);


                                    String responsestr = sendBodyOverHTTP(record.toString(),WiFiCollectorService.masterserver_rest);
                                    JSONObject responseobj = new JSONObject(responsestr);


                                    if(!responseobj.getString("status").equals("ok")){
                                        throw new Exception("Server doesnt accept our request");
                                    }

                                } catch (Exception e) {

                                    showAlert("There was an error during uploading: " + e.getMessage() + "\n Please try again later.","Error");
                                    e.printStackTrace();
                                    break;
                                }

                                progdlg_upload.setProgress(i);

                            }

                            */
                           Thread t = new Thread(){
                               @Override
                               public void start(){
                                   if(libfile.getWifiRecords(getApplicationContext()).length() != 0){



                                   OkHttpClient client = new OkHttpClient.Builder()
                                           .connectTimeout(10, TimeUnit.SECONDS)
                                           .writeTimeout(30, TimeUnit.SECONDS)
                                           .readTimeout(60 * 5, TimeUnit.SECONDS)
                                           .build();


                                   File path = getApplicationContext().getFilesDir();
                                   File file = new File(path, "records.json");
                                   try {

                                       RequestBody formBody = new MultipartBody.Builder()
                                               .setType(MultipartBody.FORM)
                                               .addFormDataPart("file", file.getName(),
                                                       RequestBody.create(MediaType.parse("application/json"), file))
                                               .build();
                                       Request request = new Request.Builder()
                                               .url(WiFiCollectorService.masterserver_rest)
                                               .post(formBody)
                                               .build();

                                       Response response = client.newCall(request).execute();

                                       file.delete();
                                       runOnUiThread(() -> {
                                            showAlert("Thank you for submitting your WiFi records!", "Upload success");
                                       });

                                   } catch (Exception e) {
                                       e.printStackTrace();
                                       runOnUiThread(() -> {
                                           //Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                           showAlert("There was an error during uploading: " + e.getMessage() + "\n\nPlease check your internet connectivity and try again later.", "Upload failed");
                                       });

                                   }




                                   }else{
                                       runOnUiThread(() -> {
                                           showAlert("Before you can upload records, please collect some WiFi Networks in Offline Mode first", "Upload failed");
                                       });
                                   }

                                   progdlg_upload.dismiss();

                                 //

                                   runOnUiThread(() -> {
                                       updateofflinerecordsnumber();
                                   });

                               }
                           };
                            t.start();









                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

    }

    private void updateofflinerecordsnumber() {

        lblofflinerecords.setText(libfile.getWifiRecords(getApplicationContext()).length() + " offline records");
    }

    private void showAlert( String message , String title) {
System.out.println("Show Alert: [" + title + "] with content " + message);
        runOnUiThread(() -> {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle( title )
                    .setIcon(R.drawable.ic_wifi)
                    .setMessage(message)
//     .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//      public void onClick(DialogInterface dialoginterface, int i) {
//          dialoginterface.cancel();
//          }})
                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialoginterface, int i) {
                        }
                    }).show();
        });

    }




    public static String sendBodyOverHTTP(String body, String uri) throws IOException {
        URL url = new URL(uri);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Length", String.valueOf(body.length()));

        // Write data
        OutputStream os = connection.getOutputStream();
        os.write(body.getBytes());

        // Read response
        String responseSB = "";
        BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String line;
        while ((line = br.readLine()) != null)
            responseSB += line;

        // Close streams
        br.close();
        os.close();
        return responseSB;
    }





}