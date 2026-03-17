package com.luxpro.max;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.FirebaseDatabase;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminAnnouncementAdapter extends RecyclerView.Adapter<AdminAnnouncementAdapter.ViewHolder> {
    private final Context context;
    private final List<AnnouncementModel> list;

    public AdminAnnouncementAdapter(Context context, List<AnnouncementModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_announcement, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AnnouncementModel model = list.get(position);

        holder.txtTitle.setText(model.getTitle());
        if (model.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.txtDate.setText(sdf.format(new Date(model.getTimestamp())));
        } else {
            holder.txtDate.setText(context.getString(R.string.now));
        }

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle(R.string.delete_ann_title)
                    .setMessage(context.getString(R.string.delete_ann_confirm, model.getTitle()))
                    .setPositiveButton(R.string.delete, (dialog, which) -> {
                        FirebaseDatabase.getInstance().getReference("Announcements")
                                .child(model.getId())
                                .removeValue()
                                .addOnSuccessListener(aVoid -> Toast.makeText(context, context.getString(R.string.ann_deleted_msg), Toast.LENGTH_SHORT).show());
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtDate;
        ImageButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtAdminAnnTitle);
            txtDate = itemView.findViewById(R.id.txtAdminAnnDate);
            btnDelete = itemView.findViewById(R.id.btnAdminDeleteAnn);
        }
    }
}
