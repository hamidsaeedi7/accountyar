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

public class DeviceTest extends Instrumentation {
  public void onCreate(Bundle args) {
    super.onCreate(args);
    start();
  }

  void check(boolean v, String m) throws Exception {
    if (!v) throw new Exception(m);
  }

  public void onStart() {
    Bundle result = new Bundle();
    byte[] key = null, reopened = null;
    char[] pass = "qa-only-master-password".toCharArray(),
        backpass = "qa-only-backup-password".toCharArray();
    try {
      Context c = getTargetContext();
      VaultStore store = new VaultStore(c);
      JSONObject account =
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
          new JSONObject().put("version", 1).put("accounts", new JSONArray().put(account));
      key = store.create(pass, data);
      check(
          store
              .load(key)
              .getJSONArray("accounts")
              .getJSONObject(0)
              .getString("password")
              .equals(account.getString("password")),
          "Vault round-trip");
      reopened = store.unlock(pass);
      check(Arrays.equals(key, reopened), "Master unlock");
      boolean rejected = false;
      try {
        store.unlock("wrong password".toCharArray());
      } catch (Exception good) {
        rejected = true;
      }
      check(rejected, "Wrong master password accepted");
      byte[] file;
      try (InputStream in = new FileInputStream(new File(c.getNoBackupFilesDir(), "vault.ayv"))) {
        file = VaultStore.readLimited(in, 1024 * 1024);
      }
      String disk = new String(file, StandardCharsets.UTF_8);
      check(!disk.contains("qa-secret") && !disk.contains("qa@example"), "Plaintext on disk");
      byte[] backup = store.backup(key, backpass);
      check(!new String(backup, StandardCharsets.UTF_8).contains("qa-secret"), "Plaintext backup");
      check(
          VaultStore.restore(backup, backpass).getJSONArray("accounts").length() == 1,
          "Backup restore");
      rejected = false;
      try {
        VaultStore.restore(backup, "wrong backup".toCharArray());
      } catch (Exception good) {
        rejected = true;
      }
      check(rejected, "Wrong backup password accepted");
      JSONObject tampered = new JSONObject(new String(backup, StandardCharsets.UTF_8));
      byte[] payload = Crypto.unb64(tampered.getString("payload"));
      payload[payload.length - 1] ^= 1;
      tampered.put("payload", Crypto.b64(payload));
      rejected = false;
      try {
        VaultStore.restore(tampered.toString().getBytes(StandardCharsets.UTF_8), backpass);
      } catch (Exception good) {
        rejected = true;
      }
      check(rejected, "Backup tampering accepted");
      ReminderEngine.sync(c, data.getJSONArray("accounts"));
      JSONObject index = ReminderEngine.read(c);
      check(index.getJSONArray("entries").length() == 1, "Reminder index");
      String serial = index.toString();
      check(
          !serial.contains("qa-secret") && !serial.contains("qa@example"),
          "Credentials in reminder index");
      try (InputStream in =
          new FileInputStream(new File(c.getNoBackupFilesDir(), "reminders.ayr"))) {
        String raw = new String(VaultStore.readLimited(in, 1024 * 1024), StandardCharsets.UTF_8);
        check(!raw.contains("QA Account"), "Unencrypted reminder metadata");
      }
      byte[] nextKey = store.changeMaster(key, "qa-new-master-passphrase".toCharArray());
      check(!Arrays.equals(key, nextKey), "Vault key not rotated");
      Crypto.wipe(key);
      key = nextKey;
      Crypto.wipe(reopened);
      reopened = store.unlock("qa-new-master-passphrase".toCharArray());
      check(Arrays.equals(key, reopened), "Master change");
      account.put("name", "QA Updated");
      store.save(key, data);
      check(
          store
              .load(key)
              .getJSONArray("accounts")
              .getJSONObject(0)
              .getString("name")
              .equals("QA Updated"),
          "Save edited account");
      data.put("accounts", new JSONArray());
      store.save(key, data);
      ReminderEngine.sync(c, data.getJSONArray("accounts"));
      check(store.load(key).getJSONArray("accounts").length() == 0, "Delete account");
      check(
          ReminderEngine.read(c).getJSONArray("entries").length() == 0, "Cancel deleted reminders");
      result.putString(
          "result",
          "PASS: device vault encryption, wrong passwords, encrypted backup, restore, tamper"
              + " rejection, Keystore reminder index, master change, account edit/delete and"
              + " reminder cancellation.");
      finish(-1, result);
    } catch (Throwable e) {
      result.putString("result", "FAIL: " + e.getClass().getName() + ": " + e.getMessage());
      finish(0, result);
    } finally {
      Crypto.wipe(key);
      Crypto.wipe(reopened);
      Crypto.wipe(pass);
      Crypto.wipe(backpass);
    }
  }
}
