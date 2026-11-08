package ru.smr.report;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.net.Uri;
import android.provider.Settings;
import android.widget.*;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;

final class UpdateChecker {
 private static final AtomicBoolean busy = new AtomicBoolean();
 private static final String ROOT = "https://github.com/bormaufa-gif/smr-report/releases/download/";
 private static boolean alive(Activity a) { return !a.isFinishing() && !a.isDestroyed(); }
 private static void message(Activity a, String text) { if (alive(a)) new AlertDialog.Builder(a).setTitle("Обновление ПТС Ai").setMessage(text).setPositiveButton("Закрыть", null).show(); }
 private static HttpURLConnection connect(String address) throws Exception {
  for (int i=0;i<6;i++) {
   URL url = new URL(address); String host = url.getHost();
   if (!"https".equals(url.getProtocol()) || !(host.equals("api.github.com") || host.equals("github.com") || host.endsWith(".githubusercontent.com"))) throw new IOException("Недопустимый адрес загрузки");
   HttpURLConnection c = (HttpURLConnection) url.openConnection();
   c.setConnectTimeout(10000); c.setReadTimeout(15000); c.setInstanceFollowRedirects(false); c.setRequestProperty("User-Agent", "PTS-Ai-Android");
   int status = c.getResponseCode();
   if (status >= 300 && status < 400) { String location=c.getHeaderField("Location"); c.disconnect(); if(location==null) throw new IOException(); address=new URL(url,location).toString(); continue; }
   if (status != 200) { c.disconnect(); throw new IOException(status==404 ? "Опубликованный релиз не найден или недоступен" : "Сервер недоступен: " + status); }
   return c;
  }
  throw new IOException("Слишком много перенаправлений");
 }
 private static String text(String address, int limit) throws Exception {
  HttpURLConnection c=connect(address);
  try(InputStream in=c.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
   byte[] b=new byte[4096]; int n;
   while((n=in.read(b))!=-1) { if(out.size()+n>limit) throw new IOException(); out.write(b,0,n); }
   return out.toString("UTF-8");
  } finally { c.disconnect(); }
 }
 private static String asset(JSONObject release, String name) throws Exception {
  JSONArray assets=release.getJSONArray("assets");
  for(int i=0;i<assets.length();i++) { JSONObject a=assets.getJSONObject(i); if(name.equals(a.optString("name"))) { String url=a.getString("browser_download_url"); if(url.startsWith(ROOT)) return url; } }
  throw new IOException("В релизе отсутствует " + name);
 }
 static void check(Activity a, Button button) {
  if(!busy.compareAndSet(false,true)) { if(button!=null) Toast.makeText(a,"Проверка или загрузка уже выполняется",Toast.LENGTH_SHORT).show(); return; }
  if(button!=null) { button.setEnabled(false); button.setText("Проверяем…"); }
  new Thread(() -> {
   try {
    JSONObject release=new JSONObject(text("https://api.github.com/repos/bormaufa-gif/smr-report/releases/latest",1048576));
    String version=release.getString("tag_name").replaceFirst("^v","");
    if(release.optBoolean("draft") || release.optBoolean("prerelease") || !version.matches("1\\.1\\.[0-9]+")) throw new IOException("Нет совместимого стабильного релиза");
    long code=100L+Long.parseLong(version.substring(4));
    PackageInfo installed=a.getPackageManager().getPackageInfo(a.getPackageName(),0);
    if(code<=installed.versionCode) { if(button!=null) a.runOnUiThread(()->message(a,"Новых обновлений нет. Установлена версия " + installed.versionName)); return; }
    String apk=asset(release,"app-release.apk"), checksum=asset(release,"app-release.apk.sha256");
    String hash=text(checksum,4096).trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
    if(!hash.matches("[a-f0-9]{64}")) throw new IOException("Некорректная контрольная сумма");
    a.runOnUiThread(()-> { if(alive(a)) new AlertDialog.Builder(a).setTitle("Доступна версия " + version).setMessage("Скачать обновление внутри приложения? Сначала сохраните незавершённый отчёт. Установку подтвердит Android.").setNegativeButton("Позже",null).setPositiveButton("Обновить",(d,w)->download(a,apk,hash,version,code)).show(); });
   } catch(Exception e) { if(button!=null) a.runOnUiThread(()->message(a,"Не удалось проверить обновления. Проверьте интернет и доступность опубликованного релиза.")); }
   finally { busy.set(false); a.runOnUiThread(()-> { if(button!=null && alive(a)) { button.setEnabled(true); button.setText("Проверить обновления"); } }); }
  },"pts-check").start();
 }
 private static void verify(Activity a, File file, String hash, String version, long code) throws Exception {
  MessageDigest digest=MessageDigest.getInstance("SHA-256");
  try(InputStream in=new FileInputStream(file)) { byte[] b=new byte[65536]; int n; while((n=in.read(b))!=-1) digest.update(b,0,n); }
  StringBuilder hex=new StringBuilder(); for(byte b:digest.digest()) hex.append(String.format(Locale.ROOT,"%02x",b&255));
  if(!hash.equals(hex.toString())) throw new IOException("Контрольная сумма не совпала");
  PackageManager pm=a.getPackageManager();
  PackageInfo apk=pm.getPackageArchiveInfo(file.getAbsolutePath(),PackageManager.GET_SIGNATURES);
  PackageInfo current=pm.getPackageInfo(a.getPackageName(),PackageManager.GET_SIGNATURES);
  if(apk==null || !a.getPackageName().equals(apk.packageName) || !version.equals(apk.versionName) || apk.versionCode!=code || apk.versionCode<=current.versionCode) throw new IOException("Неверная версия или пакет APK");
  if(apk.signatures==null || current.signatures==null || apk.signatures.length==0 || !new HashSet<>(Arrays.asList(apk.signatures)).equals(new HashSet<>(Arrays.asList(current.signatures)))) throw new IOException("Подпись APK не совпадает");
 }
 private static void download(Activity a, String url, String hash, String version, long code) {
  if(!busy.compareAndSet(false,true)) { message(a,"Другая проверка или загрузка ещё выполняется. Повторите позже."); return; }
  AtomicBoolean cancel=new AtomicBoolean();
  LinearLayout box=new LinearLayout(a); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(32,24,32,24);
  TextView label=new TextView(a); label.setText("Начинаем загрузку…"); box.addView(label);
  ProgressBar bar=new ProgressBar(a,null,android.R.attr.progressBarStyleHorizontal); bar.setMax(100); box.addView(bar);
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Загрузка обновления").setView(box).setNegativeButton("Отмена",(d,w)->cancel.set(true)).setOnCancelListener(d->cancel.set(true)).create(); dialog.show();
  new Thread(()-> {
   File file=new File(a.getCacheDir(),"update-"+UUID.randomUUID()+".apk"); boolean success=false;
   try {
    HttpURLConnection c=connect(url);
    try(InputStream in=c.getInputStream(); OutputStream out=new FileOutputStream(file)) {
     long total=c.getContentLengthLong(), received=0; byte[] b=new byte[65536]; int n, previous=-1;
     if(total>104857600) throw new IOException("APK слишком большой");
     while((n=in.read(b))!=-1) {
      if(cancel.get() || !alive(a)) throw new IOException("Загрузка отменена");
      received+=n; if(received>104857600) throw new IOException("APK слишком большой"); out.write(b,0,n);
      int percent=total>0 ? (int)(100*received/total) : -1;
      if(percent!=previous || percent<0) { previous=percent; final long size=received; a.runOnUiThread(()-> { if(alive(a)) { bar.setIndeterminate(percent<0); if(percent>=0) bar.setProgress(percent); label.setText(percent>=0 ? "Загружено: "+percent+"%" : "Загружено: "+size/1024+" КБ"); } }); }
     }
     if(total>0 && received!=total) throw new IOException("Загрузка неполная");
    } finally { c.disconnect(); }
    if(cancel.get() || !alive(a)) throw new IOException("Загрузка отменена");
    a.runOnUiThread(()-> { if(alive(a)) label.setText("Проверяем файл и подпись…"); });
    verify(a,file,hash,version,code);
    if(cancel.get() || !alive(a)) throw new IOException("Загрузка отменена");
    success=true;
    a.runOnUiThread(()-> { if(alive(a)) { dialog.dismiss(); installDialog(a,file); } else file.delete(); });
   } catch(Exception e) { a.runOnUiThread(()-> { if(alive(a)) { dialog.dismiss(); if(!cancel.get()) message(a,"Обновление не установлено. Ошибка загрузки или проверки APK. Повторите позже."); } }); }
   finally { if(!success) file.delete(); busy.set(false); }
  },"pts-download").start();
 }
 private static void installDialog(Activity a, File file) {
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Обновление проверено").setMessage("Нажмите «Установить». Если Android попросит разрешение для ПТС Ai, включите его, вернитесь сюда и снова нажмите «Установить».").setNegativeButton("Позже",(d,w)->file.delete()).setPositiveButton("Установить",null).setOnCancelListener(d->file.delete()).create();
  dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v-> {
   try {
    if(!a.getPackageManager().canRequestPackageInstalls()) {
     a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+a.getPackageName()))); return;
    }
    Uri uri=Uri.parse("content://ru.smr.report.updates/"+file.getName());
    Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    intent.setClipData(ClipData.newRawUri("APK",uri)); a.startActivity(intent); dialog.dismiss();
   } catch(Exception e) { message(a,"Не удалось открыть установщик. Проверьте разрешение на установку приложений."); }
  })); dialog.show();
 }
}
