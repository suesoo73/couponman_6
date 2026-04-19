package com.example.couponman_6;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebDashboardActivity extends android.app.Activity {
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ScreenOrientationHelper.applyOrientation(this);
        setContentView(R.layout.activity_web_dashboard);

        webView = findViewById(R.id.dashboardWebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new DashboardWebViewClient());
        webView.loadUrl("file:///android_asset/web/dashboard.html");
    }

    @Override
    protected void onResume() {
        super.onResume();
        ScreenOrientationHelper.applyOrientation(this);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }
        super.onBackPressed();
    }

    private class DashboardWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return handleUrl(request.getUrl());
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return handleUrl(Uri.parse(url));
        }

        private boolean handleUrl(Uri uri) {
            if (!"app".equals(uri.getScheme())) {
                return false;
            }

            String host = uri.getHost();
            Intent intent = null;

            if ("customer-management".equals(host)) {
                intent = new Intent(WebDashboardActivity.this, CustomerManagementActivity.class);
            } else if ("coupon-management".equals(host)) {
                intent = new Intent(WebDashboardActivity.this, CouponManagementActivity.class);
            } else if ("management-info".equals(host)) {
                intent = new Intent(WebDashboardActivity.this, ManagementInfoActivity.class);
            } else if ("coupon-list".equals(host)) {
                intent = new Intent(WebDashboardActivity.this, CouponListActivity.class);
            } else if ("qr-scan".equals(host)) {
                intent = new Intent(WebDashboardActivity.this, QRScanActivity.class);
            } else if ("server-info".equals(host)) {
                intent = new Intent(WebDashboardActivity.this, ServerInfoActivity.class);
            } else if ("admin-settings".equals(host)) {
                intent = new Intent(WebDashboardActivity.this, AdminSettingsActivity.class);
            }

            if (intent != null) {
                startActivity(intent);
                return true;
            }

            return false;
        }
    }
}
