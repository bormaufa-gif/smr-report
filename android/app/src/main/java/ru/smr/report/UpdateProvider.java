package ru.smr.report;
import android.content.*;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.*;
public class UpdateProvider extends ContentProvider {
 public boolean onCreate() { return true; }
 private File file(Uri uri) throws FileNotFoundException {
  String name = uri.getLastPathSegment();
  if (!"ru.smr.report.updates".equals(uri.getAuthority()) || name == null || !name.matches("update-[a-f0-9-]+\\.apk") || !uri.getPath().equals("/" + name)) throw new FileNotFoundException();
  return new File(getContext().getCacheDir(), name);
 }
 public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
  if (!"r".equals(mode)) throw new FileNotFoundException();
  return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.MODE_READ_ONLY);
 }
 public String getType(Uri uri) { return "application/vnd.android.package-archive"; }
 public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
  try {
   File f = file(uri);
   String[] cols = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
   MatrixCursor cursor = new MatrixCursor(cols); Object[] row = new Object[cols.length];
   for (int i=0;i<cols.length;i++) { if (OpenableColumns.DISPLAY_NAME.equals(cols[i])) row[i]=f.getName(); else if (OpenableColumns.SIZE.equals(cols[i])) row[i]=f.length(); }
   cursor.addRow(row); return cursor;
  } catch (FileNotFoundException e) { return null; }
 }
 public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
 public int delete(Uri uri, String selection, String[] args) { throw new UnsupportedOperationException(); }
 public int update(Uri uri, ContentValues values, String selection, String[] args) { throw new UnsupportedOperationException(); }
}
