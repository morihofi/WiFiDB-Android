package de.morihofi.wifidb;

import android.content.Context;
import android.os.Build;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.LinkedList;
import java.util.stream.Stream;

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
    public static void appendToFile(Context c, String fileName, String content, Boolean showerrortoastonfail){
        System.out.println("Append to file " + fileName);
        File path = c.getFilesDir();
        File appendTo = new File(path, fileName);


        try (FileWriter fw = new FileWriter(appendTo, true);
                BufferedWriter bw = new BufferedWriter(fw)) {

            bw.write(content);
            bw.newLine();   // add new line, System.lineSeparator()




        } catch (Exception e) {
            if(showerrortoastonfail){
                Toast.makeText(c, "Error reading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }




    public static void appendWifiRecord(Context c, JSONObject wifirecordobj)  {


        try {
            //Check if it has GPS location found
            if(wifirecordobj.getInt("loc_lon") == 0 || wifirecordobj.getInt("loc_lat") == 0){
                return;
            }
        } catch (JSONException e) {
        }
/*
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

        orgcontentobj.put(wifirecordobj);

        writeToFile(c, "records.json", orgcontentobj.toString(), true);
*/
    appendToFile(c,"records.jsonl",wifirecordobj.toString(),false);

    }

    public static int countWiFiRecords(Context c){

        int lineCount = 0;
        File path = c.getFilesDir();
        File f = new File(path, "records.jsonl");


        try {
            LineNumberReader lineNumberReader = new LineNumberReader(new FileReader(f));
            lineNumberReader.skip(Long.MAX_VALUE);
            lineCount = lineNumberReader.getLineNumber();
            lineNumberReader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }


        return lineCount;
    }

    @Deprecated
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

    public static Boolean existsFile(Context c, String fileName){
        System.out.println("Checking if file exists: " + fileName);
        File path = c.getFilesDir();

        File f = new File(path, fileName);
        return f.exists();


    }

    public static LinkedList<String> readFromFileToLinkedList(Context c, String fileName){
        File path = c.getFilesDir();
        File file = new File(path, fileName);
        LinkedList<String> list = new LinkedList<>();

        try(BufferedReader br = new BufferedReader(new FileReader(file))) {
            for(String line; (line = br.readLine()) != null; ) {
                // process the line.
                list.addLast(line);
            }
            // line is not visible here.
        } catch (IOException e) {
            e.printStackTrace();
        }

        return list;
    }


}
