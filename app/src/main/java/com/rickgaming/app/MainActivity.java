package com.rickgaming.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.net.URISyntaxException;

/**
 * RICK GAMING — সাইটটা অ্যাপের ভেতরেই (WebView) চলবে।
 * Telegram / WhatsApp / YouTube / ফোন / মেইল — সব বাইরের লিংক ফোনের আসল অ্যাপে খুলবে,
 * তাই "ERR_UNKNOWN_URL_SCHEME" এরর আর আসবে না।
 */
public class MainActivity extends Activity {

    private static final String TARGET_URL = "https://rick-biswas.github.io/RICK-GAMING/";
    private static final String SITE_HOST = "rick-biswas.github.io";
    private static final int FILE_CHOOSER_REQUEST = 1001;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // সাদা স্ট্যাটাস বার (সাইটের হেডারের সাথে মানানসই)
        getWindow().setStatusBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);
        // নতুন Android-এ স্ট্যাটাস বারের নিচে সাইট ঢুকে যাওয়া ঠেকায়
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            v.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });

        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(webView);
        setContentView(root);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);          // সাইট চালানোর জন্য
        s.setDomStorageEnabled(true);          // localStorage (অর্ডার, অ্যাডমিন লগইন) সেভ থাকার জন্য
        s.setDatabaseEnabled(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl());
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                // শুধু মূল পেজ না এলে বন্ধুত্বপূর্ণ বার্তা দেখাবে
                if (request.isForMainFrame()) {
                    showOffline(view);
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            // alert()/confirm() ঠিকমতো দেখানোর জন্য (WebChromeClient সেট থাকলে WebView নিজেই ডায়ালগ দেখায়)
            // অ্যাডমিনে ছবি আপলোডের "বদলান" বাটন চালানোর জন্য
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), FILE_CHOOSER_REQUEST);
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(TARGET_URL);
        }
    }

    /**
     * true = আমরা নিজে সামলেছি (WebView-তে লোড হবে না)
     * false = সাইটের নিজের পেজ, WebView-তেই লোড হবে
     */
    private boolean handleUrl(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase();

        // নিজের সাইট → অ্যাপের ভেতরেই
        if ((scheme.equals("http") || scheme.equals("https")) && host.equals(SITE_HOST)) {
            return false;
        }

        // intent:// লিংক (কিছু সাইট/ইউটিউব এটা ব্যবহার করে)
        if (scheme.equals("intent")) {
            try {
                Intent intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setComponent(null);
                intent.setSelector(null);
                startActivity(intent);
            } catch (URISyntaxException | ActivityNotFoundException e) {
                String fallback = uri.getQueryParameter("browser_fallback_url");
                openExternal(fallback != null ? Uri.parse(fallback) : null);
            }
            return true;
        }

        // বাকি সব (t.me, wa.me, youtube.com, tg:, whatsapp:, tel:, mailto: ...) → ফোনের আসল অ্যাপে
        openExternal(uri);
        return true;
    }

    private void openExternal(Uri uri) {
        if (uri == null) return;
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, uri);
            i.addCategory(Intent.CATEGORY_BROWSABLE);
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            // অ্যাপ ইনস্টল নেই — https হলে ব্রাউজারে খুলবে, নাহলে চুপ থাকবে (ক্র্যাশ করবে না)
            try {
                String scheme = uri.getScheme() == null ? "" : uri.getScheme();
                if (scheme.startsWith("http")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                }
            } catch (ActivityNotFoundException ignored) {
            }
        }
    }

    private void showOffline(WebView view) {
        String html = "<html><head><meta name='viewport' content='width=device-width,initial-scale=1'></head>"
                + "<body style='font-family:sans-serif;text-align:center;padding:40px 20px;color:#0f172a'>"
                + "<h2>ইন্টারনেট সংযোগ নেই</h2><p style='color:#64748b'>সংযোগ চালু করে আবার চেষ্টা করুন।</p>"
                + "<button onclick=\"location.href='" + TARGET_URL + "'\" "
                + "style='background:#2563eb;color:#fff;border:0;border-radius:12px;padding:14px 26px;font-size:16px'>"
                + "আবার চেষ্টা করুন</button></body></html>";
        view.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(
                        WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                filePathCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // ব্যাক বাটন: আগে সাইটের ভেতরে পেছনে যাবে, শেষে অ্যাপ বন্ধ হবে
    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (webView != null) webView.saveState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) webView.onPause();
        CookieManager.getInstance().flush();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            ((ViewGroup) webView.getParent()).removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
