package com.example.couponman_6;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
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
    // 쿠폰 잔고 관련 뷰들 제거 (레이아웃에서 삭제됨)
    // private TextView tvCashBalance;
    // private TextView tvPointBalance;
    // private TextView tvCouponStatus;
    private Button btnToggleScan;
    private Button btnClearResults;
    private Button btnSwitchCamera;
    private Button btnBack;
    
    private boolean isScanning = false;
    private int scanCount = 0;
    private StringBuilder scanResults = new StringBuilder();
    
    // 데이터베이스 DAO
    private CouponDAO couponDAO;
    private EmployeeDAO employeeDAO;
    private CorporateDAO corporateDAO;
    private TransactionDAO transactionDAO;
    
    // 카메라 설정
    private boolean isUsingFrontCamera = false;
    
    // 음성 재생
    private MediaPlayer mediaPlayer;

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
        // 쿠폰 잔고 관련 뷰들 제거 (레이아웃에서 삭제됨)
        // tvCashBalance = findViewById(R.id.tvCashBalance);
        // tvPointBalance = findViewById(R.id.tvPointBalance);
        // tvCouponStatus = findViewById(R.id.tvCouponStatus);
        btnToggleScan = findViewById(R.id.btnToggleScan);
        btnClearResults = findViewById(R.id.btnClearResults);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        btnBack = findViewById(R.id.btnBack);
        
        updateScanCount();
        updateCameraSwitchButtonText();
        // resetBalanceDisplay(); // 잔고 표시 제거
    }
    
    private void initializeDatabase() {
        try {
            couponDAO = new CouponDAO(this);
            employeeDAO = new EmployeeDAO(this);
            corporateDAO = new CorporateDAO(this);
            transactionDAO = new TransactionDAO(this);
            Log.i(TAG, "[DB-INIT] DAO 초기화 완료");
        } catch (Exception e) {
            Log.e(TAG, "[DB-INIT] DAO 초기화 실패", e);
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

        // 상단 화살표 뒤로가기 버튼 설정
        findViewById(R.id.btnBackArrow).setOnClickListener(new View.OnClickListener() {
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
        
        Log.i(TAG, "[SCAN-RESULT] QR 스캔 결과: " + result);
        
        // 쿠폰 및 연관 정보 확인 (UI 업데이트 포함)
        checkCouponBalance(result);
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
     * 스캔된 QR 코드로 쿠폰 및 연관 정보 확인
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
            if (couponDAO != null && employeeDAO != null && corporateDAO != null && transactionDAO != null) {
                try {
                    // DAO 데이터베이스 연결 열기
                    couponDAO.open();
                    employeeDAO.open();
                    corporateDAO.open();
                    transactionDAO.open();
                    
                    Coupon coupon = couponDAO.getCouponByCode(couponCode);
                    if (coupon != null) {
                        // 직원 정보 조회
                        Employee employee = employeeDAO.getEmployeeById(coupon.getEmployeeId());
                        
                        // 회사 정보 조회
                        Corporate corporate = null;
                        if (employee != null) {
                            corporate = corporateDAO.getCorporateById(employee.getCorporateId());
                        }
                        
                        // 상세 정보 로그 출력
                        logCouponDetails(coupon, employee, corporate);
                        
                        // UI에 결과 표시
                        displayCouponInfo(couponCode, coupon, employee, corporate);
                        
                        // 가격 설정에 따른 차감 처리
                        boolean deductionSuccess = applyPriceDeduction(coupon, employee, corporate);
                        
                        // 하단 잔고 표시 업데이트
                        updateBalanceDisplay(coupon, employee, corporate);
                        
                    } else {
                        Log.w(TAG, "[COUPON-BALANCE] 쿠폰을 찾을 수 없습니다: " + couponCode);
                        displayCouponNotFound(couponCode);
                        
                        // 하단 잔고 표시 - 찾을 수 없음
                        updateBalanceDisplayNotFound();
                        
                        // 쿠폰을 찾을 수 없음 - 실패 음성 재생
                        playAudioFeedback(false);
                    }
                } finally {
                    // DAO 데이터베이스 연결 닫기
                    try { couponDAO.close(); } catch (Exception e) { Log.w(TAG, "Error closing couponDAO", e); }
                    try { employeeDAO.close(); } catch (Exception e) { Log.w(TAG, "Error closing employeeDAO", e); }
                    try { corporateDAO.close(); } catch (Exception e) { Log.w(TAG, "Error closing corporateDAO", e); }
                    try { transactionDAO.close(); } catch (Exception e) { Log.w(TAG, "Error closing transactionDAO", e); }
                }
            } else {
                Log.e(TAG, "[COUPON-BALANCE] DAO가 초기화되지 않았습니다");
                updateBalanceDisplayError("시스템 오류");
                
                // 시스템 오류 - 실패 음성 재생
                playAudioFeedback(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "[COUPON-CHECK] 쿠폰 확인 중 오류", e);
            updateBalanceDisplayError("오류 발생");
            
            // 일반 오류 - 실패 음성 재생
            playAudioFeedback(false);
        }
    }
    
    /**
     * 쿠폰 상세 정보 로그 출력
     */
    private void logCouponDetails(Coupon coupon, Employee employee, Corporate corporate) {
        Log.i(TAG, "[COUPON-DETAILS] ==========================================");
        Log.i(TAG, "[COUPON-DETAILS] === 쿠폰 정보 ===");
        Log.i(TAG, "[COUPON-DETAILS] 쿠폰 ID: " + coupon.getCouponId());
        Log.i(TAG, "[COUPON-DETAILS] 쿠폰 코드: " + coupon.getFullCouponCode());
        Log.i(TAG, "[COUPON-DETAILS] 현금 잔고: " + coupon.getCashBalance() + "원");
        Log.i(TAG, "[COUPON-DETAILS] 포인트 잔고: " + coupon.getPointBalance() + "P");
        Log.i(TAG, "[COUPON-DETAILS] 상태: " + coupon.getStatus());
        Log.i(TAG, "[COUPON-DETAILS] 유효기간: " + coupon.getExpireDate());
        
        if (employee != null) {
            Log.i(TAG, "[COUPON-DETAILS] === 직원 정보 ===");
            Log.i(TAG, "[COUPON-DETAILS] 직원 ID: " + employee.getEmployeeId());
            Log.i(TAG, "[COUPON-DETAILS] 이름: " + employee.getName());
            Log.i(TAG, "[COUPON-DETAILS] 전화번호: " + employee.getPhone());
            Log.i(TAG, "[COUPON-DETAILS] 이메일: " + employee.getEmail());
            Log.i(TAG, "[COUPON-DETAILS] 부서: " + employee.getDepartment());
        }
        
        if (corporate != null) {
            Log.i(TAG, "[COUPON-DETAILS] === 회사 정보 ===");
            Log.i(TAG, "[COUPON-DETAILS] 회사 ID: " + corporate.getCustomerId());
            Log.i(TAG, "[COUPON-DETAILS] 회사명: " + corporate.getName());
            Log.i(TAG, "[COUPON-DETAILS] 사업자등록번호: " + corporate.getBusinessNumber());
            Log.i(TAG, "[COUPON-DETAILS] 대표자: " + corporate.getRepresentative());
        }
        Log.i(TAG, "[COUPON-DETAILS] ==========================================");
    }
    
    /**
     * UI에 쿠폰 정보 표시
     */
    private void displayCouponInfo(String scannedCode, Coupon coupon, Employee employee, Corporate corporate) {
        StringBuilder displayText = new StringBuilder();
        displayText.append("✅ 쿠폰 발견!\n\n");
        
        // 쿠폰 정보
        displayText.append("📋 쿠폰 정보\n");
        displayText.append("코드: ").append(coupon.getFullCouponCode()).append("\n");
        displayText.append("💰 현금: ").append(String.format("%,d", (int)coupon.getCashBalance())).append("원\n");
        displayText.append("🎯 포인트: ").append(String.format("%,d", (int)coupon.getPointBalance())).append("P\n");
        displayText.append("📅 유효기간: ").append(coupon.getExpireDate()).append("\n");
        displayText.append("📊 상태: ").append(coupon.getStatus()).append("\n\n");
        
        // 직원 정보
        if (employee != null) {
            displayText.append("👤 직원 정보\n");
            displayText.append("이름: ").append(employee.getName()).append("\n");
            displayText.append("📱 전화: ").append(employee.getPhone()).append("\n");
            if (employee.getEmail() != null && !employee.getEmail().isEmpty()) {
                displayText.append("📧 이메일: ").append(employee.getEmail()).append("\n");
            }
            if (employee.getDepartment() != null && !employee.getDepartment().isEmpty()) {
                displayText.append("🏢 부서: ").append(employee.getDepartment()).append("\n");
            }
            displayText.append("\n");
        }
        
        // 회사 정보
        if (corporate != null) {
            displayText.append("🏭 회사 정보\n");
            displayText.append("회사명: ").append(corporate.getName()).append("\n");
            if (corporate.getBusinessNumber() != null && !corporate.getBusinessNumber().isEmpty()) {
                displayText.append("사업자번호: ").append(corporate.getBusinessNumber()).append("\n");
            }
            if (corporate.getRepresentative() != null && !corporate.getRepresentative().isEmpty()) {
                displayText.append("대표자: ").append(corporate.getRepresentative()).append("\n");
            }
        }
        
        updateScanResultDisplay(displayText.toString());
    }
    
    /**
     * 쿠폰을 찾을 수 없을 때 UI 표시
     */
    private void displayCouponNotFound(String scannedCode) {
        StringBuilder displayText = new StringBuilder();
        displayText.append("❌ 쿠폰을 찾을 수 없습니다\n\n");
        displayText.append("스캔된 코드: ").append(scannedCode).append("\n\n");
        displayText.append("• 쿠폰 코드가 올바른지 확인해주세요\n");
        displayText.append("• 쿠폰이 등록되어 있는지 확인해주세요\n");
        displayText.append("• 관리자에게 문의하세요");
        
        updateScanResultDisplay(displayText.toString());
    }
    
    /**
     * 스캔 결과 UI 업데이트
     */
    private void updateScanResultDisplay(String content) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date());
        
        String scanEntry = "[" + scanCount + "] " + timestamp + "\n" + content + "\n\n";
        
        scanResults.insert(0, scanEntry);
        
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tvScanResult.setText(scanResults.toString());
                tvLastScanTime.setText("마지막 스캔: " + timestamp);
            }
        });
    }

    private void clearResults() {
        scanResults.setLength(0);
        scanCount = 0;
        tvScanResult.setText("QR 코드를 스캔하면 여기에 결과가 표시됩니다.");
        tvLastScanTime.setText("마지막 스캔: 없음");
        updateScanCount();
        resetBalanceDisplay();
    }
    
    /**
     * 잔고 표시 초기화 - 레이아웃에서 제거되어 사용하지 않음
     */
    private void resetBalanceDisplay() {
        // 잔고 표시 UI가 제거되어 더 이상 사용하지 않음
        // tvCashBalance.setText("0원");
        // tvPointBalance.setText("0P");
        // tvCouponStatus.setText("QR 코드를 스캔하면 잔고 정보가 표시됩니다");
    }
    
    /**
     * 쿠폰 잔고 정보 업데이트
     */
    private void updateBalanceDisplay(Coupon coupon, Employee employee, Corporate corporate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // 현금 잔고 표시
                // 잔고 표시 UI가 제거되어 스캔 결과에 잔고 정보를 포함하도록 수정
                // 기존 updateBalanceDisplay 함수를 호출할 때 이미 표시되도록 되어 있음
                
                // 잔고 정보는 displayCouponInfo에서 처리
            }
        });
    }
    
    /**
     * 쿠폰을 찾을 수 없을 때 잔고 표시 업데이트 - 더 이상 사용하지 않음
     */
    private void updateBalanceDisplayNotFound() {
        // 잔고 표시 UI가 제거되어 displayCouponNotFound에서 처리
        // runOnUiThread(new Runnable() {
        //     @Override
        //     public void run() {
        //         tvCashBalance.setText("---");
        //         tvPointBalance.setText("---");
        //         tvCouponStatus.setText("❌ 쿠폰을 찾을 수 없습니다");
        //     }
        // });
    }
    
    /**
     * 에러 발생 시 잔고 표시 업데이트 - 더 이상 사용하지 않음
     */
    private void updateBalanceDisplayError(String errorMessage) {
        // 잔고 표시 UI가 제거되어 스캔 결과에 오류 표시
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String currentResult = tvScanResult.getText().toString();
                if (currentResult.contains("QR 코드를 스캔하면")) {
                    tvScanResult.setText("⚠️ " + errorMessage);
                } else {
                    tvScanResult.setText(currentResult + "\n\n⚠️ " + errorMessage);
                }
            }
        });
    }

    private void updateScanCount() {
        tvScanCount.setText("총 스캔 횟수: " + scanCount);
    }
    
    /**
     * 현재 시간에 따른 가격 차감 적용
     */
    private boolean applyPriceDeduction(Coupon coupon, Employee employee, Corporate corporate) {
        try {
            Log.i(TAG, "[PRICE-DEDUCTION] 가격 차감 처리 시작");
            
            // 가격 설정 불러오기
            SharedPreferences prefs = getSharedPreferences("PriceSettings", MODE_PRIVATE);
            
            // 시간대별 기능 활성화 여부 확인
            boolean enableTimeBasedDeduction = prefs.getBoolean("enableTimeBasedDeduction", false);
            boolean allowNegativeBalance = prefs.getBoolean("allowNegativeBalance", false);
            String pointDeductionMethod = prefs.getString("pointDeductionMethod", "후순위");
            
            Log.i(TAG, "[PRICE-DEDUCTION] 시간대별 차감 활성화: " + enableTimeBasedDeduction);
            Log.i(TAG, "[PRICE-DEDUCTION] 마이너스 잔고 허용: " + allowNegativeBalance);
            Log.i(TAG, "[PRICE-DEDUCTION] 포인트 차감 방식: " + pointDeductionMethod);
            
            int cashDeduction = 0;
            String periodName = "기본";
            
            if (enableTimeBasedDeduction) {
                // 현재 시간에 따른 시간대별 차감액 계산
                DeductionInfo deductionInfo = calculateCurrentDeduction(prefs);
                cashDeduction = deductionInfo.cashAmount;
                periodName = deductionInfo.periodName;
            } else {
                // 기본 차감액 사용
                cashDeduction = prefs.getInt("default_cashDeduction", 4000);
                periodName = "기본";
            }
            
            // Lambda에서 사용하기 위해 final 변수로 복사
            final int finalCashDeduction = cashDeduction;
            final String finalPeriodName = periodName;
            
            Log.i(TAG, "[PRICE-DEDUCTION] 적용할 현금 차감액: " + cashDeduction + "원 (" + periodName + " 시간대)");
            
            // 현재 쿠폰 잔고 확인
            double currentCash = coupon.getCashBalance();
            double currentPoints = coupon.getPointBalance();
            
            Log.i(TAG, "[PRICE-DEDUCTION] 차감 전 잔고 - 현금: " + currentCash + "원, 포인트: " + currentPoints + "P");
            
            // 잔고 검사
            if (currentCash < cashDeduction && !allowNegativeBalance) {
                Log.w(TAG, "[PRICE-DEDUCTION] 현금 잔고 부족 (현금: " + currentCash + "원 < 차감액: " + cashDeduction + "원)");
                
                runOnUiThread(() -> {
                    Toast.makeText(this, 
                        "현금 잔고가 부족합니다!\n현재: " + String.format("%,d", (int)currentCash) + "원\n필요: " + String.format("%,d", finalCashDeduction) + "원", 
                        Toast.LENGTH_LONG).show();
                    // 잔고 부족 메시지를 스캔 결과에 추가
                    String currentResult = tvScanResult.getText().toString();
                    tvScanResult.setText(currentResult + "\n❌ 현금 잔고 부족");
                });
                
                // 결제 실패 음성 재생
                playAudioFeedback(false);
                
                return false;
            }
            
            // 차감 적용
            double newCashBalance = currentCash - cashDeduction;
            
            Log.i(TAG, "[PRICE-DEDUCTION] 차감 적용 - 현금: " + currentCash + "원 → " + newCashBalance + "원");
            
            // 쿠폰 업데이트
            coupon.setCashBalance(newCashBalance);
            
            // 데이터베이스에 업데이트
            couponDAO.open();
            transactionDAO.open();
            try {
                int updateResult = couponDAO.updateCoupon(coupon);
                if (updateResult > 0) {
                    Log.i(TAG, "[PRICE-DEDUCTION] 쿠폰 업데이트 성공");
                    
                    // 거래 기록 생성
                    Transaction transaction = new Transaction();
                    transaction.setCouponId(coupon.getCouponId());
                    transaction.setTransactionType("DEDUCTION"); // 차감 거래
                    transaction.setAmount(finalCashDeduction); // 차감 금액
                    transaction.setBalanceType(Transaction.BALANCE_TYPE_CASH); // 현금 거래
                    transaction.setDescription("QR 스캔 " + finalPeriodName + " 시간대 차감");
                    transaction.setTransactionDate(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
                    transaction.setBalanceBefore(currentCash); // 차감 전 잔고
                    transaction.setBalanceAfter(newCashBalance); // 차감 후 잔고
                    
                    long transactionId = transactionDAO.insertTransaction(transaction);
                    
                    if (transactionId > 0) {
                        Log.i(TAG, "[PRICE-DEDUCTION] 거래 기록 생성 성공 - ID: " + transactionId);
                    } else {
                        Log.w(TAG, "[PRICE-DEDUCTION] 거래 기록 생성 실패");
                    }
                    
                    // 성공 메시지 표시
                    runOnUiThread(() -> {
                        Toast.makeText(this, 
                            "💰 차감 완료!\n" + finalPeriodName + " 시간대: " + String.format("%,d", finalCashDeduction) + "원 차감\n" +
                            "현금 잔고: " + String.format("%,d", (int)currentCash) + "원 → " + String.format("%,d", (int)newCashBalance) + "원", 
                            Toast.LENGTH_LONG).show();
                        // 차감 완료 메시지를 스캔 결과에 추가
                        String currentResult = tvScanResult.getText().toString();
                        tvScanResult.setText(currentResult + "\n✅ 차감 완료 (" + finalPeriodName + " 시간대)");
                    });
                    
                    // 결제 성공 음성 재생
                    playAudioFeedback(true);
                    
                    return true;
                } else {
                    Log.e(TAG, "[PRICE-DEDUCTION] 쿠폰 업데이트 실패");
                    
                    runOnUiThread(() -> {
                        Toast.makeText(this, "차감 처리 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
                        // 차감 실패 메시지를 스캔 결과에 추가
                        String currentResult = tvScanResult.getText().toString();
                        tvScanResult.setText(currentResult + "\n❌ 차감 처리 실패");
                    });
                    
                    // 결제 실패 음성 재생
                    playAudioFeedback(false);
                    
                    return false;
                }
            } finally {
                couponDAO.close();
                transactionDAO.close();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "[PRICE-DEDUCTION] 가격 차감 처리 중 오류", e);
            
            runOnUiThread(() -> {
                Toast.makeText(this, "차감 처리 중 오류: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                // 처리 오류 메시지를 스캔 결과에 추가
                String currentResult = tvScanResult.getText().toString();
                tvScanResult.setText(currentResult + "\n❌ 처리 오류");
            });
            
            // 결제 실패 음성 재생
            playAudioFeedback(false);
            
            return false;
        }
    }
    
    /**
     * 차감 정보를 담는 클래스
     */
    private static class DeductionInfo {
        int cashAmount;
        String periodName;
        
        DeductionInfo(int cashAmount, String periodName) {
            this.cashAmount = cashAmount;
            this.periodName = periodName;
        }
    }
    
    /**
     * 현재 시간에 따른 차감액 계산
     */
    private DeductionInfo calculateCurrentDeduction(SharedPreferences prefs) {
        // 현재 시간 구하기
        java.util.Calendar now = java.util.Calendar.getInstance();
        int hour = now.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = now.get(java.util.Calendar.MINUTE);
        String currentTime = String.format("%02d:%02d", hour, minute);
        
        Log.i(TAG, "[TIME-CALC] 현재 시간: " + currentTime);
        
        // 시간대별 설정 불러오기
        String breakfastStart = prefs.getString("breakfast_startTime", "07:00");
        String breakfastEnd = prefs.getString("breakfast_endTime", "10:59");
        int breakfastCash = prefs.getInt("breakfast_cashDeduction", 3000);
        
        String lunchStart = prefs.getString("lunch_startTime", "11:00");
        String lunchEnd = prefs.getString("lunch_endTime", "14:59");
        int lunchCash = prefs.getInt("lunch_cashDeduction", 5000);
        
        String dinnerStart = prefs.getString("dinner_startTime", "15:00");
        String dinnerEnd = prefs.getString("dinner_endTime", "21:59");
        int dinnerCash = prefs.getInt("dinner_cashDeduction", 7000);
        
        int defaultCash = prefs.getInt("default_cashDeduction", 4000);
        
        // 현재 시간이 어느 시간대에 속하는지 확인
        if (isTimeInRange(currentTime, breakfastStart, breakfastEnd)) {
            Log.i(TAG, "[TIME-CALC] 아침 시간대 적용: " + breakfastCash + "원");
            return new DeductionInfo(breakfastCash, "아침");
        } else if (isTimeInRange(currentTime, lunchStart, lunchEnd)) {
            Log.i(TAG, "[TIME-CALC] 점심 시간대 적용: " + lunchCash + "원");
            return new DeductionInfo(lunchCash, "점심");
        } else if (isTimeInRange(currentTime, dinnerStart, dinnerEnd)) {
            Log.i(TAG, "[TIME-CALC] 저녁 시간대 적용: " + dinnerCash + "원");
            return new DeductionInfo(dinnerCash, "저녁");
        } else {
            Log.i(TAG, "[TIME-CALC] 기본 시간대 적용: " + defaultCash + "원");
            return new DeductionInfo(defaultCash, "기본");
        }
    }
    
    /**
     * 시간이 특정 범위에 속하는지 확인
     */
    private boolean isTimeInRange(String currentTime, String startTime, String endTime) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
            java.util.Date current = sdf.parse(currentTime);
            java.util.Date start = sdf.parse(startTime);
            java.util.Date end = sdf.parse(endTime);
            
            return current.compareTo(start) >= 0 && current.compareTo(end) <= 0;
        } catch (Exception e) {
            Log.e(TAG, "[TIME-RANGE] 시간 범위 확인 중 오류", e);
            return false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isScanning) {
            barcodeView.resume();
        }
    }

    /**
     * 음성 파일 재생
     */
    private void playAudioFeedback(boolean paymentSuccess) {
        try {
            Log.i(TAG, "[AUDIO] 음성 재생 시작 - 결제 " + (paymentSuccess ? "성공" : "실패"));
            
            // 기존 MediaPlayer가 있으면 해제
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }
            
            // 결제 결과에 따른 음성 파일 선택
            int audioResource = paymentSuccess ? R.raw.payment_success : R.raw.payment_failed;
            String audioType = paymentSuccess ? "payment_success.mp3" : "payment_failed.mp3";
            
            Log.i(TAG, "[AUDIO] 재생할 파일: " + audioType);
            
            // MediaPlayer 생성 및 설정
            mediaPlayer = MediaPlayer.create(this, audioResource);
            
            if (mediaPlayer != null) {
                // 재생 완료 리스너 설정
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        Log.i(TAG, "[AUDIO] 음성 재생 완료");
                        if (mp != null) {
                            mp.release();
                        }
                        mediaPlayer = null;
                    }
                });
                
                // 에러 리스너 설정
                mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    @Override
                    public boolean onError(MediaPlayer mp, int what, int extra) {
                        Log.e(TAG, "[AUDIO] 음성 재생 오류 - what: " + what + ", extra: " + extra);
                        if (mp != null) {
                            mp.release();
                        }
                        mediaPlayer = null;
                        return true;
                    }
                });
                
                // 음성 재생 시작
                mediaPlayer.start();
                Log.i(TAG, "[AUDIO] 음성 재생 시작됨");
                
            } else {
                Log.e(TAG, "[AUDIO] MediaPlayer 생성 실패 - 리소스를 찾을 수 없습니다: " + audioType);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "[AUDIO] 음성 재생 중 오류", e);
            
            // MediaPlayer 정리
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.release();
                } catch (Exception ex) {
                    Log.w(TAG, "[AUDIO] MediaPlayer 해제 중 오류", ex);
                }
                mediaPlayer = null;
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
        
        // MediaPlayer 정리
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.w(TAG, "[AUDIO] onPause에서 MediaPlayer 해제 중 오류", e);
            }
            mediaPlayer = null;
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        // MediaPlayer 정리
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                }
                mediaPlayer.release();
            } catch (Exception e) {
                Log.w(TAG, "[AUDIO] onDestroy에서 MediaPlayer 해제 중 오류", e);
            }
            mediaPlayer = null;
        }
    }
}