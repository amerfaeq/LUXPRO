package com.luxpro.max;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.luxpro.max.BuildConfig;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import androidx.recyclerview.widget.RecyclerView;

public class AdminFragment extends Fragment {

    private DatabaseReference mDb;
    private String lastGeneratedKeys = "";
    private boolean currentUserActive = false;

    // Keys
    private EditText edKeyDays, edKeyCount;
    private TextView txtGeneratedKeys;
    private Button btnGenerateKeys, btnCopyKeys;

    // Rewards
    private EditText edGiftPassword;
    private TextView txtCurrentReward;
    private Button btnUpdateReward;

    // Users
    private EditText edUserUid, edExtendDays;
    private TextView txtUserInfo;
    private Button btnSearchUser, btnExtendUser, btnToggleUserActive;

    // Announcements
    private EditText edAnnTitle, edAnnContent, edAnnLink;
    private android.widget.CheckBox cbAnnPinned;
    private Button btnPublishAnn;
    private RecyclerView rvAdminAnnouncements;
    private AdminAnnouncementAdapter annAdapter;
    private java.util.List<AnnouncementModel> adminAnnList;

    // Resellers
    private EditText edResellerName, edResellerTelegram, edResellerImageUrl;
    private Spinner spinnerCountry;
    private TextView txtResellerStatus;
    private Button btnAddReseller, btnDeleteReseller;

    // Force Update
    private EditText edUpdateVersion, edUpdateApkUrl;
    private TextView txtCurrentVersion;
    private Button btnPublishUpdate;

    // Sentinel Health Monitor
    private TextView txtSentinelGlobalStatus;
    private RecyclerView rvSentinelFeed;
    private SentinelLogAdapter sentinelAdapter;
    private java.util.List<SentinelLogModel> sentinelLogList;
    private Button btnDownloadTrace, btnPushOffset;



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin, container, false);
        mDb = FirebaseDatabase.getInstance().getReference();
        initViews(view);
        setupCountrySpinner();
        loadCurrentData();
        loadSentinelData();
        setupListeners();
        setupSentinelListeners();
        return view;
    }

    private void initViews(View v) {
        edKeyDays = v.findViewById(R.id.edKeyDays);
        edKeyCount = v.findViewById(R.id.edKeyCount);
        txtGeneratedKeys = v.findViewById(R.id.txtGeneratedKeys);
        btnGenerateKeys = v.findViewById(R.id.btnGenerateKeys);
        btnCopyKeys = v.findViewById(R.id.btnCopyKeys);

        edGiftPassword = v.findViewById(R.id.edGiftPassword); // Used for Global Reward Key

        // Sentinel
        txtSentinelGlobalStatus = v.findViewById(R.id.txtSentinelGlobalStatus);
        rvSentinelFeed = v.findViewById(R.id.rvSentinelFeed);
        btnDownloadTrace = v.findViewById(R.id.btnDownloadTrace);
        btnPushOffset = v.findViewById(R.id.btnPushOffset);
        
        sentinelLogList = new java.util.ArrayList<>();
        sentinelAdapter = new SentinelLogAdapter(sentinelLogList);
        rvSentinelFeed.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        rvSentinelFeed.setAdapter(sentinelAdapter);
        // edGiftHours = v.findViewById(R.id.edGiftHours); // Not used in new system
        txtCurrentReward = v.findViewById(R.id.txtCurrentReward);
        btnUpdateReward = v.findViewById(R.id.btnUpdateReward);

        edUserUid = v.findViewById(R.id.edUserUid);
        edExtendDays = v.findViewById(R.id.edExtendDays);
        txtUserInfo = v.findViewById(R.id.txtUserInfo);
        btnSearchUser = v.findViewById(R.id.btnSearchUser);
        btnExtendUser = v.findViewById(R.id.btnExtendUser);
        btnToggleUserActive = v.findViewById(R.id.btnToggleUserActive);

        edAnnTitle = v.findViewById(R.id.edAnnTitle);
        edAnnContent = v.findViewById(R.id.edAnnContent);
        edAnnLink = v.findViewById(R.id.edAnnLink);
        cbAnnPinned = v.findViewById(R.id.cbAnnPinned);
        btnPublishAnn = v.findViewById(R.id.btnPublishAnn);
        
        rvAdminAnnouncements = v.findViewById(R.id.rvAdminAnnouncements);
        rvAdminAnnouncements.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        adminAnnList = new java.util.ArrayList<>();
        annAdapter = new AdminAnnouncementAdapter(getContext(), adminAnnList);
        rvAdminAnnouncements.setAdapter(annAdapter);

        edResellerName = v.findViewById(R.id.edResellerName);
        edResellerTelegram = v.findViewById(R.id.edResellerTelegram);
        edResellerImageUrl = v.findViewById(R.id.edResellerImageUrl);
        spinnerCountry = v.findViewById(R.id.spinnerCountry);
        txtResellerStatus = v.findViewById(R.id.txtResellerStatus);
        btnAddReseller = v.findViewById(R.id.btnAddReseller);
        btnDeleteReseller = v.findViewById(R.id.btnDeleteReseller);

        edUpdateVersion = v.findViewById(R.id.edUpdateVersion);
        edUpdateApkUrl = v.findViewById(R.id.edUpdateApkUrl);
        txtCurrentVersion = v.findViewById(R.id.txtCurrentVersion);
        btnPublishUpdate = v.findViewById(R.id.btnPublishUpdate);
        
        Button btnDeleteUpdate = v.findViewById(R.id.btnDeleteUpdate);
        btnDeleteUpdate.setOnClickListener(v1 -> {
            mDb.child("AppUpdate").removeValue().addOnCompleteListener(task -> {
                if(task.isSuccessful()) {
                    Toast.makeText(getContext(), getString(R.string.admin_delete_update_success), Toast.LENGTH_SHORT).show();
                    txtCurrentVersion.setText(getString(R.string.admin_delete_update_status));
                }
            });
        });
        
        Button btnAdminLogout = v.findViewById(R.id.btnAdminLogout);
        btnAdminLogout.setOnClickListener(v1 -> {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
            android.content.Intent restart = requireActivity().getPackageManager()
                    .getLaunchIntentForPackage(requireActivity().getPackageName());
            if (restart != null) {
                restart.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP | android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                requireActivity().finishAffinity();
                startActivity(restart);
            }
        });
    }

    private void setupCountrySpinner() {
        String[] countries = getResources().getStringArray(R.array.country_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, countries);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCountry.setAdapter(adapter);
    }

    private ValueEventListener rewardListener, announcementsListener, updateListener;

    private void loadCurrentData() {
        // المكافأة الحالية
        rewardListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!isAdded() || getContext() == null) return;
                if (!snap.exists()) {
                    txtCurrentReward.setText(getString(R.string.admin_no_reward));
                    return;
                }
                // Calculate 5-day cycle for info display
                long cycleMillis = 5 * 24 * 60 * 60 * 1000L;
                long now = TimeSyncManager.getServerTime();
                long nextCycle = ((now / cycleMillis) + 1) * cycleMillis;
                long diff = nextCycle - now;
                
                String key = snap.child("global_key").getValue(String.class);
                String timeStr = getString(R.string.admin_next_cycle, TimeUnit.MILLISECONDS.toDays(diff), (TimeUnit.MILLISECONDS.toHours(diff) % 24));
                txtCurrentReward.setText(timeStr + getString(R.string.admin_global_key, (key != null ? key : "—")));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        };
        mDb.child("Rewards").addValueEventListener(rewardListener);

        // قراءة الإعلانات الحالية لإدارتها (حذفها)
        announcementsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;
                adminAnnList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    AnnouncementModel model = ds.getValue(AnnouncementModel.class);
                    if (model != null) {
                        model.setId(ds.getKey());
                        adminAnnList.add(model);
                    }
                }
                // ترتيب الأحدث أولاً
                java.util.Collections.sort(adminAnnList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                annAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        };
        mDb.child("Announcements").addValueEventListener(announcementsListener);

        // إصدار التحديث الحالي
        updateListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!isAdded() || getContext() == null) return;
                if (!snap.exists()) {
                    txtCurrentVersion
                            .setText(getString(R.string.admin_no_update, BuildConfig.VERSION_CODE));
                    return;
                }
                Long ver = snap.child("version").getValue(Long.class);
                String url = snap.child("apk_url").getValue(String.class);
                txtCurrentVersion.setText(getString(R.string.admin_update_status, String.valueOf(ver != null ? ver : "—"), String.valueOf(BuildConfig.VERSION_CODE), (url != null ? url.substring(0, Math.min(50, url.length())) + "..." : "—")));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
            }
        };
        mDb.child("AppUpdate").addValueEventListener(updateListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mDb != null) {
            if (rewardListener != null) mDb.child("Rewards").removeEventListener(rewardListener);
            if (announcementsListener != null) mDb.child("Announcements").removeEventListener(announcementsListener);
            if (updateListener != null) mDb.child("AppUpdate").removeEventListener(updateListener);
        }
    }

    private void setupListeners() {

        // ═══ إنشاء مفاتيح ═══
        btnGenerateKeys.setOnClickListener(v -> {
            String daysStr = edKeyDays.getText().toString().trim();
            String countStr = edKeyCount.getText().toString().trim();
            if (daysStr.isEmpty() || countStr.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.admin_choose_days_keys), Toast.LENGTH_SHORT).show();
                return;
            }
            int days, count;
            try {
                days = Integer.parseInt(daysStr);
                count = Integer.parseInt(countStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), getString(R.string.admin_enter_numbers), Toast.LENGTH_SHORT).show();
                return;
            }
            if (count > 50) {
                Toast.makeText(getContext(), getString(R.string.admin_key_limit), Toast.LENGTH_SHORT).show();
                return;
            }
            btnGenerateKeys.setEnabled(false);
            btnGenerateKeys.setText(getString(R.string.admin_generating));
            StringBuilder keysBuilder = new StringBuilder();
            final int[] done = { 0 };
            for (int i = 0; i < count; i++) {
                String key = "LUX-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
                Map<String, Object> keyData = new HashMap<>();
                keyData.put("days", days);
                keyData.put("used", false);
                keyData.put("created", System.currentTimeMillis());
                mDb.child("Keys").child(key).setValue(keyData).addOnCompleteListener(task -> {
                    if (!isAdded() || getContext() == null) return;
                    done[0]++;
                    keysBuilder.append(key).append("\n");
                    if (done[0] == count) {
                        lastGeneratedKeys = keysBuilder.toString().trim();
                        txtGeneratedKeys.setText(lastGeneratedKeys);
                        txtGeneratedKeys.setVisibility(View.VISIBLE);
                        btnCopyKeys.setVisibility(View.VISIBLE);
                        btnGenerateKeys.setEnabled(true);
                        btnGenerateKeys.setText(getString(R.string.admin_generate_btn));
                        Toast.makeText(getContext(), getString(R.string.admin_keys_created, count), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnCopyKeys.setOnClickListener(v -> {
            if (!lastGeneratedKeys.isEmpty()) {
                android.content.ClipboardManager cb = (android.content.ClipboardManager) requireActivity().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                cb.setPrimaryClip(android.content.ClipData.newPlainText("Keys", lastGeneratedKeys));
                Toast.makeText(getContext(), getString(R.string.admin_keys_copied), Toast.LENGTH_SHORT).show();
            }
        });

        btnUpdateReward.setOnClickListener(v -> {
            String key = edGiftPassword.getText().toString().trim();
            if (key.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.admin_enter_global_key), Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> r = new HashMap<>();
            r.put("global_key", key);
            mDb.child("Rewards").updateChildren(r).addOnSuccessListener(u -> {
                Toast.makeText(getContext(), getString(R.string.admin_reward_updated), Toast.LENGTH_SHORT).show();
                edGiftPassword.setText("");
            });
        });

        btnSearchUser.setOnClickListener(v -> {
            String input = edUserUid.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.admin_enter_uid), Toast.LENGTH_SHORT).show();
                return;
            }
            txtUserInfo.setVisibility(View.VISIBLE);
            txtUserInfo.setText(getString(R.string.admin_searching));
            
            if (input.contains("@")) {
                mDb.child("Users").orderByChild("email").equalTo(input).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        if (!snap.exists() || snap.getChildrenCount() == 0) {
                            txtUserInfo.setText(getString(R.string.admin_email_not_found));
                            return;
                        }
                        DataSnapshot userSnap = snap.getChildren().iterator().next();
                        displayUser(userSnap);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) { txtUserInfo.setText(getString(R.string.admin_search_error)); }
                });
            } else {
                mDb.child("Users").child(input.replace(".", ",")).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        if (!snap.exists()) {
                            txtUserInfo.setText(getString(R.string.admin_uid_not_found));
                            return;
                        }
                        displayUser(snap);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) { txtUserInfo.setText(getString(R.string.admin_search_error)); }
                });
            }
        });

        btnExtendUser.setOnClickListener(v -> {
            String input = edUserUid.getText().toString().trim();
            String daysStr = edExtendDays.getText().toString().trim();
            if (input.isEmpty() || daysStr.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.admin_enter_required_data), Toast.LENGTH_SHORT).show();
                return;
            }
            long days;
            try {
                days = Long.parseLong(daysStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), getString(R.string.admin_invalid_days), Toast.LENGTH_SHORT).show();
                return;
            }
            
            resolveUserUid(input, uid -> {
                mDb.child("Users").child(uid).child("expiry_date").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        long cur = (snap.exists() && snap.getValue(Long.class) != null) ? snap.getValue(Long.class)
                                : System.currentTimeMillis();
                        if (cur < System.currentTimeMillis())
                            cur = System.currentTimeMillis();
                        mDb.child("Users").child(uid).child("expiry_date").setValue(cur + TimeUnit.DAYS.toMillis(days))
                                .addOnSuccessListener(u -> Toast
                                        .makeText(getContext(), getString(R.string.admin_extend_success, days), Toast.LENGTH_SHORT).show());
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
            });
        });

        btnToggleUserActive.setOnClickListener(v -> {
            String input = edUserUid.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.admin_enter_uid), Toast.LENGTH_SHORT).show();
                return;
            }
            resolveUserUid(input, uid -> {
                boolean newState = !currentUserActive;
                mDb.child("Users").child(uid).child("active").setValue(newState).addOnSuccessListener(u -> {
                    currentUserActive = newState;
                    updateActiveButton();
                    Toast.makeText(getContext(), newState ? getString(R.string.admin_activation_success) : getString(R.string.admin_activation_cancelled), Toast.LENGTH_SHORT).show();
                });
            });
        });

        // ═══ نشر إعلان جديد ═══
        btnPublishAnn.setOnClickListener(v -> {
            String title = edAnnTitle.getText().toString().trim();
            String content = edAnnContent.getText().toString().trim();
            String link = edAnnLink.getText().toString().trim();
            boolean isPinned = cbAnnPinned.isChecked();

            if (content.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.admin_ann_content_required), Toast.LENGTH_SHORT).show();
                return;
            }

            String id = "ANN_" + System.currentTimeMillis();
            AnnouncementModel model = new AnnouncementModel(
                    id,
                    title.isEmpty() ? getString(R.string.home_admin_notice) : title,
                    content,
                    link,
                    isPinned,
                    System.currentTimeMillis()
            );

            mDb.child("Announcements").child(id).setValue(model).addOnSuccessListener(u -> {
                Toast.makeText(getContext(), getString(R.string.admin_ann_published), Toast.LENGTH_SHORT).show();
                edAnnTitle.setText("");
                edAnnContent.setText("");
                edAnnLink.setText("");
                cbAnnPinned.setChecked(false);
            }).addOnFailureListener(e -> Toast.makeText(getContext(), getString(R.string.admin_error, e.getMessage()), Toast.LENGTH_LONG).show());
        });

        // ═══ إضافة موزع ═══
        btnAddReseller.setOnClickListener(v -> {
            String name = edResellerName.getText().toString().trim();
            String tg = edResellerTelegram.getText().toString().trim();
            String imgUrl = edResellerImageUrl.getText().toString().trim();
            String country = spinnerCountry.getSelectedItem().toString()
                    .replaceAll("[^\\p{L}\\p{N} ]", "").trim(); // أزل الإيموجي من اسم الدولة

            if (name.isEmpty() || tg.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.admin_enter_reseller_data), Toast.LENGTH_SHORT).show();
                return;
            }

            // تحويل رابط Google Drive لرابط عرض مباشر
            String directImageUrl = convertDriveUrl(imgUrl);

            Map<String, Object> reseller = new HashMap<>();
            reseller.put("name", name);
            reseller.put("telegram", tg);
            reseller.put("image", directImageUrl);
            reseller.put("country", country);

            String key = name.toLowerCase().replace(" ", "_") + "_" + System.currentTimeMillis() % 10000;
            mDb.child("Resellers").child(country).child(key).setValue(reseller)
                    .addOnSuccessListener(u -> {
                        txtResellerStatus.setVisibility(View.VISIBLE);
                        txtResellerStatus.setText(getString(R.string.admin_reseller_added, name, country));
                        edResellerName.setText("");
                        edResellerTelegram.setText("");
                        edResellerImageUrl.setText("");
                        Toast.makeText(getContext(), getString(R.string.admin_add_success), Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(
                            e -> Toast.makeText(getContext(), getString(R.string.admin_error, e.getMessage()), Toast.LENGTH_LONG).show());
        });

        // ═══ حذف موزع (يحذف بالاسم) ═══
        btnDeleteReseller.setOnClickListener(v -> {
            String name = edResellerName.getText().toString().trim();
            String country = spinnerCountry.getSelectedItem().toString()
                    .replaceAll("[^\\p{L}\\p{N} ]", "").trim();
            if (name.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.admin_enter_reseller_name), Toast.LENGTH_SHORT).show();
                return;
            }
            mDb.child("Resellers").child(country)
                    .orderByChild("name").equalTo(name)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snap) {
                            if (!snap.exists()) {
                                txtResellerStatus.setVisibility(View.VISIBLE);
                                txtResellerStatus.setText(getString(R.string.admin_reseller_not_found));
                                return;
                            }
                            for (DataSnapshot child : snap.getChildren())
                                child.getRef().removeValue();
                            txtResellerStatus.setVisibility(View.VISIBLE);
                            txtResellerStatus.setText(getString(R.string.admin_reseller_deleted, name));
                            Toast.makeText(getContext(), getString(R.string.admin_delete_success), Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError e) {
                        }
                    });
        });

        // ═══ نشر التحديث الإجباري ═══
        btnPublishUpdate.setOnClickListener(v -> {
            String verStr = edUpdateVersion.getText().toString().trim();
            String apkUrl = edUpdateApkUrl.getText().toString().trim();
            if (verStr.isEmpty() || apkUrl.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.admin_enter_update_data), Toast.LENGTH_SHORT).show();
                return;
            }
            long version;
            try {
                version = Long.parseLong(verStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), getString(R.string.admin_version_number_error), Toast.LENGTH_SHORT).show();
                return;
            }
            Map<String, Object> update = new HashMap<>();
            update.put("version", version);
            update.put("apk_url", apkUrl);
            mDb.child("AppUpdate").updateChildren(update).addOnSuccessListener(u -> {
                Toast.makeText(getContext(), getString(R.string.admin_update_published, String.valueOf(version)), Toast.LENGTH_LONG)
                        .show();
                edUpdateVersion.setText("");
                edUpdateApkUrl.setText("");
            }).addOnFailureListener(e -> Toast.makeText(getContext(), getString(R.string.admin_error, e.getMessage()), Toast.LENGTH_LONG).show());
        });
    }

    /** يحول رابط Google Drive للعرض المباشر */
    private String convertDriveUrl(String url) {
        if (url == null || url.isEmpty())
            return "";
        // https://drive.google.com/file/d/{ID}/view →
        // https://drive.google.com/uc?export=view&id={ID}
        if (url.contains("drive.google.com/file/d/")) {
            try {
                String id = url.split("/file/d/")[1].split("/")[0];
                return "https://drive.google.com/uc?export=view&id=" + id;
            } catch (Exception e) {
                return url;
            }
        }
        return url;
    }

    private void updateActiveButton() {
        btnToggleUserActive.setText(currentUserActive ? getString(R.string.admin_btn_deactivate) : getString(R.string.admin_btn_activate));
        btnToggleUserActive.setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(currentUserActive ? 0xFFCC0000 : 0xFF007700));
    }

    /** يحل UID المستخدم سواء كان UID مباشر أو بريد إلكتروني */
    private void resolveUserUid(String input, UidCallback callback) {
        if (input.contains("@")) {
            mDb.child("Users").orderByChild("email").equalTo(input)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snap) {
                            if (snap.exists() && snap.getChildrenCount() > 0) {
                                String uid = snap.getChildren().iterator().next().getKey();
                                callback.onResolved(uid);
                            } else {
                                Toast.makeText(getContext(), getString(R.string.admin_email_not_found), Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError e) {
                            Toast.makeText(getContext(), getString(R.string.admin_search_error), Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            callback.onResolved(input.replace(".", ","));
        }
    }

    private void displayUser(DataSnapshot snap) {
        String uid = snap.getKey();
        String email = snap.child("email").getValue(String.class);
        Boolean active = snap.child("active").getValue(Boolean.class);
        Long expiry = snap.child("expiry_date").getValue(Long.class);

        currentUserActive = Boolean.TRUE.equals(active);
        updateActiveButton();

        String expiryStr = "—";
        if (expiry != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            expiryStr = sdf.format(new java.util.Date(expiry));
        }

        txtUserInfo.setText(
                getString(R.string.admin_uid_prefix) + uid + "\n" +
                getString(R.string.gift_email) + (email != null ? email : "—") + "\n" +
                "✅ " + (Boolean.TRUE.equals(active) ? getString(R.string.admin_user_active) : getString(R.string.admin_user_suspended)) + "\n" +
                "📅 ⏳ " + expiryStr
        );
    }

    private void loadSentinelData() {
        // Global Status
        mDb.child("Sentinel").child("GlobalStatus").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                String status = snap.child("text").getValue(String.class);
                Boolean ok = snap.child("is_ok").getValue(Boolean.class);
                if (status != null) {
                    txtSentinelGlobalStatus.setText(status);
                    txtSentinelGlobalStatus.setTextColor(Boolean.TRUE.equals(ok) ? 0xFF00FF88 : 0xFFFF4444);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });

        // Logs Feed
        mDb.child("Sentinel").child("Logs").limitToLast(50).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                sentinelLogList.clear();
                for (DataSnapshot s : snap.getChildren()) {
                    SentinelLogModel model = s.getValue(SentinelLogModel.class);
                    if (model != null) {
                        // In a real scenario, we'd decrypt model.encryptedTrace here using the Master Key
                        sentinelLogList.add(0, model); // Newest first
                    }
                }
                sentinelAdapter.notifyDataSetChanged();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });
    }

    private void setupSentinelListeners() {
        btnDownloadTrace.setOnClickListener(v -> {
            if (sentinelLogList.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.sentinel_no_logs), Toast.LENGTH_SHORT).show();
                return;
            }
            SentinelLogModel latest = sentinelLogList.get(0);
            String decrypted = MasterKeyUtils.decryptTrace(latest.scanStatus);
            
            android.util.Log.d("SENTINEL_ADMIN", "Decrypted Trace: " + decrypted);
            
            // Show result in a Dialog or Toast for now
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(requireContext());
            builder.setTitle("Sentinel Trace Analysis");
            builder.setMessage("HWID: " + latest.hwid + "\n\nDECRYPTED PAYLOAD:\n" + decrypted);
            builder.setPositiveButton("OK", null);
            builder.show();
            
            Toast.makeText(getContext(), getString(R.string.sentinel_trace_logged), Toast.LENGTH_LONG).show();
        });

        btnPushOffset.setOnClickListener(v -> {
            mDb.child("Sentinel").child("GlobalOffset").setValue("0x" + Long.toHexString(System.currentTimeMillis()))
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), getString(R.string.sentinel_offset_pushed), Toast.LENGTH_SHORT).show());
        });
    }

    interface UidCallback {
        void onResolved(String uid);
    }
}
