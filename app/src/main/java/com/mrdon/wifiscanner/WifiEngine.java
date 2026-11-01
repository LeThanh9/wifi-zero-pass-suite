package com.mrdon.wifiscanner;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.net.wifi.hotspot2.PasspointConfiguration;
import android.net.wifi.hotspot2.pps.HomeSp;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WifiEngine: Manages all standard Android WLAN scanning and zero-pass connection methods.
 * Architecture supports Android 8.0 (API 26) through Android 14+ (API 34).
 */
public class WifiEngine {

    private static final String TAG = "WifiEngine";

    private final Context context;
    private final WifiManager wifiManager;
    private final ConnectivityManager connectivityManager;

    // In-memory Community / Known Open Hotspot Directory
    private static final Map<String, String> COMMUNITY_HOTSPOTS = new HashMap<>();

    static {
        // Pre-shared public / cafe common default credentials for automatic pairing
        COMMUNITY_HOTSPOTS.put("CoffeeHouse_Guest", "coffeehouse2024");
        COMMUNITY_HOTSPOTS.put("Highlands_Free_WiFi", "12345678");
        COMMUNITY_HOTSPOTS.put("PhucLong_Guest", "phuclongtea");
        COMMUNITY_HOTSPOTS.put("Free_City_WiFi", ""); // Open network
    }

    public WifiEngine(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.wifiManager = (WifiManager) this.context.getSystemService(Context.WIFI_SERVICE);
        this.connectivityManager = (ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public boolean isWifiEnabled() {
        return wifiManager != null && wifiManager.isWifiEnabled();
    }

    public boolean triggerScan() {
        if (wifiManager == null) return false;
        return wifiManager.startScan();
    }

    public List<ScanResult> getScanResults() {
        if (wifiManager == null) return Collections.emptyList();
        try {
            return wifiManager.getScanResults();
        } catch (SecurityException e) {
            Log.e(TAG, "Missing location/nearby permission for scanning", e);
            return Collections.emptyList();
        }
    }

    /**
     * Determine security classification of a ScanResult.
     */
    public static String getSecurityType(ScanResult result) {
        String cap = result.capabilities.toUpperCase();
        if (cap.contains("WPA3")) return "WPA3";
        if (cap.contains("WPA2")) return "WPA2";
        if (cap.contains("WPA")) return "WPA";
        if (cap.contains("WEP")) return "WEP";
        if (cap.contains("EAP")) return "802.1x EAP";
        return "OPEN";
    }

    public static boolean isOpenNetwork(ScanResult result) {
        return "OPEN".equalsIgnoreCase(getSecurityType(result));
    }

    public static boolean supportsWps(ScanResult result) {
        return result.capabilities != null && result.capabilities.toUpperCase().contains("WPS");
    }

    // =========================================================================
    // METHOD 1: CONNECT TO OPEN NETWORKS WITHOUT PASSWORD
    // =========================================================================
    public boolean connectOpenNetwork(String ssid) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiNetworkSuggestion suggestion = new WifiNetworkSuggestion.Builder()
                    .setSsid(ssid)
                    .setIsAppInteractionRequired(false)
                    .build();

            List<WifiNetworkSuggestion> list = new ArrayList<>();
            list.add(suggestion);
            int status = wifiManager.addNetworkSuggestions(list);
            return status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS;
        } else {
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + ssid + "\"";
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            int netId = wifiManager.addNetwork(conf);
            if (netId != -1) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(netId, true);
                wifiManager.reconnect();
                return true;
            }
            return false;
        }
    }

    // =========================================================================
    // METHOD 2: CONNECT VIA QR CODE SCAN WITHOUT USER ENTERING PASSWORD
    // =========================================================================
    public boolean connectViaQrCode(String qrContent) {
        QrWifiModel model = parseWifiQr(qrContent);
        if (model == null || model.ssid == null || model.ssid.isEmpty()) {
            return false;
        }

        if ("OPEN".equalsIgnoreCase(model.type) || model.password.isEmpty()) {
            return connectOpenNetwork(model.ssid);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            WifiNetworkSuggestion.Builder builder = new WifiNetworkSuggestion.Builder()
                    .setSsid(model.ssid)
                    .setIsHiddenSsid(model.hidden);

            if ("WPA3".equalsIgnoreCase(model.type)) {
                builder.setWpa3Passphrase(model.password);
            } else {
                builder.setWpa2Passphrase(model.password);
            }

            int status = wifiManager.addNetworkSuggestions(Collections.singletonList(builder.build()));
            return status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS;
        } else {
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = "\"" + model.ssid + "\"";
            conf.preSharedKey = "\"" + model.password + "\"";
            int netId = wifiManager.addNetwork(conf);
            if (netId != -1) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(netId, true);
                wifiManager.reconnect();
                return true;
            }
            return false;
        }
    }

    // =========================================================================
    // METHOD 3: WI-FI EASY CONNECT (DPP - Device Provisioning Protocol)
    // =========================================================================
    @RequiresApi(api = Build.VERSION_CODES.Q)
    public boolean isEasyConnectSupported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && wifiManager.isEasyConnectSupported();
    }

    // =========================================================================
    // METHOD 4: PASSPOINT (Hotspot 2.0 / 802.11u) ROAMING CONFIGURATION
    // =========================================================================
    public boolean registerPasspointProfile(String fqdn, String friendlyName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                PasspointConfiguration config = new PasspointConfiguration();
                HomeSp homeSp = new HomeSp();
                homeSp.setFqdn(fqdn);
                homeSp.setFriendlyName(friendlyName);
                config.setHomeSp(homeSp);
                wifiManager.addOrUpdatePasspointConfiguration(config);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Passpoint registration failed", e);
                return false;
            }
        }
        return false;
    }

    // =========================================================================
    // METHOD 5: COMMUNITY / PRE-SHARED HOTSPOTS AUTO-MATCHING
    // =========================================================================
    public boolean checkAndConnectCommunityMatch(String ssid) {
        if (COMMUNITY_HOTSPOTS.containsKey(ssid)) {
            String knownPassword = COMMUNITY_HOTSPOTS.get(ssid);
            if (knownPassword == null || knownPassword.isEmpty()) {
                return connectOpenNetwork(ssid);
            } else {
                String qrFormat = "WIFI:T:WPA;S:" + ssid + ";P:" + knownPassword + ";;";
                return connectViaQrCode(qrFormat);
            }
        }
        return false;
    }

    // =========================================================================
    // METHOD 6: CAPTIVE PORTAL AUTO-ASSISTANT
    // =========================================================================
    public boolean isCaptivePortalActive() {
        if (connectivityManager == null) return false;
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) return false;
        NetworkCapabilities caps = connectivityManager.getNetworkCapabilities(activeNetwork);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL);
    }

    // =========================================================================
    // QR PARSER MODEL & HELPER
    // =========================================================================
    public static class QrWifiModel {
        public String type = "WPA";
        public String ssid = "";
        public String password = "";
        public boolean hidden = false;
    }

    public static QrWifiModel parseWifiQr(String raw) {
        if (raw == null || !raw.startsWith("WIFI:")) return null;
        QrWifiModel model = new QrWifiModel();
        String payload = raw.substring(5);
        String[] tokens = payload.split(";");
        for (String t : tokens) {
            if (t.startsWith("T:")) model.type = t.substring(2);
            else if (t.startsWith("S:")) model.ssid = t.substring(2);
            else if (t.startsWith("P:")) model.password = t.substring(2);
            else if (t.startsWith("H:")) model.hidden = "true".equalsIgnoreCase(t.substring(2));
        }
        return model;
    }
}

