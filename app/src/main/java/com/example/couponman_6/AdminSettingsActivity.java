package com.example.couponman_6;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class AdminSettingsActivity extends AppCompatActivity {

    private static final String TAG = "AdminSettingsActivity";
    private Switch switchNotifications;
    private Switch switchAutoSync;
    private Switch switchKeepScreenOn;
    private EditText etAdminUserId;
    private EditText etAdminPassword;
    private EditText etParkingUrl;
    private TextView tvServerIpAddress;
    private TextView tvServerPort;
    private TextView tvServerUrl;
    private Button btnBusinessSettings;
    private Button btnSave;
    private Button btnBack;

    private SharedPreferences sharedPreferences;
    private SharedPreferences systemSettings;
    private static final int SERVER_PORT = 8080;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_settings);

        initializeViews();
        loadSettings();
        setupClickListeners();
        applyKeepScreenOnSetting();
        loadServerInfo();
    }

    private void initializeViews() {
        switchNotifications = findViewById(R.id.switchNotifications);
        switchAutoSync = findViewById(R.id.switchAutoSync);
        switchKeepScreenOn = findViewById(R.id.switchKeepScreenOn);
        etAdminUserId = findViewById(R.id.etAdminUserId);
        etAdminPassword = findViewById(R.id.etAdminPassword);
        etParkingUrl = findViewById(R.id.etParkingUrl);
        tvServerIpAddress = findViewById(R.id.tvServerIpAddress);
        tvServerPort = findViewById(R.id.tvServerPort);
        tvServerUrl = findViewById(R.id.tvServerUrl);
        btnBusinessSettings = findViewById(R.id.btnBusinessSettings);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);

        sharedPreferences = getSharedPreferences("AdminSettings", MODE_PRIVATE);
        systemSettings = getSharedPreferences("SystemSettings", MODE_PRIVATE);
    }

    private void loadSettings() {
        switchNotifications.setChecked(sharedPreferences.getBoolean("notifications", true));
        switchAutoSync.setChecked(sharedPreferences.getBoolean("auto_sync", false));
        switchKeepScreenOn.setChecked(sharedPreferences.getBoolean("keep_screen_on", true));
        etAdminUserId.setText(sharedPreferences.getString("admin_user_id", "admin"));
        etAdminPassword.setText(sharedPreferences.getString("admin_password", ""));

        // 주차등록 URL 로드
        etParkingUrl.setText(systemSettings.getString("parking_registration_url", ""));
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

        // 상단 화살표 뒤로가기 버튼 설정
        findViewById(R.id.btnBackArrow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void saveSettings() {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.putBoolean("notifications", switchNotifications.isChecked());
        editor.putBoolean("auto_sync", switchAutoSync.isChecked());
        editor.putBoolean("keep_screen_on", switchKeepScreenOn.isChecked());

        // API 인증 정보 저장
        String userId = etAdminUserId.getText().toString().trim();
        String password = etAdminPassword.getText().toString().trim();

        if (userId.isEmpty()) {
            Toast.makeText(this, "관리자 아이디를 입력해주세요.", Toast.LENGTH_SHORT).show();
            etAdminUserId.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "관리자 패스워드를 입력해주세요.", Toast.LENGTH_SHORT).show();
            etAdminPassword.requestFocus();
            return;
        }

        editor.putString("admin_user_id", userId);
        editor.putString("admin_password", password);

        editor.apply();

        // 주차등록 URL 저장 (SystemSettings에 저장)
        SharedPreferences.Editor systemEditor = systemSettings.edit();
        String parkingUrl = etParkingUrl.getText().toString().trim();
        systemEditor.putString("parking_registration_url", parkingUrl);
        systemEditor.apply();

        Log.i(TAG, "[ADMIN-AUTH] 관리자 인증 정보 저장 완료 - 아이디: " + userId);
        Log.i(TAG, "[PARKING-URL] 주차등록 URL 저장 완료 - URL: " + parkingUrl);
        Toast.makeText(this, "설정이 저장되었습니다.", Toast.LENGTH_SHORT).show();

        // 화면 잠김 방지 설정 즉시 적용
        applyKeepScreenOnSetting();
    }
    
    /**
     * 화면 잠김 방지 설정 적용
     */
    private void applyKeepScreenOnSetting() {
        try {
            boolean keepScreenOn = sharedPreferences.getBoolean("keep_screen_on", true);
            
            if (keepScreenOn) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                Log.i(TAG, "[SCREEN-SETTING] 화면 잠김 방지 활성화");
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                Log.i(TAG, "[SCREEN-SETTING] 화면 잠김 방지 비활성화");
            }
        } catch (Exception e) {
            Log.e(TAG, "[SCREEN-SETTING] 화면 잠김 방지 설정 중 오류", e);
        }
    }
    
    /**
     * 서버 정보 로드 및 표시
     */
    private void loadServerInfo() {
        // 백그라운드 스레드에서 IP 주소 가져오기
        new Thread(new Runnable() {
            @Override
            public void run() {
                final String ipAddress = getIPAddress();
                
                // UI 스레드에서 화면 업데이트
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            // IP 주소 표시
                            tvServerIpAddress.setText(ipAddress != null ? ipAddress : "알 수 없음");
                            
                            // 포트 번호 표시
                            tvServerPort.setText(String.valueOf(SERVER_PORT));
                            
                            // 서버 URL 표시
                            String serverUrl = ipAddress != null ? 
                                "http://" + ipAddress + ":" + SERVER_PORT : 
                                "http://localhost:" + SERVER_PORT;
                            tvServerUrl.setText(serverUrl);
                            
                            Log.i(TAG, "[SERVER-INFO] 서버 정보 로드 완료 - IP: " + ipAddress + ", Port: " + SERVER_PORT);
                        } catch (Exception e) {
                            Log.e(TAG, "[SERVER-INFO] 서버 정보 표시 중 오류", e);
                            tvServerIpAddress.setText("오류 발생");
                            tvServerUrl.setText("http://localhost:" + SERVER_PORT);
                        }
                    }
                });
            }
        }).start();
    }
    
    /**
     * 디바이스의 IP 주소 가져오기
     */
    private String getIPAddress() {
        try {
            // Wi-Fi IP 주소 먼저 확인
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            if (wifiManager != null) {
                int ipInt = wifiManager.getConnectionInfo().getIpAddress();
                if (ipInt != 0) {
                    String wifiIp = Formatter.formatIpAddress(ipInt);
                    Log.d(TAG, "[IP-INFO] Wi-Fi IP 주소: " + wifiIp);
                    return wifiIp;
                }
            }
            
            // 네트워크 인터페이스를 통해 IP 주소 가져오기
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        // IPv4 주소인지 확인 (IPv6 제외)
                        if (sAddr != null && sAddr.indexOf(':') < 0) {
                            Log.d(TAG, "[IP-INFO] 네트워크 인터페이스 IP 주소: " + sAddr);
                            return sAddr;
                        }
                    }
                }
            }
            
            Log.w(TAG, "[IP-INFO] IP 주소를 찾을 수 없음 - localhost 사용");
            return "127.0.0.1";
            
        } catch (Exception e) {
            Log.e(TAG, "[IP-INFO] IP 주소 가져오기 실패", e);
            return null;
        }
    }
}