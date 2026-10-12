package ir.legendcoder.accountyar;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.json.*;

public final class VaultStore {
  private final AtomicFile file;

  public VaultStore(Context c) {
    file = new AtomicFile(new File(c.getNoBackupFilesDir(), "vault.ayv"));
  }

  public boolean exists() {
    return file.getBaseFile().exists();
  }

  private JSONObject read() throws Exception {
    try (InputStream in = file.openRead()) {
      return new JSONObject(new String(readLimited(in, 16 * 1024 * 1024), StandardCharsets.UTF_8));
    }
  }

  private void write(JSONObject value) throws Exception {
    FileOutputStream out = null;
    try {
      out = file.startWrite();
      out.write(value.toString().getBytes(StandardCharsets.UTF_8));
      file.finishWrite(out);
    } catch (Exception e) {
      if (out != null) file.failWrite(out);
      throw e;
    }
  }

  public byte[] create(char[] password, JSONObject data) throws Exception {
    byte[] salt = Crypto.random(32),
        master = Crypto.derive(password, salt, Crypto.ITERATIONS),
        key = Crypto.random(32);
    try {
      JSONObject root =
          new JSONObject()
              .put("format", "accountyar-vault")
              .put("version", 1)
              .put("iterations", Crypto.ITERATIONS)
              .put("salt", Crypto.b64(salt))
              .put("wrapped", Crypto.b64(Crypto.seal(master, key, "accountyar/master/v1")))
              .put(
                  "data",
                  Crypto.b64(
                      Crypto.seal(
                          key,
                          data.toString().getBytes(StandardCharsets.UTF_8),
                          "accountyar/data/v1")));
      write(root);
      return key;
    } catch (Exception e) {
      Crypto.wipe(key);
      throw e;
    } finally {
      Crypto.wipe(master);
    }
  }

  public byte[] unlock(char[] password) throws Exception {
    JSONObject root = read();
    validate(root);
    byte[] master =
        Crypto.derive(password, Crypto.unb64(root.getString("salt")), root.getInt("iterations"));
    try {
      return Crypto.open(master, Crypto.unb64(root.getString("wrapped")), "accountyar/master/v1");
    } finally {
      Crypto.wipe(master);
    }
  }

  public JSONObject load(byte[] key) throws Exception {
    JSONObject root = read();
    validate(root);
    byte[] plain = Crypto.open(key, Crypto.unb64(root.getString("data")), "accountyar/data/v1");
    try {
      JSONObject data = new JSONObject(new String(plain, StandardCharsets.UTF_8));
      validateData(data);
      return data;
    } finally {
      Crypto.wipe(plain);
    }
  }

  public void save(byte[] key, JSONObject data) throws Exception {
    validateData(data);
    JSONObject root = read();
    root.put(
        "data",
        Crypto.b64(
            Crypto.seal(
                key, data.toString().getBytes(StandardCharsets.UTF_8), "accountyar/data/v1")));
    write(root);
  }

  public byte[] changeMaster(byte[] oldKey, char[] pass) throws Exception {
    JSONObject root = read();
    JSONObject data = load(oldKey);
    byte[] salt = Crypto.random(32),
        master = Crypto.derive(pass, salt, Crypto.ITERATIONS),
        nextKey = Crypto.random(32);
    try {
      root.put("salt", Crypto.b64(salt))
          .put("iterations", Crypto.ITERATIONS)
          .put("wrapped", Crypto.b64(Crypto.seal(master, nextKey, "accountyar/master/v1")))
          .put(
              "data",
              Crypto.b64(
                  Crypto.seal(
                      nextKey,
                      data.toString().getBytes(StandardCharsets.UTF_8),
                      "accountyar/data/v1")));
      write(root);
      return nextKey;
    } catch (Exception e) {
      Crypto.wipe(nextKey);
      throw e;
    } finally {
      Crypto.wipe(master);
    }
  }

  public byte[] backup(byte[] key, char[] pass) throws Exception {
    JSONObject data = load(key);
    byte[] salt = Crypto.random(32), derived = Crypto.derive(pass, salt, Crypto.ITERATIONS);
    try {
      return new JSONObject()
          .put("format", "accountyar-backup")
          .put("version", 1)
          .put("iterations", Crypto.ITERATIONS)
          .put("salt", Crypto.b64(salt))
          .put(
              "payload",
              Crypto.b64(
                  Crypto.seal(
                      derived,
                      data.toString().getBytes(StandardCharsets.UTF_8),
                      "accountyar/backup/v1")))
          .toString()
          .getBytes(StandardCharsets.UTF_8);
    } finally {
      Crypto.wipe(derived);
    }
  }

  public static JSONObject restore(byte[] file, char[] pass) throws Exception {
    JSONObject root = new JSONObject(new String(file, StandardCharsets.UTF_8));
    if (!"accountyar-backup".equals(root.optString("format")) || root.optInt("version") != 1)
      throw new IOException("Unsupported backup");
    byte[]
        derived =
            Crypto.derive(pass, Crypto.unb64(root.getString("salt")), root.getInt("iterations")),
        plain = null;
    try {
      plain = Crypto.open(derived, Crypto.unb64(root.getString("payload")), "accountyar/backup/v1");
      JSONObject d = new JSONObject(new String(plain, StandardCharsets.UTF_8));
      validateData(d);
      return d;
    } finally {
      Crypto.wipe(derived);
      Crypto.wipe(plain);
    }
  }

  static void validate(JSONObject r) throws Exception {
    if (!"accountyar-vault".equals(r.optString("format")) || r.optInt("version") != 1)
      throw new IOException("Unsupported vault");
  }

  public static void validateData(JSONObject d) throws Exception {
    JSONArray a = d.getJSONArray("accounts");
    if (a.length() > 2000) throw new IOException("Too many accounts");
    java.util.HashSet<String> ids = new java.util.HashSet<>();
    for (int i = 0; i < a.length(); i++) {
      JSONObject o = a.getJSONObject(i);
      String id = o.getString("id");
      java.util.UUID.fromString(id);
      if (!ids.add(id)) throw new IOException("Duplicate id");
      if (o.getString("name").trim().isEmpty() || o.getString("name").length() > 120)
        throw new IOException("Invalid account");
      String due = o.optString("due");
      if (!due.isEmpty()) Jalali.from(java.time.LocalDate.parse(due)).gregorian();
      String repeat = o.optString("repeat", "none");
      if (!Arrays.asList("none", "monthly", "yearly").contains(repeat))
        throw new IOException("Invalid recurrence");
      int anchor = o.optInt("anchor", 1);
      if (anchor < 1 || anchor > 31) throw new IOException("Invalid day");
    }
  }

  public static byte[] readLimited(InputStream in, int max) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] buf = new byte[8192];
    int n;
    while ((n = in.read(buf)) != -1) {
      if (out.size() + n > max) throw new IOException("File is too large");
      out.write(buf, 0, n);
    }
    return out.toByteArray();
  }
}
