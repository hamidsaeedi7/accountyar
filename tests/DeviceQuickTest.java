package ir.legendcoder.accountyar.test;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import ir.legendcoder.accountyar.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.json.*;

public class DeviceQuickTest extends Instrumentation {
  public void onCreate(Bundle b) {
    super.onCreate(b);
    start();
  }

  void check(boolean b, String s) throws Exception {
    if (!b) throw new Exception(s);
  }

  public void onStart() {
    Bundle result = new Bundle();
    byte[] key = null, newKey = null;
    try {
      Context c = getTargetContext();
      VaultStore store = new VaultStore(c);
      JSONObject record =
          new JSONObject()
              .put("id", UUID.randomUUID().toString())
              .put("name", "QA Account")
              .put("username", "qa@example.test")
              .put("password", "qa-secret-NEVER-PLAINTEXT-123")
              .put("due", Jalali.today().plusDays(7).toString())
              .put("repeat", "monthly")
              .put("anchor", Jalali.from(Jalali.today().plusDays(7)).day)
              .put("remind", true);
      JSONObject data =
          new JSONObject().put("version", 1).put("accounts", new JSONArray().put(record));
      key = store.create("qa-old-master-password".toCharArray(), data);
      check(
          store
              .load(key)
              .getJSONArray("accounts")
              .getJSONObject(0)
              .getString("password")
              .equals(record.getString("password")),
          "Device vault round trip");
      try (InputStream in = new FileInputStream(new File(c.getNoBackupFilesDir(), "vault.ayv"))) {
        String raw = new String(VaultStore.readLimited(in, 1024 * 1024), StandardCharsets.UTF_8);
        check(!raw.contains("qa-secret") && !raw.contains("qa@example"), "Plaintext on disk");
      }
      ReminderEngine.sync(c, data.getJSONArray("accounts"));
      JSONObject index = ReminderEngine.read(c);
      check(index.getJSONArray("entries").length() == 1, "Keystore reminder index");
      check(
          !index.toString().contains("qa-secret") && !index.toString().contains("qa@example"),
          "Credentials in reminder index");
      try (InputStream in =
          new FileInputStream(new File(c.getNoBackupFilesDir(), "reminders.ayr"))) {
        check(
            !new String(VaultStore.readLimited(in, 1024 * 1024), StandardCharsets.UTF_8)
                .contains("QA Account"),
            "Unencrypted reminder index");
      }
      newKey = store.changeMaster(key, "UI-test-master-passphrase".toCharArray());
      check(!Arrays.equals(key, newKey), "Vault key not rotated");
      check(
          store.load(newKey).getJSONArray("accounts").length() == 1,
          "Data not preserved after rotation");
      boolean rejected = false;
      try {
        store.load(key);
      } catch (Exception good) {
        rejected = true;
      }
      check(rejected, "Old key accepted after rotation");
      record.put("name", "QA Edited");
      store.save(newKey, data);
      check(
          store
              .load(newKey)
              .getJSONArray("accounts")
              .getJSONObject(0)
              .getString("name")
              .equals("QA Edited"),
          "Edit persistence");
      data.put("accounts", new JSONArray());
      store.save(newKey, data);
      ReminderEngine.sync(c, data.getJSONArray("accounts"));
      check(store.load(newKey).getJSONArray("accounts").length() == 0, "Delete account");
      check(ReminderEngine.read(c).getJSONArray("entries").length() == 0, "Cancel reminders");
      result.putString(
          "result",
          "PASS: Android vault create/load/save, encrypted disk, Keystore reminder metadata, no"
              + " credentials in reminders, master change with key rotation, preserved data, old"
              + " key rejected, account edit/delete and alarm cancellation.");
      finish(-1, result);
    } catch (Throwable e) {
      result.putString("result", "FAIL: " + e.getClass().getName() + ": " + e.getMessage());
      finish(0, result);
    } finally {
      Crypto.wipe(key);
      Crypto.wipe(newKey);
    }
  }
}
