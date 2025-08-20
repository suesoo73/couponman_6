package com.example.couponman_6;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ServerInfoActivity extends AppCompatActivity {

    private static final int SERVER_PORT = 8080;
    
    private TextView tvServerStatus;
    private TextView tvServerAddress;
    private TextView tvServerPort;
    private TextView tvStartTime;
    private TextView tvApiEndpoints;
    private TextView tvEmulatorInfo;
    private TextView tvPortForwardInfo;
    private Button btnStartServer;
    private Button btnPortForwardHelp;
    private Button btnStopServer;
    private Button btnRefresh;
    private Button btnBack;

    private ApiServerService apiServerService;
    private boolean isServiceBound = false;
    private boolean isServerRunning = false;
    private String serverStartTime = "";
    private Handler uiHandler;
    
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ApiServerService.LocalBinder binder = (ApiServerService.LocalBinder) service;
            apiServerService = binder.getService();
            isServiceBound = true;
            updateServerInfo();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };

    private BroadcastReceiver serverStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = intent.getStringExtra("status");
            if ("started".equals(status)) {
                isServerRunning = true;
                serverStartTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date());
            } else if ("stopped".equals(status)) {
                isServerRunning = false;
                serverStartTime = "";
            }
            updateServerInfo();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_info);

        initializeViews();
        setupClickListeners();
        
        uiHandler = new Handler(Looper.getMainLooper());
        
        // 서비스 바인딩
        Intent serviceIntent = new Intent(this, ApiServerService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        
        // 브로드캐스트 리시버 등록
        IntentFilter filter = new IntentFilter("com.example.couponman_6.SERVER_STATUS");
        registerReceiver(serverStatusReceiver, filter);
        
        // 서비스가 이미 실행 중인지 확인
        checkServerStatus();
    }

    private void initializeViews() {
        tvServerStatus = findViewById(R.id.tvServerStatus);
        tvServerAddress = findViewById(R.id.tvServerAddress);
        tvServerPort = findViewById(R.id.tvServerPort);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvApiEndpoints = findViewById(R.id.tvApiEndpoints);
        tvEmulatorInfo = findViewById(R.id.tvEmulatorInfo);
        tvPortForwardInfo = findViewById(R.id.tvPortForwardInfo);
        btnStartServer = findViewById(R.id.btnStartServer);
        btnStopServer = findViewById(R.id.btnStopServer);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnPortForwardHelp = findViewById(R.id.btnPortForwardHelp);
        btnBack = findViewById(R.id.btnBack);
        
        ImageButton btnBackArrow = findViewById(R.id.btnBackArrow);
        if (btnBackArrow != null) {
            btnBackArrow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
        }
        
        checkIfEmulator();
    }

    private void setupClickListeners() {
        btnStartServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startServer();
            }
        });

        btnStopServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopServer();
            }
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateServerInfo();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        if (btnPortForwardHelp != null) {
            btnPortForwardHelp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showPortForwardingHelp();
                }
            });
        }
    }

    private void startServer() {
        if (isServerRunning) {
            Toast.makeText(this, "서버가 이미 실행 중입니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent serviceIntent = new Intent(this, ApiServerService.class);
        serviceIntent.setAction("START_SERVER");
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        
        Toast.makeText(this, "서버를 시작하는 중...", Toast.LENGTH_SHORT).show();
    }
    
    private void checkServerStatus() {
        if (isServiceBound && apiServerService != null) {
            isServerRunning = apiServerService.isServerRunning();
            if (isServerRunning) {
                serverStartTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date());
            }
            updateServerInfo();
        }
    }

    private void stopServer() {
        if (!isServerRunning) {
            Toast.makeText(this, "서버가 실행되지 않고 있습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent serviceIntent = new Intent(this, ApiServerService.class);
        serviceIntent.setAction("STOP_SERVER");
        startService(serviceIntent);
        
        Toast.makeText(this, "서버를 중지하는 중...", Toast.LENGTH_SHORT).show();
    }

    private void updateServerInfo() {
        String ipAddress = getIPAddress();
        
        if (isServerRunning) {
            tvServerStatus.setText("실행 중");
            tvServerStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
            btnStartServer.setEnabled(false);
            btnStopServer.setEnabled(true);
        } else {
            tvServerStatus.setText("중지됨");
            tvServerStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
            btnStartServer.setEnabled(true);
            btnStopServer.setEnabled(false);
        }

        tvServerAddress.setText(ipAddress != null ? ipAddress : "IP 주소를 가져올 수 없습니다");
        tvServerPort.setText(String.valueOf(SERVER_PORT));
        tvStartTime.setText(serverStartTime.isEmpty() ? "서버가 실행되지 않음" : serverStartTime);
        
        updateApiEndpoints(ipAddress);
    }

    private void updateApiEndpoints(String ipAddress) {
        if (ipAddress == null || !isServerRunning) {
            tvApiEndpoints.setText("서버가 실행되지 않음");
            return;
        }

        String baseUrl = "http://" + ipAddress + ":" + SERVER_PORT;
        StringBuilder endpoints = new StringBuilder();
        endpoints.append("기본 정보:\n");
        endpoints.append("• ").append(baseUrl).append("/api\n\n");
        endpoints.append("쿠폰 관리:\n");
        endpoints.append("• GET ").append(baseUrl).append("/api/coupons\n");
        endpoints.append("• POST ").append(baseUrl).append("/api/coupons\n");
        endpoints.append("• GET ").append(baseUrl).append("/api/coupons/{id}\n");
        endpoints.append("• PUT ").append(baseUrl).append("/api/coupons/{id}\n\n");
        endpoints.append("쿠폰 검증:\n");
        endpoints.append("• POST ").append(baseUrl).append("/api/coupons/validate\n\n");
        endpoints.append("서버 상태:\n");
        endpoints.append("• GET ").append(baseUrl).append("/api/server/status");

        tvApiEndpoints.setText(endpoints.toString());
    }

    private String getIPAddress() {
        try {
            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                int ipInt = wifiInfo.getIpAddress();
                if (ipInt != 0) {
                    return String.format(Locale.getDefault(), "%d.%d.%d.%d",
                            (ipInt & 0xff),
                            (ipInt >> 8 & 0xff),
                            (ipInt >> 16 & 0xff),
                            (ipInt >> 24 & 0xff));
                }
            }

            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress() && !addr.isLinkLocalAddress()) {
                        String sAddr = addr.getHostAddress();
                        if (sAddr != null && sAddr.indexOf(':') < 0) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isEmulator() {
        return Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || "google_sdk".equals(Build.PRODUCT)
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu");
    }

    private void checkIfEmulator() {
        if (isEmulator()) {
            tvEmulatorInfo.setVisibility(View.VISIBLE);
            tvPortForwardInfo.setVisibility(View.VISIBLE);
            if (btnPortForwardHelp != null) {
                btnPortForwardHelp.setVisibility(View.VISIBLE);
            }
            
            String emulatorInfo = "에뮬레이터에서 실행 중";
            tvEmulatorInfo.setText(emulatorInfo);
            
            String portForwardInfo = "PC에서 접속:\n" +
                    "http://localhost:" + SERVER_PORT + "/api\n" +
                    "또는\n" +
                    "http://127.0.0.1:" + SERVER_PORT + "/api";
            tvPortForwardInfo.setText(portForwardInfo);
        } else {
            tvEmulatorInfo.setVisibility(View.GONE);
            tvPortForwardInfo.setVisibility(View.GONE);
            if (btnPortForwardHelp != null) {
                btnPortForwardHelp.setVisibility(View.GONE);
            }
        }
    }

    private void showPortForwardingHelp() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("포트포워딩 설정 방법");
        
        String message = "AVD 에뮬레이터에서 실행 중인 서버에 PC에서 접속하려면:\n\n" +
                "1. 자동 포트포워딩 (권장):\n" +
                "   서버가 실행되면 자동으로 설정됩니다.\n" +
                "   PC 브라우저에서 http://localhost:" + SERVER_PORT + " 접속\n\n" +
                "2. 수동 포트포워딩 (필요시):\n" +
                "   명령 프롬프트(CMD)에서 실행:\n" +
                "   adb forward tcp:" + SERVER_PORT + " tcp:" + SERVER_PORT + "\n\n" +
                "3. 포트포워딩 확인:\n" +
                "   adb forward --list\n\n" +
                "4. 포트포워딩 제거:\n" +
                "   adb forward --remove tcp:" + SERVER_PORT + "\n\n" +
                "참고:\n" +
                "• 에뮬레이터 IP: 10.0.2.2 (호스트 PC)\n" +
                "• localhost = 127.0.0.1 = PC 자신\n" +
                "• 서버 포트: " + SERVER_PORT;
        
        builder.setMessage(message);
        builder.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        
        builder.setNeutralButton("ADB 명령 복사", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String adbCommand = "adb forward tcp:" + SERVER_PORT + " tcp:" + SERVER_PORT;
                android.content.ClipboardManager clipboard = 
                        (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("ADB Command", adbCommand);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(ServerInfoActivity.this, "ADB 명령이 클립보드에 복사되었습니다.", Toast.LENGTH_SHORT).show();
            }
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // 언바인드 서비스
        if (isServiceBound) {
            unbindService(serviceConnection);
            isServiceBound = false;
        }
        
        // 브로드캐스트 리시버 해제
        try {
            unregisterReceiver(serverStatusReceiver);
        } catch (IllegalArgumentException e) {
            // 이미 해제된 경우 무시
        }
        
        // 서버는 Service에서 관리하므로 여기서는 중지하지 않음
        // 사용자가 명시적으로 중지하지 않는 한 계속 실행됨
    }
}