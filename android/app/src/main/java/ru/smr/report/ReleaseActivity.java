package ru.smr.report;
import android.os.Bundle;
public class ReleaseActivity extends VoiceActivity {
 private static boolean checked;
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  if (!checked) { checked = true; UpdateChecker.check(this, null); }
 }
}
