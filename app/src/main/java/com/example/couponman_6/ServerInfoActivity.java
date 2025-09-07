package com.example.couponman_6;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ServerInfoActivity extends AppCompatActivity implements WebSocketClient.WebSocketListener {

    private static final int SERVER_PORT = 8080;
    private static final String PREF_NAME = "WebSocketSettings";
    private static final String PREF_URI = "websocket_uri";
    private static final String PREF_CLIENT_ID = "client_id";
    private static final String PREF_PASSWORD = "password";
    
    private TextView tvServerStatus;
    private TextView tvServerAddress;
    private TextView tvServerPort;
    private TextView tvStartTime;
    private TextView tvWebSocketStatus;
    private EditText etWebSocketUri;
    private EditText etClientId;
    private EditText etPassword;
    private Button btnStartServer;
    private Button btnStopServer;
    private Button btnRefresh;
    private Button btnBack;
    private Button btnConnectWebSocket;
    private Button btnDisconnectWebSocket;

    private ApiServerService apiServerService;
    private boolean isServiceBound = false;
    private boolean isServerRunning = false;
    private String serverStartTime = "";
    private Handler uiHandler;
    private WebSocketClient webSocketClient;
    private SharedPreferences preferences;
    
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
        
        // Initialize WebSocket client and preferences
        webSocketClient = new WebSocketClient();
        webSocketClient.setListener(this);
        preferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // Load saved settings
        loadWebSocketSettings();
        updateWebSocketStatus();
        
        // 서비스 바인딩
        Intent serviceIntent = new Intent(this, ApiServerService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        
        // 브로드캐스트 리시버 등록
        IntentFilter filter = new IntentFilter("com.example.couponman_6.SERVER_STATUS");
        
        // Android 14+ requires explicit export flag for broadcast receivers
        if (android.os.Build.VERSION.SDK_INT >= 34) { // API 34 = Android 14
            registerReceiver(serverStatusReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(serverStatusReceiver, filter);
        }
    }

    private void initializeViews() {
        tvServerStatus = findViewById(R.id.tvServerStatus);
        tvServerAddress = findViewById(R.id.tvServerAddress);
        tvServerPort = findViewById(R.id.tvServerPort);
        tvStartTime = findViewById(R.id.tvStartTime);
        tvWebSocketStatus = findViewById(R.id.tvWebSocketStatus);
        etWebSocketUri = findViewById(R.id.etWebSocketUri);
        etClientId = findViewById(R.id.etClientId);
        etPassword = findViewById(R.id.etPassword);
        btnStartServer = findViewById(R.id.btnStartServer);
        btnStopServer = findViewById(R.id.btnStopServer);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnBack = findViewById(R.id.btnBack);
        btnConnectWebSocket = findViewById(R.id.btnConnectWebSocket);
        btnDisconnectWebSocket = findViewById(R.id.btnDisconnectWebSocket);
        
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

        btnConnectWebSocket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectWebSocket();
            }
        });

        btnDisconnectWebSocket.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnectWebSocket();
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

    private void connectWebSocket() {
        String uri = etWebSocketUri.getText().toString().trim();
        String clientId = etClientId.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        Log.d("ServerInfo", "WebSocket connect requested");
        Log.d("ServerInfo", "URI: " + uri);
        Log.d("ServerInfo", "Client ID: " + clientId);
        
        if (uri.isEmpty()) {
            Toast.makeText(this, "웹소켓 URI를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (clientId.isEmpty()) {
            Toast.makeText(this, "클라이언트 ID를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (password.isEmpty()) {
            Toast.makeText(this, "패스워드를 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!uri.startsWith("ws://") && !uri.startsWith("wss://")) {
            Toast.makeText(this, "URI는 ws:// 또는 wss://로 시작해야 합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Save settings
        saveWebSocketSettings(uri, clientId, password);
        
        // Build connection URL with query parameters
        String connectionUrl = buildConnectionUrl(uri, clientId, password);
        Log.d("ServerInfo", "Final connection URL: " + connectionUrl);
        
        // Connect
        webSocketClient.connect(connectionUrl);
        Toast.makeText(this, "웹소켓 서버에 연결 중...", Toast.LENGTH_SHORT).show();
    }
    
    private void disconnectWebSocket() {
        if (webSocketClient != null && webSocketClient.isConnected()) {
            webSocketClient.disconnect();
            Toast.makeText(this, "웹소켓 연결을 끊는 중...", Toast.LENGTH_SHORT).show();
        }
    }
    
    private String buildConnectionUrl(String baseUri, String clientId, String password) {
        String separator = baseUri.contains("?") ? "&" : "?";
        return baseUri + separator + 
               "web_server=main_web_server" +
               "&client_id=" + clientId +
               "&type=web_server" +
               "&password=" + password;
    }
    
    private void saveWebSocketSettings(String uri, String clientId, String password) {
        preferences.edit()
                .putString(PREF_URI, uri)
                .putString(PREF_CLIENT_ID, clientId)
                .putString(PREF_PASSWORD, password)
                .apply();
    }
    
    private void loadWebSocketSettings() {
        String savedUri = preferences.getString(PREF_URI, "");
        String savedClientId = preferences.getString(PREF_CLIENT_ID, "");
        String savedPassword = preferences.getString(PREF_PASSWORD, "");
        
        if (!savedUri.isEmpty()) {
            etWebSocketUri.setText(savedUri);
        }
        if (!savedClientId.isEmpty()) {
            etClientId.setText(savedClientId);
        }
        if (!savedPassword.isEmpty()) {
            etPassword.setText(savedPassword);
        }
    }
    
    private void handleApiRequest(JSONObject request) {
        try {
            int requestId = request.getInt("id");
            String method = request.getString("method");
            String endpoint = request.getString("endpoint");
            JSONObject data = request.optJSONObject("data");
            String token = request.optString("token");
            
            Log.d("ServerInfo", "Handling API request - ID: " + requestId + ", Method: " + method + ", Endpoint: " + endpoint);
            Log.d("ServerInfo", "Token: " + (token != null && !token.isEmpty() ? token.substring(0, Math.min(20, token.length())) + "..." : "null"));
            
            // Handle login request
            if ("/api/login".equals(endpoint) && "POST".equals(method) && data != null) {
                // Forward the login request to the actual ApiServer
                forwardApiRequestToLocalServer(requestId, method, endpoint, data, token);
            }
            // Handle other API endpoints - forward all to local server
            else {
                forwardApiRequestToLocalServer(requestId, method, endpoint, data, token);
            }
            
        } catch (JSONException e) {
            Log.e("ServerInfo", "Error handling API request: " + e.getMessage());
        }
    }
    
    private void forwardApiRequestToLocalServer(int requestId, String method, String endpoint, JSONObject data, String token) {
        // Run API call in background thread
        new Thread(() -> {
            try {
                // Get local server IP and port
                String serverIP = getIPAddress();
                if (serverIP == null) {
                    serverIP = "127.0.0.1";
                }
                String apiUrl = "http://" + serverIP + ":" + SERVER_PORT + endpoint;
                
                Log.d("ServerInfo", "Forwarding API request to: " + apiUrl);
                Log.d("ServerInfo", "Request data: " + (data != null ? data.toString() : "null"));
                
                URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                
                // Set request method and headers
                conn.setRequestMethod(method);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                
                // Add Authorization header if token is provided
                if (token != null && !token.isEmpty()) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                    Log.d("ServerInfo", "Added Authorization header with token");
                }
                
                conn.setDoInput(true);
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                
                // Send request body for POST/PUT methods
                if ("POST".equals(method) || "PUT".equals(method)) {
                    conn.setDoOutput(true);
                    
                    // Always send a body for POST/PUT, even if it's empty
                    String requestBody = data != null ? data.toString() : "{}";
                    Log.d("ServerInfo", "Sending request body: " + requestBody);
                    
                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = requestBody.getBytes("utf-8");
                        os.write(input, 0, input.length);
                        os.flush();
                    }
                } else {
                    // For GET, DELETE methods, don't set DoOutput
                    conn.setDoOutput(false);
                }
                
                // Get response
                int responseCode = conn.getResponseCode();
                Log.d("ServerInfo", "API response code: " + responseCode);
                
                BufferedReader reader;
                if (responseCode >= 200 && responseCode < 300) {
                    reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"));
                } else {
                    reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "utf-8"));
                }
                
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                String responseBody = response.toString();
                Log.d("ServerInfo", "API response body: " + responseBody);
                
                // Parse response and forward via WebSocket
                JSONObject apiResponse = new JSONObject(responseBody);
                
                JSONObject wsResponse = new JSONObject();
                wsResponse.put("type", "api_response");
                wsResponse.put("id", requestId);
                wsResponse.put("success", responseCode >= 200 && responseCode < 300);
                wsResponse.put("data", apiResponse);
                
                // Send response back via WebSocket
                if (webSocketClient != null && webSocketClient.isConnected()) {
                    webSocketClient.sendMessage(wsResponse.toString());
                    Log.d("ServerInfo", "Forwarded API response: " + wsResponse.toString());
                    
                    // Show toast on UI thread
                    boolean loginSuccess = apiResponse.optBoolean("success", false);
                    runOnUiThread(() -> {
                        String message = loginSuccess ? "로그인 성공" : "로그인 실패";
                        Toast.makeText(this, message + ": " + apiResponse.optString("message"), Toast.LENGTH_SHORT).show();
                    });
                } else {
                    Log.e("ServerInfo", "WebSocket not connected, cannot send response");
                }
                
            } catch (Exception e) {
                Log.e("ServerInfo", "Error forwarding API request: " + e.getMessage(), e);
                
                // Send error response
                try {
                    JSONObject errorResponse = new JSONObject();
                    errorResponse.put("type", "api_response");
                    errorResponse.put("id", requestId);
                    errorResponse.put("success", false);
                    
                    JSONObject errorData = new JSONObject();
                    errorData.put("success", false);
                    errorData.put("message", "Internal server error: " + e.getMessage());
                    errorResponse.put("data", errorData);
                    
                    if (webSocketClient != null && webSocketClient.isConnected()) {
                        webSocketClient.sendMessage(errorResponse.toString());
                    }
                    
                    runOnUiThread(() -> {
                        Toast.makeText(this, "API 호출 오류: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                    
                } catch (JSONException jsonE) {
                    Log.e("ServerInfo", "Error creating error response: " + jsonE.getMessage());
                }
            }
        }).start();
    }
    
    private void updateWebSocketStatus() {
        runOnUiThread(() -> {
            boolean isConnected = webSocketClient != null && webSocketClient.isConnected();
            Log.d("ServerInfo", "Updating WebSocket status - isConnected: " + isConnected);
            
            if (isConnected) {
                tvWebSocketStatus.setText("연결됨");
                tvWebSocketStatus.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                btnConnectWebSocket.setEnabled(false);
                btnDisconnectWebSocket.setEnabled(true);
            } else {
                tvWebSocketStatus.setText("연결 끊김");
                tvWebSocketStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                btnConnectWebSocket.setEnabled(true);
                btnDisconnectWebSocket.setEnabled(false);
            }
        });
    }
    
    // WebSocketListener implementation
    @Override
    public void onConnected() {
        Log.d("ServerInfo", "WebSocket onConnected callback");
        updateWebSocketStatus();
        Toast.makeText(this, "웹소켓 서버에 연결되었습니다.", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onMessage(String message) {
        Log.d("ServerInfo", "WebSocket onMessage: " + message);
        
        // Parse and handle API requests
        try {
            JSONObject jsonMessage = new JSONObject(message);
            String type = jsonMessage.optString("type");
            
            if ("api_request".equals(type)) {
                handleApiRequest(jsonMessage);
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(this, "메시지 수신: " + message, Toast.LENGTH_SHORT).show();
                });
            }
        } catch (JSONException e) {
            Log.e("ServerInfo", "Failed to parse message as JSON: " + e.getMessage());
            runOnUiThread(() -> {
                Toast.makeText(this, "메시지 수신: " + message, Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    @Override
    public void onDisconnected() {
        Log.d("ServerInfo", "WebSocket onDisconnected callback");
        updateWebSocketStatus();
        Toast.makeText(this, "웹소켓 연결이 끊어졌습니다.", Toast.LENGTH_SHORT).show();
    }
    
    @Override
    public void onError(String error) {
        Log.e("ServerInfo", "WebSocket onError: " + error);
        updateWebSocketStatus();
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // Disconnect WebSocket
        if (webSocketClient != null) {
            webSocketClient.disconnect();
        }
        
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