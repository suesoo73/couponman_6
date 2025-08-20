package com.example.couponman_6;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class BusinessSettingsActivity extends AppCompatActivity {

    private EditText etBusinessNumber;
    private EditText etCompanyName;
    private EditText etOwnerName;
    private EditText etPhoneNumber;
    private Button btnSave;
    private Button btnBack;

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_business_settings);

        initializeViews();
        loadBusinessSettings();
        setupClickListeners();
    }

    private void initializeViews() {
        etBusinessNumber = findViewById(R.id.etBusinessNumber);
        etCompanyName = findViewById(R.id.etCompanyName);
        etOwnerName = findViewById(R.id.etOwnerName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);

        sharedPreferences = getSharedPreferences("BusinessSettings", MODE_PRIVATE);
    }

    private void loadBusinessSettings() {
        etBusinessNumber.setText(sharedPreferences.getString("business_number", ""));
        etCompanyName.setText(sharedPreferences.getString("company_name", ""));
        etOwnerName.setText(sharedPreferences.getString("owner_name", ""));
        etPhoneNumber.setText(sharedPreferences.getString("phone_number", ""));
    }

    private void setupClickListeners() {
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveBusinessSettings();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private void saveBusinessSettings() {
        String businessNumber = etBusinessNumber.getText().toString().trim();
        String companyName = etCompanyName.getText().toString().trim();
        String ownerName = etOwnerName.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        if (!validateInputs(businessNumber, companyName)) {
            return;
        }

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("business_number", businessNumber);
        editor.putString("company_name", companyName);
        editor.putString("owner_name", ownerName);
        editor.putString("phone_number", phoneNumber);
        editor.apply();

        Toast.makeText(this, "사업자 정보가 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }

    private boolean validateInputs(String businessNumber, String companyName) {
        if (TextUtils.isEmpty(businessNumber)) {
            Toast.makeText(this, "사업자등록번호는 필수 입력 항목입니다.", Toast.LENGTH_SHORT).show();
            etBusinessNumber.requestFocus();
            return false;
        }

        if (businessNumber.contains("-")) {
            Toast.makeText(this, "사업자등록번호는 '-' 없이 입력해주세요.", Toast.LENGTH_SHORT).show();
            etBusinessNumber.requestFocus();
            return false;
        }

        if (businessNumber.length() != 10) {
            Toast.makeText(this, "사업자등록번호는 10자리 숫자여야 합니다.", Toast.LENGTH_SHORT).show();
            etBusinessNumber.requestFocus();
            return false;
        }

        if (!businessNumber.matches("\\d+")) {
            Toast.makeText(this, "사업자등록번호는 숫자만 입력 가능합니다.", Toast.LENGTH_SHORT).show();
            etBusinessNumber.requestFocus();
            return false;
        }

        if (TextUtils.isEmpty(companyName)) {
            Toast.makeText(this, "회사명은 필수 입력 항목입니다.", Toast.LENGTH_SHORT).show();
            etCompanyName.requestFocus();
            return false;
        }

        return true;
    }
}