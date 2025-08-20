package com.example.couponman_6;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnAdminSettings = findViewById(R.id.btnAdminSettings);
        Button btnQRScan = findViewById(R.id.btnQRScan);
        Button btnServerInfo = findViewById(R.id.btnServerInfo);

        btnAdminSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AdminSettingsActivity.class);
                startActivity(intent);
            }
        });

        btnQRScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, QRScanActivity.class);
                startActivity(intent);
            }
        });

        btnServerInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ServerInfoActivity.class);
                startActivity(intent);
            }
        });
    }
}