package com.example.couponman_6;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.SharedPreferences;
import android.util.Log;

public final class ScreenOrientationHelper {
    private static final String TAG = "ScreenOrientationHelper";
    private static final String PREF_NAME = "AdminSettings";
    private static final String KEY_FORCE_ROTATION = "force_rotation";

    private ScreenOrientationHelper() {
    }

    public static void applyOrientation(Activity activity) {
        if (activity == null) {
            return;
        }

        try {
            SharedPreferences preferences = activity.getSharedPreferences(PREF_NAME, Activity.MODE_PRIVATE);
            boolean forceRotation = preferences.getBoolean(KEY_FORCE_ROTATION, false);

            activity.setRequestedOrientation(
                forceRotation
                    ? ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                    : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            );

            Log.i(TAG, "[ORIENTATION] " + (forceRotation ? "강제회전 활성화" : "세로 고정"));
        } catch (Exception e) {
            Log.e(TAG, "[ORIENTATION] 화면 방향 적용 오류", e);
        }
    }
}
