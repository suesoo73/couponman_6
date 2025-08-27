package com.example.couponman_6;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import java.io.IOException;

public class ApiServerService extends Service {
    private static final String TAG = "ApiServerService";
    private static final String CHANNEL_ID = "ApiServerChannel";
    private static final int NOTIFICATION_ID = 1001;
    private static final int SERVER_PORT = 8080;

    private ApiServer apiServer;
    private boolean isServerRunning = false;
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        ApiServerService getService() {
            return ApiServerService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("START_SERVER".equals(action)) {
                startApiServer();
            } else if ("STOP_SERVER".equals(action)) {
                stopApiServer();
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "API 서버 서비스",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("API 서버가 실행 중일 때 표시되는 알림");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent stopIntent = new Intent(this, ApiServerService.class);
        stopIntent.setAction("STOP_SERVER");
        PendingIntent stopPendingIntent = PendingIntent.getService(
                this,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("쿠폰 매니저 API 서버")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .addAction(android.R.drawable.ic_media_pause, "서버 중지", stopPendingIntent);

        return builder.build();
    }

    public void startApiServer() {
        if (isServerRunning) {
            Log.d(TAG, "서버가 이미 실행 중입니다.");
            return;
        }

        try {
            apiServer = new ApiServer(SERVER_PORT, this);
            apiServer.start();
            isServerRunning = true;
            
            startForeground(NOTIFICATION_ID, createNotification("API 서버가 포트 " + SERVER_PORT + "에서 실행 중"));
            
            Log.i(TAG, "=== API 서버가 시작되었습니다 ===");
            Log.i(TAG, "포트: " + SERVER_PORT);
            Log.i(TAG, "로컬 접속: http://localhost:" + SERVER_PORT + "/api");
            Log.i(TAG, "에뮬레이터에서 PC 접속: http://127.0.0.1:" + SERVER_PORT + "/api");
            Log.i(TAG, "================================");
            
            // 서버 시작 브로드캐스트 전송
            Intent broadcastIntent = new Intent("com.example.couponman_6.SERVER_STATUS");
            broadcastIntent.putExtra("status", "started");
            sendBroadcast(broadcastIntent);
            
        } catch (IOException e) {
            Log.e(TAG, "서버 시작 실패: " + e.getMessage(), e);
            stopSelf();
        }
    }

    public void stopApiServer() {
        if (apiServer != null && isServerRunning) {
            apiServer.stop();
            apiServer = null;
            isServerRunning = false;
            
            Log.d(TAG, "API 서버가 중지되었습니다.");
            
            // 서버 중지 브로드캐스트 전송
            Intent broadcastIntent = new Intent("com.example.couponman_6.SERVER_STATUS");
            broadcastIntent.putExtra("status", "stopped");
            sendBroadcast(broadcastIntent);
        }
        
        stopForeground(true);
        stopSelf();
    }

    public boolean isServerRunning() {
        return isServerRunning;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopApiServer();
    }
}