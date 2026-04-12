package com.example.couponman_6;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CouponListActivity extends AppCompatActivity {

    private static final String TAG = "CouponListActivity";

    private Spinner spinnerCorporate;
    private Spinner spinnerEmployee;
    private Button btnSearch;
    private Button btnBack;
    private LinearLayout couponListContainer;
    private TextView tvEmptyMessage;

    private CorporateDAO corporateDAO;
    private EmployeeDAO employeeDAO;
    private CouponDAO couponDAO;

    private List<Corporate> corporates = new ArrayList<>();
    private List<Employee> employees = new ArrayList<>();
    private final Map<Integer, String> employeeNameById = new HashMap<>();
    private Corporate selectedCorporate = null;
    private Employee selectedEmployee = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenOrientationHelper.applyOrientation(this);
        setContentView(R.layout.activity_coupon_list);

        // DAO 초기화
        corporateDAO = new CorporateDAO(this);
        employeeDAO = new EmployeeDAO(this);
        couponDAO = new CouponDAO(this);

        // UI 초기화
        initializeViews();

        // 거래처 목록 로드
        loadCorporates();
    }

    private void initializeViews() {
        spinnerCorporate = findViewById(R.id.spinnerCorporate);
        spinnerEmployee = findViewById(R.id.spinnerEmployee);
        btnSearch = findViewById(R.id.btnSearch);
        btnBack = findViewById(R.id.btnBack);
        couponListContainer = findViewById(R.id.couponListContainer);
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage);

        // 뒤로가기 버튼
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // 거래처 선택 리스너
        spinnerCorporate.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedCorporate = corporates.get(position - 1);
                    loadEmployees(selectedCorporate.getCustomerId());
                } else {
                    selectedCorporate = null;
                    employees.clear();
                    employeeNameById.clear();
                    updateEmployeeSpinner();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCorporate = null;
            }
        });

        // 직원 선택 리스너
        spinnerEmployee.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    selectedEmployee = employees.get(position - 1);
                } else {
                    selectedEmployee = null;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedEmployee = null;
            }
        });

        // 조회 버튼
        btnSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchCoupons();
            }
        });
    }

    /**
     * 거래처 목록 로드
     */
    private void loadCorporates() {
        boolean opened = false;
        try {
            corporateDAO.open();
            opened = true;
            corporates = corporateDAO.getAllCorporates();

            List<String> corporateNames = new ArrayList<>();
            corporateNames.add("거래처 선택");
            for (Corporate corporate : corporates) {
                corporateNames.add(corporate.getName());
            }

            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                corporateNames
            );
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerCorporate.setAdapter(adapter);

            Log.i(TAG, "거래처 목록 로드 완료: " + corporates.size() + "개");

        } catch (Exception e) {
            Log.e(TAG, "거래처 목록 로드 오류", e);
            Toast.makeText(this, "거래처 목록을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
        } finally {
            if (opened) {
                corporateDAO.close();
            }
        }
    }

    /**
     * 직원 목록 로드
     */
    private void loadEmployees(int corporateId) {
        boolean opened = false;
        try {
            employeeDAO.open();
            opened = true;
            employees = employeeDAO.getEmployeesByCorporateId(corporateId);
            employeeNameById.clear();
            for (Employee employee : employees) {
                employeeNameById.put(employee.getEmployeeId(), employee.getName());
            }

            updateEmployeeSpinner();

            Log.i(TAG, "직원 목록 로드 완료: " + employees.size() + "개");

        } catch (Exception e) {
            Log.e(TAG, "직원 목록 로드 오류", e);
            Toast.makeText(this, "직원 목록을 불러올 수 없습니다", Toast.LENGTH_SHORT).show();
        } finally {
            if (opened) {
                employeeDAO.close();
            }
        }
    }

    /**
     * 직원 스피너 업데이트
     */
    private void updateEmployeeSpinner() {
        List<String> employeeNames = new ArrayList<>();
        employeeNames.add("전체 직원");
        for (Employee employee : employees) {
            employeeNames.add(employee.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            employeeNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerEmployee.setAdapter(adapter);
    }

    /**
     * 쿠폰 조회
     */
    private void searchCoupons() {
        if (selectedCorporate == null) {
            Toast.makeText(this, "거래처를 선택해주세요", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean opened = false;
        try {
            couponDAO.open();
            opened = true;
            List<Coupon> coupons;

            if (selectedEmployee != null) {
                // 특정 직원의 쿠폰 조회
                coupons = couponDAO.getCouponsByEmployeeId(selectedEmployee.getEmployeeId());
                Log.i(TAG, String.format("직원 '%s'의 쿠폰 조회: %d개",
                    selectedEmployee.getName(), coupons.size()));
            } else {
                // 거래처 전체 쿠폰을 한 번에 조회
                coupons = couponDAO.getCouponsByCorporateId(selectedCorporate.getCustomerId());
                Log.i(TAG, String.format("거래처 '%s'의 전체 쿠폰 조회: %d개",
                    selectedCorporate.getName(), coupons.size()));
            }

            // 쿠폰 목록 표시
            displayCoupons(coupons);

        } catch (Exception e) {
            Log.e(TAG, "쿠폰 조회 오류", e);
            Toast.makeText(this, "쿠폰 조회 중 오류가 발생했습니다", Toast.LENGTH_SHORT).show();
        } finally {
            if (opened) {
                couponDAO.close();
            }
        }
    }

    /**
     * 쿠폰 목록 표시
     */
    private void displayCoupons(List<Coupon> coupons) {
        // 기존 목록 초기화
        couponListContainer.removeAllViews();

        if (coupons.isEmpty()) {
            tvEmptyMessage.setText("조회된 쿠폰이 없습니다");
            tvEmptyMessage.setVisibility(View.VISIBLE);
            couponListContainer.addView(tvEmptyMessage);
            return;
        }

        tvEmptyMessage.setVisibility(View.GONE);

        NumberFormat currencyFormat = NumberFormat.getInstance(Locale.KOREA);

        // 각 쿠폰을 카드 형태로 표시
        for (Coupon coupon : coupons) {
            LinearLayout cardView = createCouponCard(coupon, currencyFormat);
            couponListContainer.addView(cardView);
        }

        Toast.makeText(this, coupons.size() + "개의 쿠폰을 찾았습니다", Toast.LENGTH_SHORT).show();
    }

    /**
     * 쿠폰 카드 생성
     */
    private LinearLayout createCouponCard(Coupon coupon, NumberFormat currencyFormat) {
        LinearLayout card = new LinearLayout(this);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        cardParams.setMargins(0, 0, 0, 16);
        card.setLayoutParams(cardParams);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.card_background);
        card.setPadding(20, 20, 20, 20);
        card.setElevation(4);

        // 쿠폰 번호
        TextView tvCouponCode = new TextView(this);
        tvCouponCode.setText("🎫 " + coupon.getFullCouponCode());
        tvCouponCode.setTextSize(16);
        tvCouponCode.setTextColor(Color.parseColor("#333333"));
        tvCouponCode.setTypeface(null, android.graphics.Typeface.BOLD);
        card.addView(tvCouponCode);

        // 구분선
        View divider1 = new View(this);
        LinearLayout.LayoutParams dividerParams1 = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            2
        );
        dividerParams1.setMargins(0, 12, 0, 12);
        divider1.setLayoutParams(dividerParams1);
        divider1.setBackgroundColor(Color.parseColor("#eeeeee"));
        card.addView(divider1);

        // 직원 정보
        String employeeName = coupon.getRecipientName();
        if (employeeName == null || employeeName.trim().isEmpty()) {
            employeeName = employeeNameById.get(coupon.getEmployeeId());
        }

        if (employeeName != null && !employeeName.trim().isEmpty()) {
            TextView tvEmployee = new TextView(this);
            tvEmployee.setText("👤 " + employeeName);
            tvEmployee.setTextSize(14);
            tvEmployee.setTextColor(Color.parseColor("#666666"));
            card.addView(tvEmployee);
        }

        // 잔액 정보
        LinearLayout balanceLayout = new LinearLayout(this);
        balanceLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams balanceParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        balanceParams.setMargins(0, 8, 0, 0);
        balanceLayout.setLayoutParams(balanceParams);

        TextView tvCash = new TextView(this);
        tvCash.setText("💵 " + currencyFormat.format(coupon.getCashBalance()) + "원");
        tvCash.setTextSize(15);
        tvCash.setTextColor(Color.parseColor("#4CAF50"));
        tvCash.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams cashParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1
        );
        tvCash.setLayoutParams(cashParams);
        balanceLayout.addView(tvCash);

        TextView tvPoint = new TextView(this);
        tvPoint.setText("⭐ " + currencyFormat.format(coupon.getPointBalance()) + "P");
        tvPoint.setTextSize(15);
        tvPoint.setTextColor(Color.parseColor("#FF9800"));
        tvPoint.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams pointParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1
        );
        tvPoint.setLayoutParams(pointParams);
        balanceLayout.addView(tvPoint);

        card.addView(balanceLayout);

        // 상태 및 만료일
        LinearLayout statusLayout = new LinearLayout(this);
        statusLayout.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        statusParams.setMargins(0, 8, 0, 0);
        statusLayout.setLayoutParams(statusParams);

        // 상태
        TextView tvStatus = new TextView(this);
        String statusText = "";
        String statusColor = "#666666";

        if ("ACTIVE".equals(coupon.getStatus())) {
            statusText = "✅ 활성";
            statusColor = "#4CAF50";
        } else if ("USED".equals(coupon.getStatus())) {
            statusText = "✔️ 사용완료";
            statusColor = "#9E9E9E";
        } else if ("EXPIRED".equals(coupon.getStatus())) {
            statusText = "⏰ 만료";
            statusColor = "#F44336";
        }

        tvStatus.setText(statusText);
        tvStatus.setTextSize(13);
        tvStatus.setTextColor(Color.parseColor(statusColor));
        LinearLayout.LayoutParams statusTextParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1
        );
        tvStatus.setLayoutParams(statusTextParams);
        statusLayout.addView(tvStatus);

        // 만료일
        TextView tvExpireDate = new TextView(this);
        tvExpireDate.setText("📅 " + formatDate(coupon.getExpireDate()));
        tvExpireDate.setTextSize(13);
        tvExpireDate.setTextColor(Color.parseColor("#666666"));
        LinearLayout.LayoutParams expireDateParams = new LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1
        );
        tvExpireDate.setLayoutParams(expireDateParams);
        tvExpireDate.setGravity(Gravity.END);
        statusLayout.addView(tvExpireDate);

        card.addView(statusLayout);

        // 결제 유형
        TextView tvPaymentType = new TextView(this);
        String paymentTypeText = "";

        if (Coupon.PAYMENT_TYPE_PREPAID.equals(coupon.getPaymentType())) {
            paymentTypeText = "💳 선불";
        } else if (Coupon.PAYMENT_TYPE_POSTPAID.equals(coupon.getPaymentType())) {
            paymentTypeText = "💳 후불";
        } else if (Coupon.PAYMENT_TYPE_CUSTOM.equals(coupon.getPaymentType())) {
            paymentTypeText = "💳 임의결제";
        }

        tvPaymentType.setText(paymentTypeText);
        tvPaymentType.setTextSize(13);
        tvPaymentType.setTextColor(Color.parseColor("#666666"));
        LinearLayout.LayoutParams paymentTypeParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        );
        paymentTypeParams.setMargins(0, 4, 0, 0);
        tvPaymentType.setLayoutParams(paymentTypeParams);
        card.addView(tvPaymentType);

        return card;
    }

    /**
     * 날짜 포맷 변환
     */
    private String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
            Date date = inputFormat.parse(dateStr);
            return outputFormat.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (corporateDAO != null) {
            corporateDAO.close();
        }
        if (employeeDAO != null) {
            employeeDAO.close();
        }
        if (couponDAO != null) {
            couponDAO.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ScreenOrientationHelper.applyOrientation(this);
    }
}
