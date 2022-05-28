package de.morihofi.wifidb;

import android.content.Context;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class libfile {
    public static void writeToFile(Context c, String fileName, String content, Boolean showerrortoastonfail){
        System.out.println("Writing to file " + fileName);
        File path = c.getFilesDir();
        try {
            FileOutputStream writer = new FileOutputStream(new File(path, fileName));
            writer.write(content.getBytes());
            writer.close();
        }catch (Exception e){
            if(showerrortoastonfail){
                Toast.makeText(c, "Error writing to file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }

        }

    }
    public static String readFromFile(Context c, String fileName, Boolean showerrortoastonfail){
        System.out.println("Reading from file " + fileName);
        File path = c.getFilesDir();
       File readFrom = new File(path, fileName);
       byte[] content = new byte[(int) readFrom.length()];

       try {
           FileInputStream stream = new FileInputStream(readFrom);
           stream.read(content);
           stream.close();
           return new String(content);
       } catch (Exception e) {
           if(showerrortoastonfail){
               Toast.makeText(c, "Error reading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
           }

           return null;
        }
    }

    public static void appendWifiRecord(Context c, JSONObject wifirecordobj)  {
        String orgcontent = readFromFile(c,"records.json", false);
        JSONArray orgcontentobj;

        try {
            //Check if it has GPS location found
            if(wifirecordobj.getInt("loc_lon") == 0 || wifirecordobj.getInt("loc_lat") == 0){
                return;
            }
        } catch (JSONException e) {
        }


        if(orgcontent == null){
            orgcontentobj = new JSONArray();
        }else{
            try {
                orgcontentobj = new JSONArray(orgcontent);
            } catch (JSONException e) {
                orgcontentobj = new JSONArray();
            }
        }

        orgcontentobj.put(wifirecordobj);

        writeToFile(c, "records.json", orgcontentobj.toString(), true);
    }


    public static JSONArray getWifiRecords(Context c)  {
        String orgcontent = readFromFile(c,"records.json", false);
        JSONArray orgcontentobj;

       if(orgcontent == null){
            orgcontentobj = new JSONArray();
        }else{
            try {
                orgcontentobj = new JSONArray(orgcontent);
            } catch (JSONException e) {
                orgcontentobj = new JSONArray();
            }
        }
        return orgcontentobj;
    }

    public static void deleteFile(Context c, String fileName){
        System.out.println("Writing to file " + fileName);
        File path = c.getFilesDir();
        try {
            File f = new File(path, fileName);
            f.delete();
        }catch (Exception e){


        }

    }



}
