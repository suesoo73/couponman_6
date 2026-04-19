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

public class CouponRechargeActivity extends AppCompatActivity {
    private Spinner corporateSpinner;
    private Spinner presetSpinner;
    private Spinner rechargeModeSpinner;
    private EditText presetNameEditText;
    private EditText usageLimitEditText;
    private EditText expireDateEditText;
    private EditText availableDaysEditText;
    private CheckBox extendExpiredCheckBox;
    private LinearLayout targetContainer;

    private final List<Corporate> corporates = new ArrayList<>();
    private final List<CouponPreset> presets = new ArrayList<>();
    private CorporateDAO corporateDAO;
    private CouponPresetStore presetStore;
    private CouponBatchService couponBatchService;

    private final ActivityResultLauncher<String[]> importLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), this::handleImportResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenOrientationHelper.applyOrientation(this);
        setContentView(R.layout.activity_coupon_recharge);

        corporateSpinner = findViewById(R.id.spinnerRechargeCorporate);
        presetSpinner = findViewById(R.id.spinnerRechargePreset);
        rechargeModeSpinner = findViewById(R.id.spinnerRechargeMode);
        presetNameEditText = findViewById(R.id.etRechargePresetName);
        usageLimitEditText = findViewById(R.id.etRechargeUsageLimit);
        expireDateEditText = findViewById(R.id.etRechargeExpireDate);
        availableDaysEditText = findViewById(R.id.etRechargeAvailableDays);
        extendExpiredCheckBox = findViewById(R.id.checkExtendExpired);
        targetContainer = findViewById(R.id.containerRechargeTargets);

        corporateDAO = new CorporateDAO(this);
        presetStore = new CouponPresetStore(this);
        couponBatchService = new CouponBatchService(this);

        setupModeSpinner();
        setupButtons();
        loadCorporates();
        loadPresets();
        addTargetRow(new CouponTargetDraft("", ""), "최근 쿠폰: 조회 필요");
    }

    private void setupModeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"기존 잔액 초기화 후 새 한도 적용", "기존 잔액에 추가 충전"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rechargeModeSpinner.setAdapter(adapter);
    }

    private void setupButtons() {
        findViewById(R.id.btnRechargeAddTarget).setOnClickListener(v ->
                addTargetRow(new CouponTargetDraft("", ""), "최근 쿠폰: 조회 필요"));
        findViewById(R.id.btnRechargeLoadPreset).setOnClickListener(v -> applySelectedPreset());
        findViewById(R.id.btnRechargePreview).setOnClickListener(v -> previewCoupons());
        findViewById(R.id.btnRechargeImportExcel).setOnClickListener(v ->
                importLauncher.launch(new String[]{
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "application/vnd.ms-excel",
                        "text/comma-separated-values",
                        "text/plain"
                }));
        findViewById(R.id.btnRechargeSavePreset).setOnClickListener(v -> savePreset());
        findViewById(R.id.btnRechargeExecute).setOnClickListener(v -> rechargeCoupons());
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
        presets.addAll(presetStore.getPresets());
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

    private void savePreset() {
        String presetName = presetNameEditText.getText().toString().trim();
        Corporate corporate = getSelectedCorporate();
        if (presetName.isEmpty() || corporate == null) {
            Toast.makeText(this, "프리셋 이름과 거래처를 확인하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        presetStore.savePreset(new CouponPreset(presetName, corporate.getCustomerId(), collectDrafts()));
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
            addTargetRow(draft, "최근 쿠폰: 조회 필요");
        }
    }

    private void previewCoupons() {
        Corporate corporate = getSelectedCorporate();
        if (corporate == null) {
            Toast.makeText(this, "거래처를 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        List<CouponBatchService.CouponMatchResult> results =
                couponBatchService.previewRechargeTargets(corporate.getCustomerId(), collectDrafts());
        targetContainer.removeAllViews();
        for (CouponBatchService.CouponMatchResult result : results) {
            String latest = result.getCoupon() == null
                    ? "최근 쿠폰: 신규 발행 예정"
                    : "최근 쿠폰: #" + result.getCoupon().getCouponId() + " / " +
                    result.getCoupon().getCashBalance() + " / " + result.getCoupon().getExpireDate();
            addTargetRow(result.getDraft(), latest);
        }
    }

    private void rechargeCoupons() {
        Corporate corporate = getSelectedCorporate();
        if (corporate == null) {
            Toast.makeText(this, "거래처를 선택하세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        double usageLimit = parseDouble(usageLimitEditText.getText().toString());
        boolean additive = rechargeModeSpinner.getSelectedItemPosition() == 1;
        int count = couponBatchService.rechargeCoupons(
                corporate.getCustomerId(),
                collectDrafts(),
                usageLimit,
                expireDateEditText.getText().toString().trim(),
                availableDaysEditText.getText().toString().trim(),
                additive,
                extendExpiredCheckBox.isChecked()
        );
        Toast.makeText(this, count + "건의 쿠폰 충전/신규발행을 처리했습니다.", Toast.LENGTH_LONG).show();
        previewCoupons();
    }

    private void handleImportResult(Uri uri) {
        if (uri == null) {
            return;
        }
        try {
            List<CouponTargetDraft> drafts = ExcelEmployeeImporter.importFile(this, uri);
            for (CouponTargetDraft draft : drafts) {
                addTargetRow(draft, "최근 쿠폰: 조회 필요");
            }
            Toast.makeText(this, drafts.size() + "명의 직원 자료를 가져왔습니다.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "파일 업로드 실패: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addTargetRow(CouponTargetDraft draft, String latestCouponText) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_coupon_target_row, targetContainer, false);
        ((CheckBox) row.findViewById(R.id.checkSelected)).setChecked(draft.isSelected());
        ((EditText) row.findViewById(R.id.etTargetName)).setText(draft.getName());
        ((EditText) row.findViewById(R.id.etTargetCode)).setText(draft.getEmployeeCode());
        ((TextView) row.findViewById(R.id.tvLatestCoupon)).setText(latestCouponText);
        ((Button) row.findViewById(R.id.btnRemoveTarget)).setOnClickListener(v -> targetContainer.removeView(row));
        targetContainer.addView(row);
    }

    private List<CouponTargetDraft> collectDrafts() {
        List<CouponTargetDraft> drafts = new ArrayList<>();
        for (int i = 0; i < targetContainer.getChildCount(); i++) {
            View row = targetContainer.getChildAt(i);
            CouponTargetDraft draft = new CouponTargetDraft(
                    ((EditText) row.findViewById(R.id.etTargetName)).getText().toString().trim(),
                    ((EditText) row.findViewById(R.id.etTargetCode)).getText().toString().trim()
            );
            draft.setSelected(((CheckBox) row.findViewById(R.id.checkSelected)).isChecked());
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
