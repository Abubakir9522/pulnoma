package uz.mojal.pulnoma;

import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private static final int REQ_FILE = 41;
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#F6F3EC"));
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setTextZoom(100);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if ("file".equals(u.getScheme())) return false;
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, u));
                } catch (Exception ignored) {
                }
                return true;
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(i, "Zaxira faylini tanlang"), REQ_FILE);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        web.addJavascriptInterface(new Bridge(), "Android");
        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE) {
            if (fileCallback != null) {
                fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                fileCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (web == null) {
            finish();
            return;
        }
        web.evaluateJavascript("(window.handleBack && window.handleBack()) ? 'y' : 'n'", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (value == null || !value.contains("y")) finish();
            }
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (web != null) web.evaluateJavascript("window.onSystemTheme && window.onSystemTheme()", null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (web != null) web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }

    private static String safeName(String name) {
        String n = name == null ? "fayl.txt" : name.replaceAll("[^A-Za-z0-9._-]", "_");
        return n.isEmpty() ? "fayl.txt" : n;
    }

    /** JavaScript'dan chaqiriladigan funksiyalar (window.Android). */
    private class Bridge {

        @JavascriptInterface
        public boolean isNight() {
            int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            return mode == Configuration.UI_MODE_NIGHT_YES;
        }

        @JavascriptInterface
        public void setStatusBar(final String color, final boolean dark) {
            runOnUiThread(new Runnable() {
                @Override
                @SuppressWarnings("deprecation")
                public void run() {
                    try {
                        int c = Color.parseColor(color);
                        Window w = getWindow();
                        w.setStatusBarColor(c);
                        w.setNavigationBarColor(c);
                        web.setBackgroundColor(c);
                        View decor = w.getDecorView();
                        int flags = decor.getSystemUiVisibility();
                        int light = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                        if (Build.VERSION.SDK_INT >= 26) light |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                        flags = dark ? (flags & ~light) : (flags | light);
                        decor.setSystemUiVisibility(flags);
                    } catch (Exception ignored) {
                    }
                }
            });
        }

        @JavascriptInterface
        public void shareText(final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_TEXT, text);
                    try {
                        startActivity(Intent.createChooser(i, "Yuborish"));
                    } catch (Exception ignored) {
                    }
                }
            });
        }

        private Uri writeShareFile(String name, String content) throws Exception {
            String fn = safeName(name);
            File dir = new File(getCacheDir(), "share");
            if (!dir.exists()) dir.mkdirs();
            FileOutputStream out = new FileOutputStream(new File(dir, fn));
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.close();
            return Uri.parse("content://" + ShareProvider.AUTHORITY + "/" + fn);
        }

        private Intent sendIntent(String name, String mime, Uri uri) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(mime);
            i.putExtra(Intent.EXTRA_STREAM, uri);
            i.putExtra(Intent.EXTRA_SUBJECT, safeName(name));
            i.putExtra(Intent.EXTRA_TITLE, safeName(name));
            i.setClipData(ClipData.newRawUri(safeName(name), uri));
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            return i;
        }

        @JavascriptInterface
        public void tap() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    web.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
                }
            });
        }

        @JavascriptInterface
        public void shareFile(final String name, final String mime, String content) {
            try {
                final Uri uri = writeShareFile(name, content);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            startActivity(Intent.createChooser(sendIntent(name, mime, uri), "Yuborish"));
                        } catch (Exception ignored) {
                        }
                    }
                });
            } catch (Exception ignored) {
            }
        }

        /** Faylni to'g'ridan-to'g'ri Google Drive ilovasiga yuboradi ("Drive'ga saqlash" oynasi ochiladi). */
        @JavascriptInterface
        public boolean saveToDrive(String name, String mime, String content) {
            try {
                Uri uri = writeShareFile(name, content);
                Intent i = sendIntent(name, mime, uri);
                i.setPackage("com.google.android.apps.docs");
                startActivity(i);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @JavascriptInterface
        public boolean saveToDownloads(String name, String mime, String content) {
            if (Build.VERSION.SDK_INT < 29) return false;
            try {
                ContentResolver cr = getContentResolver();
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, safeName(name));
                v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/Pulnoma");
                Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) return false;
                OutputStream out = cr.openOutputStream(uri);
                if (out == null) return false;
                out.write(content.getBytes(StandardCharsets.UTF_8));
                out.close();
                return true;
            } catch (Exception e) {
                return false;
            }
        }
    }
}
