import ir.legendcoder.accountyar.*;
import java.time.*;
import java.util.*;

public class CoreTest {
  static void check(boolean b, String msg) {
    if (!b) throw new AssertionError(msg);
  }

  public static void main(String[] args) throws Exception {
    check(new Jalali(1405, 7, 20).gregorian().equals(LocalDate.of(2026, 10, 12)), "Known date");
    int days = 0;
    for (LocalDate g = LocalDate.of(1921, 3, 21);
        g.isBefore(LocalDate.of(2121, 3, 20));
        g = g.plusDays(1)) {
      Jalali j = Jalali.from(g);
      if (j.year >= 1300 && j.year <= 1500) {
        check(j.gregorian().equals(g), "Round trip " + g);
        days++;
      }
    }
    check(Jalali.length(1405, 12) == 29, "Esfand 1405 length");
    check(Jalali.length(1403, 12) == 30, "Leap year 1403");
    LocalDate initial = new Jalali(1405, 6, 31).gregorian();
    LocalDate next = Jalali.next(initial, "monthly", 31);
    check(Jalali.from(next).numeric().equals("1405/07/30"), "Clamp 31-day anchor");
    for (int i = 0; i < 6; i++) next = Jalali.next(next, "monthly", 31);
    check(Jalali.from(next).numeric().equals("1406/01/31"), "Restore anchor after Esfand");
    try {
      new Jalali(1405, 12, 30).gregorian();
      throw new AssertionError("Invalid accepted");
    } catch (IllegalArgumentException ok) {
    }
    check(Jalali.ascii("۱۴۰۵/٠٧/20").equals("1405/07/20"), "Persian and Arabic digits");
    char[] p = "A long test passphrase 123!".toCharArray();
    byte[] salt = Crypto.random(32),
        key = Crypto.derive(p, salt, Crypto.ITERATIONS),
        plain = "user@example.com; secret password".getBytes("UTF-8");
    byte[] seal = Crypto.seal(key, plain, "test/v1");
    check(Arrays.equals(plain, Crypto.open(key, seal, "test/v1")), "Crypto round trip");
    check(!Arrays.equals(seal, Crypto.seal(key, plain, "test/v1")), "Unique nonces");
    byte[] corrupt = seal.clone();
    corrupt[corrupt.length - 1] ^= 1;
    try {
      Crypto.open(key, corrupt, "test/v1");
      throw new AssertionError("Tamper accepted");
    } catch (javax.crypto.AEADBadTagException ok) {
    }
    try {
      Crypto.open(key, seal, "wrong-context");
      throw new AssertionError("Wrong AAD accepted");
    } catch (javax.crypto.AEADBadTagException ok) {
    }
    byte[] wrong = Crypto.derive("wrong passphrase 123".toCharArray(), salt, Crypto.ITERATIONS);
    try {
      Crypto.open(wrong, seal, "test/v1");
      throw new AssertionError("Wrong key accepted");
    } catch (javax.crypto.AEADBadTagException ok) {
    }
    Crypto.wipe(p);
    Crypto.wipe(key);
    check(key[0] == 0 && p[0] == '\0', "Clear sensitive arrays");
    System.out.println(
        "PASS: "
            + days
            + " date round-trips, known dates, leap/clamped renewals, wrong-password rejection,"
            + " tamper detection, context separation and random IVs.");
  }
}
