package com.luxpro.max;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

import androidx.recyclerview.widget.RecyclerView;

public class HomeFragment extends Fragment {

    private final String GAME_PACKAGE = "com.miniclip.eightballpool";
    private final int OVERLAY_PERMISSION_CODE = 123;
    private DatabaseReference mDatabase;
    private RecyclerView rvPinnedAnnouncements;
    private RecyclerView rvNormalAnnouncements;
    private View pinnedSectionContainer;

    private AnnouncementAdapter pinnedAdapter;
    private AnnouncementAdapter normalAdapter;

    private java.util.List<AnnouncementModel> pinnedList;
    private java.util.List<AnnouncementModel> normalList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        Button btnStartGame = view.findViewById(R.id.btnStartGame);

        rvPinnedAnnouncements = view.findViewById(R.id.rvPinnedAnnouncements);
        rvNormalAnnouncements = view.findViewById(R.id.rvNormalAnnouncements);
        pinnedSectionContainer = view.findViewById(R.id.pinnedSectionContainer);

        rvPinnedAnnouncements.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
        rvNormalAnnouncements.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));

        pinnedList = new java.util.ArrayList<>();
        normalList = new java.util.ArrayList<>();

        pinnedAdapter = new AnnouncementAdapter(getContext(), pinnedList);
        normalAdapter = new AnnouncementAdapter(getContext(), normalList);

        rvPinnedAnnouncements.setAdapter(pinnedAdapter);
        rvNormalAnnouncements.setAdapter(normalAdapter);

        fetchAnnouncements();
        if (btnStartGame != null) {
            btnStartGame.setOnClickListener(v -> handleStartGameSequence());
        }
        return view;
    }

    private void handleStartGameSequence() {
        if (!isGameInstalled()) {
            openPlayStore();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(getContext())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getContext().getPackageName()));
            startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
        } else {
            launchGameWithMaxProtection();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OVERLAY_PERMISSION_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(getContext())) {
                Toast.makeText(getContext(), getString(R.string.overlay_permission_granted), Toast.LENGTH_SHORT).show();
                launchGameWithMaxProtection();
            } else {
                Toast.makeText(getContext(), getString(R.string.overlay_permission_denied), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void launchGameWithMaxProtection() {
        MainActivityDashboard activity = (MainActivityDashboard) getActivity();
        if (activity != null) {

            Intent gameIntent = getContext().getPackageManager().getLaunchIntentForPackage(GAME_PACKAGE);
            if (gameIntent != null) {
                startActivity(gameIntent);

                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        // تشغيل المنيو الجديد (Native C++)
                        com.luxpro.max.Main.Start(getContext());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    // ✅ تم التصحيح هنا: حذف الأقواس لأن isEngineReady متغير وليس دالة
                    if (activity.isEngineReady) {
                        Toast.makeText(getContext(), getString(R.string.protected_msg), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), getString(R.string.engine_loading), Toast.LENGTH_SHORT).show();
                    }
                }, 2000);

            } else {
                Toast.makeText(getContext(), getString(R.string.game_not_found), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void fetchAnnouncements() {
        mDatabase.child("Announcements").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || getContext() == null) return;
                pinnedList.clear();
                normalList.clear();

                for (DataSnapshot ds : snapshot.getChildren()) {
                    AnnouncementModel model = ds.getValue(AnnouncementModel.class);
                    if (model != null) {
                        model.setId(ds.getKey());
                        if (model.isPinned()) {
                            pinnedList.add(model);
                        } else {
                            normalList.add(model);
                        }
                    }
                }

                java.util.Collections.sort(pinnedList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));
                java.util.Collections.sort(normalList, (a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()));

                if (pinnedList.isEmpty()) {
                    pinnedSectionContainer.setVisibility(View.GONE);
                } else {
                    pinnedSectionContainer.setVisibility(View.VISIBLE);
                }

                pinnedAdapter.notifyDataSetChanged();
                normalAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private boolean isGameInstalled() {
        try {
            android.content.pm.PackageManager pm = getContext().getPackageManager();
            try {
                pm.getPackageInfo(GAME_PACKAGE, 0);
                return true;
            } catch (Exception ignored) {}

            if (pm.getLaunchIntentForPackage(GAME_PACKAGE) != null) {
                return true;
            }

            java.util.List<android.content.pm.PackageInfo> packages = pm.getInstalledPackages(0);
            for (android.content.pm.PackageInfo packageInfo : packages) {
                String pkgName = packageInfo.packageName.toLowerCase();
                if (pkgName.contains("eightballpool") || pkgName.contains("8ballpool")) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void openPlayStore() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + GAME_PACKAGE)));
        } catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + GAME_PACKAGE)));
        }
    }
}