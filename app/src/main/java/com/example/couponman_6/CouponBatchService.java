package com.example.couponman_6;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class CouponBatchService {
    public static class CouponMatchResult {
        private final CouponTargetDraft draft;
        private final Coupon coupon;

        public CouponMatchResult(CouponTargetDraft draft, Coupon coupon) {
            this.draft = draft;
            this.coupon = coupon;
        }

        public CouponTargetDraft getDraft() {
            return draft;
        }

        public Coupon getCoupon() {
            return coupon;
        }
    }

    private final EmployeeDAO employeeDAO;
    private final CouponDAO couponDAO;
    private final TransactionDAO transactionDAO;
    private final SharedPreferences adminSettings;

    public CouponBatchService(Context context) {
        employeeDAO = new EmployeeDAO(context);
        couponDAO = new CouponDAO(context);
        transactionDAO = new TransactionDAO(context);
        adminSettings = context.getSharedPreferences("AdminSettings", Context.MODE_PRIVATE);
    }

    public List<CouponMatchResult> previewRechargeTargets(int corporateId, List<CouponTargetDraft> drafts) {
        List<CouponMatchResult> results = new ArrayList<>();
        employeeDAO.open();
        couponDAO.open();
        try {
            for (CouponTargetDraft draft : drafts) {
                if (!draft.isValid()) {
                    continue;
                }
                Employee employee = findEmployee(corporateId, draft);
                Coupon coupon = employee != null ? couponDAO.getLatestRechargeableCouponByEmployeeId(employee.getEmployeeId()) : null;
                results.add(new CouponMatchResult(draft, coupon));
            }
        } finally {
            couponDAO.close();
            employeeDAO.close();
        }
        return results;
    }

    public int issueCoupons(int corporateId, List<CouponTargetDraft> drafts, double usageLimit,
                            String expireDate, String availableDays) {
        int createdCount = 0;
        employeeDAO.open();
        couponDAO.open();
        transactionDAO.open();
        try {
            for (CouponTargetDraft draft : drafts) {
                if (!draft.isValid()) {
                    continue;
                }
                Employee employee = findOrCreateEmployee(corporateId, draft);
                Coupon coupon = new Coupon(employee.getEmployeeId(), usageLimit, 0.0, expireDate,
                        Coupon.PAYMENT_TYPE_PREPAID, availableDays);
                long couponId = couponDAO.insertCoupon(coupon);
                if (couponId > 0) {
                    Coupon savedCoupon = couponDAO.getCouponById((int) couponId);
                    if (savedCoupon != null) {
                        Transaction issueTransaction = new Transaction(
                                savedCoupon.getCouponId(),
                                usageLimit,
                                Transaction.TYPE_ISSUE,
                                Transaction.BALANCE_TYPE_CASH,
                                0.0,
                                usageLimit,
                                buildAuditText("신규 쿠폰 발행", draft.getName())
                        );
                        transactionDAO.insertTransaction(issueTransaction);
                        createdCount++;
                    }
                }
            }
        } finally {
            transactionDAO.close();
            couponDAO.close();
            employeeDAO.close();
        }
        return createdCount;
    }

    public int rechargeCoupons(int corporateId, List<CouponTargetDraft> drafts, double usageLimit,
                               String expireDate, String availableDays, boolean additive,
                               boolean extendExpired) {
        int processedCount = 0;
        employeeDAO.open();
        couponDAO.open();
        transactionDAO.open();
        try {
            for (CouponTargetDraft draft : drafts) {
                if (!draft.isValid()) {
                    continue;
                }
                Employee employee = findOrCreateEmployee(corporateId, draft);
                Coupon targetCoupon = couponDAO.getLatestRechargeableCouponByEmployeeId(employee.getEmployeeId());
                if (targetCoupon == null) {
                    Coupon newCoupon = new Coupon(employee.getEmployeeId(), usageLimit, 0.0, expireDate,
                            Coupon.PAYMENT_TYPE_PREPAID, availableDays);
                    long newId = couponDAO.insertCoupon(newCoupon);
                    if (newId > 0) {
                        Coupon savedCoupon = couponDAO.getCouponById((int) newId);
                        if (savedCoupon != null) {
                            transactionDAO.insertTransaction(new Transaction(
                                    savedCoupon.getCouponId(),
                                    usageLimit,
                                    Transaction.TYPE_ISSUE,
                                    Transaction.BALANCE_TYPE_CASH,
                                    0.0,
                                    usageLimit,
                                    buildAuditText("충전 대상 신규 발행", draft.getName())
                            ));
                            processedCount++;
                        }
                    }
                    continue;
                }

                if (targetCoupon.isExpired() && !extendExpired) {
                    continue;
                }

                double beforeBalance = targetCoupon.getCashBalance();
                double afterBalance = additive ? beforeBalance + usageLimit : usageLimit;

                boolean updated = couponDAO.updateCouponRechargeState(
                        targetCoupon.getCouponId(),
                        afterBalance,
                        expireDate,
                        availableDays,
                        Coupon.STATUS_ACTIVE
                );

                if (updated) {
                    transactionDAO.insertTransaction(new Transaction(
                            targetCoupon.getCouponId(),
                            additive ? usageLimit : afterBalance,
                            Transaction.TYPE_CHARGE,
                            Transaction.BALANCE_TYPE_CASH,
                            beforeBalance,
                            afterBalance,
                            buildAuditText(additive ? "기존 잔액 추가 충전" : "기존 잔액 초기화 후 재충전", draft.getName())
                    ));
                    processedCount++;
                }
            }
        } finally {
            transactionDAO.close();
            couponDAO.close();
            employeeDAO.close();
        }
        return processedCount;
    }

    private Employee findEmployee(int corporateId, CouponTargetDraft draft) {
        return employeeDAO.getEmployeeByCorporateAndCode(corporateId, draft.resolveEmployeeCode());
    }

    private Employee findOrCreateEmployee(int corporateId, CouponTargetDraft draft) {
        Employee existing = findEmployee(corporateId, draft);
        if (existing != null) {
            existing.setName(draft.getName());
            existing.setEmployeeCode(draft.resolveEmployeeCode());
            employeeDAO.updateEmployee(existing);
            return existing;
        }

        Employee employee = new Employee(
                corporateId,
                draft.getName(),
                draft.resolveEmployeeCode(),
                ""
        );
        long employeeId = employeeDAO.insertEmployee(employee);
        employee.setEmployeeId((int) employeeId);
        return employee;
    }

    private String buildAuditText(String action, String targetName) {
        String adminId = adminSettings.getString("admin_user_id", "admin");
        return action + " / 담당자 " + adminId + " / 대상 " + targetName;
    }
}
