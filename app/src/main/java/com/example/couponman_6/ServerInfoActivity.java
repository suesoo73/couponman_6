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
    private Button btnStartServer;
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
            checkServerStatus();
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
    }

    private void initializeViews() {
        tvServerStatus = findViewById(R.id.tvServerStatus);
        tvServerAddress = findViewById(R.id.tvServerAddress);
        tvServerPort = findViewById(R.id.tvServerPort);
        tvStartTime = findViewById(R.id.tvStartTime);
        btnStartServer = findViewById(R.id.btnStartServer);
        btnStopServer = findViewById(R.id.btnStopServer);
        btnRefresh = findViewById(R.id.btnRefresh);
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
                checkServerStatus();
                Toast.makeText(ServerInfoActivity.this, "서버 상태를 새로 고침했습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
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
            boolean currentServerStatus = apiServerService.isServerRunning();
            if (currentServerStatus != isServerRunning) {
                // 상태가 변경되었으면 업데이트
                isServerRunning = currentServerStatus;
                if (isServerRunning && serverStartTime.isEmpty()) {
                    serverStartTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(new Date());
                } else if (!isServerRunning) {
                    serverStartTime = "";
                }
            }
            updateServerInfo();
        } else {
            // 서비스가 바인딩되지 않았으면 기본 상태로 설정
            isServerRunning = false;
            serverStartTime = "";
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


    @Override
    protected void onResume() {
        super.onResume();
        // Activity가 다시 활성화될 때 서버 상태 재확인
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkServerStatus();
            }
        }, 500); // 0.5초 후에 상태 확인
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