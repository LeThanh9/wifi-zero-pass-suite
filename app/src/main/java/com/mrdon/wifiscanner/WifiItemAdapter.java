package com.mrdon.wifiscanner;

import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class WifiItemAdapter extends RecyclerView.Adapter<WifiItemAdapter.ViewHolder> {

    public interface OnConnectClickListener {
        void onConnectClick(ScanResult result, boolean isOpen);
    }

    private final List<ScanResult> list = new ArrayList<>();
    private final OnConnectClickListener listener;

    public WifiItemAdapter(OnConnectClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<ScanResult> newResults) {
        list.clear();
        if (newResults != null) {
            list.addAll(newResults);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wifi, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScanResult result = list.get(position);

        String ssid = (result.SSID != null && !result.SSID.isEmpty()) ? result.SSID : "(Mạng ẩn)";
        holder.tvSsid.setText(ssid);
        holder.tvBssid.setText("BSSID: " + result.BSSID);

        // Signal calculate (0 - 100%)
        int signalLevel = WifiManager.calculateSignalLevel(result.level, 100);
        holder.tvSignal.setText(signalLevel + "%");
        if (signalLevel >= 70) {
            holder.tvSignal.setTextColor(Color.parseColor("#00ffcc"));
        } else if (signalLevel >= 40) {
            holder.tvSignal.setTextColor(Color.parseColor("#ffcc00"));
        } else {
            holder.tvSignal.setTextColor(Color.parseColor("#ff5555"));
        }

        // Security
        String security = WifiEngine.getSecurityType(result);
        holder.tvSecurityBadge.setText(security);

        // Band
        String bandStr = (result.frequency > 4900) ? "5 GHz" : "2.4 GHz";
        if (result.frequency > 5925) bandStr = "6 GHz (Wi-Fi 6E)";
        holder.tvBandBadge.setText(bandStr);

        // Zero-pass status
        boolean isOpen = WifiEngine.isOpenNetwork(result);
        if (isOpen) {
            holder.tvZeroPassBadge.setVisibility(View.VISIBLE);
            holder.tvZeroPassBadge.setText("✅ Mạng Mở (1-Click)");
            holder.tvZeroPassBadge.setTextColor(Color.parseColor("#00e676"));
            holder.btnConnect.setText("Kết Nối Ngay");
        } else {
            boolean wps = WifiEngine.supportsWps(result);
            if (wps) {
                holder.tvZeroPassBadge.setVisibility(View.VISIBLE);
                holder.tvZeroPassBadge.setText("WPS Hỗ Trợ");
                holder.tvZeroPassBadge.setTextColor(Color.parseColor("#ffbb33"));
            } else {
                holder.tvZeroPassBadge.setVisibility(View.GONE);
            }
            holder.btnConnect.setText("Chi Tiết");
        }

        holder.btnConnect.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConnectClick(result, isOpen);
            }
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSsid, tvBssid, tvSignal, tvSecurityBadge, tvBandBadge, tvZeroPassBadge;
        Button btnConnect;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSsid = itemView.findViewById(R.id.tvSsid);
            tvBssid = itemView.findViewById(R.id.tvBssid);
            tvSignal = itemView.findViewById(R.id.tvSignal);
            tvSecurityBadge = itemView.findViewById(R.id.tvSecurityBadge);
            tvBandBadge = itemView.findViewById(R.id.tvBandBadge);
            tvZeroPassBadge = itemView.findViewById(R.id.tvZeroPassBadge);
            btnConnect = itemView.findViewById(R.id.btnConnect);
        }
    }
}

