package de.morihofi.wifidb;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Config {
    //192.168.178.43:8080/wifidb-server
    public static final String masterserver = getMasterserver(); //"tomcat.rs1.morihofi.de/wifidb-server"; //without / at the end
    public static final Boolean masterserver_usetls = getMasterserverTLS(); //false;

    public static String getSecureCharIfNeeded(){

        if (masterserver_usetls){
            return "s";
        }else{
            return "";
        }

    }


    private static OkHttpClient client = new OkHttpClient();

    public static JSONObject getServiceInfo() throws IOException, JSONException {

        try {
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url("http://data.morihofi.de/updatecheck/de.morihofi.wifidb/serviceinfo.php")
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            if (!response.isSuccessful()){
                //  throw new IOException("Unexpected code " + response);
            }

            JSONObject jobj = new JSONObject(response.body().string());


            return jobj;
        }catch(Exception ex){
            return null;
        }


    }


    public static String getMasterserver(){
        try {
            JSONObject service = getServiceInfo();


            if(service != null){
                return service.getString("masterserver");
            }else{
                return "";
            }




        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }
    public static Boolean getMasterserverTLS(){
        try {
            JSONObject service = getServiceInfo();
            if(service != null){
                return service.getBoolean("usetls");
            }else{
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }



}
