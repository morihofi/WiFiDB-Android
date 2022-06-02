package de.morihofi.wifidb;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;

public class DatabaseOnlineStatusActivity extends AppCompatActivity {

    Button btnConnect;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_online_status);

        btnConnect = (Button) findViewById(R.id.btnConnect);



    }
}