package com.luxpro.vip;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AnnouncementAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_PINNED = 1;
    private static final int TYPE_NORMAL = 2;

    private final List<AnnouncementModel> list;
    private final Context context;
    private int lastPosition = -1;

    public AnnouncementAdapter(Context context, List<AnnouncementModel> list) {
        this.context = context;
        this.list = list;
    }

    @Override
    public int getItemViewType(int position) {
        if (list.get(position).isPinned()) {
            return TYPE_PINNED;
        }
        return TYPE_NORMAL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_PINNED) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_announcement_pinned, parent, false);
            return new PinnedViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_announcement, parent, false);
            return new NormalViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AnnouncementModel model = list.get(position);
        
        // Add Entrance Animation
        setAnimation(holder.itemView, position);

        if (holder instanceof PinnedViewHolder) {
            PinnedViewHolder pinnedHolder = (PinnedViewHolder) holder;
            pinnedHolder.txtTitle.setText(model.getTitle());
            pinnedHolder.txtContent.setText(model.getContent());
            
            if (model.getTimestamp() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                pinnedHolder.txtDate.setText(sdf.format(new Date(model.getTimestamp())));
            } else {
                pinnedHolder.txtDate.setText(context.getString(R.string.now));
            }

            // Apply continuous pulse animation to the pinned container for VIP effect
            Animation pulse = AnimationUtils.loadAnimation(context, R.anim.pulse_infinite);
            pinnedHolder.itemView.startAnimation(pulse);

            if (model.getLinkUrl() != null && !model.getLinkUrl().trim().isEmpty()) {
                pinnedHolder.btnLink.setVisibility(View.VISIBLE);
                pinnedHolder.btnLink.setOnClickListener(v -> openLink(model.getLinkUrl().trim()));
            } else {
                pinnedHolder.btnLink.setVisibility(View.GONE);
            }
            
        } else if (holder instanceof NormalViewHolder) {
            NormalViewHolder normalHolder = (NormalViewHolder) holder;
            normalHolder.txtTitle.setText(model.getTitle());
            normalHolder.txtContent.setText(model.getContent());
            
            if (model.getTimestamp() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                normalHolder.txtDate.setText(sdf.format(new Date(model.getTimestamp())));
            } else {
                normalHolder.txtDate.setText(context.getString(R.string.now));
            }

            if (model.getLinkUrl() != null && !model.getLinkUrl().trim().isEmpty()) {
                normalHolder.btnLink.setVisibility(View.VISIBLE);
                normalHolder.btnLink.setOnClickListener(v -> openLink(model.getLinkUrl().trim()));
            } else {
                normalHolder.btnLink.setVisibility(View.GONE);
            }
        }
    }

    private void openLink(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            context.startActivity(intent);
        } catch (Exception e) {
            // Ignored
        }
    }
    
    private void setAnimation(View viewToAnimate, int position) {
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_glitch);
            viewToAnimate.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // --- ViewHolders ---

    public static class NormalViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtContent, txtDate;
        Button btnLink;

        public NormalViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtAnnTitle);
            txtContent = itemView.findViewById(R.id.txtAnnContent);
            txtDate = itemView.findViewById(R.id.txtAnnDate);
            btnLink = itemView.findViewById(R.id.btnAnnLink);
        }
    }

    public static class PinnedViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtContent, txtDate;
        Button btnLink;

        public PinnedViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtAnnTitle);
            txtContent = itemView.findViewById(R.id.txtAnnContent);
            txtDate = itemView.findViewById(R.id.txtAnnDate);
            btnLink = itemView.findViewById(R.id.btnAnnLink);
        }
    }
}
