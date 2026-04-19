package com.example.couponman_6;

import java.util.ArrayList;
import java.util.List;

public class CouponPreset {
    private String name;
    private int corporateId;
    private List<CouponTargetDraft> targets = new ArrayList<>();

    public CouponPreset() {
    }

    public CouponPreset(String name, int corporateId, List<CouponTargetDraft> targets) {
        this.name = name;
        this.corporateId = corporateId;
        this.targets = targets;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCorporateId() {
        return corporateId;
    }

    public void setCorporateId(int corporateId) {
        this.corporateId = corporateId;
    }

    public List<CouponTargetDraft> getTargets() {
        return targets;
    }

    public void setTargets(List<CouponTargetDraft> targets) {
        this.targets = targets;
    }
}
