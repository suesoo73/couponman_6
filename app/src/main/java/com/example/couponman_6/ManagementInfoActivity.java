package com.example.couponman_6;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ManagementInfoActivity extends AppCompatActivity {
    private TextView tvCorporateCount;
    private TextView tvEmployeeCount;
    private TextView tvCouponCount;
    private CorporateDAO corporateDAO;
    private EmployeeDAO employeeDAO;
    private CouponDAO couponDAO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenOrientationHelper.applyOrientation(this);
        setContentView(R.layout.activity_management_info);

        tvCorporateCount = findViewById(R.id.tvManagementCorporateCount);
        tvEmployeeCount = findViewById(R.id.tvManagementEmployeeCount);
        tvCouponCount = findViewById(R.id.tvManagementCouponCount);
        Button btnRefresh = findViewById(R.id.btnManagementRefresh);
        Button btnServerInfo = findViewById(R.id.btnManagementServerInfo);
        Button btnBack = findViewById(R.id.btnManagementBack);

        corporateDAO = new CorporateDAO(this);
        employeeDAO = new EmployeeDAO(this);
        couponDAO = new CouponDAO(this);

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadSummary();
            }
        });

        btnServerInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ManagementInfoActivity.this, ServerInfoActivity.class));
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        loadSummary();
    }

    @Override
    protected void onResume() {
        super.onResume();
        ScreenOrientationHelper.applyOrientation(this);
        loadSummary();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        corporateDAO.close();
        employeeDAO.close();
        couponDAO.close();
    }

    private void loadSummary() {
        boolean corporateOpened = false;
        boolean employeeOpened = false;
        boolean couponOpened = false;

        try {
            corporateDAO.open();
            corporateOpened = true;
            employeeDAO.open();
            employeeOpened = true;
            couponDAO.open();
            couponOpened = true;

            tvCorporateCount.setText(String.valueOf(corporateDAO.getCorporateCount()));
            tvEmployeeCount.setText(String.valueOf(employeeDAO.getEmployeeCount()));
            tvCouponCount.setText(String.valueOf(couponDAO.getCouponCount()));
        } catch (Exception e) {
            Toast.makeText(this, "경영정보를 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
        } finally {
            if (corporateOpened) {
                corporateDAO.close();
            }
            if (employeeOpened) {
                employeeDAO.close();
            }
            if (couponOpened) {
                couponDAO.close();
            }
        }
    }
}
