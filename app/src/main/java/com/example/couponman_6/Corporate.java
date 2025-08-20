package com.example.couponman_6;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Corporate {
    private int customerId;
    private String name;
    private String businessNumber;
    private String representative;
    private String phone;
    private String email;
    private String address;
    private String createdAt;

    // 기본 생성자
    public Corporate() {
    }

    // 모든 필드를 포함한 생성자
    public Corporate(int customerId, String name, String businessNumber, String representative, 
                    String phone, String email, String address, String createdAt) {
        this.customerId = customerId;
        this.name = name;
        this.businessNumber = businessNumber;
        this.representative = representative;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.createdAt = createdAt;
    }

    // 새로운 거래처 생성용 생성자 (ID 제외)
    public Corporate(String name, String businessNumber, String representative, 
                    String phone, String email, String address) {
        this.name = name;
        this.businessNumber = businessNumber;
        this.representative = representative;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.createdAt = getCurrentTimestamp();
    }

    // Getter와 Setter 메소드들
    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBusinessNumber() {
        return businessNumber;
    }

    public void setBusinessNumber(String businessNumber) {
        this.businessNumber = businessNumber;
    }

    public String getRepresentative() {
        return representative;
    }

    public void setRepresentative(String representative) {
        this.representative = representative;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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
     * 사업자등록번호가 유효한 형식인지 확인
     * (10자리 숫자)
     */
    public boolean isValidBusinessNumber() {
        return businessNumber != null && 
               businessNumber.matches("\\d{10}") && 
               businessNumber.length() == 10;
    }

    /**
     * 이메일이 유효한 형식인지 확인
     */
    public boolean isValidEmail() {
        if (email == null || email.trim().isEmpty()) {
            return true; // 이메일은 선택사항이므로 비어있어도 유효
        }
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    /**
     * 필수 필드가 모두 입력되었는지 확인
     */
    public boolean isValidForSave() {
        return name != null && !name.trim().isEmpty();
        // businessNumber는 선택사항으로 처리 (일부 기업은 사업자등록번호가 없을 수 있음)
    }

    /**
     * 객체를 JSON 형태의 문자열로 변환 (디버깅용)
     */
    @Override
    public String toString() {
        return "Corporate{" +
                "customerId=" + customerId +
                ", name='" + name + '\'' +
                ", businessNumber='" + businessNumber + '\'' +
                ", representative='" + representative + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", address='" + address + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }

    /**
     * 두 Corporate 객체가 같은지 비교 (ID 기준)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Corporate corporate = (Corporate) obj;
        return customerId == corporate.customerId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(customerId);
    }

    /**
     * 표시용 이름 반환 (회사명 + 대표자)
     */
    public String getDisplayName() {
        if (representative != null && !representative.trim().isEmpty()) {
            return name + " (" + representative + ")";
        }
        return name;
    }

    /**
     * 사업자등록번호를 형식에 맞게 표시 (XXX-XX-XXXXX)
     */
    public String getFormattedBusinessNumber() {
        if (businessNumber == null || businessNumber.length() != 10) {
            return businessNumber;
        }
        
        return businessNumber.substring(0, 3) + "-" + 
               businessNumber.substring(3, 5) + "-" + 
               businessNumber.substring(5);
    }
}