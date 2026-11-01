package com.mrdon.wifiscanner;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQ_CODE = 1001;

    private WifiEngine wifiEngine;
    private WifiItemAdapter adapter;
    private SwipeRefreshLayout swipeRefresh;
    private TextView tvHeaderStatus;
    private ChipGroup chipGroupFilter;

    private List<ScanResult> fullResults = new ArrayList<>();

    private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false);
            swipeRefresh.setRefreshing(false);
            if (success) {
                fullResults = wifiEngine.getScanResults();
                tvHeaderStatus.setText("Đã quét thấy " + fullResults.size() + " mạng xung quanh");
                applyCurrentFilter();
            } else {
                tvHeaderStatus.setText("Không thể cập nhật danh sách quét mới.");
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wifiEngine = new WifiEngine(this);

        tvHeaderStatus = findViewById(R.id.tvHeaderStatus);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        chipGroupFilter = findViewById(R.id.chipGroupFilter);
        MaterialButton btnScanQr = findViewById(R.id.btnScanQr);
        RecyclerView rvWifiList = findViewById(R.id.rvWifiList);

        adapter = new WifiItemAdapter(this::onWifiItemSelected);
        rvWifiList.setLayoutManager(new LinearLayoutManager(this));
        rvWifiList.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::startWifiScan);

        btnScanQr.setOnClickListener(v -> launchQrScanner());

        chipGroupFilter.setOnCheckedStateChangeListener((group, checkedIds) -> applyCurrentFilter());

        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        List<String> needed = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.CAMERA);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.NEARBY_WIFI_DEVICES) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.NEARBY_WIFI_DEVICES);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERMISSION_REQ_CODE);
        } else {
            startWifiScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ_CODE) {
            boolean allGranted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            if (allGranted) {
                startWifiScan();
            } else {
                Toast.makeText(this, "Cần cấp quyền vị trí / thiết bị gần đây để quét Wi-Fi!", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void startWifiScan() {
        if (!wifiEngine.isWifiEnabled()) {
            Toast.makeText(this, "Vui lòng bật Wi-Fi trên thiết bị!", Toast.LENGTH_SHORT).show();
            swipeRefresh.setRefreshing(false);
            return;
        }

        swipeRefresh.setRefreshing(true);
        tvHeaderStatus.setText("Đang phát probe request quét các AP...");
        boolean initiated = wifiEngine.triggerScan();
        if (!initiated) {
            swipeRefresh.setRefreshing(false);
            // Fallback: try reading available cached results
            fullResults = wifiEngine.getScanResults();
            applyCurrentFilter();
        }
    }

    private void applyCurrentFilter() {
        int checkedId = chipGroupFilter.getCheckedChipId();
        List<ScanResult> filtered = new ArrayList<>();

        for (ScanResult sr : fullResults) {
            if (checkedId == R.id.chipOpenOnly) {
                if (WifiEngine.isOpenNetwork(sr)) {
                    filtered.add(sr);
                }
            } else if (checkedId == R.id.chipCommunity) {
                // Check if matches known open or shared SSID
                if (wifiEngine.checkAndConnectCommunityMatch(sr.SSID)) {
                    filtered.add(sr);
                }
            } else {
                filtered.add(sr);
            }
        }
        adapter.updateData(filtered);
    }

    private void onWifiItemSelected(ScanResult result, boolean isOpen) {
        String ssid = result.SSID;
        if (isOpen) {
            new AlertDialog.Builder(this)
                    .setTitle("Kết Nối Mạng Mở (No Pass)")
                    .setMessage("Mạng '" + ssid + "' không yêu cầu mật khẩu.\nBạn có muốn kết nối ngay?")
                    .setPositiveButton("Kết Nối", (dialog, which) -> {
                        boolean ok = wifiEngine.connectOpenNetwork(ssid);
                        if (ok) {
                            Toast.makeText(this, "Đã gửi yêu cầu kết nối tới " + ssid, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Không thể kích hoạt kết nối tự động.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        } else {
            // Check community database match first
            boolean communityOk = wifiEngine.checkAndConnectCommunityMatch(ssid);
            if (communityOk) {
                Toast.makeText(this, "Đã tìm thấy cấu hình điểm phát chung và nạp tự động!", Toast.LENGTH_LONG).show();
                return;
            }

            // Info dialog for protected APs
            new AlertDialog.Builder(this)
                    .setTitle(ssid)
                    .setMessage("Bảo mật: " + WifiEngine.getSecurityType(result) + "\n\n"
                            + "Phương thức kết nối không cần gõ mật khẩu:\n"
                            + "1. Nhấn 'Quét QR' để đọc mã QR Wi-Fi từ điện thoại khác hoặc điểm phát.\n"
                            + "2. Sử dụng chuẩn Wi-Fi Easy Connect (DPP) hoặc nhấn nút WPS trên router nếu có hỗ trợ.")
                    .setPositiveButton("Quét QR Mạng Này", (dialog, which) -> launchQrScanner())
                    .setNegativeButton("Đóng", null)
                    .show();
        }
    }

    private void launchQrScanner() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setPrompt("Hướng camera vào mã QR Wi-Fi để tự động nạp cấu hình");
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String raw = result.getContents();
                boolean success = wifiEngine.connectViaQrCode(raw);
                if (success) {
                    new AlertDialog.Builder(this)
                            .setTitle("Nạp Mã QR Thành Công")
                            .setMessage("Hệ thống đã nhận diện thông tin và tự động kích hoạt kết nối không cần nhập pass!")
                            .setPositiveButton("OK", null)
                            .show();
                } else {
                    Toast.makeText(this, "Mã QR không đúng định dạng WIFI:S:...;P:...;;", Toast.LENGTH_LONG).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(wifiScanReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            unregisterReceiver(wifiScanReceiver);
        } catch (IllegalArgumentException ignored) {}
    }
}

