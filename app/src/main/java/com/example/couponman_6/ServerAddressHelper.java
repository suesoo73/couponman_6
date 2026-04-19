package com.example.couponman_6;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public final class ServerAddressHelper {
    private ServerAddressHelper() {
    }

    public static String getDeviceIpAddress(Context context) {
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    int ipInt = wifiInfo.getIpAddress();
                    if (ipInt != 0) {
                        return Formatter.formatIpAddress(ipInt);
                    }
                }
            }

            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces) {
                List<InetAddress> addresses = Collections.list(networkInterface.getInetAddresses());
                for (InetAddress address : addresses) {
                    if (!address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
                        String hostAddress = address.getHostAddress();
                        if (hostAddress != null && hostAddress.indexOf(':') < 0) {
                            return hostAddress;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return null;
    }

    public static String getDashboardUrl(Context context, int port) {
        String ipAddress = getDeviceIpAddress(context);
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return "http://localhost:" + port + "/dashboard";
        }
        return "http://" + ipAddress + ":" + port + "/dashboard";
    }

    public static String getApiUrl(Context context, int port) {
        String ipAddress = getDeviceIpAddress(context);
        if (ipAddress == null || ipAddress.trim().isEmpty()) {
            return "http://localhost:" + port + "/api";
        }
        return "http://" + ipAddress + ":" + port + "/api";
    }
}
