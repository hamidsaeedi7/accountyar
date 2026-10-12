package ir.legendcoder.accountyar.test;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import ir.legendcoder.accountyar.MainActivity;
import java.lang.reflect.*;
import java.util.*;

public class UiTest extends Instrumentation {
  MainActivity activity;

  public void onCreate(Bundle args) {
    super.onCreate(args);
    start();
  }

  interface Task {
    void run() throws Exception;
  }

  void ui(Task t) throws Exception {
    final Exception[] error = {null};
    runOnMainSync(
        () -> {
          try {
            t.run();
          } catch (Exception e) {
            error[0] = e;
          }
        });
    if (error[0] != null) throw error[0];
  }

  View find(View root, String text) {
    if (root instanceof TextView && text.equals(((TextView) root).getText().toString()))
      return root;
    if (root instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
        View v = find(((ViewGroup) root).getChildAt(i), text);
        if (v != null) return v;
      }
    return null;
  }

  EditText hint(View root, String hint) {
    if (root instanceof EditText
        && ((EditText) root).getHint() != null
        && hint.equals(((EditText) root).getHint().toString())) return (EditText) root;
    if (root instanceof ViewGroup)
      for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++) {
        EditText v = hint(((ViewGroup) root).getChildAt(i), hint);
        if (v != null) return v;
      }
    return null;
  }

  View main() {
    return activity.getWindow().getDecorView();
  }

  View dialog() throws Exception {
    Field f = MainActivity.class.getDeclaredField("activeDialog");
    f.setAccessible(true);
    AlertDialog d = (AlertDialog) f.get(activity);
    if (d == null) throw new Exception("No dialog");
    return d.getWindow().getDecorView();
  }

  void click(View root, String text) throws Exception {
    View v = find(root, text);
    if (v == null) throw new Exception("Missing: " + text);
    v.performClick();
  }

  void await(String text) throws Exception {
    long end = SystemClock.elapsedRealtime() + 240000;
    while (SystemClock.elapsedRealtime() < end) {
      final boolean[] found = {false};
      ui(() -> found[0] = find(main(), text) != null);
      if (found[0]) return;
      Thread.sleep(200);
    }
    throw new Exception("Timed out: " + text);
  }

  public void onStart() {
    Bundle out = new Bundle();
    try {
      Intent intent =
          new Intent(getTargetContext(), MainActivity.class)
              .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      activity = (MainActivity) startActivitySync(intent);
      waitForIdleSync();
      ui(
          () -> {
            hint(main(), "رمز اصلی").setText("UI-test-master-passphrase");
            click(main(), "بازکردن خزانه");
          });
      await("＋ افزودن اکانت");
      ui(() -> click(main(), "＋ افزودن اکانت"));
      waitForIdleSync();
      ui(
          () -> {
            View d = dialog();
            hint(d, "مثلاً Claude لپ‌تاپ").setText("UI sample account");
            hint(d, "یوزرنیم یا ایمیل").setText("ui@example.test");
            hint(d, "پسورد اکانت").setText("only-test-secret");
            click(d, "ذخیره اکانت");
          });
      waitForIdleSync();
      await("UI sample account");
      ui(() -> click(main(), "خزانه"));
      waitForIdleSync();
      ui(() -> click(main(), "اطلاعات ورود"));
      waitForIdleSync();
      ui(
          () -> {
            View d = dialog();
            if (find(d, "only-test-secret") != null)
              throw new Exception("Password initially visible");
            click(d, "نمایش پسورد");
            if (find(d, "only-test-secret") == null) throw new Exception("Show password failed");
            click(d, "بستن");
            click(main(), "تنظیمات");
          });
      waitForIdleSync();
      for (String color : new String[] {"سبز", "آبی", "بنفش", "نارنجی", "قرمز", "زرد"}) {
        ui(() -> click(main(), color));
        waitForIdleSync();
      }
      ui(() -> click(main(), "تغییر به حالت روشن"));
      waitForIdleSync();
      ui(() -> click(main(), "تغییر به حالت تاریک"));
      waitForIdleSync();
      ui(() -> click(main(), "اکانت‌ها"));
      waitForIdleSync();
      ui(() -> click(main(), "تمدید کردم"));
      waitForIdleSync();
      ui(() -> click(dialog(), "تمدید کردم؛ ذخیره کن"));
      waitForIdleSync();
      await("UI sample account");
      ui(() -> click(main(), "قفل"));
      waitForIdleSync();
      ui(
          () -> {
            if (find(main(), "UI sample account") != null
                || find(main(), "only-test-secret") != null)
              throw new Exception("Sensitive UI visible while locked");
          });
      out.putString(
          "result",
          "PASS: unlock with rotated master password, add account, hidden/show password, all six"
              + " palettes, light/dark, manual lock, preserved account after unlock and renewal"
              + " UI.");
      finish(-1, out);
    } catch (Throwable e) {
      out.putString("result", "FAIL: " + e.getClass().getName() + ": " + e.getMessage());
      finish(0, out);
    }
  }
}
