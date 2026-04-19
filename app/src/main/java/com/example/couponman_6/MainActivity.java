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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

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
        ScreenOrientationHelper.applyOrientation(this);
        setContentView(R.layout.activity_main);

        applyKeepScreenOnSetting();
        startApiServer();
        initializeViews();
        registerServerStatusReceiver();

        // QR 스캔 버튼 (메인)
        Button btnQRScan = findViewById(R.id.btnQRScan);
        btnQRScan.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, QRScanActivity.class);
            startActivity(intent);
        });

        // 햄버거 메뉴
        ImageButton btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> showPopupMenu(v));
    }

    private void showPopupMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "웹 대쉬보드");
        popup.getMenu().add(0, 2, 0, "관리자 기본설정");
        popup.getMenu().add(0, 3, 0, "서버 정보");
        popup.getMenu().add(0, 4, 0, "🚗 주차등록");
        popup.getMenu().add(0, 5, 0, "🎫 쿠폰 조회");

        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    startActivity(new Intent(this, WebDashboardActivity.class));
                    return true;
                case 2:
                    startActivity(new Intent(this, AdminSettingsActivity.class));
                    return true;
                case 3:
                    startActivity(new Intent(this, ServerInfoActivity.class));
                    return true;
                case 4:
                    openParkingRegistration();
                    return true;
                case 5:
                    startActivity(new Intent(this, CouponListActivity.class));
                    return true;
            }
            return false;
        });
        popup.show();
    }

    private void openParkingRegistration() {
        try {
            SharedPreferences systemSettings = getSharedPreferences("SystemSettings", MODE_PRIVATE);
            String parkingUrl = systemSettings.getString("parking_registration_url", "");
            if (parkingUrl == null || parkingUrl.trim().isEmpty()) {
                Toast.makeText(this, "주차등록 URL이 설정되지 않았습니다.\n관리자 기본설정에서 URL을 설정해주세요.", Toast.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(MainActivity.this, ParkingWebViewActivity.class);
            intent.putExtra("parking_url", parkingUrl);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "주차등록 화면 열기 오류", e);
            Toast.makeText(this, "주차등록 화면을 열 수 없습니다: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void applyKeepScreenOnSetting() {
        try {
            SharedPreferences adminSettings = getSharedPreferences("AdminSettings", MODE_PRIVATE);
            boolean keepScreenOn = adminSettings.getBoolean("keep_screen_on", true);
            if (keepScreenOn) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        } catch (Exception e) {
            Log.e(TAG, "[SCREEN-SETTING] 화면 잠김 방지 설정 중 오류", e);
        }
    }

    private void startApiServer() {
        try {
            String dashboardUrl = ServerAddressHelper.getDashboardUrl(this, SERVER_PORT);
            Intent serviceIntent = new Intent(this, ApiServerService.class);
            serviceIntent.setAction("START_SERVER");
            startService(serviceIntent);
            Toast.makeText(this, "외부 PC 접속 주소\n" + dashboardUrl, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e(TAG, "[SERVER-AUTO] API 서버 서비스 시작 실패", e);
            Toast.makeText(this, "API 서버 시작 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ScreenOrientationHelper.applyOrientation(this);
        applyKeepScreenOnSetting();
    }

    private void initializeViews() {
        serverStatusIndicator = findViewById(R.id.serverStatusIndicator);
        serverStatusDot = findViewById(R.id.serverStatusDot);
        serverStatusText = findViewById(R.id.serverStatusText);
    }

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
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            registerReceiver(serverStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(serverStatusReceiver, filter);
        }
    }

    private void updateServerStatusUI(boolean isRunning) {
        if (isRunning) {
            serverStatusIndicator.setVisibility(View.VISIBLE);
            serverStatusDot.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(0xFF4CAF50));
            serverStatusText.setText("API 서버");
        } else {
            serverStatusIndicator.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (serverStatusReceiver != null) {
            unregisterReceiver(serverStatusReceiver);
        }
    }
}
