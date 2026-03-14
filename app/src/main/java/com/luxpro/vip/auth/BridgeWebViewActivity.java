package com.luxpro.vip.auth;

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
import com.luxpro.vip.R;

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
        getWindow().setLayout(width, height);

        webView = findViewById(R.id.webview_bridge);
        progressBar = findViewById(R.id.progress_webview);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setUserAgentString("Mozilla/5.0 (Linux; Android 10; LUX-PRO) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.99 Mobile Safari/537.36");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
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
            webView.loadUrl(url);
        } else {
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
