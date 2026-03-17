package com.luxpro.max;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.concurrent.TimeUnit;

public class RewardsFragment extends Fragment {

    private ImageView imgChest;
    private TextView timerDisplay, txtRewardKey, txtHours, txtMinutes, txtSeconds;
    private Button btnShowGift, btnCopyRewardKey;
    private LinearLayout layoutGiftDetails, layoutGiftReady;
    private Animation boxAppearAnim, fadeInAnim;
    private MediaPlayer soundAppear, soundOpen, soundReward;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rewards, container, false);

        imgChest          = view.findViewById(R.id.imgChest);
        timerDisplay      = view.findViewById(R.id.timerDisplay);
        btnShowGift       = view.findViewById(R.id.btnShowGift);
        layoutGiftDetails = view.findViewById(R.id.layoutGiftDetails);
        layoutGiftReady   = view.findViewById(R.id.layoutGiftReady);
        txtRewardKey      = view.findViewById(R.id.txtRewardKey);
        btnCopyRewardKey  = view.findViewById(R.id.btnCopyRewardKey);
        txtHours          = view.findViewById(R.id.txtHours);
        txtMinutes        = view.findViewById(R.id.txtMinutes);
        txtSeconds        = view.findViewById(R.id.txtSeconds);

        boxAppearAnim = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in_bounce);
        fadeInAnim = AnimationUtils.loadAnimation(getContext(), R.anim.item_fade_in);
        setupSounds();
        handleRewardSystem();

        return view;
    }

    private void setupSounds() {
        try {
            soundAppear = MediaPlayer.create(getContext(), R.raw.box_appear);
            soundOpen = MediaPlayer.create(getContext(), R.raw.box_open);
            soundReward = MediaPlayer.create(getContext(), R.raw.reward_sound);
        } catch (Exception ignored) {}
    }

    private com.google.firebase.database.ValueEventListener rewardListener;
    private CountDownTimer countdownTimer;

    private void handleRewardSystem() {
        rewardListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                if (!isAdded() || getContext() == null) return;
                
                // 5-day cycle = 432,000,000 ms
                long cycleMillis = 5 * 24 * 60 * 60 * 1000L;
                long windowMillis = 60 * 60 * 1000L; // 1-hour window
                long now = TimeSyncManager.getServerTime();
                
                long currentCycleStart = (now / cycleMillis) * cycleMillis;
                long windowEnd = currentCycleStart + windowMillis;
                long nextCycleStart = currentCycleStart + cycleMillis;

                // Check if user already claimed for THIS window
                String windowId = "reward_win_" + (currentCycleStart / cycleMillis);
                android.content.SharedPreferences prefs = requireContext().getSharedPreferences("LUX_REWARDS", android.content.Context.MODE_PRIVATE);
                boolean claimed = prefs.getBoolean(windowId, false);

                if (now < windowEnd && !claimed) {
                    // We are inside the 1-hour open window and not claimed yet
                    btnShowGift.setVisibility(View.VISIBLE);
                    layoutGiftReady.setVisibility(View.VISIBLE);
                    // Show READY in LED boxes
                    if (txtHours != null) txtHours.setText("00");
                    if (txtMinutes != null) txtMinutes.setText("00");
                    if (txtSeconds != null) txtSeconds.setText("00");
                    setupGiftAction(snapshot, windowId, windowEnd);
                } else {
                    // Window closed or already claimed -> show countdown to NEXT cycle
                    btnShowGift.setVisibility(View.GONE);
                    layoutGiftReady.setVisibility(View.GONE);
                    startCountdown(nextCycleStart - now);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
        FirebaseDatabase.getInstance().getReference("Rewards").addValueEventListener(rewardListener);
    }

    private void setupGiftAction(DataSnapshot snapshot, String windowId, long windowEnd) {
        btnShowGift.setOnClickListener(v -> {
            btnShowGift.setVisibility(View.GONE);
            imgChest.setVisibility(View.VISIBLE);
            imgChest.setImageResource(R.drawable.chest_closed);
            if (soundAppear != null) soundAppear.start();
            imgChest.startAnimation(boxAppearAnim);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (!isAdded() || getContext() == null) return;
                imgChest.setImageResource(R.drawable.chest_open);
                if (soundOpen != null) soundOpen.start();
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (!isAdded() || getContext() == null) return;
                    imgChest.setVisibility(View.GONE);
                    String rewardKey = snapshot.child("global_key").getValue(String.class);
                    showGiftDetails(rewardKey != null ? rewardKey : "LUX-FREE-TEMP");
                    
                    // Mark as claimed for this window
                    requireContext().getSharedPreferences("LUX_REWARDS", android.content.Context.MODE_PRIVATE)
                            .edit().putBoolean(windowId, true).apply();
                }, 1500);
            }, 1000);
        });
    }

    private void showGiftDetails(String key) {
        layoutGiftDetails.setVisibility(View.VISIBLE);
        layoutGiftDetails.startAnimation(fadeInAnim);
        if (soundReward != null) soundReward.start();
        txtRewardKey.setText(key);

        btnCopyRewardKey.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Reward Key", key);
            clipboard.setPrimaryClip(clip);
            android.widget.Toast.makeText(getContext(), getString(R.string.hwid_copied_toast), android.widget.Toast.LENGTH_SHORT).show();
        });
    }

    private void startCountdown(long duration) {
        if (countdownTimer != null) countdownTimer.cancel();
        countdownTimer = new CountDownTimer(duration, 1000) {
            public void onTick(long millisUntilFinished) {
                if (!isAdded() || getContext() == null) return;
                long h = TimeUnit.MILLISECONDS.toHours(millisUntilFinished);
                long m = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) % 60;
                long s = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60;
                if (txtHours   != null) txtHours.setText(String.format("%02d", h));
                if (txtMinutes != null) txtMinutes.setText(String.format("%02d", m));
                if (txtSeconds != null) txtSeconds.setText(String.format("%02d", s));
            }
            public void onFinish() {
                if (!isAdded() || getContext() == null) return;
                btnShowGift.setVisibility(View.VISIBLE);
                layoutGiftReady.setVisibility(View.VISIBLE);
                if (txtHours   != null) txtHours.setText("00");
                if (txtMinutes != null) txtMinutes.setText("00");
                if (txtSeconds != null) txtSeconds.setText("00");
            }
        }.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (rewardListener != null) {
            FirebaseDatabase.getInstance().getReference("Rewards").removeEventListener(rewardListener);
        }
        if (countdownTimer != null) {
            countdownTimer.cancel();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (soundAppear != null) soundAppear.release();
        if (soundOpen != null) soundOpen.release();
        if (soundReward != null) soundReward.release();
    }
}