package ru.smr.report;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.widget.Button;
import android.widget.Toast;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.json.JSONArray;
import org.json.JSONObject;

final class UpdateChecker {
 private static int compare(String a, String b) {
  if (!a.matches("[0-9]+\\.[0-9]+\\.[0-9]+") || !b.matches("[0-9]+\\.[0-9]+\\.[0-9]+")) throw new IllegalArgumentException();
  String[] aa = a.split("\\."), bb = b.split("\\.");
  for (int i = 0; i < 3; i++) {
   int c = Long.compare(Long.parseLong(aa[i]), Long.parseLong(bb[i]));
   if (c != 0) return c;
  }
  return 0;
 }
 static void check(Activity activity, Button button) {
  button.setEnabled(false); button.setText("Проверяем…");
  new Thread(() -> {
   String message;
   String download = null;
   HttpURLConnection connection = null;
   try {
    String installed = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0).versionName;
    connection = (HttpURLConnection) new URL("https://api.github.com/repos/bormaufa-gif/smr-report/releases/latest").openConnection();
    connection.setConnectTimeout(10000); connection.setReadTimeout(10000);
    connection.setRequestProperty("Accept", "application/vnd.github+json");
    connection.setRequestProperty("User-Agent", "PTS-Ai-Android");
    int status = connection.getResponseCode();
    if (status == 404) {
     message = "Опубликованный релиз не найден или недоступен. Установлена версия " + installed + ".";
    } else if (status == 403 || status == 429) {
     message = "GitHub временно ограничил проверку. Попробуйте позже.";
    } else if (status != 200) {
     message = "Не удалось проверить обновления. Код ответа: " + status;
    } else {
     ByteArrayOutputStream bytes = new ByteArrayOutputStream();
     try (InputStream input = connection.getInputStream()) {
      byte[] buffer = new byte[4096]; int n;
      while ((n = input.read(buffer)) != -1) {
       if (bytes.size() + n > 1048576) throw new java.io.IOException();
       bytes.write(buffer, 0, n);
      }
     }
     JSONObject release = new JSONObject(bytes.toString("UTF-8"));
     String latest = release.getString("tag_name").replaceFirst("^v", "");
     if (release.optBoolean("draft") || release.optBoolean("prerelease")) {
      message = "Стабильный релиз пока недоступен.";
     } else if (compare(latest, installed) <= 0) {
      message = "Новая версия не найдена. Установлена версия " + installed + ".";
     } else {
      JSONArray assets = release.getJSONArray("assets");
      for (int i = 0; i < assets.length(); i++) {
       JSONObject asset = assets.getJSONObject(i);
       if ("app-release.apk".equals(asset.optString("name"))) {
        String candidate = asset.getString("browser_download_url");
        Uri uri = Uri.parse(candidate);
        if ("https".equals(uri.getScheme()) && "github.com".equals(uri.getHost()) && uri.getPath() != null && uri.getPath().startsWith("/bormaufa-gif/smr-report/releases/download/")) download = candidate;
       }
      }
      message = download == null ? "Версия " + latest + " опубликована, но APK пока недоступен." : "Доступна версия " + latest + ". Установлена " + installed + ".\nСкачать APK через браузер? Установку нужно подтвердить самостоятельно.";
     }
    }
   } catch (IllegalArgumentException e) {
    message = "Не удалось сравнить версии. Релиз должен иметь тег v1.1.N, соответствующий версии APK.";
   } catch (Exception e) {
    message = "Не удалось проверить обновления. Проверьте интернет и повторите позже.";
   } finally { if (connection != null) connection.disconnect(); }
   final String text = message, url = download;
   activity.runOnUiThread(() -> {
    if (activity.isFinishing() || activity.isDestroyed()) return;
    button.setEnabled(true); button.setText("Проверить обновления");
    AlertDialog.Builder dialog = new AlertDialog.Builder(activity).setTitle("Обновление PTS Ai").setMessage(text).setNegativeButton("Закрыть", null);
    if (url != null) dialog.setPositiveButton("Скачать APK", (d, which) -> {
     try { activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
     catch (Exception e) { Toast.makeText(activity, "Не удалось открыть браузер", Toast.LENGTH_LONG).show(); }
    });
    dialog.show();
   });
  }, "pts-update-check").start();
 }
}
