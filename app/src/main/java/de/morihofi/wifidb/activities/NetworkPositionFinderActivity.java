package de.morihofi.wifidb.activities;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

import de.morihofi.wifidb.Config;
import de.morihofi.wifidb.R;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NetworkPositionFinderActivity extends AppCompatActivity {

    private WifiManager wifiManager;
    private final int MY_PERMISSIONS_ACCESS_COARSE_LOCATION = 1;

    private double lat = 0.0;
    private double lng = 0.0;
    private int acc = 0;
    private String address = "";

    private String masterServerGetGeoLocationEndpoint = "http" + Config.getSecureCharIfNeeded() + "://" + Config.masterserver + "/api/getgeolocation";

    private ProgressDialog progdlg_status;


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_position_finder);
        setTitle(R.string.activity_networkpositionfinder);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Button btn_locatemebynetworks = (Button) findViewById(R.id.btn_locatemebynetworks);
        Button btn_showonmap = (Button) findViewById(R.id.btn_showonmap);
        TextView lbl_address_val = (TextView) findViewById(R.id.lbl_address_val);
        TextView lbl_latitude = (TextView) findViewById(R.id.lbl_latitude);
        TextView lbl_longitude = (TextView) findViewById(R.id.lbl_longitude);
        TextView lbl_accuracy = (TextView) findViewById(R.id.lbl_accuracy);
        progdlg_status = new ProgressDialog(NetworkPositionFinderActivity.this);

        final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                try {

                    JSONArray wifiAPs = new JSONArray();


                    if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())) {
                        List<ScanResult> wifiList = wifiManager.getScanResults();

                        for (ScanResult scanResult : wifiList) {
                            JSONObject network = new JSONObject();
                            network.put("macAddress", scanResult.BSSID.toUpperCase(Locale.ROOT).replace(":", "-"));
                            network.put("signalStrength", scanResult.level);
                            wifiAPs.put(network);
                        }

                    }

                    JSONObject requestObj = new JSONObject();
                    requestObj.put("wifiAccessPoints", wifiAPs);


                    System.out.println(requestObj.toString());

                    runOnUiThread(() -> {
                        progdlg_status.setMessage(getApplicationContext().getString(R.string.msg_searchposition_text_srvproc));
                    });

                    if (Config.masterserver.equals("") || Config.masterserver == null) {
                        runOnUiThread(() -> {
                            AlertDialog.Builder dialog = new AlertDialog.Builder(NetworkPositionFinderActivity.this);
                            dialog.setTitle(getApplicationContext().getResources().getString(R.string.msg_nomasterserver_title))
                                    .setIcon(R.drawable.ic_baseline_location_on_24)
                                    .setMessage(getApplicationContext().getResources().getString(R.string.msg_nomasterserver_text))
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialoginterface, int i) {
                                            MainActivity.killApp();
                                        }
                                    }).show();

                        });
                    }

                    try {

                        //Get location using main api
                        JSONObject locObj = new JSONObject(MainActivity.sendBodyOverHTTP(requestObj.toString(), masterServerGetGeoLocationEndpoint));

                        acc = locObj.getInt("accuracy");
                        lat = locObj.getJSONObject("location").getDouble("lat");
                        lng = locObj.getJSONObject("location").getDouble("lng");

                        if (acc == 0 && lat == 0.0 && lng == 0.0) {
                            runOnUiThread(() -> {
                                progdlg_status.dismiss();
                                new AlertDialog.Builder(NetworkPositionFinderActivity.this)
                                        .setTitle(R.string.msg_searchposition_nothingfound_title)
                                        .setMessage(getString(R.string.msg_searchposition_nothingfound_text))
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.btn_letsgo, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                finish();
                                            }
                                        }).show();
                            });
                        } else {

                            runOnUiThread(() -> {
                                progdlg_status.setMessage(getApplicationContext().getString(R.string.msg_searchposition_text_nominatim));
                            });

                            //Get address using nominatim
                            HttpUrl.Builder urlBuilder = HttpUrl.parse("https://nominatim.openstreetmap.org/reverse").newBuilder();
                            urlBuilder.addQueryParameter("lat", String.valueOf(lat));
                            urlBuilder.addQueryParameter("lon", String.valueOf(lng));
                            urlBuilder.addQueryParameter("format", "json");
                            String url = urlBuilder.build().toString();
                            Log.i("NetPosFind", "Get Nominatim");
                            Request request = new Request.Builder().url(url).build();
                            OkHttpClient client = new OkHttpClient();
                            try (Response response = client.newCall(request).execute()) {
                                JSONObject responseObj = new JSONObject(response.body().string());

                                address = responseObj.getString("display_name");


                            } catch (Exception ex) {
                                address = "Nominatim failed: " + ex.getMessage();
                            }


                            runOnUiThread(() -> {
                                lbl_address_val.setText(address);
                                lbl_latitude.setText(String.format(getApplicationContext().getString(R.string.lbl_latitude_val), String.valueOf(lat)));
                                lbl_longitude.setText(String.format(getApplicationContext().getString(R.string.lbl_longitude_val), String.valueOf(lng)));
                                lbl_accuracy.setText(String.format(getApplicationContext().getString(R.string.lbl_accuracy_val), String.valueOf(acc)));
                                progdlg_status.dismiss();
                                btn_showonmap.setEnabled(true);
                                btn_locatemebynetworks.setEnabled(true);
                            });


                        }

                    } catch (Exception ex) {

                        runOnUiThread(() -> {
                            AlertDialog.Builder dialog = new AlertDialog.Builder(NetworkPositionFinderActivity.this);
                            dialog.setTitle(getApplicationContext().getResources().getString(R.string.msg_error_title))
                                    .setIcon(R.drawable.ic_baseline_location_on_24)
                                    .setMessage(String.format(getApplicationContext().getResources().getString(R.string.msg_error_text), ex.getMessage()))
                                    .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialoginterface, int i) {
                                            progdlg_status.dismiss();
                                            btn_locatemebynetworks.setEnabled(true);
                                            btn_showonmap.setEnabled(false);
                                            btn_locatemebynetworks.setEnabled(true);
                                            lat = 0.0;
                                            lng = 0.0;
                                            acc = 0;
                                            address = "";
                                        }
                                    }).show();

                        });
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        try {
            registerReceiver(wifiScanReceiver,
                    new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        } catch (Exception e) {
            e.printStackTrace();
        }


        btn_locatemebynetworks.setOnClickListener(v -> {

            btn_showonmap.setEnabled(false);
            btn_locatemebynetworks.setEnabled(false);
            progdlg_status.setMax(100);
            progdlg_status.setTitle(getApplicationContext().getString(R.string.msg_searchposition_title));
            progdlg_status.setMessage(getApplicationContext().getString(R.string.msg_searchposition_text_scanwifi));
            progdlg_status.setCancelable(false);
            progdlg_status.show();


            wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (!wifiManager.isWifiEnabled()) {
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.toast_turning_wifi_on), Toast.LENGTH_LONG).show();
                wifiManager.setWifiEnabled(true);
            }

            if (ActivityCompat.checkSelfPermission(NetworkPositionFinderActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        NetworkPositionFinderActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, MY_PERMISSIONS_ACCESS_COARSE_LOCATION);
            } else {

                wifiManager.startScan();
            }
        });


        btn_showonmap.setOnClickListener(v -> {
            String uri = String.format(Locale.ENGLISH, "geo:%f,%f", lat, lng);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            NetworkPositionFinderActivity.this.startActivity(intent);

        });

    }


}