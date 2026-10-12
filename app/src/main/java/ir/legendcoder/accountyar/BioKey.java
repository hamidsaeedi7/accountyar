package ir.legendcoder.accountyar;

import android.app.Activity;
import android.content.*;
import android.hardware.biometrics.*;
import android.os.CancellationSignal;
import android.security.keystore.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

public final class BioKey {
  static final String ALIAS = "accountyar.biometric.v1";

  public interface Callback {
    void success(byte[] key);

    void error(String message);
  }

  static android.content.SharedPreferences prefs(Activity a) {
    return a.getSharedPreferences("biometric", 0);
  }

  public static boolean enabled(Activity a) {
    return prefs(a).contains("data");
  }

  public static boolean available(Activity a) {
    return a.getSystemService(BiometricManager.class)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        == BiometricManager.BIOMETRIC_SUCCESS;
  }

  public static void disable(Activity a) {
    prefs(a).edit().clear().commit();
    try {
      KeyStore s = KeyStore.getInstance("AndroidKeyStore");
      s.load(null);
      s.deleteEntry(ALIAS);
    } catch (Exception ignored) {
    }
  }

  public static CancellationSignal run(Activity a, byte[] enroll, Callback callback) {
    CancellationSignal cancel = new CancellationSignal();
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      if (enroll != null) {
        disable(a);
        KeyGenerator g = KeyGenerator.getInstance("AES", "AndroidKeyStore");
        g.init(
            new KeyGenParameterSpec.Builder(
                    ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                .setInvalidatedByBiometricEnrollment(true)
                .build());
        cipher.init(Cipher.ENCRYPT_MODE, g.generateKey());
      } else {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        cipher.init(
            Cipher.DECRYPT_MODE,
            (javax.crypto.SecretKey) ks.getKey(ALIAS, null),
            new GCMParameterSpec(128, Crypto.unb64(prefs(a).getString("iv", ""))));
      }
      cipher.updateAAD("accountyar/bio/v1".getBytes(java.nio.charset.StandardCharsets.UTF_8));
      BiometricPrompt prompt =
          new BiometricPrompt.Builder(a)
              .setTitle(enroll != null ? "فعال‌سازی اثر انگشت" : "بازکردن خزانه")
              .setSubtitle("احراز هویت امن اندروید")
              .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
              .setNegativeButton("انصراف", a.getMainExecutor(), (d, w) -> {})
              .build();
      prompt.authenticate(
          new BiometricPrompt.CryptoObject(cipher),
          cancel,
          a.getMainExecutor(),
          new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
              try {
                Cipher cc = result.getCryptoObject().getCipher();
                if (enroll != null) {
                  byte[] enc = cc.doFinal(enroll);
                  prefs(a)
                      .edit()
                      .putString("iv", Crypto.b64(cc.getIV()))
                      .putString("data", Crypto.b64(enc))
                      .commit();
                  callback.success(null);
                } else callback.success(cc.doFinal(Crypto.unb64(prefs(a).getString("data", ""))));
              } catch (Exception e) {
                callback.error("اثر انگشت قابل استفاده نیست؛ با رمز اصلی وارد شو.");
              }
            }

            @Override
            public void onAuthenticationError(int code, CharSequence err) {
              callback.error("احراز هویت انجام نشد؛ رمز اصلی همچنان قابل استفاده است.");
            }
          });
    } catch (Exception e) {
      disable(a);
      callback.error("اثر انگشت در دسترس نیست؛ از رمز اصلی استفاده کن.");
    }
    return cancel;
  }
}
