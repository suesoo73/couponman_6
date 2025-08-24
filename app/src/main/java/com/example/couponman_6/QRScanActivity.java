package com.example.couponman_6;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.camera.CameraSettings;
import com.google.zxing.ResultPoint;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.util.Log;

public class QRScanActivity extends AppCompatActivity {

    private static final String TAG = "QRScanActivity";
    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    
    private DecoratedBarcodeView barcodeView;
    private TextView tvScanResult;
    private TextView tvLastScanTime;
    private TextView tvScanCount;
    private Button btnToggleScan;
    private Button btnClearResults;
    private Button btnSwitchCamera;
    private Button btnBack;
    
    private boolean isScanning = false;
    private int scanCount = 0;
    private StringBuilder scanResults = new StringBuilder();
    
    // 데이터베이스 DAO
    private CouponDAO couponDAO;
    
    // 카메라 설정
    private boolean isUsingFrontCamera = false;

    private BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (result.getText() != null) {
                handleScanResult(result.getText());
            }
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scan);

        initializeViews();
        setupClickListeners();
        initializeDatabase();
        applyKeepScreenOnSetting();
        checkCameraPermission();
    }

    private void initializeViews() {
        barcodeView = findViewById(R.id.barcodeView);
        tvScanResult = findViewById(R.id.tvScanResult);
        tvLastScanTime = findViewById(R.id.tvLastScanTime);
        tvScanCount = findViewById(R.id.tvScanCount);
        btnToggleScan = findViewById(R.id.btnToggleScan);
        btnClearResults = findViewById(R.id.btnClearResults);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        btnBack = findViewById(R.id.btnBack);
        
        updateScanCount();
        updateCameraSwitchButtonText();
    }
    
    private void initializeDatabase() {
        try {
            couponDAO = new CouponDAO(this);
            Log.i(TAG, "[DB-INIT] 쿠폰 DAO 초기화 완료");
        } catch (Exception e) {
            Log.e(TAG, "[DB-INIT] 쿠폰 DAO 초기화 실패", e);
        }
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

    private void setupClickListeners() {
        btnToggleScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleScanning();
            }
        });

        btnClearResults.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearResults();
            }
        });

        btnSwitchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.CAMERA}, 
                    CAMERA_PERMISSION_REQUEST);
        } else {
            initializeCamera();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera();
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void initializeCamera() {
        setupCameraSettings();
        barcodeView.decodeContinuous(callback);
        startScanning();
    }
    
    private void setupCameraSettings() {
        CameraSettings cameraSettings = barcodeView.getBarcodeView().getCameraSettings();
        if (cameraSettings != null) {
            // 후면 카메라를 기본으로 설정
            cameraSettings.setRequestedCameraId(isUsingFrontCamera ? 1 : 0);
        }
        Log.i(TAG, "[CAMERA] 카메라 설정 완료 - " + (isUsingFrontCamera ? "전면" : "후면") + " 카메라");
    }

    private void toggleScanning() {
        if (isScanning) {
            stopScanning();
        } else {
            startScanning();
        }
    }

    private void startScanning() {
        barcodeView.resume();
        isScanning = true;
        btnToggleScan.setText("스캔 중지");
        btnToggleScan.setBackgroundResource(R.drawable.button_stop);
    }

    private void stopScanning() {
        barcodeView.pause();
        isScanning = false;
        btnToggleScan.setText("스캔 시작");
        btnToggleScan.setBackgroundResource(R.drawable.button_style);
    }

    private void handleScanResult(String result) {
        scanCount++;
        
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        
        Log.i(TAG, "[SCAN-RESULT] QR 스캔 결과: " + result);
        
        // 쿠폰 잔고 확인
        checkCouponBalance(result);
        
        String scanEntry = "[" + scanCount + "] " + timestamp + "\n" + 
                          "결과: " + result + "\n\n";
        
        scanResults.insert(0, scanEntry);
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvScanResult.setText(scanResults.toString());
                tvLastScanTime.setText("마지막 스캔: " + timestamp);
                updateScanCount();
                
                barcodeView.pause();
                
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (isScanning) {
                            barcodeView.resume();
                        }
                    }
                }, 2000);
            }
        });
    }
    
    /**
     * 카메라 전환 (전면/후면)
     */
    private void switchCamera() {
        try {
            Log.i(TAG, "[CAMERA-SWITCH] 카메라 전환 시작 - 현재: " + (isUsingFrontCamera ? "전면" : "후면"));
            
            // 현재 스캔 상태 저장
            boolean wasScanning = isScanning;
            
            // 스캔 중지
            if (isScanning) {
                stopScanning();
            }
            
            // 카메라 전환
            isUsingFrontCamera = !isUsingFrontCamera;
            
            // 카메라 설정 변경
            CameraSettings cameraSettings = barcodeView.getBarcodeView().getCameraSettings();
            if (cameraSettings != null) {
                cameraSettings.setRequestedCameraId(isUsingFrontCamera ? 1 : 0);
                
                // 바코드 뷰 재시작
                barcodeView.pause();
                barcodeView.resume();
                
                Log.i(TAG, "[CAMERA-SWITCH] 카메라 전환 완료 - 변경된 카메라: " + (isUsingFrontCamera ? "전면" : "후면"));
                
                // 이전 스캔 상태 복원
                if (wasScanning) {
                    startScanning();
                }
                
                // 토스트 메시지
                Toast.makeText(this, 
                    (isUsingFrontCamera ? "전면" : "후면") + " 카메라로 전환되었습니다", 
                    Toast.LENGTH_SHORT).show();
                    
                // 버튼 텍스트 업데이트
                updateCameraSwitchButtonText();
                
            } else {
                Log.e(TAG, "[CAMERA-SWITCH] 카메라 설정을 가져올 수 없습니다");
                Toast.makeText(this, "카메라 전환에 실패했습니다", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "[CAMERA-SWITCH] 카메라 전환 중 오류", e);
            Toast.makeText(this, "카메라 전환 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 카메라 전환 버튼 텍스트 업데이트
     */
    private void updateCameraSwitchButtonText() {
        if (btnSwitchCamera != null) {
            btnSwitchCamera.setText(isUsingFrontCamera ? "후면 카메라" : "전면 카메라");
        }
    }
    
    /**
     * 스캔된 QR 코드로 쿠폰 잔고 확인
     */
    private void checkCouponBalance(String qrData) {
        try {
            String couponCode = qrData.trim();
            
            // 기존 형식 호환성을 위해 "coupon:" 접두사가 있으면 제거
            if (couponCode.startsWith("coupon:")) {
                String[] parts = couponCode.split(":");
                if (parts.length >= 2) {
                    couponCode = parts[1];
                }
            }
            
            Log.i(TAG, "[COUPON-CHECK] 쿠폰 코드: " + couponCode);
            
            // 데이터베이스에서 쿠폰 조회
            if (couponDAO != null) {
                Coupon coupon = couponDAO.getCouponByCode(couponCode);
                if (coupon != null) {
                    Log.i(TAG, "[COUPON-BALANCE] ==========================================");
                    Log.i(TAG, "[COUPON-BALANCE] 쿠폰 ID: " + coupon.getCouponId());
                    Log.i(TAG, "[COUPON-BALANCE] 쿠폰 코드: " + coupon.getFullCouponCode());
                    Log.i(TAG, "[COUPON-BALANCE] 현금 잔고: " + coupon.getCashBalance() + "원");
                    Log.i(TAG, "[COUPON-BALANCE] 포인트 잔고: " + coupon.getPointBalance() + "P");
                    Log.i(TAG, "[COUPON-BALANCE] 상태: " + coupon.getStatus());
                    Log.i(TAG, "[COUPON-BALANCE] 유효기간: " + coupon.getExpireDate());
                    Log.i(TAG, "[COUPON-BALANCE] ==========================================");
                } else {
                    Log.w(TAG, "[COUPON-BALANCE] 쿠폰을 찾을 수 없습니다: " + couponCode);
                }
            } else {
                Log.e(TAG, "[COUPON-BALANCE] CouponDAO가 초기화되지 않았습니다");
            }
        } catch (Exception e) {
            Log.e(TAG, "[COUPON-CHECK] 쿠폰 잔고 확인 중 오류", e);
        }
    }

    private void clearResults() {
        scanResults.setLength(0);
        scanCount = 0;
        tvScanResult.setText("QR 코드를 스캔하면 여기에 결과가 표시됩니다.");
        tvLastScanTime.setText("마지막 스캔: 없음");
        updateScanCount();
    }

    private void updateScanCount() {
        tvScanCount.setText("총 스캔 횟수: " + scanCount);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isScanning) {
            barcodeView.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }
}