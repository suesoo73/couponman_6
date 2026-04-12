package com.example.couponman_6;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Employee {
    private int employeeId;
    private int corporateId;
    private String name;
    private String employeeCode;
    private String phone;
    private String email;
    private String department;
    private String createdAt;

    public Employee() {
    }

    public Employee(int employeeId, int corporateId, String name, String employeeCode,
                    String department, String createdAt) {
        this.employeeId = employeeId;
        this.corporateId = corporateId;
        this.name = name;
        this.employeeCode = employeeCode;
        this.department = department;
        this.createdAt = createdAt;
    }

    public Employee(int corporateId, String name, String employeeCode, String department) {
        this.corporateId = corporateId;
        this.name = name;
        this.employeeCode = employeeCode;
        this.department = department;
        this.createdAt = getCurrentTimestamp();
    }

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

    public String getEmployeeCode() {
        return employeeCode;
    }

    public void setEmployeeCode(String employeeCode) {
        this.employeeCode = employeeCode;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
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

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isValidForSave() {
        return corporateId > 0 && employeeCode != null && !employeeCode.trim().isEmpty();
    }

    public String getDisplayName() {
        String baseName = name != null && !name.trim().isEmpty() ? name : employeeCode;
        if (department != null && !department.trim().isEmpty()) {
            return baseName + " (" + department + ")";
        }
        return baseName;
    }

    public String resolveIdentifier() {
        if (employeeCode != null && !employeeCode.trim().isEmpty()) {
            return employeeCode.trim();
        }
        return name != null ? name.trim() : "";
    }

    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date());
    }
}
