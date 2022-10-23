package de.morihofi.wifidb.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.ITileSource;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import de.morihofi.wifidb.Config;
import de.morihofi.wifidb.R;
import de.morihofi.wifidb.Tools;
import de.morihofi.wifidb.libfile;
import de.morihofi.wifidb.libgeo;
import de.morihofi.wifidb.services.WiFiCollectorService;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView lblofflinerecords;
    Button btnstart = null;
    Button btnstop = null;
    Button btnkill = null;
    Button btnuploadofflinerecs = null;
    ProgressDialog progdlg_upload;
    BroadcastReceiver updateUIReciver;
    OkHttpClient client = new OkHttpClient();
    SharedPreferences preferences;

    TextView lbl_status_wifinetworks = null;
    TextView lbl_status_gpsacc = null;
    TextView lbl_status_rescanint = null;
    TextView lbl_status_coordinates = null;
    TextView lbl_status_status = null;
    MapView maposm = null;

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED && grantResults[2] == PackageManager.PERMISSION_GRANTED) {

                // permission was granted, yay! Do the
                // contacts-related task you need to do.
            } else {


                //set positive button
                AlertDialog alertDialog = new AlertDialog.Builder(this)
                        .setIcon(android.R.drawable.ic_dialog_alert) //set icon
                        .setTitle(R.string.msg_insufficientpermissions_title) //set title
                        .setMessage(R.string.msg_insufficientpermissions_text) //set message
                        .setPositiveButton(R.string.btn_close, (dialogInterface, i) -> {
                            //set what would happen when positive button is clicked
                            finish();
                        })
                        .show();

            }
            return;

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
/*
    public static void runJavascriptOnWebView(WebView webview, String js) {
        // before Kitkat, the only way to run javascript was to load a url that starts with "javascript:".
        // Starting in Kitkat, the "javascript:" method still works, but it expects the rest of the string
        // to be URL encoded, unlike previous versions. Rather than URL encode for Kitkat and above,
        // use the new evaluateJavascript method.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            webview.loadUrl("javascript:" + js);
        } else {
            webview.evaluateJavascript(js, null);
        }
    }
*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.men_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            //case R.id.men_clearprevscanslist:
            //    startActivity(new Intent(this, DatabaseOnlineStatusActivity.class));
            //    return true;
            case R.id.men_previousscans:
                startActivity(new Intent(this, PreviousScansActivity.class));
                return true;
            case R.id.men_findpositionbynetwork:
                startActivity(new Intent(this, NetworkPositionFinderActivity.class));
                return true;
            default:
                return true;

        }

    }

    /*
    public void InstallApp() {


        //installtion permission

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(!getPackageManager().canRequestPackageInstalls()){
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse(String.format("package:%s", getPackageName()))), 1);
            }
        }

    }
    */


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Ignore android.os.NetworkOnMainThreadException
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(this.getPackageName(), 0);
            int version = pInfo.versionCode;


            if (version < Config.min_supported_version) {

                AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                dialog.setTitle(getApplicationContext().getResources().getString(R.string.msg_apptoold_title))
                        .setIcon(R.drawable.ic_baseline_browser_updated_24)
                        .setMessage(getApplicationContext().getResources().getString(R.string.msg_apptoold_text))
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialoginterface, int i) {
                                Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
                                Bundle b = new Bundle();
                                b.putBoolean("forceupdate", true);
                                intent.putExtras(b); //Put your id to your next Intent
                                startActivity(intent);
                                finish();
                            }
                        }).show();

            }

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getApplicationContext().getPackageName();
            PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);


            if (pm.isIgnoringBatteryOptimizations(packageName)) {
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            //check if wifi scan throttle is enabled
            Boolean isthrottleenabled = false;
            try {
                if (Settings.Global.getInt(this.getContentResolver(), "wifi_scan_throttle_enabled") == 1) {
                    //check if wifi scan throttle is enabled
                    System.out.println("throttle enabled");
                    isthrottleenabled = true;
                } else {
                    //check if wifi scan throttle is disabled, all good!
                    System.out.println("throttle disabled");
                }
            } catch (Settings.SettingNotFoundException e) {
                System.out.println("throttle not found");
            }
            //First option may not work on all android devices
            if (!isthrottleenabled) {
                WifiManager wmgr = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wmgr.isScanThrottleEnabled()) {
                    isthrottleenabled = true;
                }
            }

            if (isthrottleenabled) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder
                        .setMessage(R.string.msg_scanthrottlingenabled_text)
                        .setTitle(R.string.msg_scanthrottlingenabled_title)
                        .setPositiveButton(R.string.btn_opensettings, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
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
        //webview = (WebView)  findViewById(R.id.webView);
        btnuploadofflinerecs = (Button) findViewById(R.id.btnuploadofflinerecs);

        lbl_status_wifinetworks = (TextView) findViewById(R.id.lbl_status_wifinetworks);
        lbl_status_gpsacc = (TextView) findViewById(R.id.lbl_status_gpsacc);
        lbl_status_rescanint = (TextView) findViewById(R.id.lbl_status_rescanint);
        lbl_status_coordinates = (TextView) findViewById(R.id.lbl_status_coordinates);
        lbl_status_status = (TextView) findViewById(R.id.lbl_status_status);
        maposm = (MapView) findViewById(R.id.maposm);

        String theme = preferences.getString("theme", "system");
        if (theme.equals("system")) {
            //setTheme(android.R.style.Theme);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        }
        if (theme.equals("light")) {
            //setTheme(android.R.style.Theme_Light);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        if (theme.equals("dark")) {
            //setTheme(android.R.style.Theme_Black);
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }

            if (preferences.getBoolean("useonlinemaptiles", true)) {

            maposm.setVisibility(View.VISIBLE);

            maposm.setUseDataConnection(true);
            //maposm.setBuiltInZoomControls(true);
            maposm.setMultiTouchControls(true);
            maposm.getController().setZoom(14.0);

            try {
                PackageManager pm = this.getPackageManager();
                String packageName = this.getPackageName();
                PackageInfo pInfo = pm.getPackageInfo(packageName, 0);
                String version = pInfo.versionName;
                Configuration.getInstance().setUserAgentValue(this.getPackageName() + "/" + version);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            final ITileSource tileSource = TileSourceFactory.DEFAULT_TILE_SOURCE;
            maposm.setTileSource(tileSource);

        } else {
            maposm.setVisibility(View.INVISIBLE);
        }


        ActivityCompat.requestPermissions(MainActivity.this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},
                1);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if(!getPackageManager().canRequestPackageInstalls()){
                startActivityForResult(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                        .setData(Uri.parse(String.format("package:%s", getPackageName()))), 1);
            }
        }
        //Storage Permission

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }



        //webview.getSettings().setJavaScriptEnabled(true);
        //webview.loadUrl("file:///android_asset/status.html");

        updateofflinerecordsnumber();

        InitializeServerVariables();

        //Check for existing records.json and auto migrate
        DoRecordsMigration();


        IntentFilter filter = new IntentFilter();
        filter.addAction("wificollectorservice.to.activity.transfer");
        updateUIReciver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //UI update here
                if (intent != null) {
                    //Toast.makeText(context, , Toast.LENGTH_LONG).show();
                    int wifi_networks = intent.getIntExtra("wifis", -1);
                    String gps_state = intent.getStringExtra("gps_state");
                    String wsstate = intent.getStringExtra("wsstate");
                    Double loc_lat = intent.getDoubleExtra("gps_lat", 0);
                    Double loc_lon = intent.getDoubleExtra("gps_lon", 0);
                    Boolean is_scan_running = intent.getBooleanExtra("is_scan_running", false);
                    int rescan_interval = intent.getIntExtra("rescan_interval", -1);
                    Boolean stopflag = intent.getBooleanExtra("stopflag", false);
                    Boolean offlinemode = intent.getBooleanExtra("offlinemode", true);
                    int gps_acc = intent.getIntExtra("gps_radius", 0);

                    if (stopflag) {
                        btnstart.setEnabled(true);
                        btnstop.setEnabled(false);

                        lbl_status_wifinetworks.setText(R.string.status_na);
                        lbl_status_gpsacc.setText(R.string.status_na);
                        lbl_status_rescanint.setText(R.string.status_na);
                        lbl_status_coordinates.setText(R.string.status_na);
                        lbl_status_status.setText(R.string.status_na);
                    } else {
                        btnstop.setEnabled(true);
                        btnstart.setEnabled(false);

                        lbl_status_rescanint.setText(rescan_interval + " s");
                        lbl_status_wifinetworks.setText(String.valueOf(wifi_networks));

                        //runJavascriptOnWebView(webview, "updateValue(\"val_title_scaninterval\", \"" + "(" + String.format(getApplicationContext().getResources().getString(R.string.status_scanevery_x_seconds),rescan_interval) +  ")" + "\")");

                        //runJavascriptOnWebView(webview, "updateValue(\"val_wifi\", \"" + wifi_networks + "\")");

                        if (gps_state.equals("searching")) {

                            lbl_status_gpsacc.setText(R.string.status_na);
                            lbl_status_coordinates.setText(R.string.status_gps_searching);
                            //runJavascriptOnWebView(webview, "updateValue(\"val_gps\", \"" + gps_state + "\")");
                        } else {
                            lbl_status_gpsacc.setText(gps_acc + " m");
                            //runJavascriptOnWebView(webview, "updateValue(\"val_gps\", \"" + libgeo.CoordinateString(loc_lat, loc_lon).replace("\"","\\" + "\"") + "\")");
                            lbl_status_coordinates.setText(libgeo.CoordinateString(loc_lat, loc_lon));

                            if (preferences.getBoolean("useonlinemaptiles", true)) {
                                IMapController mapController = maposm.getController();
                                //mapController.setZoom(14.0);
                                GeoPoint startPoint = new GeoPoint(loc_lat, loc_lon);
                                mapController.setCenter(startPoint);

                                maposm.getOverlays().clear();

                                Marker startMarker = new Marker(maposm);
                                startMarker.setPosition(startPoint);
                                startMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                                maposm.getOverlays().add(startMarker);
                            }

                        }

                        if (wsstate.equals("disconnected")) {


                            if (offlinemode) {
                                //runJavascriptOnWebView(webview, "updateValue(\"val_status\", \"" + getApplicationContext().getResources().getString(R.string.status_offlinewifirecording) + "\")");
                                lbl_status_status.setText(getApplicationContext().getResources().getString(R.string.status_offlinewifirecording));
                            } else {
                                //runJavascriptOnWebView(webview, "updateValue(\"val_status\", \"" + getApplicationContext().getResources().getString(R.string.status_disconnected) + "\")");
                                lbl_status_status.setText(getApplicationContext().getResources().getString(R.string.status_disconnected));
                            }


                        } else {

                            if (is_scan_running) {
                                //runJavascriptOnWebView(webview, "updateValue(\"val_status\", \"" + getApplicationContext().getResources().getString(R.string.status_scanningwifinetworks) + "\")");
                                lbl_status_status.setText(getApplicationContext().getResources().getString(R.string.status_scanningwifinetworks));
                            } else {
                                //runJavascriptOnWebView(webview, "updateValue(\"val_status\", \"" + getApplicationContext().getResources().getString(R.string.status_connected) + "\")");
                                lbl_status_status.setText(getApplicationContext().getResources().getString(R.string.status_connected));
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
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    if (!preferences.getBoolean("offline_mode", true)) {
                        showAlert(getApplicationContext().getResources().getString(R.string.msg_noonlinerecording_text), getApplicationContext().getResources().getString(R.string.msg_noonlinerecording_title));
                        return;
                    }
                }

                btnstart.setEnabled(false);
                btnstop.setEnabled(true);

                Intent serviceIntent = new Intent(v.getContext(), WiFiCollectorService.class);
                serviceIntent.putExtra("action", "start");
                serviceIntent.putExtra("offlinemode", preferences.getBoolean("offline_mode", true));
                serviceIntent.putExtra("rescan_interval", preferences.getString("general_rescan_interval", "15"));
                serviceIntent.putExtra("contributor", preferences.getString("general_contributorname", ""));


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
                serviceIntent.putExtra("action", "stop");
                startService(serviceIntent);

                // Intent serviceIntent = new Intent(v.getContext(), WiFiCollectorService.class);

                // stopService(serviceIntent);

            }
        });

        btnkill.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {


                killApp();


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
                progdlg_upload.setMessage(getApplicationContext().getResources().getString(R.string.msg_uploadrecords_text)); // Setting Message
                progdlg_upload.setTitle(R.string.msg_uploadrecords_title); // Setting Title
                progdlg_upload.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style
                progdlg_upload.setMax(100);
                progdlg_upload.show(); // Display Progress Dialog
                progdlg_upload.setCancelable(false);
                btnuploadofflinerecs.setEnabled(false);
                new Thread(() -> {
                    try {
                        // JSONArray recordsarr = libfile.getWifiRecords(getApplicationContext());

                        Thread t = new Thread() {
                            @Override
                            public void start() {


                                JSONObject serviceinfo = null;

                                try {

                                    if (Config.masterserver.equals("") || Config.masterserver == null) {
                                        throw new Exception("No Masterserver");

                                    }
                                    serviceinfo = Config.getServiceInfo();

                                    if (
                                            !serviceinfo.has("timeout_connect") ||
                                                    !serviceinfo.has("timeout_write") ||
                                                    !serviceinfo.has("timeout_read")
                                    ) {
                                        throw new Exception("No Timeouts in Server Data");
                                    }


                                } catch (Exception e) {
                                    //e.printStackTrace();
                                    runOnUiThread(() -> {
                                        progdlg_upload.dismiss();
                                        AlertDialog.Builder dialog = new AlertDialog.Builder(MainActivity.this);
                                        dialog.setTitle(getApplicationContext().getResources().getString(R.string.msg_nomasterserver_title))
                                                .setIcon(R.drawable.ic_wifi)
                                                .setMessage(getApplicationContext().getResources().getString(R.string.msg_nomasterserver_text))
                                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialoginterface, int i) {
                                                        killApp();
                                                    }
                                                }).show();
                                    });


                                }


                                if (libfile.countWiFiRecords(getApplicationContext()) != 0) {


                                    try {
                                        client = new OkHttpClient.Builder()
                                                .connectTimeout(serviceinfo.getInt("timeout_connect"), TimeUnit.SECONDS)
                                                .writeTimeout(serviceinfo.getInt("timeout_write"), TimeUnit.SECONDS)
                                                .readTimeout(serviceinfo.getInt("timeout_read"), TimeUnit.SECONDS)
                                                .build();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }


                                    File path = getApplicationContext().getFilesDir();
                                    File recordsfile = new File(path, "records.jsonl");
                                    try {

                                        RequestBody formBody = new MultipartBody.Builder()
                                                .setType(MultipartBody.FORM)
                                                .addFormDataPart("file", recordsfile.getName(),
                                                        RequestBody.create(MediaType.parse("text/plain"), recordsfile))
                                                .build();
                                        Request request = new Request.Builder()
                                                .url(WiFiCollectorService.masterserver_restJSONL)
                                                .post(formBody)
                                                .build();

                                        Response response = client.newCall(request).execute();


                                        if (response.code() != 200) {
                                            throw new Exception("Response code " + response.code() + " (" + Tools.getHTTPMessageFromStatusCode(response.code()) + ")");
                                        } else {

                                            JSONObject responsejson = new JSONObject(response.body().string());

                                            String scanid = responsejson.getString("scanid");

                                            recordsfile.delete();
                                            libfile.addScanID(getApplicationContext(), scanid);
                                            runOnUiThread(() -> {
                                                showAlert(getApplicationContext().getResources().getString(R.string.msg_uploadsuccess_text) + "\n\nScanID: " + scanid, getApplicationContext().getResources().getString(R.string.msg_uploadsuccess_title));
                                            });
                                        }


                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        progdlg_upload.dismiss();
                                        runOnUiThread(() -> {
                                            //Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                                            showAlert(getApplicationContext().getResources().getString(R.string.msg_uploadfailed_text) + "\n\nDetails:\n" + e.getMessage(), getApplicationContext().getResources().getString(R.string.msg_uploadfailed_title));
                                        });

                                    }


                                } else {
                                    runOnUiThread(() -> {
                                        progdlg_upload.dismiss();
                                        showAlert(getApplicationContext().getResources().getString(R.string.msg_nothingtoupload_text), getApplicationContext().getResources().getString(R.string.msg_nothingtoupload_title));
                                    });
                                }



                                progdlg_upload.dismiss();

                                runOnUiThread(() -> {
                                    btnuploadofflinerecs.setEnabled(true);
                                    updateofflinerecordsnumber();
                                });


                            }
                        };
                        t.start();


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });

    }

    static void killApp() {
        int id = Process.myPid();
        Process.killProcess(id);
    }

    /**
     * This function does migrate offline records from json array to jsonl format (one json object each line)
     */
    private void DoRecordsMigration() {
        //Do migration if file exists
        if (libfile.existsFile(getApplicationContext(), "records.json")) {
            ProgressDialog progressDialog;
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setMax(100); // Progress Dialog Max Value --> updated later
            progressDialog.setMessage(getApplicationContext().getResources().getString(R.string.msg_recordsmigration_text)); // Setting Message
            progressDialog.setTitle(R.string.msg_recordsmigration_title); // Setting Title
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL); // Progress Dialog Style Horizontal
            progressDialog.show(); // Display Progress Dialog
            progressDialog.setCancelable(false);
            new Thread(new Runnable() {
                @Override
                public void run() {

                    try {
                        String orgcontent = libfile.readFromFile(getApplicationContext(), "records.json", false);
                        JSONArray orgcontentobj;

                        if (orgcontent == null) {
                            orgcontentobj = new JSONArray();
                        } else {
                            try {
                                orgcontentobj = new JSONArray(orgcontent);
                            } catch (JSONException e) {
                                orgcontentobj = new JSONArray();
                            }
                        }
                        progressDialog.setMax(orgcontentobj.length()); // Progress Dialog Max Value

                        int i = 0;
                        while (true) {

                            progressDialog.setProgress(i);

                            libfile.appendToFile(getApplicationContext(), "records.jsonl", orgcontentobj.get(i).toString(), false);

                            i++;
                            if (orgcontentobj.length() == i) {
                                break;
                            }

                        }

                        progressDialog.dismiss();

                        final int i_migrated = i;
                        if (i_migrated != 0) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, String.format(getApplicationContext().getResources().getString(R.string.toast_x_records_migrated), i_migrated), Toast.LENGTH_LONG).show());
                        }

                        libfile.deleteFile(getApplicationContext(), "records.json");


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();


        }

    }

    private void InitializeServerVariables() {
        //Do variable initialization

        ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage(getApplicationContext().getResources().getString(R.string.msg_initializing_text)); // Setting Message
        progressDialog.setTitle(R.string.msg_initializing_title); // Setting Title
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Horizontal
        progressDialog.show(); // Display Progress Dialog
        progressDialog.setCancelable(false);
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    System.out.println("Get Masterserver...");
                    Config.masterserver = Config.getMasterserver();

                    //Android 4.x no tls
                    System.out.println("Get TLS support-status...");
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        Config.masterserver_usetls = false;
                    } else {
                        Config.masterserver_usetls = Config.getMasterserverTLS();
                    }

                    System.out.println("Get minimum App Service Version...");
                    Config.min_supported_version = Config.getMinSupportedVersion();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                progressDialog.dismiss();
            }
        }).start();


    }


    private void updateofflinerecordsnumber() {

        ProgressDialog progressDialog;
        progressDialog = new ProgressDialog(MainActivity.this);
        runOnUiThread(() -> {

            progressDialog.setMessage(getApplicationContext().getResources().getString(R.string.msg_updateofflinerecs_text)); // Update Offlinecounter Message
            progressDialog.setTitle(getApplicationContext().getResources().getString(R.string.msg_updateofflinerecs_title)); // Update Offlinecounter Title
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
            progressDialog.show(); // Display Progress Dialog
            progressDialog.setCancelable(false);
        });

        new Thread(new Runnable() {
            public void run() {
                try {

                    int n = libfile.countWiFiRecords(getApplicationContext());


                    runOnUiThread(() -> {
                        lblofflinerecords.setText(String.format(getApplicationContext().getResources().getString(R.string.num_offline_records), n));
                    });


                } catch (Exception e) {
                    e.printStackTrace();
                }
                progressDialog.dismiss();
            }
        }).start();


    }

    private void showAlert(String message, String title) {
        System.out.println("Show Alert: [" + title + "] with content " + message);
        runOnUiThread(() -> {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setTitle(title)
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