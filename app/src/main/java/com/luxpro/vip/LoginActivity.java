package com.luxpro.vip;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Objects;
import android.os.Build;
import android.provider.Settings;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.Call;
import java.io.IOException;

public class LoginActivity extends BaseActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private EditText edEmail, edPassword;
    private CheckBox cbRemember;
    private Button btnLogin, btnBuyKey, btnDevContact, btnRegister, btnLangToggle;
    private TextView txtForgotPassword;
    private SharedPreferences preferences;
    private RelativeLayout loadingOverlay;
    private ScrollView loginScrollView;
    
    // Bridge UI
    private LinearLayout layoutBridgeContainer;
    private TextView txtBridgeStatus;
    private ProgressBar progressBridge;
    private Button btnLaunchBridge;
    private String pendingProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        initViews();
        setupListeners();
        
        continueInitialization();
    }

    private void continueInitialization() {
        // Handle the intent FIRST to see if we need to show the Bridge UI
        handleIntent(getIntent());

        // Only perform automatic login check if we are NOT in bridge mode
        if (this.pendingProvider == null) {
            if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().isEmailVerified()) {
                checkUserStatus(mAuth.getUid());
            } else {
                loadPreferences();
            }
        } else {
            android.util.Log.i("LUX_AUTH", "Skipping automatic login check because Bridge UI is ACTIVE for: " + pendingProvider);
        }
    }



    // Called when the activity is already running (launchMode="singleTop")
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent); // Update the intent
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        
        String action = intent.getAction();
        Uri data = intent.getData();

        // 1. Handle Deep Link Redirects (luxpro://auth)
        if (Intent.ACTION_VIEW.equals(action) && data != null) {
            if ("luxpro".equals(data.getScheme()) && "auth".equals(data.getHost())) {
                String authToken = data.getQueryParameter("auth_token");
                String error = data.getQueryParameter("error");

                if (authToken != null) {
                    // Token received — DO NOT LOG (security risk)
                    processToken(authToken);
                } else if (error != null) {
                    Toast.makeText(this, getString(R.string.error), Toast.LENGTH_SHORT).show();
                }
            }
        }
        
        // 2. Handle Intent from Floating Menu
        if (intent.hasExtra("oauth_provider")) {
            this.pendingProvider = intent.getStringExtra("oauth_provider");
            android.util.Log.i("LUX_AUTH", "Found oauth_provider in intent: " + pendingProvider);
        }
        
        if (this.pendingProvider != null) {
            setupBridgeUI(this.pendingProvider);
        }
    }

    private void setupBridgeUI(String provider) {
        this.pendingProvider = provider;
        android.util.Log.i("LUX_AUTH", "Setting up Bridge UI for: " + provider);
        
        if (layoutBridgeContainer != null) {
            // Hide main login form and any loading screens to ensure Bridge is exclusive
            if (loginScrollView != null) {
                loginScrollView.setVisibility(View.GONE);
                android.util.Log.d("LUX_AUTH", "loginScrollView hidden");
            }
            if (loadingOverlay != null) {
                loadingOverlay.setVisibility(View.GONE);
                android.util.Log.d("LUX_AUTH", "loadingOverlay hidden");
            }
            
            layoutBridgeContainer.setVisibility(View.VISIBLE);
            layoutBridgeContainer.bringToFront();
            txtBridgeStatus.setText(getString(R.string.bridge_status_connecting, provider.toUpperCase()));
            
            android.util.Log.i("LUX_AUTH", "Bridge Container now VISIBLE");
            
            // Trigger Chucker Handshake immediately to monitor the start of the session
            performBridgeHandshake(provider);
            
            btnLaunchBridge.setOnClickListener(v -> {
                txtBridgeStatus.setText(getString(R.string.bridge_status_ready));
                progressBridge.setIndeterminate(true);
                btnLaunchBridge.setEnabled(false);
                btnLaunchBridge.setText(getString(R.string.bridge_loading));
                
                // Small delay to show "Connecting" effect before launching the actual tab
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    startOAuthFlow(pendingProvider);
                    // Reset UI for next time or if user comes back
                    btnLaunchBridge.setEnabled(true);
                    btnLaunchBridge.setText(getString(R.string.bridge_btn_connect));
                }, 1500);
            });
        } else {
            android.util.Log.e("LUX_AUTH", "CRITICAL: layoutBridgeContainer is NULL");
        }
    }
    private void processToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            // Absolute Zero: Submit token via encrypted bridge
            if (NativeEngine.isLibraryLoaded()) {
                // The submitAuthToken method now handles AES encryption internally
                NativeEngine.getInstance().submitAuthToken(token);
                
                // Anti-Detection: Set a spoofed serial for the session
                String spoofedSerial = "LUX-" + System.currentTimeMillis();
                NativeEngine.getInstance().setSpoofedSerial(spoofedSerial);
            }
            
            // Check if bridge is active before proceeding
            if (NativeEngine.isLibraryLoaded() && !NativeEngine.getInstance().isBridgeActive()) {
                android.util.Log.e("LUX_AUTH", "CRITICAL: Auth Bridge NOT ACTIVE. Aborting login.");
                Toast.makeText(this, "Security Violation: Bridge Inactive", Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            
            // Update Floating Menu UI via Broadcast
            Intent updateIntent = new Intent("com.luxpro.vip.UPDATE_ACCOUNT");
            updateIntent.putExtra("user_name", "LUX_ELITE_USER");
            updateIntent.putExtra("provider", pendingProvider != null ? pendingProvider : "guest");
            sendBroadcast(updateIntent);
            
            Toast.makeText(this, "Secure Tunnel Established", Toast.LENGTH_SHORT).show();
            
            // LAUNCH THE GAME
            Intent gameIntent = getPackageManager().getLaunchIntentForPackage("com.miniclip.eightballpool");
            if (gameIntent != null) {
                gameIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(gameIntent);
            }
            finish();
        } else {
            Toast.makeText(this, "Login Failed", Toast.LENGTH_SHORT).show();
            finish();
        }
    }


    // Call this method to initiate the Internal WebView flow for any OAuth provider
    public void startOAuthFlow(String provider) {
        String loginUrl;
        switch (provider) {
            case "google":
                // Real Google Client ID: 361205504410-rea45ohp2bqlkruu5dogi8paf6phl4k8.apps.googleusercontent.com
                loginUrl = "https://accounts.google.com/o/oauth2/v2/auth?client_id=361205504410-rea45ohp2bqlkruu5dogi8paf6phl4k8.apps.googleusercontent.com&redirect_uri=https://lux-pro-3e3f2.firebaseapp.com/__/auth/handler&response_type=token&scope=profile email";
                break;
            case "miniclip":
            case "bind":
                loginUrl = "https://lux-pro-3e3f2.firebaseapp.com/miniclip.html";
                break;
            default:
                loginUrl = "https://your-auth-server.com/login?provider=" + provider;
                break;
        }
        
        android.util.Log.i("LUX_AUTH", "Launching Internal Bridge WebView for: " + provider);
        Intent intent = new Intent(this, com.luxpro.vip.auth.BridgeWebViewActivity.class);
        intent.putExtra("url", loginUrl);
        startActivity(intent);
    }

    private void initViews() {
        edEmail = findViewById(R.id.edEmail);
        edPassword = findViewById(R.id.edPassword);
        cbRemember = findViewById(R.id.cbRemember);
        btnLogin = findViewById(R.id.btnLogin);
        btnBuyKey = findViewById(R.id.btnResellers);
        btnDevContact = findViewById(R.id.btnDevContact);
        btnRegister = findViewById(R.id.btnRegister);
        btnLangToggle = findViewById(R.id.btnLangToggle);
        txtForgotPassword = findViewById(R.id.txtForgotPass);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        preferences = getSharedPreferences("LUX_PREFS", MODE_PRIVATE);
        
        // Bridge UI Initialization
        layoutBridgeContainer = findViewById(R.id.layout_bridge_container);
        txtBridgeStatus = findViewById(R.id.txt_bridge_status);
        progressBridge = findViewById(R.id.progress_bridge);
        btnLaunchBridge = findViewById(R.id.btn_launch_bridge);
        loginScrollView = findViewById(R.id.login_scroll_view);
    }

    // Removed loadRewardPreview

    private void loadPreferences() {
        if (preferences.getBoolean("remember", false)) {
            edEmail.setText(preferences.getString("email", ""));
            // Password is never stored — user must re-enter for security
            cbRemember.setChecked(true);
        }
    }

    private void setupListeners() {
        if (btnRegister != null)
            btnRegister.setOnClickListener(v -> {
                startActivity(new Intent(this, RegisterActivity.class));
                overridePendingTransition(R.anim.slide_in_glitch, R.anim.slide_out_glitch);
            });
        // Privacy policy removed from gateway
        if (btnDevContact != null)
            btnDevContact.setOnClickListener(
                    v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/Y8YY9"))));
        if (btnBuyKey != null)
            btnBuyKey.setOnClickListener(v -> startActivity(new Intent(this, ResellerActivity.class)));
        if (txtForgotPassword != null)
            txtForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());

        // Attach Haptic NeonPulse Effects
        NeonTouchListener neonPulsar = new NeonTouchListener(this);
        if (btnLogin != null)
            btnLogin.setOnTouchListener(neonPulsar);
        if (btnRegister != null)
            btnRegister.setOnTouchListener(neonPulsar);
        if (btnBuyKey != null)
            btnBuyKey.setOnTouchListener(neonPulsar);
        if (btnDevContact != null)
            btnDevContact.setOnTouchListener(neonPulsar);
        if (btnLangToggle != null)
            btnLangToggle.setOnTouchListener(neonPulsar);

        if (btnLangToggle != null) {
            btnLangToggle.setOnClickListener(v -> toggleLanguage());
        }

        btnLogin.setOnClickListener(v -> {
            String email = edEmail.getText().toString().trim();
            String password = edPassword.getText().toString().trim();
            if (email.isEmpty() || password.isEmpty())
                return;
            btnLogin.setEnabled(false);
            showLoading(true);
            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    handleRememberMe(email, password);
                    checkUserStatus(Objects.requireNonNull(mAuth.getUid()));
                } else {
                    showLoading(false);
                    String errorMsg = Objects.requireNonNull(task.getException()).getMessage();
                    Toast.makeText(this, getString(R.string.login_failed_error, errorMsg), Toast.LENGTH_LONG).show();
                    btnLogin.setEnabled(true);
                }
            });
        });
    }

    private void showForgotPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.reset_password);
        final EditText input = new EditText(this);
        input.setHint(R.string.reset_email_hint);
        builder.setView(input);
        builder.setPositiveButton(R.string.send, (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                mAuth.sendPasswordResetEmail(email).addOnCompleteListener(task -> Toast.makeText(LoginActivity.this,
                        task.isSuccessful() ? getString(R.string.reset_sent) : getString(R.string.error),
                        Toast.LENGTH_SHORT).show());
            }
        });
        builder.setNegativeButton(R.string.cancel, (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void handleRememberMe(String email, String password) {
        if (cbRemember.isChecked()) {
            // Security: Store email only, NEVER the password in plaintext
            preferences.edit()
                .putString("email", email)
                .putBoolean("remember", true)
                .apply();
        } else {
            preferences.edit()
                .remove("email")
                .putBoolean("remember", false)
                .apply();
        }
    }

    private void showLoading(boolean show) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void checkUserStatus(String userId) {
        showLoading(true);

        // Fetch user info first instead of fetching the entire Admins tree
        mDatabase.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                if (!snap.exists()) {
                    // Check if Admin instead (Admins might not have a Users node)
                    checkIfAdmin(userId);
                    return;
                }

                Boolean isActive = snap.child("active").getValue(Boolean.class);
                String savedHwid = snap.child("hwid").getValue(String.class);
                String currentHwid = android.provider.Settings.Secure.getString(getContentResolver(),
                        android.provider.Settings.Secure.ANDROID_ID);

                showLoading(false);

                if (Boolean.TRUE.equals(isActive)) {
                    // HWID Match check bypasses if saving new HWID
                    if (savedHwid != null && !savedHwid.equals(currentHwid)) {
                        showHwidLockDialog();
                        return;
                    }
                    startActivity(new Intent(LoginActivity.this, MainActivityDashboard.class));
                    overridePendingTransition(R.anim.slide_in_glitch, R.anim.slide_out_glitch);
                    finish(); // ✅ أغلق LoginActivity بعد الانتقال
                } else {
                    startActivity(new Intent(LoginActivity.this, ActivationActivity.class));
                    overridePendingTransition(R.anim.slide_in_glitch, R.anim.slide_out_glitch);
                    finish(); // ✅ أغلق LoginActivity بعد الانتقال
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) {
                showLoading(false);
                if (btnLogin != null) btnLogin.setEnabled(true);
            }
        });
    }

    private void checkIfAdmin(String userId) {
        FirebaseDatabase.getInstance().getReference("Admins").child(userId)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot adminSnap) {
                    showLoading(false);
                    if (adminSnap.exists() && Boolean.TRUE.equals(adminSnap.getValue(Boolean.class))) {
                        startActivity(new Intent(LoginActivity.this, MainActivityDashboard.class));
                        overridePendingTransition(R.anim.slide_in_glitch, R.anim.slide_out_glitch);
                        finish(); // ✅ أغلق LoginActivity بعد الانتقال
                    } else {
                        Toast.makeText(LoginActivity.this, getString(R.string.login_failed_account_not_found), Toast.LENGTH_SHORT).show();
                        if (btnLogin != null) btnLogin.setEnabled(true);
                        mAuth.signOut();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError e) {
                    showLoading(false);
                    if (btnLogin != null) btnLogin.setEnabled(true);
                }
            });
    }

    private void showHwidLockDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.security_hwid_lock_title)
                .setMessage(R.string.security_hwid_lock_msg)
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> {
                    mAuth.signOut();
                    if (btnLogin != null)
                        btnLogin.setEnabled(true);
                })
                .show();
    }

    // setLoading and showPrivacyPolicy removed

    private void toggleLanguage() {
        String currentLang = LocaleHelper.getLanguage(this);
        String targetLang = currentLang.equals("ar") ? "en" : "ar";
        LocaleHelper.setLocale(this, targetLang);

        // Restart activity with an animation to show glitch effect
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_glitch, R.anim.slide_out_glitch);
        finish();
    }

    private void performBridgeHandshake(String provider) {
        OkHttpClient client = com.luxpro.vip.auth.AuthBridgeClient.getInstance(this);
        
        // Simulating the handshake call that the game or bridge might perform.
        // This ensures Chucker starts monitoring the session immediately.
        String handshakeUrl = "https://your-auth-server.com/bridge_handshake?provider=" + provider;
        
        Request request = new Request.Builder()
                .url(handshakeUrl)
                .addHeader("X-Lux-Bridge", "Intercept-Active")
                .addHeader("X-Device-Spoof", "Active")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                android.util.Log.e("LUX_AUTH", "Chucker Monitor: Handshake entry FAILED (Expected if server is offline).");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                android.util.Log.i("LUX_AUTH", "Chucker Monitor: Handshake entry logged. Code: " + response.code());
                response.close();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}