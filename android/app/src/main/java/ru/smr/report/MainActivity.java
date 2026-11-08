package ru.smr.report;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.*;
import android.widget.*;

public class MainActivity extends Activity {
 private WebView web;
 private ValueCallback<Uri[]> upload;
 private LinearLayout root;
 private final String base = "https://bormaufa-gif.github.io/smr-report/";
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
  root.setOnApplyWindowInsetsListener((v, i) -> {
   v.setPadding(i.getSystemWindowInsetLeft(), i.getSystemWindowInsetTop(), i.getSystemWindowInsetRight(), i.getSystemWindowInsetBottom()); return i;
  });
  setContentView(root); showLogin();
 }
 private void addUpdateButton() {
  Button update = new Button(this);
  update.setText("Проверить обновления");
  update.setOnClickListener(v -> UpdateChecker.check(this, update));
  root.addView(update);
 }
 private void showLogin() {
  if (web != null) { web.stopLoading(); web.destroy(); web = null; }
  root.removeAllViews();
  TextView title = new TextView(this); title.setText("Отчёт СМР\nВведите свой код доступа"); title.setTextSize(24); title.setPadding(24,48,24,24); root.addView(title);
  EditText code = new EditText(this); code.setSingleLine(true); code.setHint("Код доступа"); code.setInputType(0x00080001); root.addView(code);
  Button enter = new Button(this); enter.setText("Войти"); root.addView(enter);
  enter.setOnClickListener(v -> { String value = code.getText().toString().trim(); if (value.isEmpty()) { code.setError("Введите код"); return; } openReport(value); });
  addUpdateButton();
 }
 private void openReport(String code) {
  root.removeAllViews();
  Button change = new Button(this); change.setText("Сменить код"); change.setOnClickListener(v -> showLogin()); root.addView(change);
  addUpdateButton();
  web = new WebView(this); root.addView(web, new LinearLayout.LayoutParams(-1,0,1));
  WebSettings settings = web.getSettings(); settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true); settings.setAllowFileAccess(false); settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  CookieManager.getInstance().setAcceptCookie(true);
  web.setWebViewClient(new WebViewClient() {
   @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
    Uri uri = request.getUrl();
    if ("https".equals(uri.getScheme()) && "bormaufa-gif.github.io".equals(uri.getHost()) && uri.getPath() != null && uri.getPath().startsWith("/smr-report/")) return false;
    if ("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()) || "mailto".equals(uri.getScheme()) || "tel".equals(uri.getScheme())) {
     try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception e) { Toast.makeText(MainActivity.this, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show(); }
    }
    return true;
   }
  });
  web.setWebChromeClient(new WebChromeClient() {
   @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
    if (upload != null) upload.onReceiveValue(null); upload = callback;
    try { startActivityForResult(params.createIntent(), 100); } catch (Exception e) { upload.onReceiveValue(null); upload = null; }
    return true;
   }
  });
  web.loadUrl(base + "#code=" + Uri.encode(code));
 }
 @Override protected void onActivityResult(int request, int result, Intent data) {
  super.onActivityResult(request, result, data);
  if (request == 100 && upload != null) { upload.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result, data)); upload = null; }
 }
 @Override public void onBackPressed() { if (web != null && web.canGoBack()) web.goBack(); else if (web != null) showLogin(); else super.onBackPressed(); }
 @Override protected void onDestroy() { if (upload != null) upload.onReceiveValue(null); if (web != null) web.destroy(); super.onDestroy(); }
}
