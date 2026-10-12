import ir.legendcoder.accountyar.Jalali;
import java.time.LocalDate;

public class DateDump {
  public static void main(String[] a) {
    for (LocalDate d = LocalDate.of(1921, 3, 21);
        d.isBefore(LocalDate.of(2121, 3, 20));
        d = d.plusDays(7)) {
      Jalali j = Jalali.from(d);
      System.out.println(d + "," + j.year + "," + j.month + "," + j.day);
    }
  }
}
