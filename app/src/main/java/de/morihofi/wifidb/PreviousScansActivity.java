package de.morihofi.wifidb;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

public class PreviousScansActivity extends AppCompatActivity {
    ListView lvpreviousscans;
    LinkedList<String> scans;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_previous_scans);

        lvpreviousscans = (ListView) findViewById(R.id.lvpreviousscans);

        setTitle(R.string.previousscans);

        // add back arrow to toolbar
        if (getSupportActionBar() != null){
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        scans = new LinkedList<>();
        JSONArray jsonarr = null;
        try {
            System.out.println(libfile.readFromFile(getApplicationContext(), "scans.json", false));

            jsonarr = new JSONArray(libfile.readFromFile(getApplicationContext(), "scans.json", false));


        } catch (Exception e) {
            e.printStackTrace();
            jsonarr = new JSONArray();
        }

        for (int i=0; i < jsonarr.length(); i++) {
            try {
                JSONObject jsonobj = jsonarr.getJSONObject(i);

                Long time = jsonobj.getLong("time");
                String scanid =jsonobj.getString("scanid");

                Locale locale = Locale.getDefault();
                SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
                String date = dateFormat.format(new Date((long)time*1000));

                scans.add(scanid + " (" + date + ")");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        ArrayAdapter adapter = new ArrayAdapter<String>(this,
                R.layout.listview_singleline, scans);

        lvpreviousscans.setAdapter(adapter);

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



}