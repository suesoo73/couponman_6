package com.example.couponman_6;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
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
import com.google.zxing.ResultPoint;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QRScanActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    
    private DecoratedBarcodeView barcodeView;
    private TextView tvScanResult;
    private TextView tvLastScanTime;
    private TextView tvScanCount;
    private Button btnToggleScan;
    private Button btnClearResults;
    private Button btnBack;
    
    private boolean isScanning = false;
    private int scanCount = 0;
    private StringBuilder scanResults = new StringBuilder();

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
        checkCameraPermission();
    }

    private void initializeViews() {
        barcodeView = findViewById(R.id.barcodeView);
        tvScanResult = findViewById(R.id.tvScanResult);
        tvLastScanTime = findViewById(R.id.tvLastScanTime);
        tvScanCount = findViewById(R.id.tvScanCount);
        btnToggleScan = findViewById(R.id.btnToggleScan);
        btnClearResults = findViewById(R.id.btnClearResults);
        btnBack = findViewById(R.id.btnBack);
        
        updateScanCount();
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
        barcodeView.decodeContinuous(callback);
        startScanning();
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