package com.luxpro.vip;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import java.util.HashMap;
import java.util.Map;

public class ActivationActivity extends BaseActivity {
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    private EditText keyInput;
    private TextView statusText, deviceDetails;
    private Button btnActivate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activation);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        keyInput = findViewById(R.id.keyInput);
        statusText = findViewById(R.id.statusText);
        deviceDetails = findViewById(R.id.deviceDetails);
        btnActivate = findViewById(R.id.btnActivate);

        String userHWID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        deviceDetails.setText(getString(R.string.device_id_prefix) + userHWID);

        NeonTouchListener neonPulsar = new NeonTouchListener(this);
        btnActivate.setOnTouchListener(neonPulsar);

        btnActivate.setOnClickListener(v -> {
            String enteredKey = keyInput.getText().toString().trim();
            if (enteredKey.isEmpty()) {
                Toast.makeText(this, getString(R.string.enter_key_error), Toast.LENGTH_SHORT).show();
                return;
            }

        btnActivate.setEnabled(false); // Prevent double-tap during async Firebase call
        mDatabase.child("Rewards").child("global_key").get().addOnSuccessListener(snapshot -> {
            String globalKey = snapshot.getValue(String.class);
            if (enteredKey.equals(globalKey)) {
                activateRewardKey(enteredKey, userHWID);
            } else if (enteredKey.startsWith("FREE-")) {
                activateFreeKey(enteredKey, userHWID);
            } else {
                activatePrivateKey(enteredKey, userHWID);
            }
        }).addOnFailureListener(e -> {
            btnActivate.setEnabled(true);
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show();
        });
        });
    }

    private void activateRewardKey(String enteredKey, String userHWID) {
        // Reward key lasts for the duration of the 1-hour window in the 5-day cycle
        long cycleMillis = 5 * 24 * 60 * 60 * 1000L;
        long windowMillis = 1 * 60 * 60 * 1000L;
        long now = TimeSyncManager.getServerTime();
        long currentCycleStart = (now / cycleMillis) * cycleMillis;
        long windowEnd = currentCycleStart + windowMillis;

        if (now >= windowEnd) {
            Toast.makeText(this, getString(R.string.reward_expired_error), Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> update = new HashMap<>();
        update.put("active", true);
        update.put("expiry_date", windowEnd); // Everyone expires at the same second
        update.put("license", enteredKey);
        update.put("hwid", null); // Open for everyone

        String uid = mAuth.getUid();
        if (uid != null) {
            mDatabase.child("Users").child(uid).updateChildren(update);
        }
        
        Toast.makeText(this, getString(R.string.reward_granted_msg, new java.util.Date(windowEnd).toString()), Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, MainActivityDashboard.class));
        overridePendingTransition(R.anim.slide_in_glitch, R.anim.slide_out_glitch);
        finish();
    }

    private void activatePrivateKey(String enteredKey, String userHWID) {
        mDatabase.child("Keys").child(enteredKey).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Boolean used = snapshot.child("used").getValue(Boolean.class);
                if (Boolean.TRUE.equals(used)) {
                    Toast.makeText(this, getString(R.string.key_used_invalid), Toast.LENGTH_SHORT).show();
                    btnActivate.setEnabled(true);
                    return;
                }
                Long daysVal = snapshot.child("days").getValue(Long.class);
                if (daysVal == null) {
                    Toast.makeText(this, getString(R.string.invalid_key_data), Toast.LENGTH_SHORT).show();
                    btnActivate.setEnabled(true);
                    return;
                }
                long expiryTime = TimeSyncManager.getServerTime() + (daysVal * 24 * 60 * 60 * 1000L);

                // Security: Mark key as USED FIRST to prevent race condition
                mDatabase.child("Keys").child(enteredKey).child("used").setValue(true)
                    .addOnSuccessListener(v -> {
                        String uid = mAuth.getUid();
                        if (uid == null) { btnActivate.setEnabled(true); return; }
                        Map<String, Object> update = new HashMap<>();
                        update.put("active", true);
                        update.put("hwid", userHWID);
                        update.put("expiry_date", expiryTime);
                        update.put("license", enteredKey);
                        mDatabase.child("Users").child(uid).updateChildren(update)
                            .addOnSuccessListener(v2 -> {
                                Toast.makeText(this, getString(R.string.vip_activated_msg), Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, MainActivityDashboard.class));
                                overridePendingTransition(R.anim.slide_in_glitch, R.anim.slide_out_glitch);
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                // Rollback: key was marked used but user not activated — undo
                                mDatabase.child("Keys").child(enteredKey).child("used").setValue(false);
                                btnActivate.setEnabled(true);
                                Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show();
                            });
                    })
                    .addOnFailureListener(e -> {
                        btnActivate.setEnabled(true);
                        Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show();
                    });
            } else {
                Toast.makeText(this, getString(R.string.invalid_key), Toast.LENGTH_SHORT).show();
                btnActivate.setEnabled(true);
            }
        }).addOnFailureListener(e -> {
            btnActivate.setEnabled(true);
            Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show();
        });
    }

    private void activateFreeKey(String enteredKey, String userHWID) {
        mDatabase.child("FreeKeys").child(enteredKey).get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                long durationHours = 2;
                long expiryTime = TimeSyncManager.getServerTime() + (durationHours * 60 * 60 * 1000L);

                Map<String, Object> update = new HashMap<>();
                update.put("active", true);
                update.put("expiry_date", expiryTime);
                update.put("license", enteredKey);
                // Free keys from 'FreeKeys' node might still use HWID if you want, 
                // but usually they don't.
                update.put("hwid", null); 

                String freeUid = mAuth.getUid();
                if (freeUid != null) {
                    mDatabase.child("Users").child(freeUid).updateChildren(update);
                }

                Toast.makeText(this, getString(R.string.free_access_granted), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivityDashboard.class));
                overridePendingTransition(R.anim.slide_in_glitch, R.anim.slide_out_glitch);
                finish();
            } else {
                Toast.makeText(this, getString(R.string.invalid_free_key), Toast.LENGTH_SHORT).show();
            }
        });
    }
}