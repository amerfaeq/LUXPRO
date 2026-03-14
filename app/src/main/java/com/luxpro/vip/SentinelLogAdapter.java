package com.luxpro.vip;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class SentinelLogAdapter extends RecyclerView.Adapter<SentinelLogAdapter.LogViewHolder> {

    private final List<SentinelLogModel> logList;

    public SentinelLogAdapter(List<SentinelLogModel> logList) {
        this.logList = logList;
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sentinel_log, parent, false);
        return new LogViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        SentinelLogModel model = logList.get(position);
        holder.txtTime.setText(model.time);
        holder.txtStatus.setText(MasterKeyUtils.decryptTrace(model.scanStatus));
        holder.txtHwid.setText(holder.itemView.getContext().getString(R.string.hm_hwid_prefix) + model.hwid);
        holder.txtVersion.setText(holder.itemView.getContext().getString(R.string.hm_game_ver_prefix) + model.gameVersion);
        
        // Dynamic color for status
        if (model.scanStatus != null && model.scanStatus.contains("FAIL")) {
            holder.txtStatus.setTextColor(0xFFFF4444); // Red
        } else {
            holder.txtStatus.setTextColor(0xFF00FF88); // Green
        }
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView txtTime, txtStatus, txtHwid, txtVersion;

        public LogViewHolder(@NonNull View v) {
            super(v);
            txtTime = v.findViewById(R.id.txtLogTime);
            txtStatus = v.findViewById(R.id.txtLogStatus);
            txtHwid = v.findViewById(R.id.txtLogHwid);
            txtVersion = v.findViewById(R.id.txtLogVersion);
        }
    }
}
