package com.example.couponman_6;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CouponPresetStore {
    private static final String PREF_NAME = "CouponPresetStore";
    private static final String KEY_PRESETS = "presets";

    private final SharedPreferences preferences;
    private final Gson gson = new Gson();
    private final Type listType = new TypeToken<List<CouponPreset>>(){}.getType();

    public CouponPresetStore(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<CouponPreset> getPresets() {
        String json = preferences.getString(KEY_PRESETS, "[]");
        List<CouponPreset> presets = gson.fromJson(json, listType);
        return presets != null ? presets : new ArrayList<>();
    }

    public void savePreset(CouponPreset preset) {
        List<CouponPreset> presets = getPresets();
        presets.removeIf(item -> item.getName() != null && item.getName().equals(preset.getName()));
        presets.add(preset);
        preferences.edit().putString(KEY_PRESETS, gson.toJson(presets, listType)).apply();
    }
}
