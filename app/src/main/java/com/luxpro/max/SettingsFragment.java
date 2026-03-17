package com.luxpro.max;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.concurrent.TimeUnit;

public class SettingsFragment extends Fragment {

    private TextView txtTimeRemaining, txtUserEmail;
    private Button btnBuyKeys, btnPrivacy, btnToggleLanguage, btnTelegramLink;
    private SeekBar seekBarIconSize, seekBarIconOpacity;
    private android.content.SharedPreferences preferences;
    // Reference to Firebase listener for cleanup
    private com.google.firebase.database.DatabaseReference expiryRef;
    private com.google.firebase.database.ValueEventListener expiryListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        txtTimeRemaining = view.findViewById(R.id.txtProfileSubscription);
        txtUserEmail = view.findViewById(R.id.txtProfileEmail);
        btnBuyKeys = view.findViewById(R.id.btnSettingsResellers);
        btnPrivacy = view.findViewById(R.id.btnPrivacy);
        btnTelegramLink = view.findViewById(R.id.btnTelegram);
        btnToggleLanguage = view.findViewById(R.id.btnLanguageToggleSettings);
        seekBarIconSize = view.findViewById(R.id.seekIconSize);
        seekBarIconOpacity = view.findViewById(R.id.seekIconOpacity);
        TextView txtHwidSettings = view.findViewById(R.id.txtHwidSettings);

        preferences = requireActivity().getSharedPreferences("LUX_PREFS", Context.MODE_PRIVATE);

        // HWID
        String hwid = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        txtHwidSettings.setText("LUX-" + hwid);
        txtHwidSettings.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireActivity()
                    .getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("HWID", hwid));
            Toast.makeText(getContext(), getString(R.string.hwid_copied), Toast.LENGTH_SHORT).show();
        });

        displayUserProfile();
        fetchRemainingTime();
        setupCustomization();

        btnTelegramLink.setOnClickListener(
                v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Y8YY9"))));
        btnPrivacy.setOnClickListener(v -> showPrivacyPolicy());
        btnBuyKeys.setOnClickListener(v -> startActivity(new Intent(requireActivity(), ResellerActivity.class)));

        // ── زر تبديل اللغة ──
        btnToggleLanguage.setOnClickListener(v -> {
            String newLang = LocaleHelper.toggleLanguage(requireContext());
            Toast.makeText(getContext(),
                    newLang.equals(LocaleHelper.LANG_AR) ? getString(R.string.switched_to_ar) : getString(R.string.switched_to_en),
                    Toast.LENGTH_SHORT).show();
            // إعادة تشغيل MainActivity لتطبيق اللغة على كل الشاشات
            Intent restart = requireActivity().getPackageManager()
                    .getLaunchIntentForPackage(requireActivity().getPackageName());
            if (restart != null) {
                restart.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                requireActivity().finishAffinity();
                startActivity(restart);
            }
        });

        // ── زر الخروج ──
        Button btnLogout = view.findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle(R.string.logout_title)
                        .setMessage(getString(R.string.logout_confirm))
                        .setPositiveButton(R.string.yes, (d, w) -> {
                            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
                            Intent intent = new Intent(requireActivity(), LoginActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            requireActivity().finish();
                        })
                        .setNegativeButton(R.string.no, null)
                        .show();
            });
        }

        return view;
    }

    private void setupCustomization() {
        seekBarIconSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                preferences.edit().putInt("icon_size", progress).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekBarIconOpacity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                preferences.edit().putInt("icon_opacity", progress).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private void updateFloatingService() {
        // Obsolete: Native Menu handles its own rendering
    }

    private void displayUserProfile() {
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            txtUserEmail.setText(getString(R.string.email_label) +
                    FirebaseAuth.getInstance().getCurrentUser().getEmail());
        }
    }

    private void fetchRemainingTime() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        expiryRef = FirebaseDatabase.getInstance().getReference("Users").child(uid).child("expiry_date");
        expiryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;
                if (snapshot.exists()) {
                    Long val = snapshot.getValue(Long.class);
                    if (val == null) return;
                    long expiryTime = val;
                    long diff = expiryTime - TimeSyncManager.getServerTime();
                    if (diff > 0) {
                        long days = TimeUnit.MILLISECONDS.toDays(diff);
                        long hours = TimeUnit.MILLISECONDS.toHours(diff) % 24;
                        txtTimeRemaining.setText(getString(R.string.subscription_label)
                                + days + " " + getString(R.string.days_left)
                                + " " + hours + getString(R.string.hours_short));
                    } else {
                        txtTimeRemaining.setText(getString(R.string.subscription_expired));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        expiryRef.addValueEventListener(expiryListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Remove Firebase listener to prevent memory/connection leak
        if (expiryRef != null && expiryListener != null) {
            expiryRef.removeEventListener(expiryListener);
        }
    }

    private void showPrivacyPolicy() {
        new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.privacy_policy))
                .setMessage(getString(R.string.privacy_msg))
                .setPositiveButton("OK", null)
                .show();
    }
}