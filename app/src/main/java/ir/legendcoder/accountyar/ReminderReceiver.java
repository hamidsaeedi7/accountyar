package ir.legendcoder.accountyar;

import android.content.*;
import java.time.*;
import org.json.*;

public final class ReminderReceiver extends BroadcastReceiver {
  @Override
  public void onReceive(Context c, Intent i) {
    String action = i.getAction();
    if (!"ir.legendcoder.accountyar.REMIND".equals(action)) {
      if (Intent.ACTION_BOOT_COMPLETED.equals(action)
          || Intent.ACTION_TIME_CHANGED.equals(action)
          || Intent.ACTION_TIMEZONE_CHANGED.equals(action)
          || Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)
          || Intent.ACTION_USER_PRESENT.equals(action)
          || "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED".equals(action))
        ReminderEngine.reschedule(c);
      return;
    }
    try {
      String id = i.getStringExtra("id");
      int d = i.getIntExtra("offset", -1);
      if (d != 7 && d != 3 && d != 1) return;
      ReminderEngine.deliver(c, id, d);
    } catch (Exception ignored) {
    }
  }
}
