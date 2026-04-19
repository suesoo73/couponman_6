package com.example.couponman_6;

public class CouponTargetDraft {
    private String name;
    private String employeeCode;
    private boolean selected = true;

    public CouponTargetDraft() {
    }

    public CouponTargetDraft(String name, String employeeCode) {
        this.name = name;
        this.employeeCode = employeeCode;
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

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String resolveEmployeeCode() {
        if (employeeCode != null && !employeeCode.trim().isEmpty()) {
            return employeeCode.trim();
        }
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        return "";
    }

    public boolean isValid() {
        return selected && name != null && !name.trim().isEmpty() && !resolveEmployeeCode().isEmpty();
    }
}
