package com.example.couponman_6;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    
    private static final String TAG = "MainActivity";
    private static final int SERVER_PORT = 8080;
    
    private LinearLayout serverStatusIndicator;
    private View serverStatusDot;
    private TextView serverStatusText;
    private BroadcastReceiver serverStatusReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 화면 잠김 방지 설정 적용
        applyKeepScreenOnSetting();
        
        // API 서버 자동 시작
        startApiServer();

        // UI 요소 초기화
        initializeViews();
        
        // 서버 상태 브로드캐스트 리시버 등록
        registerServerStatusReceiver();

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
    
    /**
     * 화면 잠김 방지 설정 적용
     */
    private void applyKeepScreenOnSetting() {
        try {
            SharedPreferences adminSettings = getSharedPreferences("AdminSettings", MODE_PRIVATE);
            boolean keepScreenOn = adminSettings.getBoolean("keep_screen_on", true);
            
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
     * API 서버를 자동으로 시작하는 메서드 (ApiServerService 사용)
     */
    private void startApiServer() {
        try {
            Log.i(TAG, "[SERVER-AUTO] API 서버 자동 시작 시도 - 포트: " + SERVER_PORT);
            
            // ApiServerService를 통해 서버 시작
            Intent serviceIntent = new Intent(this, ApiServerService.class);
            serviceIntent.setAction("START_SERVER");
            startService(serviceIntent);
            
            Toast.makeText(this, 
                "API 서버가 자동으로 시작됩니다\nhttp://localhost:" + SERVER_PORT, 
                Toast.LENGTH_LONG).show();
            
        } catch (Exception e) {
            Log.e(TAG, "[SERVER-AUTO] API 서버 서비스 시작 실패", e);
            Toast.makeText(this, "API 서버 시작 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        
        // 화면 잠김 방지 설정 재적용 (설정이 변경될 수 있으므로)
        applyKeepScreenOnSetting();
    }
    
    /**
     * UI 요소들을 초기화하는 메서드
     */
    private void initializeViews() {
        serverStatusIndicator = findViewById(R.id.serverStatusIndicator);
        serverStatusDot = findViewById(R.id.serverStatusDot);
        serverStatusText = findViewById(R.id.serverStatusText);
    }
    
    /**
     * 서버 상태 브로드캐스트 리시버를 등록하는 메서드
     */
    private void registerServerStatusReceiver() {
        serverStatusReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String status = intent.getStringExtra("status");
                if ("started".equals(status)) {
                    updateServerStatusUI(true);
                } else if ("stopped".equals(status)) {
                    updateServerStatusUI(false);
                }
            }
        };
        
        IntentFilter filter = new IntentFilter("com.example.couponman_6.SERVER_STATUS");
        
        // Android 14+ requires explicit export flag for broadcast receivers
        if (android.os.Build.VERSION.SDK_INT >= 34) { // API 34 = Android 14
            registerReceiver(serverStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(serverStatusReceiver, filter);
        }
    }
    
    /**
     * 서버 상태에 따라 UI를 업데이트하는 메서드
     */
    private void updateServerStatusUI(boolean isRunning) {
        if (isRunning) {
            serverStatusIndicator.setVisibility(View.VISIBLE);
            serverStatusDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50)); // 초록색
            serverStatusText.setText("API 서버");
        } else {
            serverStatusIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 브로드캐스트 리시버 해제
        if (serverStatusReceiver != null) {
            unregisterReceiver(serverStatusReceiver);
        }
        
        Log.i(TAG, "[SERVER-AUTO] MainActivity 종료 - 서버는 백그라운드에서 계속 실행");
    }
}