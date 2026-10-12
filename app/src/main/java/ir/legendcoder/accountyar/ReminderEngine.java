package ir.legendcoder.accountyar;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.security.keystore.*;
import android.util.AtomicFile;
import java.io.*;
import java.security.KeyStore;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import org.json.*;

public final class ReminderEngine {
  static final String CHANNEL = "renewals_v1", ALIAS = "accountyar.reminders.v1";
  static final int[] OFFSETS = {7, 3, 1};

  public static String status(Context c) {
    return isEnabled(c)
        ? "اعلان‌ها فعال هستند"
        : "اعلان‌ها غیرفعال هستند؛ برای دریافت یادآورها فعالشان کن";
  }

  private static javax.crypto.SecretKey key() throws Exception {
    KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
    ks.load(null);
    if (ks.containsAlias(ALIAS)) return (javax.crypto.SecretKey) ks.getKey(ALIAS, null);
    KeyGenerator gen = KeyGenerator.getInstance("AES", "AndroidKeyStore");
    gen.init(
        new KeyGenParameterSpec.Builder(
                ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build());
    return gen.generateKey();
  }

  private static AtomicFile file(Context c) {
    return new AtomicFile(new File(c.getNoBackupFilesDir(), "reminders.ayr"));
  }

  public static JSONObject read(Context c) throws Exception {
    if (!file(c).getBaseFile().exists()) return new JSONObject().put("entries", new JSONArray());
    byte[] raw;
    try (InputStream in = file(c).openRead()) {
      raw = VaultStore.readLimited(in, 2 * 1024 * 1024);
    }
    JSONObject e = new JSONObject(new String(raw, java.nio.charset.StandardCharsets.UTF_8));
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(
        Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Crypto.unb64(e.getString("iv"))));
    cipher.updateAAD("accountyar/reminders/v1".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    byte[] p = cipher.doFinal(Crypto.unb64(e.getString("data")));
    try {
      return new JSONObject(new String(p, java.nio.charset.StandardCharsets.UTF_8));
    } finally {
      Crypto.wipe(p);
    }
  }

  static void write(Context c, JSONObject idx) throws Exception {
    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(Cipher.ENCRYPT_MODE, key());
    cipher.updateAAD("accountyar/reminders/v1".getBytes(java.nio.charset.StandardCharsets.UTF_8));
    JSONObject e =
        new JSONObject()
            .put("iv", Crypto.b64(cipher.getIV()))
            .put(
                "data",
                Crypto.b64(
                    cipher.doFinal(
                        idx.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8))));
    FileOutputStream out = null;
    AtomicFile f = file(c);
    try {
      out = f.startWrite();
      out.write(e.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
      f.finishWrite(out);
    } catch (Exception x) {
      if (out != null) f.failWrite(out);
      throw x;
    }
  }

  public static void sync(Context c, JSONArray accounts) throws Exception {
    JSONObject old;
    try {
      old = read(c);
    } catch (Exception e) {
      old = new JSONObject().put("entries", new JSONArray());
    }
    cancel(c, old);
    JSONArray entries = new JSONArray();
    for (int i = 0; i < accounts.length(); i++) {
      JSONObject o = accounts.getJSONObject(i);
      if (!o.optString("due").isEmpty() && o.optBoolean("remind", true))
        entries.put(
            new JSONObject()
                .put("id", o.getString("id"))
                .put("name", o.getString("name"))
                .put("due", o.getString("due")));
    }
    JSONObject sent = new JSONObject(), oldSent = old.optJSONObject("sent");
    if (oldSent != null)
      for (int i = 0; i < entries.length(); i++) {
        JSONObject o = entries.getJSONObject(i);
        for (int d : OFFSETS) {
          String token = token(o, d);
          if (oldSent.has(token)) sent.put(token, true);
        }
      }
    JSONObject idx = new JSONObject().put("entries", entries).put("sent", sent);
    write(c, idx);
    schedule(c, idx);
  }

  private static PendingIntent pi(Context c, String id, int offset) {
    Intent in =
        new Intent(c, ReminderReceiver.class)
            .setAction("ir.legendcoder.accountyar.REMIND")
            .setData(Uri.parse("accountyar://reminder/" + id + "/" + offset))
            .putExtra("id", id)
            .putExtra("offset", offset);
    return PendingIntent.getBroadcast(
        c, 0, in, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
  }

  private static void cancel(Context c, JSONObject idx) throws Exception {
    AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
    JSONArray a = idx.getJSONArray("entries");
    for (int i = 0; i < a.length(); i++)
      for (int d : OFFSETS) am.cancel(pi(c, a.getJSONObject(i).getString("id"), d));
  }

  public static void schedule(Context c, JSONObject idx) throws Exception {
    channel(c);
    AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
    JSONArray a = idx.getJSONArray("entries");
    Instant now = Instant.now();
    for (int i = 0; i < a.length(); i++) {
      JSONObject o = a.getJSONObject(i);
      LocalDate due = LocalDate.parse(o.getString("due"));
      for (int offset : OFFSETS) {
        if (idx.optJSONObject("sent") != null
            && idx.optJSONObject("sent").optBoolean(token(o, offset))) continue;
        LocalDate when = due.minusDays(offset);
        if (when.isBefore(Jalali.today())) continue;
        Instant at = when.atTime(9, 0).atZone(Jalali.ZONE).toInstant();
        if (at.isBefore(now)) {
          at = now.plusSeconds(60);
        }
        PendingIntent p = pi(c, o.getString("id"), offset);
        try {
          if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms())
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), p);
          else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), p);
        } catch (SecurityException e) {
          am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), p);
        }
      }
    }
  }

  public static void reschedule(Context c) {
    try {
      JSONObject idx = read(c);
      cancel(c, idx);
      schedule(c, idx);
    } catch (Exception ignored) {
      /* A locked device is retried on USER_PRESENT / next app unlock. */
    }
  }

  static String token(JSONObject o, int d) {
    return o.optString("id") + "/" + o.optString("due") + "/" + d;
  }

  public static boolean isEnabled(Context c) {
    NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    NotificationChannel ch = nm.getNotificationChannel(CHANNEL);
    return (Build.VERSION.SDK_INT < 33
            || c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                == PackageManager.PERMISSION_GRANTED)
        && nm.areNotificationsEnabled()
        && (ch == null || ch.getImportance() != NotificationManager.IMPORTANCE_NONE);
  }

  static synchronized void deliver(Context c, String id, int offset) throws Exception {
    JSONObject idx = read(c);
    JSONArray a = idx.getJSONArray("entries");
    JSONObject sent = idx.optJSONObject("sent");
    if (sent == null) sent = new JSONObject();
    for (int i = 0; i < a.length(); i++) {
      JSONObject o = a.getJSONObject(i);
      if (o.getString("id").equals(id)
          && ChronoUnit.DAYS.between(Jalali.today(), LocalDate.parse(o.getString("due")))
              == offset) {
        String token = token(o, offset);
        if (sent.optBoolean(token) || !isEnabled(c)) return;
        notify(c, o, offset);
        sent.put(token, true);
        idx.put("sent", sent);
        write(c, idx);
        return;
      }
    }
  }

  static void channel(Context c) {
    NotificationManager nm = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
    NotificationChannel ch =
        new NotificationChannel(CHANNEL, "یادآور تمدید اکانت", NotificationManager.IMPORTANCE_HIGH);
    ch.setDescription("اعلان‌های ۷، ۳ و ۱ روز قبل از انقضا");
    ch.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
    nm.createNotificationChannel(ch);
  }

  public static void notify(Context c, JSONObject o, int offset) {
    channel(c);
    if (Build.VERSION.SDK_INT >= 33
        && c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
            != PackageManager.PERMISSION_GRANTED) return;
    String title = "تمدید " + o.optString("name", "اکانت"),
        body =
            Jalali.fa(offset + " روز تا انقضا • ")
                + Jalali.from(LocalDate.parse(o.optString("due", Jalali.today().toString())))
                    .display();
    show(c, title, body, (o.optString("id") + offset).hashCode());
  }

  public static void test(Context c) {
    channel(c);
    show(
        c, "اعلان آزمایشی اکانت‌یار", "اعلان‌های تمدید با فاصله ۷، ۳ و ۱ روز ارسال می‌شوند.", 4242);
  }

  private static void show(Context c, String title, String text, int id) {
    if (Build.VERSION.SDK_INT >= 33
        && c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")
            != PackageManager.PERMISSION_GRANTED) return;
    PendingIntent open =
        PendingIntent.getActivity(
            c,
            0,
            new Intent(c, MainActivity.class),
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    Notification pub =
        new Notification.Builder(c, CHANNEL)
            .setSmallIcon(ir.legendcoder.accountyar.R.drawable.ic_notify)
            .setContentTitle("اکانت‌یار")
            .setContentText("یک یادآور تمدید داری")
            .build();
    Notification n =
        new Notification.Builder(c, CHANNEL)
            .setSmallIcon(ir.legendcoder.accountyar.R.drawable.ic_notify)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(new Notification.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(pub)
            .build();
    ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(id, n);
  }
}
