package com.example.couponman_6;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CouponDelivery {
    private long deliveryId;
    private int couponId;
    private String deliveryType;  // EMAIL, SMS, KAKAO
    private String deliveryStatus; // PENDING, SENT, DELIVERED, FAILED, BOUNCED
    private String recipientAddress;
    private String sentAt;
    private String deliveredAt;
    private String failedAt;
    private int retryCount;
    private String lastRetryAt;
    private String errorMessage;
    private String subject;
    private String message;
    private String metadata;
    private String createdAt;
    private String updatedAt;
    
    // 런타임에만 사용되는 필드 (DB에 저장되지 않음)
    private String couponCode;

    // 발송 유형 상수
    public static final String TYPE_EMAIL = "EMAIL";
    public static final String TYPE_SMS = "SMS";
    public static final String TYPE_KAKAO = "KAKAO";

    // 발송 상태 상수
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_BOUNCED = "BOUNCED";

    // 기본 생성자
    public CouponDelivery() {
    }

    // 새로운 발송 기록 생성용 생성자
    public CouponDelivery(int couponId, String deliveryType, String recipientAddress, 
                         String subject, String message) {
        this.couponId = couponId;
        this.deliveryType = deliveryType;
        this.recipientAddress = recipientAddress;
        this.subject = subject;
        this.message = message;
        this.deliveryStatus = STATUS_PENDING;
        this.retryCount = 0;
        this.createdAt = getCurrentTimestamp();
        this.updatedAt = getCurrentTimestamp();
    }

    // Getter와 Setter 메소드들
    public long getDeliveryId() {
        return deliveryId;
    }

    public void setDeliveryId(long deliveryId) {
        this.deliveryId = deliveryId;
    }

    public int getCouponId() {
        return couponId;
    }

    public void setCouponId(int couponId) {
        this.couponId = couponId;
    }

    public String getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType) {
        this.deliveryType = deliveryType;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public String getRecipientAddress() {
        return recipientAddress;
    }

    public void setRecipientAddress(String recipientAddress) {
        this.recipientAddress = recipientAddress;
    }

    public String getCouponCode() {
        return couponCode;
    }

    public void setCouponCode(String couponCode) {
        this.couponCode = couponCode;
    }

    public String getSentAt() {
        return sentAt;
    }

    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }

    public String getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(String deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(String failedAt) {
        this.failedAt = failedAt;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getLastRetryAt() {
        return lastRetryAt;
    }

    public void setLastRetryAt(String lastRetryAt) {
        this.lastRetryAt = lastRetryAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    // JavaScript에서 사용할 추가 필드들 (JSON 직렬화용 - DB 저장 안함)
    private String status;
    private String recipient;
    private String type;

    // JavaScript에서 delivery.status로 접근할 수 있도록 하는 getter
    public String getStatus() {
        if (status != null) {
            return status;
        }
        return deliveryStatus != null ? deliveryStatus.toLowerCase() : null;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // JavaScript에서 delivery.recipient로 접근할 수 있도록 하는 getter
    public String getRecipient() {
        if (recipient != null) {
            return recipient;
        }
        return recipientAddress;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    // JavaScript에서 소문자 deliveryType에 접근하기 위한 별도 getter
    public String getType() {
        if (type != null) {
            return type;
        }
        return deliveryType != null ? deliveryType.toLowerCase() : null;
    }

    public void setType(String type) {
        this.type = type;
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
     * 발송 상태가 성공인지 확인
     */
    public boolean isSuccessful() {
        return STATUS_SENT.equals(deliveryStatus) || STATUS_DELIVERED.equals(deliveryStatus);
    }

    /**
     * 발송 상태가 실패인지 확인
     */
    public boolean isFailed() {
        return STATUS_FAILED.equals(deliveryStatus) || STATUS_BOUNCED.equals(deliveryStatus);
    }

    /**
     * 재시도 가능한지 확인 (최대 3회까지)
     */
    public boolean canRetry() {
        return isFailed() && retryCount < 3;
    }

    /**
     * 발송 유형이 유효한지 확인
     */
    public boolean isValidDeliveryType() {
        return TYPE_EMAIL.equals(deliveryType) || 
               TYPE_SMS.equals(deliveryType) || 
               TYPE_KAKAO.equals(deliveryType);
    }

    /**
     * 저장에 유효한 데이터인지 확인
     */
    public boolean isValidForSave() {
        return couponId > 0 && 
               isValidDeliveryType() && 
               recipientAddress != null && !recipientAddress.trim().isEmpty();
    }

    /**
     * 발송 유형에 따른 한글 표시명 반환
     */
    public String getDeliveryTypeDisplayName() {
        switch (deliveryType) {
            case TYPE_EMAIL:
                return "이메일";
            case TYPE_SMS:
                return "SMS";
            case TYPE_KAKAO:
                return "카카오톡";
            default:
                return deliveryType;
        }
    }

    /**
     * 발송 상태에 따른 한글 표시명 반환
     */
    public String getDeliveryStatusDisplayName() {
        switch (deliveryStatus) {
            case STATUS_PENDING:
                return "대기중";
            case STATUS_SENT:
                return "발송됨";
            case STATUS_DELIVERED:
                return "전달됨";
            case STATUS_FAILED:
                return "실패";
            case STATUS_BOUNCED:
                return "반송됨";
            default:
                return deliveryStatus;
        }
    }

    @Override
    public String toString() {
        return "CouponDelivery{" +
                "deliveryId=" + deliveryId +
                ", couponId=" + couponId +
                ", deliveryType='" + deliveryType + '\'' +
                ", deliveryStatus='" + deliveryStatus + '\'' +
                ", recipientAddress='" + recipientAddress + '\'' +
                ", retryCount=" + retryCount +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        CouponDelivery delivery = (CouponDelivery) obj;
        return deliveryId == delivery.deliveryId;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(deliveryId);
    }
}