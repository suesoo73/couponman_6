package com.example.couponman_6;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class CouponManagementActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenOrientationHelper.applyOrientation(this);
        setContentView(R.layout.activity_coupon_management);

        findViewById(R.id.btnCouponManagementIssue).setOnClickListener(v ->
                startActivity(new Intent(this, CouponIssueActivity.class)));
        findViewById(R.id.btnCouponManagementRecharge).setOnClickListener(v ->
                startActivity(new Intent(this, CouponRechargeActivity.class)));
        findViewById(R.id.btnCouponManagementList).setOnClickListener(v ->
                startActivity(new Intent(this, CouponListActivity.class)));
        findViewById(R.id.btnCouponManagementQr).setOnClickListener(v ->
                startActivity(new Intent(this, QRScanActivity.class)));
        findViewById(R.id.btnCouponManagementBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        ScreenOrientationHelper.applyOrientation(this);
    }
}
