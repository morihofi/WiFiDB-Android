package de.morihofi.wifidb.activities;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Locale;

import de.morihofi.wifidb.R;
import de.morihofi.wifidb.utils.libfile;

public class PreviousScansActivity extends AppCompatActivity {
    ListView lvpreviousscans;
    LinkedList<String> scans;
    ArrayAdapter adapter;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_previousscans, menu);
        return true;
    }

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
        reloadPreviousScansList(null);
        adapter = new ArrayAdapter<String>(this,
                R.layout.listview_singleline, scans);

        reloadPreviousScansList(adapter);



        lvpreviousscans.setAdapter(adapter);
    }

    private void reloadPreviousScansList(ArrayAdapter adapter) {
        scans.clear();
        JSONArray jsonArr = null;
        try {
            System.out.println(libfile.readFromFile(getApplicationContext(), "scans.json", false));

            jsonArr = new JSONArray(libfile.readFromFile(getApplicationContext(), "scans.json", false));


        } catch (Exception e) {
            e.printStackTrace();
            jsonArr = new JSONArray();
        }


        for (int i=0; i < jsonArr.length(); i++) {
            try {


                JSONObject jsonObj = jsonArr.getJSONObject(i);

                Long time = jsonObj.getLong("time");
                String scanId =jsonObj.getString("scanid");

                Locale locale = Locale.getDefault();
                SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
                String date = dateFormat.format(new Date((long)time*1000));

                scans.add(scanId + " (" + date + ")");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        if(adapter != null){
            adapter.notifyDataSetChanged();
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                super.onBackPressed();
                return true;
            case R.id.men_clearprevscanslist:

                AlertDialog.Builder builder = new AlertDialog.Builder(PreviousScansActivity.this);
                builder
                        .setMessage(R.string.msg_clearlist_text)
                        .setTitle(R.string.msg_clearlist_title)
                        .setPositiveButton(R.string.msgaction_yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                libfile.deleteFile(getApplicationContext(), "scans.json");

                                reloadPreviousScansList(adapter);

                            }
                        })
                        .setNegativeButton(R.string.msgaction_no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.dismiss();
                            }
                        });
                // Create the AlertDialog object and return it
                builder.create().show();

                return true;
        }
        return super.onOptionsItemSelected(item);
    }



}