package com.example.couponman_6;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Employee {
    private int employeeId;
    private int corporateId;  // 거래처 테이블의 customer_id와 연동
    private String name;
    private String phone;
    private String email;
    private String department;
    private String createdAt;

    // 기본 생성자
    public Employee() {
    }

    // 모든 필드를 포함한 생성자
    public Employee(int employeeId, int corporateId, String name, String phone, 
                   String email, String department, String createdAt) {
        this.employeeId = employeeId;
        this.corporateId = corporateId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.department = department;
        this.createdAt = createdAt;
    }

    // 새로운 직원 생성용 생성자 (ID 제외)
    public Employee(int corporateId, String name, String phone, String email, String department) {
        this.corporateId = corporateId;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.department = department;
        this.createdAt = getCurrentTimestamp();
    }

    // Getter와 Setter 메소드들
    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public int getCorporateId() {
        return corporateId;
    }

    public void setCorporateId(int corporateId) {
        this.corporateId = corporateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
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
     * 핸드폰 번호가 유효한 형식인지 확인
     * (010-XXXX-XXXX 형식)
     */
    public boolean isValidPhone() {
        return phone != null && 
               phone.matches("01[0-9]-\\d{4}-\\d{4}");
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
        return corporateId > 0 && 
               name != null && !name.trim().isEmpty() &&
               phone != null && !phone.trim().isEmpty();
    }

    /**
     * 객체를 문자열로 변환 (디버깅용)
     */
    @Override
    public String toString() {
        return "Employee{" +
                "employeeId=" + employeeId +
                ", corporateId=" + corporateId +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", department='" + department + '\'' +
                ", createdAt='" + createdAt + '\'' +
                '}';
    }

    /**
     * 두 Employee 객체가 같은지 비교 (ID 기준)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        Employee employee = (Employee) obj;
        return employeeId == employee.employeeId;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(employeeId);
    }

    /**
     * 표시용 이름 반환 (이름 + 부서)
     */
    public String getDisplayName() {
        if (department != null && !department.trim().isEmpty()) {
            return name + " (" + department + ")";
        }
        return name;
    }

    /**
     * 핸드폰 번호를 마스킹해서 표시 (010-1234-XXXX)
     */
    public String getMaskedPhone() {
        if (phone == null || phone.length() < 13) {
            return phone;
        }
        
        // 010-1234-5678 -> 010-1234-XXXX
        String prefix = phone.substring(0, 9);
        return prefix + "XXXX";
    }
}