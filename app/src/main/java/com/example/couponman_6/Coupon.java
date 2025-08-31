package com.example.couponman_6;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class Coupon {
    private static final String TAG = "Coupon";
    private int couponId;
    private String fullCouponCode;
    private int employeeId;
    private double cashBalance;
    private double pointBalance;
    private String expireDate;
    private String status;
    private String paymentType; // 'prepaid' 또는 다른 값
    private String availableDays; // 사용가능요일 (예: "1111100")
    private String createdAt;
    
    // 수신자 정보 (런타임에만 사용, DB에 저장되지 않음)
    private String recipientName;
    private String recipientPhone;
    private String recipientEmail;
    private String corporateName;
    
    // 추가 직원 및 거래처 정보 (런타임에만 사용, JOIN 쿼리 결과 저장용)
    private String employeeDepartment;
    private String corporateBusinessNumber;
    private String corporateRepresentative;
    private String corporatePhone;

    // 상태 상수
    public static final String STATUS_ACTIVE = "사용 가능";
    public static final String STATUS_EXPIRED = "만료됨";
    public static final String STATUS_SUSPENDED = "일시 중지";
    public static final String STATUS_TERMINATED = "해지됨";

    // 결제 타입 상수
    public static final String PAYMENT_TYPE_PREPAID = "prepaid";
    public static final String PAYMENT_TYPE_POSTPAID = "postpaid";

    // 기본 생성자
    public Coupon() {
    }

    // 모든 필드를 포함한 생성자
    public Coupon(int couponId, String fullCouponCode, int employeeId, 
                  double cashBalance, double pointBalance, String expireDate, 
                  String status, String paymentType, String availableDays, String createdAt) {
        this.couponId = couponId;
        this.fullCouponCode = fullCouponCode;
        this.employeeId = employeeId;
        this.cashBalance = cashBalance;
        this.pointBalance = pointBalance;
        this.expireDate = expireDate;
        this.status = status;
        this.paymentType = paymentType;
        this.availableDays = availableDays;
        this.createdAt = createdAt;
    }

    // 새로운 쿠폰 생성용 생성자 (ID 및 코드 제외)
    public Coupon(int employeeId, double cashBalance, double pointBalance, 
                  String expireDate, String paymentType, String availableDays) {
        this.employeeId = employeeId;
        this.cashBalance = cashBalance;
        this.pointBalance = pointBalance;
        this.expireDate = expireDate;
        this.status = STATUS_ACTIVE;
        this.paymentType = paymentType != null ? paymentType : PAYMENT_TYPE_PREPAID;
        this.availableDays = availableDays != null ? availableDays : "1111111"; // 기본값: 매일 사용 가능
        this.createdAt = getCurrentTimestamp();
    }

    // Getter와 Setter 메소드들
    public int getCouponId() {
        return couponId;
    }

    public void setCouponId(int couponId) {
        this.couponId = couponId;
    }

    public String getFullCouponCode() {
        return fullCouponCode;
    }

    public void setFullCouponCode(String fullCouponCode) {
        this.fullCouponCode = fullCouponCode;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public double getCashBalance() {
        return cashBalance;
    }

    public void setCashBalance(double cashBalance) {
        this.cashBalance = cashBalance;
    }

    public double getPointBalance() {
        return pointBalance;
    }

    public void setPointBalance(double pointBalance) {
        this.pointBalance = pointBalance;
    }

    public String getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(String expireDate) {
        this.expireDate = expireDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getAvailableDays() {
        return availableDays;
    }

    public void setAvailableDays(String availableDays) {
        this.availableDays = availableDays;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    // 유틸리티 메소드들

    /**
     * 현재 시간을 문자열로 반환
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }

    /**
     * 쿠폰 코드 생성
     * {발급자_사업자등록번호}-{사용가능요일}-{coupon_id(10자리_제로패딩)}-{결제_유형_코드}-{패리티}
     */
    public String generateFullCouponCode(Context context) {
        String issuerBusinessNumber = getBusinessNumberFromSharedPreferences(context);
        
        Log.i(TAG, "[COUPON-CODE] 사업자등록번호 조회: " + issuerBusinessNumber);
        if ("0000000000".equals(issuerBusinessNumber)) {
            Log.w(TAG, "[COUPON-CODE] 경고: 사업자등록번호가 기본값(0000000000)입니다. 사업자 설정을 확인하세요.");
        }
        
        // 1. 발급자 사업자등록번호 (하이픈 제거)
        String issuerCode = issuerBusinessNumber.replace("-", "");
        
        // 2. 사용가능요일 (7자리)
        String availableDaysCode = availableDays != null ? availableDays : "1111111";
        
        // 3. 쿠폰 ID (10자리 제로패딩)
        String couponIdPadded = String.format("%010d", couponId);
        
        // 4. 결제 유형 코드
        String paymentTypeCode = PAYMENT_TYPE_PREPAID.equals(paymentType) ? "1" : "2";
        
        // 5. 패리티 (3자리 랜덤)
        Random random = new Random();
        String parity = String.format("%03d", random.nextInt(1000));
        
        String fullCode = issuerCode + "-" + availableDaysCode + "-" + couponIdPadded + "-" + paymentTypeCode + "-" + parity;
        Log.i(TAG, "[COUPON-CODE] 생성된 전체 쿠폰 코드: " + fullCode);
        
        return fullCode;
    }


    /**
     * SharedPreferences에서 사업자등록번호 가져오기
     */
    private String getBusinessNumberFromSharedPreferences(Context context) {
        SharedPreferences settings = context.getSharedPreferences("BusinessSettings", Context.MODE_PRIVATE);
        String businessNumber = settings.getString("business_number", "0000000000");
        Log.d(TAG, "[COUPON-CODE] SharedPreferences에서 사업자등록번호 조회: " + businessNumber);
        return businessNumber;
    }

    /**
     * 쿠폰이 만료되었는지 확인
     */
    public boolean isExpired() {
        if (expireDate == null) return false;
        
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date expDate = sdf.parse(expireDate);
            Date today = new Date();
            
            return expDate.before(today);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 오늘 사용 가능한지 확인
     */
    public boolean isAvailableToday() {
        if (availableDays == null || availableDays.length() != 7) {
            return true; // 기본적으로 사용 가능
        }
        
        Calendar calendar = Calendar.getInstance();
        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        
        // Calendar.DAY_OF_WEEK: 일요일=1, 월요일=2, ..., 토요일=7
        // availableDays: 일월화수목금토 순서 (0-6 인덱스)
        int dayIndex = (dayOfWeek == 1) ? 0 : dayOfWeek - 1; // 일요일을 0으로 조정
        
        return availableDays.charAt(dayIndex) == '1';
    }

    /**
     * 쿠폰 사용 가능 여부 확인
     */
    public boolean isUsable() {
        return STATUS_ACTIVE.equals(status) && !isExpired() && isAvailableToday();
    }

    /**
     * 잔액 충분 여부 확인
     */
    public boolean hasSufficientBalance(double amount, boolean useCash) {
        if (useCash) {
            return cashBalance >= amount;
        } else {
            return pointBalance >= amount;
        }
    }

    /**
     * 잔액 차감
     */
    public boolean deductBalance(double amount, boolean useCash) {
        if (!hasSufficientBalance(amount, useCash)) {
            return false;
        }
        
        if (useCash) {
            cashBalance -= amount;
        } else {
            pointBalance -= amount;
        }
        
        return true;
    }

    /**
     * 잔액 충전
     */
    public void addBalance(double amount, boolean toCash) {
        if (toCash) {
            cashBalance += amount;
        } else {
            pointBalance += amount;
        }
    }

    /**
     * 총 잔액 조회
     */
    public double getTotalBalance() {
        return cashBalance + pointBalance;
    }

    /**
     * 필수 필드 유효성 확인
     */
    public boolean isValidForSave() {
        return employeeId > 0 && 
               expireDate != null && !expireDate.trim().isEmpty() &&
               status != null && !status.trim().isEmpty();
    }

    /**
     * 만료일 설정 (개월 수로)
     */
    public void setExpireDateFromMonths(int months) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, months);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        this.expireDate = sdf.format(calendar.getTime());
    }

    /**
     * 사용가능요일을 한글로 변환
     */
    public String getAvailableDaysKorean() {
        if (availableDays == null || availableDays.length() != 7) {
            return "매일";
        }
        
        String[] dayNames = {"일", "월", "화", "수", "목", "금", "토"};
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < 7; i++) {
            if (availableDays.charAt(i) == '1') {
                if (sb.length() > 0) sb.append(", ");
                sb.append(dayNames[i]);
            }
        }
        
        return sb.length() > 0 ? sb.toString() : "없음";
    }

    // 수신자 정보 getter/setter
    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getRecipientPhone() {
        return recipientPhone;
    }

    public void setRecipientPhone(String recipientPhone) {
        this.recipientPhone = recipientPhone;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getCorporateName() {
        return corporateName;
    }

    public void setCorporateName(String corporateName) {
        this.corporateName = corporateName;
    }

    public String getEmployeeDepartment() {
        return employeeDepartment;
    }

    public void setEmployeeDepartment(String employeeDepartment) {
        this.employeeDepartment = employeeDepartment;
    }

    public String getCorporateBusinessNumber() {
        return corporateBusinessNumber;
    }

    public void setCorporateBusinessNumber(String corporateBusinessNumber) {
        this.corporateBusinessNumber = corporateBusinessNumber;
    }

    public String getCorporateRepresentative() {
        return corporateRepresentative;
    }

    public void setCorporateRepresentative(String corporateRepresentative) {
        this.corporateRepresentative = corporateRepresentative;
    }

    public String getCorporatePhone() {
        return corporatePhone;
    }

    public void setCorporatePhone(String corporatePhone) {
        this.corporatePhone = corporatePhone;
    }

    @Override
    public String toString() {
        return "Coupon{" +
                "couponId=" + couponId +
                ", fullCouponCode='" + fullCouponCode + '\'' +
                ", employeeId=" + employeeId +
                ", cashBalance=" + cashBalance +
                ", pointBalance=" + pointBalance +
                ", expireDate='" + expireDate + '\'' +
                ", status='" + status + '\'' +
                ", paymentType='" + paymentType + '\'' +
                ", availableDays='" + availableDays + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Coupon coupon = (Coupon) obj;
        return couponId == coupon.couponId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(couponId);
    }
}