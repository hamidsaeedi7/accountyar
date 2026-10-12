package ir.legendcoder.accountyar;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/** Solar Hijri conversion for supported years 1300..1500. */
public final class Jalali {
  public static final ZoneId ZONE = ZoneId.of("Asia/Tehran");
  public static final String[] MONTHS = {
    "فروردین",
    "اردیبهشت",
    "خرداد",
    "تیر",
    "مرداد",
    "شهریور",
    "مهر",
    "آبان",
    "آذر",
    "دی",
    "بهمن",
    "اسفند"
  };
  public final int year, month, day;

  public Jalali(int y, int m, int d) {
    year = y;
    month = m;
    day = d;
  }

  public static LocalDate today() {
    return LocalDate.now(ZONE);
  }

  public static Jalali from(LocalDate date) {
    int gy = date.getYear(), gm = date.getMonthValue(), gd = date.getDayOfMonth();
    int[] gdm = {0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334};
    int gy2 = gm > 2 ? gy + 1 : gy;
    int days =
        355666 + 365 * gy + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400 + gd + gdm[gm - 1];
    int jy = -1595 + 33 * (days / 12053);
    days %= 12053;
    jy += 4 * (days / 1461);
    days %= 1461;
    if (days > 365) {
      jy += (days - 1) / 365;
      days = (days - 1) % 365;
    }
    int jm = days < 186 ? 1 + days / 31 : 7 + (days - 186) / 30;
    int jd = 1 + (days < 186 ? days % 31 : (days - 186) % 30);
    return new Jalali(jy, jm, jd);
  }

  private static LocalDate raw(int jy, int jm, int jd) {
    jy += 1595;
    int days =
        -355668
            + 365 * jy
            + (jy / 33) * 8
            + ((jy % 33 + 3) / 4)
            + jd
            + (jm < 7 ? (jm - 1) * 31 : (jm - 7) * 30 + 186);
    int gy = 400 * (days / 146097);
    days %= 146097;
    if (days > 36524) {
      gy += 100 * (--days / 36524);
      days %= 36524;
      if (days >= 365) days++;
    }
    gy += 4 * (days / 1461);
    days %= 1461;
    if (days > 365) {
      gy += (days - 1) / 365;
      days = (days - 1) % 365;
    }
    int gd = days + 1;
    int[] lens = {
      31, LocalDate.of(gy, 1, 1).isLeapYear() ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    };
    int gm = 1;
    while (gm <= 12 && gd > lens[gm - 1]) gd -= lens[gm++ - 1];
    return LocalDate.of(gy, gm, gd);
  }

  public static int length(int y, int m) {
    if (m < 1 || m > 12) throw new IllegalArgumentException();
    if (m <= 6) return 31;
    if (m <= 11) return 30;
    return ChronoUnit.DAYS.between(raw(y, 1, 1), raw(y + 1, 1, 1)) == 366 ? 30 : 29;
  }

  public LocalDate gregorian() {
    if (year < 1300
        || year > 1500
        || month < 1
        || month > 12
        || day < 1
        || day > length(year, month))
      throw new IllegalArgumentException("تاریخ شمسی معتبر نیست؛ سال باید بین ۱۳۰۰ و ۱۵۰۰ باشد.");
    return raw(year, month, day);
  }

  public static LocalDate next(LocalDate date, String repeat, int anchor) {
    Jalali j = from(date);
    int y = j.year, m = j.month;
    if ("monthly".equals(repeat)) {
      m++;
      if (m == 13) {
        m = 1;
        y++;
      }
    } else if ("yearly".equals(repeat)) {
      y++;
    } else return date.plusMonths(1);
    return new Jalali(y, m, Math.min(anchor, length(y, m))).gregorian();
  }

  public String numeric() {
    return String.format(java.util.Locale.US, "%04d/%02d/%02d", year, month, day);
  }

  public String display() {
    return fa(day + " " + MONTHS[month - 1] + " " + year);
  }

  public static String fa(String s) {
    StringBuilder b = new StringBuilder();
    for (char c : s.toCharArray()) b.append(c >= '0' && c <= '9' ? (char) ('۰' + c - '0') : c);
    return b.toString();
  }

  public static String ascii(String s) {
    StringBuilder b = new StringBuilder();
    for (char c : s.toCharArray())
      b.append(
          c >= '۰' && c <= '۹'
              ? (char) ('0' + c - '۰')
              : c >= '٠' && c <= '٩' ? (char) ('0' + c - '٠') : c);
    return b.toString();
  }
}
