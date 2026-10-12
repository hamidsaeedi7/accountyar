package ir.legendcoder.accountyar;

import java.security.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/** Uses platform JCA primitives. All envelopes use authenticated encryption. */
public final class Crypto {
  public static final int ITERATIONS = 600000;
  private static final SecureRandom RANDOM = new SecureRandom();

  public static byte[] random(int n) {
    byte[] v = new byte[n];
    RANDOM.nextBytes(v);
    return v;
  }

  public static String b64(byte[] data) {
    return Base64.getEncoder().encodeToString(data);
  }

  public static byte[] unb64(String data) {
    return Base64.getDecoder().decode(data);
  }

  public static byte[] derive(char[] password, byte[] salt, int iterations) throws Exception {
    if (salt.length != 32 || iterations < 600000 || iterations > 2000000)
      throw new GeneralSecurityException("Invalid KDF settings");
    PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, 256);
    try {
      return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    } finally {
      spec.clearPassword();
    }
  }

  public static byte[] seal(byte[] key, byte[] plain, String context) throws Exception {
    byte[] iv = random(12);
    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
    c.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
    c.updateAAD(context.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    byte[] enc = c.doFinal(plain), out = new byte[iv.length + enc.length];
    System.arraycopy(iv, 0, out, 0, 12);
    System.arraycopy(enc, 0, out, 12, enc.length);
    return out;
  }

  public static byte[] open(byte[] key, byte[] sealed, String context) throws Exception {
    if (sealed.length < 28) throw new GeneralSecurityException("Invalid envelope");
    Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
    c.init(
        Cipher.DECRYPT_MODE,
        new SecretKeySpec(key, "AES"),
        new GCMParameterSpec(128, Arrays.copyOfRange(sealed, 0, 12)));
    c.updateAAD(context.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    return c.doFinal(sealed, 12, sealed.length - 12);
  }

  public static void wipe(byte[] a) {
    if (a != null) Arrays.fill(a, (byte) 0);
  }

  public static void wipe(char[] a) {
    if (a != null) Arrays.fill(a, '\0');
  }
}
