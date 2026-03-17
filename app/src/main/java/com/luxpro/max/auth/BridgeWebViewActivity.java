package com.luxpro.max.auth;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import com.luxpro.max.R;

public class BridgeWebViewActivity extends AppCompatActivity {
    private WebView webView;
    private ProgressBar progressBar;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bridge_webview);

        // Professional Floating Overlay Dimensions
        int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90);
        int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.90);
        
        android.util.Log.i("LUX_BRIDGE", "Starting Bridge UI with dimensions: " + width + "x" + height);
        getWindow().setLayout(width, height);

        webView = findViewById(R.id.webview_bridge);
        progressBar = findViewById(R.id.progress_webview);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setDatabaseEnabled(true);
        
        // Absolute Security: Spoofing the 8 Ball Pool Game Container
        String spoofedUA = "MiniclipGameContainer/1.0 (Android 10; LUX-PRO; 8BallPool_v5.12.0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.99 Mobile Safari/537.36";
        webView.getSettings().setUserAgentString(spoofedUA);

        // Sync cookies immediately
        android.webkit.CookieManager.getInstance().setAcceptCookie(true);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
                
                // Absolute Security: Start monitoring cookies for session markers
                android.webkit.CookieManager cookieManager = android.webkit.CookieManager.getInstance();
                String cookies = cookieManager.getCookie(url);
                if (cookies != null && (cookies.contains("mc_session") || cookies.contains("access_token"))) {
                    // android.util.Log.i("LUX_BRIDGE", "Secure Session Captured"); // Silenced for security
                    // Pass to NativeEngine or redirect to internal handler
                    if (com.luxpro.max.NativeEngine.isLibraryLoaded()) {
                        com.luxpro.max.NativeEngine.getInstance().submitAuthToken("COOKIE:" + cookies);
                    }
                    finish();
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                
                // INTERCEPT BRIDGE REDIRECT
                if (url.startsWith("luxpro://auth")) {
                    android.util.Log.i("LUX_BRIDGE", "Intercepted Internal Redirect: " + url);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    intent.setPackage(getPackageName()); // Force internal
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                    finish();
                    return true;
                }
                
                // BLOCK EXTERNAL BROWSERS
                if (url.startsWith("intent://") || url.contains("play.google.com")) {
                    android.util.Log.w("LUX_BRIDGE", "Blocked external browser hijack: " + url);
                    return true;
                }

                return false; // Stay inside WebView
            }
        });

        String url = getIntent().getStringExtra("url");
        if (url != null) {
            android.util.Log.i("LUX_BRIDGE", "Loading URL: " + url);
            webView.loadUrl(url);
        } else {
            android.util.Log.e("LUX_BRIDGE", "No URL provided for Bridge UI!");
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
