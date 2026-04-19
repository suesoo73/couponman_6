package com.example.couponman_6;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class CustomerManagementActivity extends AppCompatActivity {
    private EditText etSearch;
    private LinearLayout customerListContainer;
    private TextView tvEmptyMessage;
    private EditText etName;
    private EditText etBusinessNumber;
    private EditText etRepresentative;
    private EditText etPhone;
    private EditText etEmail;
    private EditText etAddress;
    private TextView tvSelectedCustomer;

    private CorporateDAO corporateDAO;
    private EmployeeDAO employeeDAO;
    private List<Corporate> corporates = new ArrayList<>();
    private Corporate selectedCorporate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenOrientationHelper.applyOrientation(this);
        setContentView(R.layout.activity_customer_management);

        corporateDAO = new CorporateDAO(this);
        employeeDAO = new EmployeeDAO(this);

        etSearch = findViewById(R.id.etCustomerSearch);
        customerListContainer = findViewById(R.id.customerListContainer);
        tvEmptyMessage = findViewById(R.id.tvCustomerEmptyMessage);
        etName = findViewById(R.id.etCustomerName);
        etBusinessNumber = findViewById(R.id.etCustomerBusinessNumber);
        etRepresentative = findViewById(R.id.etCustomerRepresentative);
        etPhone = findViewById(R.id.etCustomerPhone);
        etEmail = findViewById(R.id.etCustomerEmail);
        etAddress = findViewById(R.id.etCustomerAddress);
        tvSelectedCustomer = findViewById(R.id.tvSelectedCustomer);

        Button btnSearch = findViewById(R.id.btnCustomerSearch);
        Button btnAll = findViewById(R.id.btnCustomerAll);
        Button btnNew = findViewById(R.id.btnCustomerNew);
        Button btnSave = findViewById(R.id.btnCustomerSave);
        Button btnBack = findViewById(R.id.btnCustomerBack);

        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadCorporates(etSearch.getText().toString().trim());
            }
        });

        btnAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSearch.setText("");
                loadCorporates("");
            }
        });

        btnNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedCorporate = null;
                clearForm();
                tvSelectedCustomer.setText("새 고객 등록");
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveCustomer();
            }
        });

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        clearForm();
        loadCorporates("");
    }

    @Override
    protected void onResume() {
        super.onResume();
        ScreenOrientationHelper.applyOrientation(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        corporateDAO.close();
        employeeDAO.close();
    }

    private void loadCorporates(String query) {
        boolean corporateOpened = false;
        try {
            corporateDAO.open();
            corporateOpened = true;
            if (query == null || query.trim().isEmpty()) {
                corporates = corporateDAO.getAllCorporates();
            } else {
                corporates = corporateDAO.searchCorporatesByName(query);
            }
            renderCustomerList();
        } catch (Exception e) {
            Toast.makeText(this, "고객 목록을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
        } finally {
            if (corporateOpened) {
                corporateDAO.close();
            }
        }
    }

    private void renderCustomerList() {
        customerListContainer.removeAllViews();

        if (corporates.isEmpty()) {
            tvEmptyMessage.setVisibility(View.VISIBLE);
            customerListContainer.addView(tvEmptyMessage);
            return;
        }

        tvEmptyMessage.setVisibility(View.GONE);

        for (Corporate corporate : corporates) {
            customerListContainer.addView(createCustomerCard(corporate));
        }
    }

    private View createCustomerCard(final Corporate corporate) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.card_background);
        card.setPadding(20, 20, 20, 20);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, 0, 0, 16);
        card.setLayoutParams(params);

        TextView tvName = new TextView(this);
        tvName.setText(corporate.getName());
        tvName.setTextSize(18);
        tvName.setTextColor(Color.parseColor("#1f2937"));
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(tvName);

        TextView tvInfo = new TextView(this);
        tvInfo.setText(buildCustomerInfo(corporate));
        tvInfo.setTextSize(13);
        tvInfo.setTextColor(Color.parseColor("#6b7280"));
        tvInfo.setPadding(0, 10, 0, 0);
        card.addView(tvInfo);

        card.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedCorporate = corporate;
                bindCustomerToForm(corporate);
            }
        });

        return card;
    }

    private String buildCustomerInfo(Corporate corporate) {
        StringBuilder builder = new StringBuilder();
        if (corporate.getRepresentative() != null && !corporate.getRepresentative().trim().isEmpty()) {
            builder.append("대표자: ").append(corporate.getRepresentative());
        }
        if (corporate.getBusinessNumber() != null && !corporate.getBusinessNumber().trim().isEmpty()) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append("사업자번호: ").append(corporate.getFormattedBusinessNumber());
        }
        if (corporate.getPhone() != null && !corporate.getPhone().trim().isEmpty()) {
            if (builder.length() > 0) {
                builder.append("\n");
            }
            builder.append("연락처: ").append(corporate.getPhone());
        }
        return builder.toString();
    }

    private void bindCustomerToForm(Corporate corporate) {
        tvSelectedCustomer.setText("선택 고객: " + corporate.getName());
        etName.setText(corporate.getName());
        etBusinessNumber.setText(corporate.getBusinessNumber());
        etRepresentative.setText(corporate.getRepresentative());
        etPhone.setText(corporate.getPhone());
        etEmail.setText(corporate.getEmail());
        etAddress.setText(corporate.getAddress());
    }

    private void clearForm() {
        etName.setText("");
        etBusinessNumber.setText("");
        etRepresentative.setText("");
        etPhone.setText("");
        etEmail.setText("");
        etAddress.setText("");
        tvSelectedCustomer.setText("선택된 고객이 없습니다");
    }

    private void saveCustomer() {
        String name = etName.getText().toString().trim();
        String businessNumber = etBusinessNumber.getText().toString().trim();
        String representative = etRepresentative.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String address = etAddress.getText().toString().trim();

        if (name.isEmpty()) {
            Toast.makeText(this, "고객명은 필수입니다", Toast.LENGTH_SHORT).show();
            etName.requestFocus();
            return;
        }

        if (!businessNumber.isEmpty() && !businessNumber.matches("\\d{10}")) {
            Toast.makeText(this, "사업자번호는 숫자 10자리여야 합니다", Toast.LENGTH_SHORT).show();
            etBusinessNumber.requestFocus();
            return;
        }

        if (!email.isEmpty() && !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            Toast.makeText(this, "이메일 형식을 확인해주세요", Toast.LENGTH_SHORT).show();
            etEmail.requestFocus();
            return;
        }

        boolean corporateOpened = false;
        try {
            corporateDAO.open();
            corporateOpened = true;

            if (selectedCorporate == null) {
                Corporate newCorporate = new Corporate(name, businessNumber, representative, phone, email, address);
                long id = corporateDAO.insertCorporate(newCorporate);
                if (id > 0) {
                    Toast.makeText(this, "고객이 등록되었습니다", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "고객 등록에 실패했습니다", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                selectedCorporate.setName(name);
                selectedCorporate.setBusinessNumber(businessNumber);
                selectedCorporate.setRepresentative(representative);
                selectedCorporate.setPhone(phone);
                selectedCorporate.setEmail(email);
                selectedCorporate.setAddress(address);

                int rows = corporateDAO.updateCorporate(selectedCorporate);
                if (rows > 0) {
                    Toast.makeText(this, "고객정보가 수정되었습니다", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "고객정보 수정에 실패했습니다", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            selectedCorporate = null;
            clearForm();
            loadCorporates(etSearch.getText().toString().trim());
        } catch (Exception e) {
            Toast.makeText(this, "고객 저장 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
        } finally {
            if (corporateOpened) {
                corporateDAO.close();
            }
        }
    }
}
