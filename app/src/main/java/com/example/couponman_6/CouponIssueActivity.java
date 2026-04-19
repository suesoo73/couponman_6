package com.example.couponman_6;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class CouponIssueActivity extends AppCompatActivity {
    private Spinner corporateSpinner;
    private Spinner presetSpinner;
    private EditText presetNameEditText;
    private EditText usageLimitEditText;
    private EditText expireDateEditText;
    private EditText availableDaysEditText;
    private LinearLayout targetContainer;

    private final List<Corporate> corporates = new ArrayList<>();
    private final List<CouponPreset> presets = new ArrayList<>();
    private CorporateDAO corporateDAO;
    private EmployeeDAO employeeDAO;
    private CouponPresetStore couponPresetStore;
    private CouponBatchService couponBatchService;

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleImportResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenOrientationHelper.applyOrientation(this);
        setContentView(R.layout.activity_coupon_issue);

        corporateSpinner = findViewById(R.id.spinnerIssueCorporate);
        presetSpinner = findViewById(R.id.spinnerIssuePreset);
        presetNameEditText = findViewById(R.id.etIssuePresetName);
        usageLimitEditText = findViewById(R.id.etIssueUsageLimit);
        expireDateEditText = findViewById(R.id.etIssueExpireDate);
        availableDaysEditText = findViewById(R.id.etIssueAvailableDays);
        targetContainer = findViewById(R.id.containerIssueTargets);

        corporateDAO = new CorporateDAO(this);
        employeeDAO = new EmployeeDAO(this);
        couponPresetStore = new CouponPresetStore(this);
        couponBatchService = new CouponBatchService(this);

        setupButtons();
        loadCorporates();
        loadPresets();
        addTargetRow(new CouponTargetDraft("", ""), true, "최근 쿠폰: 신규 발행");
    }

    private void setupButtons() {
        findViewById(R.id.btnIssueAddTarget).setOnClickListener(v ->
                addTargetRow(new CouponTargetDraft("", ""), true, "최근 쿠폰: 신규 발행"));
        findViewById(R.id.btnIssueLoadEmployees).setOnClickListener(v -> loadEmployees());
        findViewById(R.id.btnIssueImportExcel).setOnClickListener(v ->
                importLauncher.launch(new String[]{
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel",
                        "text/comma-separated-values",
                        "text/plain"
                }));
        findViewById(R.id.btnIssueSavePreset).setOnClickListener(v -> savePreset());
        findViewById(R.id.btnIssueLoadPreset).setOnClickListener(v -> applySelectedPreset());
        findViewById(R.id.btnIssueCreateCoupons).setOnClickListener(v -> issueCoupons());
    }

    private void loadCorporates() {
        corporateDAO.open();
        try {
            corporates.clear();
            corporates.addAll(corporateDAO.getAllCorporates());
        } finally {
            corporateDAO.close();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, buildCorporateNames());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        corporateSpinner.setAdapter(adapter);
    }

    private void loadPresets() {
        presets.clear();
        presets.addAll(couponPresetStore.getPresets());
        List<String> names = new ArrayList<>();
        if (presets.isEmpty()) {
            names.add("저장된 프리셋 없음");
        } else {
            for (CouponPreset preset : presets) {
                names.add(preset.getName());
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        presetSpinner.setAdapter(adapter);
    }

    private void loadEmployees() {
        Corporate corporate = getSelectedCorporate();
        if (corporate == null) {
            Toast.makeText(this, "거래처를 먼저 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }

        employeeDAO.open();
        try {
            targetContainer.removeAllViews();
            for (Employee employee : employeeDAO.getEmployeesByCorporateId(corporate.getCustomerId())) {
                addTargetRow(new CouponTargetDraft(employee.getName(), employee.getEmployeeCode()),
                        false, "최근 쿠폰: 기존 직원");
            }
        } finally {
            employeeDAO.close();
        }
    }

    private void savePreset() {
        String presetName = presetNameEditText.getText().toString().trim();
        Corporate corporate = getSelectedCorporate();
        if (presetName.isEmpty() || corporate == null) {
            Toast.makeText(this, "프리셋 이름과 거래처를 확인하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        CouponPreset preset = new CouponPreset(presetName, corporate.getCustomerId(), collectDrafts());
        couponPresetStore.savePreset(preset);
        loadPresets();
        Toast.makeText(this, "프리셋을 저장했습니다.", Toast.LENGTH_SHORT).show();
    }

    private void applySelectedPreset() {
        if (presets.isEmpty()) {
            return;
        }
        CouponPreset preset = presets.get(presetSpinner.getSelectedItemPosition());
        selectCorporateById(preset.getCorporateId());
        targetContainer.removeAllViews();
        for (CouponTargetDraft draft : preset.getTargets()) {
            addTargetRow(draft, false, "최근 쿠폰: 프리셋 대상");
        }
    }

    private void issueCoupons() {
        Corporate corporate = getSelectedCorporate();
        if (corporate == null) {
            Toast.makeText(this, "거래처를 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        double usageLimit = parseDouble(usageLimitEditText.getText().toString());
        if (usageLimit <= 0) {
            Toast.makeText(this, "사용한도를 입력하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        int count = couponBatchService.issueCoupons(
                corporate.getCustomerId(),
                collectDrafts(),
                usageLimit,
                expireDateEditText.getText().toString().trim(),
                availableDaysEditText.getText().toString().trim()
        );
        Toast.makeText(this, count + "건의 신규 쿠폰을 발행했습니다.", Toast.LENGTH_LONG).show();
    }

    private void handleImportResult(Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            List<CouponTargetDraft> drafts = ExcelEmployeeImporter.importFile(this, uri);
            for (CouponTargetDraft draft : drafts) {
                addTargetRow(draft, false, "최근 쿠폰: 업로드 대상");
            }
            Toast.makeText(this, drafts.size() + "명의 직원 자료를 가져왔습니다.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "파일 업로드 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addTargetRow(CouponTargetDraft draft, boolean checked, String latestCouponText) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_coupon_target_row, targetContainer, false);
        CheckBox checkBox = row.findViewById(R.id.checkSelected);
        EditText nameEditText = row.findViewById(R.id.etTargetName);
        EditText codeEditText = row.findViewById(R.id.etTargetCode);
        TextView latestCouponTextView = row.findViewById(R.id.tvLatestCoupon);
        Button removeButton = row.findViewById(R.id.btnRemoveTarget);

        checkBox.setChecked(checked || draft.isSelected());
        nameEditText.setText(draft.getName());
        codeEditText.setText(draft.getEmployeeCode());
        latestCouponTextView.setText(latestCouponText);
        removeButton.setOnClickListener(v -> targetContainer.removeView(row));
        targetContainer.addView(row);
    }

    private List<CouponTargetDraft> collectDrafts() {
        List<CouponTargetDraft> drafts = new ArrayList<>();
        for (int i = 0; i < targetContainer.getChildCount(); i++) {
            View row = targetContainer.getChildAt(i);
            CheckBox checkBox = row.findViewById(R.id.checkSelected);
            EditText nameEditText = row.findViewById(R.id.etTargetName);
            EditText codeEditText = row.findViewById(R.id.etTargetCode);

            CouponTargetDraft draft = new CouponTargetDraft(
                    nameEditText.getText().toString().trim(),
                    codeEditText.getText().toString().trim()
            );
            draft.setSelected(checkBox.isChecked());
            drafts.add(draft);
        }
        return drafts;
    }

    private Corporate getSelectedCorporate() {
        int position = corporateSpinner.getSelectedItemPosition();
        if (position < 0 || position >= corporates.size()) {
            return null;
        }
        return corporates.get(position);
    }

    private List<String> buildCorporateNames() {
        List<String> names = new ArrayList<>();
        for (Corporate corporate : corporates) {
            names.add(corporate.getName());
        }
        return names;
    }

    private void selectCorporateById(int corporateId) {
        for (int i = 0; i < corporates.size(); i++) {
            if (corporates.get(i).getCustomerId() == corporateId) {
                corporateSpinner.setSelection(i);
                return;
            }
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (Exception e) {
            return 0.0;
        }
    }
}
