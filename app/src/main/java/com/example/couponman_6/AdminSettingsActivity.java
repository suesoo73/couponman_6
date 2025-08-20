package com.example.couponman_6;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class AdminSettingsActivity extends AppCompatActivity {

    private EditText etServerUrl;
    private EditText etApiKey;
    private EditText etTimeout;
    private EditText etApiUserId;
    private EditText etApiPassword;
    private Switch switchNotifications;
    private Switch switchAutoSync;
    private Button btnBusinessSettings;
    private Button btnSave;
    private Button btnBack;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_settings);

        initializeViews();
        loadSettings();
        setupClickListeners();
    }

    private void initializeViews() {
        etServerUrl = findViewById(R.id.etServerUrl);
        etApiKey = findViewById(R.id.etApiKey);
        etTimeout = findViewById(R.id.etTimeout);
        etApiUserId = findViewById(R.id.etApiUserId);
        etApiPassword = findViewById(R.id.etApiPassword);
        switchNotifications = findViewById(R.id.switchNotifications);
        switchAutoSync = findViewById(R.id.switchAutoSync);
        btnBusinessSettings = findViewById(R.id.btnBusinessSettings);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);

        sharedPreferences = getSharedPreferences("AdminSettings", MODE_PRIVATE);
    }

    private void loadSettings() {
        etServerUrl.setText(sharedPreferences.getString("server_url", ""));
        etApiKey.setText(sharedPreferences.getString("api_key", ""));
        etTimeout.setText(String.valueOf(sharedPreferences.getInt("timeout", 30)));
        etApiUserId.setText(sharedPreferences.getString("api_user_id", "admin"));
        etApiPassword.setText(sharedPreferences.getString("api_password", ""));
        switchNotifications.setChecked(sharedPreferences.getBoolean("notifications", true));
        switchAutoSync.setChecked(sharedPreferences.getBoolean("auto_sync", false));
    }

    private void setupClickListeners() {
        btnBusinessSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminSettingsActivity.this, BusinessSettingsActivity.class);
                startActivity(intent);
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        
        editor.putString("server_url", etServerUrl.getText().toString());
        editor.putString("api_key", etApiKey.getText().toString());
        editor.putString("api_user_id", etApiUserId.getText().toString());
        editor.putString("api_password", etApiPassword.getText().toString());
        
        try {
            int timeout = Integer.parseInt(etTimeout.getText().toString());
            editor.putInt("timeout", timeout);
        } catch (NumberFormatException e) {
            editor.putInt("timeout", 30);
        }
        
        editor.putBoolean("notifications", switchNotifications.isChecked());
        editor.putBoolean("auto_sync", switchAutoSync.isChecked());
        
        editor.apply();
        
        Toast.makeText(this, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }
}