package de.morihofi.wifidb.services;

import static de.morihofi.wifidb.App.CHANNEL_ID;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.List;

import de.morihofi.wifidb.Config;
import de.morihofi.wifidb.activities.MainActivity;
import de.morihofi.wifidb.R;
import de.morihofi.wifidb.utils.libfile;

public class WiFiCollectorService extends Service implements LocationListener {


    private double loc_lat = 0;
    private double loc_lon = 0;
    private int loc_radius = 0;




    public static Boolean stopflag = false;
    private WifiManager wmgr;
    private int rescan_interval = 0;
    public static Boolean scanRunning = false;
    private int nearnetworks = 0;
    public Boolean offlinemode;

    protected LocationManager locationManager;
    protected LocationListener locationListener;
    public static WebSocketClient wsclient;

    public static String masterserver_ws = "ws" + Config.getSecureCharIfNeeded() + "://" + Config.masterserver + "/api/ws"; // "ws://192.168.178.43:8080/wifidb-server/api/ws"; //
    @Deprecated
    public static String masterserver_rest =  "http" + Config.getSecureCharIfNeeded() + "://"  + Config.masterserver + "/api/recordsubmit"; // "http://192.168.178.43:8080/wifidb-server/api/recordsubmit"; //
    public static String masterserver_restJSONL =  "http" + Config.getSecureCharIfNeeded() + "://"  + Config.masterserver + "/api/recordsubmitJSONL"; // "http://192.168.178.43:8080/wifidb-server/api/recordsubmit"; //

    public static URI masterserver_ws_URI = null;
    public static Thread t1 = null;
    public static Boolean t1firstrun = true;
    public static String contributor = "";
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public static Notification getNotification(String content, Context context){
        Intent notifIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = null;

        //This fixes the crash under Android 12
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            //Android 12 or higher
            pendingIntent = PendingIntent.getActivity(context, 0, notifIntent, PendingIntent.FLAG_MUTABLE);
        }else{
            //Android 11 and lower
            pendingIntent = PendingIntent.getActivity(context, 0, notifIntent, PendingIntent.FLAG_ONE_SHOT);
        }


        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.app_service_collectionservice))
                .setContentText(content)
                .setSmallIcon(R.drawable.ic_wifi)
                .setContentIntent(pendingIntent)
                .build();

        return notification;
    }

    @SuppressLint("MissingPermission")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String iaction = intent.getStringExtra("action");





        startForeground(1, getNotification("Starting Service...",this));


        if(iaction.equals("start")){
            wmgr = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            stopflag = false;
            t1firstrun = true;
            rescan_interval = Integer.valueOf(intent.getStringExtra("rescan_interval").toString());
            offlinemode = intent.getBooleanExtra("offlinemode", false);
            contributor = intent.getStringExtra("contributor");

            startForeground(1, getNotification("Enabling Wifi...",this));
            wmgr.setWifiEnabled(true);

            try {
                masterserver_ws_URI = new URI (masterserver_ws);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            try {
                registerReceiver(mWifiScanReceiver,
                        new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            }catch (Exception e) {
                e.printStackTrace();
            }

            wsclient = new WebSocketClient(masterserver_ws_URI){
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    // send("Hello, it is me. Mario :)");
                    System.out.println("new connection opened");

                    startForeground(1, getNotification("Connected to WebSocket",getApplicationContext()));

                    if(t1firstrun){
                        t1firstrun = false;
                        t1.start();
                    }


                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("closed with exit code " + code + " additional info: " + reason);

                    startForeground(1, getNotification("Connection closed. Exit code: " + code + "; Reason: " + reason,getApplicationContext()));

                    if(stopflag){
                        startForeground(1, getNotification("Stopping service...",getApplicationContext()));
                        stopSelf();
                    }

                }

                @Override
                public void onMessage(String message) {
                    System.out.println("received message: " + message);
                }

                @Override
                public void onMessage(ByteBuffer message) {
                    System.out.println("received ByteBuffer");
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("an error occurred:" + ex);
                }
            };
            if(!offlinemode){
                startForeground(1, getNotification("Connecting...",getApplicationContext()));
                System.out.println("Conntecting to WS");
                wsclient.connect();
            }

            scanRunning = false;

            t1 = new Thread(){
                @Override
                public void run()
                {

                    while(true){

                        if(stopflag){
                            break;
                        }





                        if(wsclient.isOpen() || offlinemode){
                            String gpsstatus = "...";

                            if(loc_lat == 0 || loc_lon == 0){
                                gpsstatus = getApplicationContext().getString(R.string.status_gps_searching);
                            }else{
                                gpsstatus = String.format(getApplicationContext().getString(R.string.status_gps_ok), loc_radius);


                            }

                            String connstatus = "" ;

                            if(offlinemode){
                                connstatus = getApplicationContext().getString(R.string.offline_mode);
                            }else{

                                if(wsclient.isOpen()){
                                    connstatus = getApplicationContext().getString(R.string.status_connected_short);
                                }else{
                                    connstatus = getApplicationContext().getString(R.string.status_disconnected_short);
                                }
                            }


                            startForeground(1, getNotification(connstatus + " | " + String.format(getApplicationContext().getString(R.string.status_wifinetworks), nearnetworks) + " | GPS: " + gpsstatus, getApplicationContext()));






                        }else{
                            startForeground(1, getNotification("Connection closed. Reconnecting...",getApplicationContext()));
                            wsclient.reconnect();
                            scanRunning = false;
                        }

                       broadcast();



                        if (!scanRunning){
                            scanRunning = true;
                            System.out.println("Start Wifi scan");
                            wmgr.startScan();
                        }


                        try {
                            Thread.sleep(rescan_interval * 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
/*
                    if(!offlinemode){
                        wsclient.close();
                    }
*/



                }
            };

            if(offlinemode){
                if(t1firstrun){
                    t1firstrun = false;
                    t1.start();
                }
            }






        }else if(iaction.equals("stop")){
            stopForeground(true);
            stopflag = true;
            loc_lat = 0;
            loc_lon = 0;
            loc_radius = 0;
            broadcast();

        }



        //do the work here
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

        return START_NOT_STICKY;

    }

    private void broadcast() {
        String wsstate = "";

        if (wsclient != null) {

            if (wsclient.isOpen()) {
                wsstate = "connected";
            } else {
                wsstate = "disconnected";
            }

        }else{
            wsstate = "disconnected";
        }


        String gpsstate = "waiting";
        if(loc_lat == 0 || loc_lon == 0){
            gpsstate = "searching";
        }else{
            gpsstate = "ok";
        }

        Intent local = new Intent();
        local.setAction("wificollectorservice.to.activity.transfer");
        local.putExtra("wsstate", wsstate);
        local.putExtra("wifis", nearnetworks);
        local.putExtra("gps_state", gpsstate);
        local.putExtra("gps_lat", loc_lat);
        local.putExtra("gps_lon", loc_lon);
        local.putExtra("gps_radius", loc_radius);
        local.putExtra("is_scan_running", scanRunning);
        local.putExtra("rescan_interval", rescan_interval);
        local.putExtra("stopflag", stopflag);
        local.putExtra("offlinemode", offlinemode);


        getApplicationContext().sendBroadcast(local);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        //txtLat = (TextView) findViewById(R.id.textview1);
        //txtLat.setText("Latitude:" + location.getLatitude() + ", Longitude:" + location.getLongitude());
        loc_lat = location.getLatitude();
        loc_lon = location.getLongitude();
        loc_radius = (int) location.getAccuracy();

    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude","disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude","enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude","status");
    }




    private final BroadcastReceiver mWifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                List<ScanResult> mScanResults = wmgr.getScanResults();
                // add your logic here

                nearnetworks = mScanResults.size();



                JSONObject jobj = new JSONObject();

                try {
                    jobj.put("loc_lon", loc_lon);
                    jobj.put("loc_lat", loc_lat);
                    jobj.put("loc_radius", loc_radius);

                    jobj.put("contributor", contributor);

                    JSONArray arrnetworks = new JSONArray();


                    for (ScanResult res : mScanResults) {
                        System.out.println(res);
                        JSONObject netobj = new JSONObject();


                        netobj.put("bssid", res.BSSID);
                        netobj.put("essid", res.SSID);
                        netobj.put("signalstrenght", res.level);
                        netobj.put("frequency", res.frequency);
                        netobj.put("capabilities", res.capabilities);

                        arrnetworks.put(netobj);

                    }

                    jobj.put("networks", arrnetworks);



                    if(loc_lat == 0 || loc_lon == 0){
                        //No Position found
                    }else{
                        if(wsclient.isOpen()){
                            wsclient.send(jobj.toString());
                        }
                        if(offlinemode){
                            libfile.appendWifiRecord(getApplicationContext(), jobj);
                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

                scanRunning = false;




            }
        }
    };




}
