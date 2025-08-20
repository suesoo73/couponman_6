package com.example.couponman_6;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Transaction {
    private int transactionId;
    private int couponId;
    private double amount;
    private String transactionType;
    private String transactionDate;
    private String balanceType; // 'cash' 또는 'point'
    private double balanceBefore; // 거래 전 잔액
    private double balanceAfter;  // 거래 후 잔액
    private String description;   // 거래 설명

    // 거래 타입 상수
    public static final String TYPE_CHARGE = "충전";
    public static final String TYPE_USE = "사용";
    public static final String TYPE_REFUND = "환불";
    public static final String TYPE_EXPIRE = "만료";
    public static final String TYPE_CANCEL = "취소";

    // 잔액 타입 상수
    public static final String BALANCE_TYPE_CASH = "cash";
    public static final String BALANCE_TYPE_POINT = "point";

    // 기본 생성자
    public Transaction() {
    }

    // 모든 필드를 포함한 생성자
    public Transaction(int transactionId, int couponId, double amount, String transactionType,
                      String transactionDate, String balanceType, double balanceBefore, 
                      double balanceAfter, String description) {
        this.transactionId = transactionId;
        this.couponId = couponId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.transactionDate = transactionDate;
        this.balanceType = balanceType;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    // 새로운 거래 생성용 생성자 (ID 제외)
    public Transaction(int couponId, double amount, String transactionType, String balanceType,
                      double balanceBefore, double balanceAfter, String description) {
        this.couponId = couponId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.transactionDate = getCurrentTimestamp();
        this.balanceType = balanceType;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
        this.description = description;
    }

    // 간단한 거래 생성용 생성자
    public Transaction(int couponId, double amount, String transactionType) {
        this.couponId = couponId;
        this.amount = amount;
        this.transactionType = transactionType;
        this.transactionDate = getCurrentTimestamp();
        this.balanceType = BALANCE_TYPE_CASH; // 기본값
    }

    // Getter와 Setter 메소드들
    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public int getCouponId() {
        return couponId;
    }

    public void setCouponId(int couponId) {
        this.couponId = couponId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(String transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getBalanceType() {
        return balanceType;
    }

    public void setBalanceType(String balanceType) {
        this.balanceType = balanceType;
    }

    public double getBalanceBefore() {
        return balanceBefore;
    }

    public void setBalanceBefore(double balanceBefore) {
        this.balanceBefore = balanceBefore;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(double balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
     * 거래가 충전인지 확인
     */
    public boolean isCharge() {
        return TYPE_CHARGE.equals(transactionType);
    }

    /**
     * 거래가 사용인지 확인
     */
    public boolean isUse() {
        return TYPE_USE.equals(transactionType);
    }

    /**
     * 거래가 환불인지 확인
     */
    public boolean isRefund() {
        return TYPE_REFUND.equals(transactionType);
    }

    /**
     * 현금 거래인지 확인
     */
    public boolean isCashTransaction() {
        return BALANCE_TYPE_CASH.equals(balanceType);
    }

    /**
     * 포인트 거래인지 확인
     */
    public boolean isPointTransaction() {
        return BALANCE_TYPE_POINT.equals(balanceType);
    }

    /**
     * 거래 금액이 양수인지 확인 (충전, 환불)
     */
    public boolean isPositiveAmount() {
        return amount > 0;
    }

    /**
     * 거래 금액이 음수인지 확인 (사용)
     */
    public boolean isNegativeAmount() {
        return amount < 0;
    }

    /**
     * 거래 금액의 절대값 반환
     */
    public double getAbsoluteAmount() {
        return Math.abs(amount);
    }

    /**
     * 거래로 인한 잔액 변화량
     */
    public double getBalanceChange() {
        return balanceAfter - balanceBefore;
    }

    /**
     * 필수 필드 유효성 확인
     */
    public boolean isValidForSave() {
        return couponId > 0 && 
               transactionType != null && !transactionType.trim().isEmpty() &&
               transactionDate != null && !transactionDate.trim().isEmpty();
    }

    /**
     * 거래 타입에 따른 금액 부호 자동 설정
     */
    public void normalizeAmount() {
        if (TYPE_USE.equals(transactionType) && amount > 0) {
            amount = -amount; // 사용은 음수로
        } else if ((TYPE_CHARGE.equals(transactionType) || TYPE_REFUND.equals(transactionType)) && amount < 0) {
            amount = Math.abs(amount); // 충전, 환불은 양수로
        }
    }

    /**
     * 거래 표시용 문자열 (금액 포함)
     */
    public String getDisplayText() {
        String balanceTypeText = isCashTransaction() ? "현금" : "포인트";
        String amountText = String.format("%,.0f원", getAbsoluteAmount());
        
        return transactionType + " (" + balanceTypeText + " " + amountText + ")";
    }

    /**
     * 거래일시를 표시용 형식으로 변환
     */
    public String getFormattedTransactionDate() {
        if (transactionDate == null) return "";
        
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault());
            
            Date date = inputFormat.parse(transactionDate);
            return outputFormat.format(date);
        } catch (Exception e) {
            return transactionDate;
        }
    }

    /**
     * 거래 상세 정보 반환
     */
    public String getDetailedInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("거래일시: ").append(transactionDate).append("\n");
        sb.append("거래유형: ").append(transactionType).append("\n");
        sb.append("잔액유형: ").append(isCashTransaction() ? "현금" : "포인트").append("\n");
        sb.append("거래금액: ").append(String.format("%,.0f원", getAbsoluteAmount())).append("\n");
        sb.append("거래전잔액: ").append(String.format("%,.0f원", balanceBefore)).append("\n");
        sb.append("거래후잔액: ").append(String.format("%,.0f원", balanceAfter)).append("\n");
        
        if (description != null && !description.trim().isEmpty()) {
            sb.append("설명: ").append(description);
        }
        
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "transactionId=" + transactionId +
                ", couponId=" + couponId +
                ", amount=" + amount +
                ", transactionType='" + transactionType + '\'' +
                ", transactionDate='" + transactionDate + '\'' +
                ", balanceType='" + balanceType + '\'' +
                ", balanceBefore=" + balanceBefore +
                ", balanceAfter=" + balanceAfter +
                ", description='" + description + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Transaction transaction = (Transaction) obj;
        return transactionId == transaction.transactionId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(transactionId);
    }
}