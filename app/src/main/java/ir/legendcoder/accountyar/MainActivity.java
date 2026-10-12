package ir.legendcoder.accountyar;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.ClipboardManager;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.text.method.PasswordTransformationMethod;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.io.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public final class MainActivity extends Activity {
  private VaultStore store;
  private byte[] vaultKey;
  private JSONObject data;
  private final ExecutorService worker = Executors.newSingleThreadExecutor();
  private final Handler handler = new Handler(Looper.getMainLooper());
  private Typeface regular, bold;
  private LinearLayout root, body, nav;
  private ScrollView scroll;
  private android.content.SharedPreferences ui;
  private int accent, bg, surface, ink, muted, border;
  private boolean dark;
  private String tab = "accounts", query = "", filter = "all";
  private int generation = 0, failures = 0;
  private long retryAt = 0;
  private boolean busy = false;
  private byte[] pendingBackup, pendingRestore;
  private CancellationSignal biometric;
  private AlertDialog activeDialog;
  private final java.util.ArrayList<AlertDialog> sensitiveDialogs = new java.util.ArrayList<>();
  private ClipData ourClip;
  private final Runnable clearClipboard = () -> clearClip();
  private static final String[] COLOR_NAMES = {"سبز", "آبی", "بنفش", "نارنجی", "قرمز", "زرد"};
  private static final int[] LIGHT_COLORS = {
    0xff087c59, 0xff195dc9, 0xff7741c6, 0xffb64f0b, 0xffc02e42, 0xff886600
  };
  private static final int[] DARK_COLORS = {
    0xff5be0a4, 0xff7ab0ff, 0xffc09aff, 0xffffb16b, 0xffff8796, 0xffffd85c
  };
  private static final String[] CATEGORY = {
    "هوش مصنوعی", "VPN", "هاست و دامنه", "نرم‌افزار", "سایر"
  };

  @Override
  public void onCreate(Bundle s) {
    super.onCreate(s);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    ui = getSharedPreferences("appearance", 0);
    store = new VaultStore(this);
    regular = Typeface.createFromAsset(getAssets(), "Vazirmatn-Regular.ttf");
    bold = Typeface.createFromAsset(getAssets(), "Vazirmatn-Bold.ttf");
    theme();
    showLocked();
  }

  private int dp(float n) {
    return (int) (n * getResources().getDisplayMetrics().density + .5f);
  }

  private void theme() {
    dark = ui.getBoolean("dark", true);
    int selected = Math.max(0, Math.min(5, ui.getInt("color", 0)));
    accent = (dark ? DARK_COLORS : LIGHT_COLORS)[selected];
    bg = dark ? 0xff10151b : 0xfff4f6f9;
    surface = dark ? 0xff1b232c : Color.WHITE;
    ink = dark ? 0xfff4f7fb : 0xff1a2637;
    muted = dark ? 0xffa2b0c1 : 0xff64748b;
    border = dark ? 0xff303c49 : 0xffe0e6ed;
  }

  private GradientDrawable shape(int color, int radius, int stroke) {
    GradientDrawable d = new GradientDrawable();
    d.setColor(color);
    d.setCornerRadius(dp(radius));
    if (stroke != 0) d.setStroke(dp(1), stroke);
    return d;
  }

  private int tint(int color, float a) {
    return (color & 0xffffff) | ((int) (255 * a) << 24);
  }

  private TextView text(String value, int sp, int color, boolean strong) {
    TextView t = new TextView(this);
    t.setText(value);
    t.setTextSize(sp);
    t.setTextColor(color);
    t.setTypeface(strong ? bold : regular);
    t.setGravity(Gravity.START);
    t.setLineSpacing(dp(3), 1);
    t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
    return t;
  }

  private LinearLayout column() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.VERTICAL);
    l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    return l;
  }

  private LinearLayout row() {
    LinearLayout l = new LinearLayout(this);
    l.setOrientation(LinearLayout.HORIZONTAL);
    l.setGravity(Gravity.CENTER_VERTICAL);
    l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    return l;
  }

  private void add(LinearLayout parent, View child, int top) {
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
    p.topMargin = dp(top);
    parent.addView(child, p);
  }

  private void weight(LinearLayout parent, View child) {
    LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1);
    p.setMargins(dp(3), 0, dp(3), 0);
    parent.addView(child, p);
  }

  private Button button(String label, boolean filled, Runnable action) {
    Button b = new Button(this);
    b.setText(label);
    b.setTextSize(13);
    b.setTypeface(bold);
    b.setAllCaps(false);
    b.setMinHeight(dp(48));
    b.setMinimumHeight(dp(48));
    b.setPadding(dp(12), dp(8), dp(12), dp(8));
    b.setTextColor(filled ? (dark ? 0xff10151b : Color.WHITE) : ink);
    b.setBackground(shape(filled ? accent : surface, 14, filled ? 0 : border));
    b.setOnClickListener(
        v -> {
          if (!busy) action.run();
        });
    return b;
  }

  private EditText field(String hint, String value, boolean password) {
    EditText e = new EditText(this);
    e.setTextSize(15);
    e.setTypeface(regular);
    e.setTextColor(ink);
    e.setHintTextColor(muted);
    e.setHint(hint);
    e.setText(value);
    e.setPadding(dp(14), dp(13), dp(14), dp(13));
    e.setBackground(shape(bg, 13, border));
    e.setSingleLine(true);
    e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
    e.setSaveEnabled(false);
    if (password) {
      e.setInputType(
          android.text.InputType.TYPE_CLASS_TEXT
              | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
              | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
      e.setTransformationMethod(PasswordTransformationMethod.getInstance());
      e.setTextDirection(View.TEXT_DIRECTION_LTR);
      e.setGravity(Gravity.START);
      e.setTypeface(regular);
    }
    return e;
  }

  private void screen(String title, String subtitle, boolean bottom) {
    theme();
    root = column();
    root.setBackgroundColor(bg);
    root.setPadding(dp(20), dp(12), dp(20), 0);
    setContentView(root);
    root.setOnApplyWindowInsetsListener(
        (v, in) -> {
          android.graphics.Insets bar =
              in.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime());
          root.setPadding(dp(20), bar.top + dp(12), dp(20), bar.bottom);
          return in;
        });
    root.requestApplyInsets();
    LinearLayout header = row();
    LinearLayout titleBox = column();
    add(titleBox, text(title, 26, ink, true), 0);
    add(titleBox, text(subtitle, 12, muted, false), 2);
    header.addView(titleBox, new LinearLayout.LayoutParams(0, -2, 1));
    if (bottom) header.addView(button("قفل", false, () -> lock()));
    add(root, header, 0);
    scroll = new ScrollView(this);
    scroll.setFillViewport(false);
    scroll.setClipToPadding(false);
    scroll.setPadding(0, dp(20), 0, dp(18));
    body = column();
    scroll.addView(body);
    root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
    if (bottom) {
      nav = row();
      nav.setPadding(0, dp(8), 0, dp(12));
      String[] labels = {"اکانت‌ها", "خزانه", "تنظیمات"}, keys = {"accounts", "vault", "settings"};
      for (int i = 0; i < 3; i++) {
        final String target = keys[i];
        Button b =
            button(
                labels[i],
                tab.equals(target),
                () -> {
                  tab = target;
                  query = "";
                  home();
                });
        weight(nav, b);
      }
      add(root, nav, 0);
    }
  }

  private LinearLayout card() {
    LinearLayout c = column();
    c.setPadding(dp(17), dp(17), dp(17), dp(17));
    c.setBackground(shape(surface, 21, border));
    return c;
  }

  private void toast(String s) {
    Toast.makeText(this, s, Toast.LENGTH_LONG).show();
  }

  private void error(String s) {
    new AlertDialog.Builder(this)
        .setTitle("اکانت‌یار")
        .setMessage(s)
        .setPositiveButton("باشه", null)
        .show();
  }

  private void showLocked() {
    screen("اکانت‌یار", "اشتراک‌ها و رمزها، در یک جای امن", false);
    LinearLayout hero = card();
    TextView badge = text("◈", 50, accent, true);
    badge.setGravity(Gravity.CENTER);
    add(hero, badge, 10);
    TextView heading =
        text(store.exists() ? "خزانه‌ات قفل است" : "خزانه شخصی‌ات را بساز", 23, ink, true);
    heading.setGravity(Gravity.CENTER);
    add(hero, heading, 8);
    add(
        hero,
        text(
            store.exists()
                ? "با رمز اصلی وارد شو. اطلاعات روی همین گوشی رمزگذاری شده‌اند."
                : "یک عبارت عبور قوی با حداقل ۱۲ نویسه انتخاب کن. رمز اصلی را در جای امن نگه دار؛"
                    + " بازیابی آن ممکن نیست.",
            14,
            muted,
            false),
        12);
    EditText pass = field("رمز اصلی", "", true);
    add(hero, pass, 20);
    EditText confirm = store.exists() ? null : field("تکرار رمز اصلی", "", true);
    if (confirm != null) add(hero, confirm, 10);
    TextView status = text("", 13, accent, false);
    add(hero, status, 8);
    Button submit =
        button(
            store.exists() ? "بازکردن خزانه" : "ساخت خزانه",
            true,
            () -> {
              if (SystemClock.elapsedRealtime() < retryAt) {
                status.setText("کمی صبر کن و دوباره تلاش کن.");
                return;
              }
              char[] pw = pass.getText().toString().toCharArray();
              if (confirm != null
                  && (pw.length < 12
                      || !pass.getText().toString().equals(confirm.getText().toString()))) {
                Crypto.wipe(pw);
                status.setText("رمز باید حداقل ۱۲ نویسه باشد و تکرارش یکسان باشد.");
                return;
              }
              pass.setText("");
              if (confirm != null) confirm.setText("");
              hideKeyboard();
              busy = true;
              int token = generation;
              status.setText("در حال بازکردن خزانه…");
              worker.execute(
                  () -> {
                    byte[] k = null;
                    JSONObject d = null;
                    Exception failure = null;
                    try {
                      if (store.exists()) {
                        k = store.unlock(pw);
                        d = store.load(k);
                      } else {
                        d = new JSONObject().put("version", 1).put("accounts", new JSONArray());
                        k = store.create(pw, d);
                      }
                    } catch (Exception e) {
                      failure = e;
                    } finally {
                      Crypto.wipe(pw);
                    }
                    final byte[] resultKey = k;
                    final JSONObject resultData = d;
                    final Exception ex = failure;
                    handler.post(
                        () -> {
                          busy = false;
                          if (token != generation) {
                            Crypto.wipe(resultKey);
                            return;
                          }
                          if (ex != null) {
                            Crypto.wipe(resultKey);
                            failures++;
                            retryAt =
                                SystemClock.elapsedRealtime()
                                    + Math.min(60000L, 1000L * (1L << Math.min(6, failures)));
                            status.setText("رمز درست نیست یا فایل خزانه قابل بازکردن نیست.");
                            return;
                          }
                          vaultKey = resultKey;
                          data = resultData;
                          failures = 0;
                          home();
                          rebuildReminders();
                          if (pendingRestore != null) restorePasswordDialog();
                        });
                  });
            });
    add(hero, submit, 8);
    if (store.exists() && BioKey.enabled(this)) {
      add(hero, button("ورود با اثر انگشت", false, () -> unlockBiometric()), 10);
    }
    add(body, hero, 15);
    add(body, text("آفلاین • بدون ارسال رمزها • پشتیبان رمزگذاری‌شده", 12, muted, false), 20);
    LinearLayout colors = row();
    weight(
        colors,
        button(
            dark ? "حالت روشن" : "حالت تاریک",
            false,
            () -> {
              ui.edit().putBoolean("dark", !dark).apply();
              showLocked();
            }));
    weight(colors, button("رنگ دلخواه", false, () -> chooseColor()));
    add(body, colors, 16);
  }

  private void unlockBiometric() {
    int token = generation;
    biometric =
        BioKey.run(
            this,
            null,
            new BioKey.Callback() {
              public void success(byte[] key) {
                biometric = null;
                if (token != generation) {
                  Crypto.wipe(key);
                  return;
                }
                try {
                  JSONObject d = store.load(key);
                  vaultKey = key;
                  data = d;
                  home();
                  rebuildReminders();
                  if (pendingRestore != null) restorePasswordDialog();
                } catch (Exception e) {
                  Crypto.wipe(key);
                  toast("خزانه باز نشد؛ با رمز اصلی وارد شو.");
                }
              }

              public void error(String m) {
                biometric = null;
                toast(m);
              }
            });
  }

  private void hideKeyboard() {
    View v = getCurrentFocus();
    if (v != null)
      ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE))
          .hideSoftInputFromWindow(v.getWindowToken(), 0);
  }

  private void lock() {
    generation++;
    for (AlertDialog dialog : new java.util.ArrayList<>(sensitiveDialogs)) dialog.dismiss();
    sensitiveDialogs.clear();
    if (activeDialog != null) {
      activeDialog.dismiss();
      activeDialog = null;
    }
    if (biometric != null) {
      biometric.cancel();
      biometric = null;
    }
    Crypto.wipe(vaultKey);
    vaultKey = null;
    data = null;
    busy = false;
    showLocked();
  }

  @Override
  protected void onStop() {
    super.onStop();
    lock();
  }

  @Override
  protected void onResume() {
    super.onResume();
    ReminderEngine.reschedule(this);
    if (vaultKey == null && !busy) showLocked();
    if (vaultKey != null && "settings".equals(tab)) home();
  }

  @Override
  public void onBackPressed() {
    if (activeDialog != null && activeDialog.isShowing()) {
      activeDialog.dismiss();
      activeDialog = null;
    } else if (vaultKey != null) lock();
    else super.onBackPressed();
  }

  private JSONArray accounts() {
    return data.optJSONArray("accounts");
  }

  private void rebuildReminders() {
    try {
      ReminderEngine.sync(this, accounts());
    } catch (Exception e) {
      toast("اطلاعات ذخیره شد؛ تنظیم یادآورها نیاز به تلاش دوباره دارد.");
    }
  }

  private boolean persist() {
    try {
      store.save(vaultKey, data);
      rebuildReminders();
      return true;
    } catch (Exception e) {
      error("ذخیره انجام نشد. فضای گوشی را بررسی کن و دوباره تلاش کن.");
      return false;
    }
  }

  private void home() {
    if (vaultKey == null) {
      showLocked();
      return;
    }
    screen("اکانت‌یار", Jalali.from(Jalali.today()).display(), true);
    if ("settings".equals(tab)) {
      settings();
      return;
    }
    list();
  }

  private long remaining(JSONObject o) {
    try {
      return ChronoUnit.DAYS.between(Jalali.today(), LocalDate.parse(o.getString("due")));
    } catch (Exception e) {
      return Long.MAX_VALUE;
    }
  }

  private String dueLabel(JSONObject o) {
    long d = remaining(o);
    if (d == Long.MAX_VALUE) return "بدون تاریخ انقضا";
    if (d < 0) return Jalali.fa((-d) + " روز از انقضا گذشته");
    if (d == 0) return "امروز منقضی می‌شود";
    return Jalali.fa(d + " روز باقی مانده");
  }

  private void list() {
    boolean vault = "vault".equals(tab);
    if (!vault) {
      int soon = 0, expired = 0;
      for (int i = 0; i < accounts().length(); i++) {
        long d = remaining(accounts().optJSONObject(i));
        if (d >= 0 && d <= 7) soon++;
        if (d < 0) expired++;
      }
      LinearLayout summary = card();
      add(summary, text("اشتراک‌ها زیر نظر تو", 18, ink, true), 0);
      add(
          summary,
          text(
              Jalali.fa(
                  accounts().length()
                      + " اکانت   •   "
                      + soon
                      + " نزدیک انقضا   •   "
                      + expired
                      + " منقضی"),
              14,
              accent,
              true),
          10);
      add(body, summary, 0);
      if (!ReminderEngine.isEnabled(this)) {
        add(body, text("برای دریافت یادآور، اجازه اعلان‌ها را فعال کن.", 13, accent, true), 12);
        add(
            body,
            button(
                "فعال‌کردن یادآورها",
                false,
                () -> {
                  if (Build.VERSION.SDK_INT >= 33
                      && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                          != PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 101);
                  else {
                    tab = "settings";
                    home();
                  }
                }),
            8);
      }
    }
    LinearLayout actions = row();
    weight(actions, button("＋ افزودن اکانت", true, () -> edit(null)));
    weight(
        actions,
        button(
            vault ? "ساخت رمز" : "فیلتر",
            false,
            () -> {
              if (vault) passwordGenerator();
              else chooseFilter();
            }));
    add(body, actions, 16);
    EditText search = field("جست‌وجوی نام اکانت", query, false);
    add(body, search, 14);
    LinearLayout results = column();
    add(body, results, 12);
    renderAccounts(results, vault);
    search.addTextChangedListener(
        new TextWatcher() {
          public void beforeTextChanged(CharSequence s, int st, int c, int a) {}

          public void onTextChanged(CharSequence s, int st, int before, int count) {
            query = s.toString();
            renderAccounts(results, vault);
          }

          public void afterTextChanged(Editable e) {}
        });
    if (!vault)
      add(
          body,
          text(
              "یادآورها: ۷، ۳ و ۱ روز قبل از انقضا، حدود ساعت ۹ صبح به وقت تهران",
              12,
              muted,
              false),
          18);
  }

  private void renderAccounts(LinearLayout target, boolean vault) {
    target.removeAllViews();
    ArrayList<JSONObject> items = new ArrayList<>();
    for (int i = 0; i < accounts().length(); i++) {
      JSONObject o = accounts().optJSONObject(i);
      if (!o.optString("name").toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT)))
        continue;
      long d = remaining(o);
      if (!vault && "soon".equals(filter) && !(d >= 0 && d <= 7)) continue;
      if (!vault && "expired".equals(filter) && d >= 0) continue;
      items.add(o);
    }
    items.sort((a, b) -> Long.compare(remaining(a), remaining(b)));
    if (items.isEmpty()) {
      LinearLayout empty = card();
      TextView mark = text("＋", 40, accent, true);
      mark.setGravity(Gravity.CENTER);
      add(empty, mark, 8);
      add(
          empty,
          text(
              accounts().length() == 0
                  ? "اولین اکانتت را اضافه کن"
                  : "اکانتی با این فیلتر پیدا نشد",
              18,
              ink,
              true),
          10);
      add(
          empty,
          text("نام، اطلاعات ورود و در صورت نیاز تاریخ انقضا را ثبت کن.", 13, muted, false),
          8);
      add(target, empty, 4);
      return;
    }
    for (JSONObject o : items) {
      LinearLayout c = card();
      LinearLayout title = row();
      TextView letter =
          text(o.optString("name").substring(0, 1).toUpperCase(Locale.ROOT), 20, accent, true);
      letter.setGravity(Gravity.CENTER);
      letter.setBackground(shape(tint(accent, .12f), 12, 0));
      title.addView(letter, new LinearLayout.LayoutParams(dp(43), dp(43)));
      TextView name = text(o.optString("name"), 18, ink, true);
      name.setPadding(dp(10), 0, dp(10), 0);
      title.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
      add(c, title, 0);
      add(
          c,
          text(
              o.optString("category", "سایر") + "   •   " + dueLabel(o),
              12,
              remaining(o) <= 7 ? accent : muted,
              false),
          12);
      if (!vault && !o.optString("due").isEmpty())
        add(
            c,
            text(
                "انقضا: " + Jalali.from(LocalDate.parse(o.optString("due"))).display(),
                13,
                muted,
                false),
            7);
      if (!o.optString("cost").isEmpty())
        add(
            c,
            text(
                "تمدید: " + Jalali.fa(o.optString("cost")) + " " + o.optString("currency", "تومان"),
                12,
                muted,
                false),
            5);
      LinearLayout r = row();
      weight(r, button(vault ? "اطلاعات ورود" : "مشاهده", false, () -> details(o)));
      if (!vault && !o.optString("due").isEmpty())
        weight(r, button("تمدید کردم", true, () -> renew(o)));
      else weight(r, button("ویرایش", false, () -> edit(o)));
      add(c, r, 15);
      add(target, c, 12);
    }
  }

  private void chooseFilter() {
    String[] labels = {"همه اکانت‌ها", "تا ۷ روز آینده", "منقضی‌شده‌ها"},
        keys = {"all", "soon", "expired"};
    int checked = Arrays.asList(keys).indexOf(filter);
    new AlertDialog.Builder(this)
        .setTitle("نمایش اکانت‌ها")
        .setSingleChoiceItems(
            labels,
            checked,
            (d, n) -> {
              filter = keys[n];
              d.dismiss();
              home();
            })
        .setNegativeButton("بستن", null)
        .show();
  }

  private void chooseColor() {
    int current = ui.getInt("color", 0);
    new AlertDialog.Builder(this)
        .setTitle("رنگ دلخواه")
        .setSingleChoiceItems(
            COLOR_NAMES,
            current,
            (d, n) -> {
              ui.edit().putInt("color", n).apply();
              d.dismiss();
              if (vaultKey == null) showLocked();
              else home();
            })
        .setNegativeButton("بستن", null)
        .show();
  }

  private AlertDialog dialog(LinearLayout content, boolean tall) {
    if (activeDialog != null) activeDialog.dismiss();
    ScrollView sc = new ScrollView(this);
    sc.setFillViewport(false);
    sc.addView(content);
    content.setPadding(dp(20), dp(20), dp(20), dp(20));
    content.setBackgroundColor(surface);
    AlertDialog d = new AlertDialog.Builder(this).setView(sc).create();
    activeDialog = d;
    sensitiveDialogs.add(d);
    d.getWindow();
    d.setOnDismissListener(
        x -> {
          hideKeyboard();
          sensitiveDialogs.remove(d);
          if (activeDialog == d) activeDialog = null;
        });
    d.show();
    if (d.getWindow() != null) {
      d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
      d.getWindow().setBackgroundDrawable(shape(surface, 24, 0));
      d.getWindow()
          .setLayout(
              getResources().getDisplayMetrics().widthPixels - dp(28),
              tall ? (int) (getResources().getDisplayMetrics().heightPixels * .82) : -2);
      d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
    return d;
  }

  private void label(LinearLayout box, String title) {
    add(box, text(title, 12, muted, true), 16);
  }

  private Spinner spinner(String[] options, int selection) {
    Spinner s = new Spinner(this);
    ArrayAdapter<String> a =
        new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, options) {
          @Override
          public View getView(int pos, View view, android.view.ViewGroup parent) {
            TextView t = (TextView) super.getView(pos, view, parent);
            t.setTextColor(ink);
            t.setTypeface(regular);
            t.setTextSize(14);
            t.setPadding(dp(12), dp(12), dp(12), dp(12));
            return t;
          }

          @Override
          public View getDropDownView(int pos, View view, android.view.ViewGroup parent) {
            TextView t = (TextView) super.getDropDownView(pos, view, parent);
            t.setTypeface(regular);
            return t;
          }
        };
    a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    s.setAdapter(a);
    s.setSelection(Math.max(0, selection));
    s.setBackground(shape(bg, 12, border));
    return s;
  }

  private CheckBox check(String label, boolean checked) {
    CheckBox c = new CheckBox(this);
    c.setText(label);
    c.setTypeface(regular);
    c.setTextSize(13);
    c.setTextColor(ink);
    c.setButtonTintList(android.content.res.ColorStateList.valueOf(accent));
    c.setChecked(checked);
    return c;
  }

  private interface DateCallback {
    void set(LocalDate d);
  }

  private void pickDate(LocalDate initial, DateCallback callback) {
    Jalali j = Jalali.from(initial);
    LinearLayout content = column();
    add(content, text("انتخاب تاریخ شمسی", 20, ink, true), 0);
    add(content, text("از راست: سال، ماه، روز", 12, muted, false), 7);
    LinearLayout fields = row();
    NumberPicker year = new NumberPicker(this),
        month = new NumberPicker(this),
        day = new NumberPicker(this);
    year.setMinValue(1300);
    year.setMaxValue(1500);
    year.setValue(Math.max(1300, Math.min(1500, j.year)));
    month.setMinValue(1);
    month.setMaxValue(12);
    month.setDisplayedValues(Jalali.MONTHS);
    month.setValue(j.month);
    day.setMinValue(1);
    day.setMaxValue(Jalali.length(year.getValue(), month.getValue()));
    day.setValue(j.day);
    year.setFormatter(n -> Jalali.fa("" + n));
    day.setFormatter(n -> Jalali.fa("" + n));
    for (NumberPicker p : new NumberPicker[] {year, month, day}) {
      p.setTextColor(ink);
      p.setWrapSelectorWheel(false);
      p.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
      fields.addView(p, new LinearLayout.LayoutParams(0, dp(170), 1));
    }
    NumberPicker.OnValueChangeListener update =
        (p, o, n) -> {
          int keep = day.getValue();
          day.setMaxValue(Jalali.length(year.getValue(), month.getValue()));
          day.setValue(Math.min(keep, day.getMaxValue()));
        };
    year.setOnValueChangedListener(update);
    month.setOnValueChangedListener(update);
    add(content, fields, 10);
    AlertDialog dateDialog = new AlertDialog.Builder(this).setView(content).create();
    content.setPadding(dp(18), dp(18), dp(18), dp(18));
    content.setBackgroundColor(surface);
    add(
        content,
        button(
            "انتخاب تاریخ",
            true,
            () -> {
              callback.set(
                  new Jalali(year.getValue(), month.getValue(), day.getValue()).gregorian());
              dateDialog.dismiss();
            }),
        8);
    sensitiveDialogs.add(dateDialog);
    dateDialog.setOnDismissListener(x -> sensitiveDialogs.remove(dateDialog));
    dateDialog.show();
    dateDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    dateDialog.getWindow().setBackgroundDrawable(shape(surface, 20, 0));
  }

  private String newPassword() {
    final String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%&*-_+=";
    java.security.SecureRandom random = new java.security.SecureRandom();
    StringBuilder s = new StringBuilder();
    for (int i = 0; i < 24; i++) s.append(chars.charAt(random.nextInt(chars.length())));
    return s.toString();
  }

  private void edit(JSONObject original) {
    LinearLayout c = column();
    add(c, text(original == null ? "افزودن اکانت" : "ویرایش اکانت", 22, ink, true), 0);
    add(c, text("اطلاعات ورود اختیاری است و در خزانه رمزگذاری می‌شود.", 12, muted, false), 6);
    EditText name =
        field("مثلاً Claude لپ‌تاپ", original == null ? "" : original.optString("name"), false);
    name.setFilters(new InputFilter[] {new InputFilter.LengthFilter(120)});
    label(c, "نام اکانت *");
    add(c, name, 6);
    label(c, "دسته‌بندی");
    Spinner category =
        spinner(
            CATEGORY,
            original == null ? 0 : Arrays.asList(CATEGORY).indexOf(original.optString("category")));
    add(c, category, 6);
    EditText username =
        field("یوزرنیم یا ایمیل", original == null ? "" : original.optString("username"), false);
    username.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT
            | android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
    username.setTextDirection(View.TEXT_DIRECTION_LTR);
    label(c, "اطلاعات ورود");
    add(c, username, 6);
    EditText password =
        field("پسورد اکانت", original == null ? "" : original.optString("password"), true);
    add(c, password, 8);
    LinearLayout tools = row();
    CheckBox reveal = check("نمایش پسورد", false);
    reveal.setOnCheckedChangeListener(
        (b, on) ->
            password.setTransformationMethod(
                on ? null : PasswordTransformationMethod.getInstance()));
    weight(tools, reveal);
    weight(tools, button("ساخت رمز", false, () -> password.setText(newPassword())));
    add(c, tools, 7);
    CheckBox expires =
        check(
            "این اکانت تاریخ انقضا دارد", original == null || !original.optString("due").isEmpty());
    add(c, expires, 15);
    LocalDate[] selected = {
      original == null || original.optString("due").isEmpty()
          ? Jalali.today().plusMonths(1)
          : LocalDate.parse(original.optString("due"))
    };
    LinearLayout expiryBox = column();
    Button date = button(Jalali.from(selected[0]).display(), false, () -> {});
    date.setOnClickListener(
        v ->
            pickDate(
                selected[0],
                d -> {
                  selected[0] = d;
                  date.setText(Jalali.from(d).display());
                }));
    label(expiryBox, "تاریخ انقضا");
    add(expiryBox, date, 6);
    Spinner repeat =
        spinner(
            new String[] {"بدون تکرار", "ماهانه شمسی", "سالانه شمسی"},
            original == null
                ? 1
                : Arrays.asList("none", "monthly", "yearly")
                    .indexOf(original.optString("repeat", "none")));
    label(expiryBox, "دوره تمدید");
    add(expiryBox, repeat, 6);
    CheckBox reminder =
        check("یادآور ۷، ۳ و ۱ روز قبل", original == null || original.optBoolean("remind", true));
    add(expiryBox, reminder, 10);
    add(c, expiryBox, 0);
    expiryBox.setVisibility(expires.isChecked() ? View.VISIBLE : View.GONE);
    expires.setOnCheckedChangeListener(
        (b, on) -> expiryBox.setVisibility(on ? View.VISIBLE : View.GONE));
    EditText cost = field("اختیاری", original == null ? "" : original.optString("cost"), false);
    cost.setInputType(
        android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
    label(c, "هزینه تمدید");
    add(c, cost, 6);
    Spinner currency =
        spinner(
            new String[] {"تومان", "دلار", "یورو"},
            original == null
                ? 0
                : Arrays.asList("تومان", "دلار", "یورو").indexOf(original.optString("currency")));
    add(c, currency, 6);
    EditText url =
        field("https://example.com", original == null ? "" : original.optString("url"), false);
    url.setInputType(
        android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
    url.setTextDirection(View.TEXT_DIRECTION_LTR);
    label(c, "لینک سایت");
    add(c, url, 6);
    EditText notes =
        field("یادداشت شخصی", original == null ? "" : original.optString("notes"), false);
    notes.setSingleLine(false);
    notes.setMinLines(3);
    notes.setMaxLines(6);
    notes.setFilters(new InputFilter[] {new InputFilter.LengthFilter(10000)});
    label(c, "یادداشت");
    add(c, notes, 6);
    TextView status = text("", 12, accent, false);
    add(c, status, 10);
    add(
        c,
        button(
            "ذخیره اکانت",
            true,
            () -> {
              String nm = name.getText().toString().trim(),
                  link = url.getText().toString().trim(),
                  amount = Jalali.ascii(cost.getText().toString().trim());
              if (nm.isEmpty()) {
                status.setText("نام اکانت را وارد کن.");
                return;
              }
              if (!link.isEmpty() && !validUrl(link)) {
                status.setText("لینک باید با https:// یا http:// شروع شود و معتبر باشد.");
                return;
              }
              try {
                if (!amount.isEmpty() && new BigDecimal(amount).signum() < 0) throw new Exception();
              } catch (Exception e) {
                status.setText("هزینه معتبر وارد کن.");
                return;
              }
              try {
                JSONObject o =
                    original == null ? new JSONObject() : new JSONObject(original.toString());
                o.put(
                        "id",
                        original == null ? UUID.randomUUID().toString() : original.getString("id"))
                    .put("name", nm)
                    .put("category", CATEGORY[category.getSelectedItemPosition()])
                    .put("username", username.getText().toString())
                    .put("password", password.getText().toString())
                    .put("due", expires.isChecked() ? selected[0].toString() : "")
                    .put(
                        "repeat",
                        new String[] {"none", "monthly", "yearly"}
                            [repeat.getSelectedItemPosition()])
                    .put(
                        "anchor",
                        original != null && original.optString("due").equals(selected[0].toString())
                            ? original.optInt("anchor", Jalali.from(selected[0]).day)
                            : Jalali.from(selected[0]).day)
                    .put("remind", reminder.isChecked())
                    .put("cost", amount)
                    .put(
                        "currency",
                        new String[] {"تومان", "دلار", "یورو"}[currency.getSelectedItemPosition()])
                    .put("url", link)
                    .put("notes", notes.getText().toString())
                    .put("updated", Instant.now().toString());
                JSONArray updated = new JSONArray();
                for (int i = 0; i < accounts().length(); i++) {
                  JSONObject a = accounts().getJSONObject(i);
                  if (original == null || !a.getString("id").equals(o.getString("id")))
                    updated.put(a);
                }
                updated.put(o);
                JSONArray old = accounts();
                data.put("accounts", updated);
                if (persist()) {
                  password.setText("");
                  username.setText("");
                  notes.setText("");
                  activeDialog.dismiss();
                  home();
                  toast("اکانت ذخیره شد.");
                } else data.put("accounts", old);
              } catch (Exception e) {
                status.setText("ذخیره انجام نشد؛ دوباره تلاش کن.");
              }
            }),
        18);
    add(
        c,
        button(
            "انصراف",
            false,
            () -> {
              password.setText("");
              activeDialog.dismiss();
            }),
        8);
    dialog(c, true);
  }

  private boolean validUrl(String link) {
    try {
      Uri u = Uri.parse(link);
      return ("https".equals(u.getScheme()) || "http".equals(u.getScheme()))
          && u.getHost() != null
          && !u.getHost().isEmpty()
          && u.getUserInfo() == null;
    } catch (Exception e) {
      return false;
    }
  }

  private void details(JSONObject o) {
    LinearLayout c = column();
    add(c, text(o.optString("name"), 24, ink, true), 0);
    add(c, text(dueLabel(o), 13, accent, true), 6);
    if (!o.optString("due").isEmpty())
      add(
          c,
          text(
              "انقضا: " + Jalali.from(LocalDate.parse(o.optString("due"))).display(),
              13,
              muted,
              false),
          8);
    if (!o.optString("username").isEmpty()) {
      label(c, "یوزرنیم / ایمیل");
      TextView u = text(o.optString("username"), 15, ink, false);
      u.setTextDirection(View.TEXT_DIRECTION_LTR);
      u.setTextIsSelectable(false);
      add(c, u, 6);
      add(c, button("کپی یوزرنیم", false, () -> copy(o.optString("username"))), 8);
    }
    if (!o.optString("password").isEmpty()) {
      label(c, "پسورد");
      TextView p = text("••••••••••••", 19, ink, false);
      p.setTextDirection(View.TEXT_DIRECTION_LTR);
      add(c, p, 7);
      CheckBox show = check("نمایش پسورد", false);
      show.setOnCheckedChangeListener(
          (b, on) -> p.setText(on ? o.optString("password") : "••••••••••••"));
      add(c, show, 6);
      add(c, button("کپی پسورد", true, () -> copy(o.optString("password"))), 7);
    }
    if (!o.optString("url").isEmpty())
      add(
          c,
          button(
              "بازکردن سایت",
              false,
              () -> {
                if (validUrl(o.optString("url")))
                  try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(o.optString("url"))));
                  } catch (Exception e) {
                    toast("مرورگری برای بازکردن لینک پیدا نشد.");
                  }
              }),
          14);
    if (!o.optString("notes").isEmpty()) {
      label(c, "یادداشت");
      add(c, text(o.optString("notes"), 14, muted, false), 6);
    }
    JSONArray history = o.optJSONArray("history");
    if (history != null && history.length() > 0) {
      label(c, "آخرین تمدیدها");
      for (int i = Math.max(0, history.length() - 5); i < history.length(); i++) {
        JSONObject h = history.optJSONObject(i);
        try {
          add(
              c,
              text(
                  Jalali.from(LocalDate.parse(h.optString("on"))).display()
                      + " ← "
                      + Jalali.from(LocalDate.parse(h.optString("due"))).display(),
                  12,
                  muted,
                  false),
              5);
        } catch (Exception ignored) {
        }
      }
    }
    LinearLayout actions = row();
    weight(actions, button("ویرایش", false, () -> edit(o)));
    weight(actions, button("حذف اکانت", false, () -> delete(o)));
    add(c, actions, 20);
    add(c, button("بستن", false, () -> activeDialog.dismiss()), 8);
    dialog(c, false);
  }

  private void delete(JSONObject o) {
    AlertDialog deleteDialog =
        new AlertDialog.Builder(this)
            .setTitle("حذف اکانت؟")
            .setMessage("اطلاعات ورود و یادآورهای «" + o.optString("name") + "» حذف می‌شوند.")
            .setNegativeButton("انصراف", null)
            .setPositiveButton(
                "حذف",
                (d, w) -> {
                  if (vaultKey == null) return;
                  try {
                    JSONArray next = new JSONArray(), old = accounts();
                    for (int i = 0; i < old.length(); i++)
                      if (!old.getJSONObject(i).getString("id").equals(o.getString("id")))
                        next.put(old.getJSONObject(i));
                    data.put("accounts", next);
                    if (persist()) {
                      if (activeDialog != null) activeDialog.dismiss();
                      home();
                      toast("اکانت حذف شد.");
                    } else data.put("accounts", old);
                  } catch (Exception e) {
                    error("حذف انجام نشد.");
                  }
                })
            .create();
    sensitiveDialogs.add(deleteDialog);
    deleteDialog.setOnDismissListener(d -> sensitiveDialogs.remove(deleteDialog));
    deleteDialog.show();
    deleteDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
  }

  private void renew(JSONObject o) {
    LocalDate base = LocalDate.parse(o.optString("due")),
        candidate =
            Jalali.next(
                base, o.optString("repeat", "none"), o.optInt("anchor", Jalali.from(base).day));
    while (!"none".equals(o.optString("repeat")) && !candidate.isAfter(Jalali.today()))
      candidate =
          Jalali.next(candidate, o.optString("repeat"), o.optInt("anchor", Jalali.from(base).day));
    final LocalDate[] newDue = {candidate};
    LinearLayout c = column();
    add(c, text("ثبت تمدید", 22, ink, true), 0);
    add(c, text(o.optString("name"), 15, muted, true), 7);
    label(c, "تاریخ انقضای جدید را تأیید کن");
    Button date = button(Jalali.from(newDue[0]).display(), false, () -> {});
    final boolean[] adjusted = {false};
    date.setOnClickListener(
        v ->
            pickDate(
                newDue[0],
                d -> {
                  newDue[0] = d;
                  adjusted[0] = true;
                  date.setText(Jalali.from(d).display());
                }));
    add(c, date, 6);
    add(c, text("یادآورها براساس تاریخ جدید دوباره تنظیم می‌شوند.", 12, muted, false), 12);
    add(
        c,
        button(
            "تمدید کردم؛ ذخیره کن",
            true,
            () -> {
              try {
                JSONObject before = new JSONObject(o.toString());
                o.put("due", newDue[0].toString());
                if (adjusted[0]) o.put("anchor", Jalali.from(newDue[0]).day);
                JSONArray h = o.optJSONArray("history");
                if (h == null) h = new JSONArray();
                h.put(
                    new JSONObject()
                        .put("on", Jalali.today().toString())
                        .put("due", newDue[0].toString())
                        .put("cost", o.optString("cost"))
                        .put("currency", o.optString("currency")));
                if (h.length() > 100) {
                  JSONArray limited = new JSONArray();
                  for (int i = h.length() - 100; i < h.length(); i++) limited.put(h.get(i));
                  h = limited;
                }
                o.put("history", h);
                if (persist()) {
                  activeDialog.dismiss();
                  home();
                  toast("تمدید ثبت شد.");
                } else {
                  for (Iterator<String> keys = before.keys(); keys.hasNext(); ) {
                    String k = keys.next();
                    o.put(k, before.get(k));
                  }
                  if (!before.has("history")) o.remove("history");
                }
              } catch (Exception e) {
                error("تمدید ثبت نشد.");
              }
            }),
        18);
    add(c, button("انصراف", false, () -> activeDialog.dismiss()), 8);
    dialog(c, false);
  }

  private void copy(String s) {
    ClipData clip = ClipData.newPlainText("اطلاعات حساس", s);
    PersistableBundle flags = new PersistableBundle();
    flags.putBoolean("android.content.extra.IS_SENSITIVE", true);
    clip.getDescription().setExtras(flags);
    ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(clip);
    ourClip = clip;
    handler.removeCallbacks(clearClipboard);
    handler.postDelayed(clearClipboard, 30000);
    toast("کپی شد؛ پاک‌کردن کلیپ‌بورد تا ۳۰ ثانیه دیگر انجام می‌شود.");
  }

  private void clearClip() {
    if (ourClip == null) return;
    try {
      ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
      ClipData current = cm.getPrimaryClip();
      if (current != null
          && current.getItemCount() > 0
          && ourClip.getItemCount() > 0
          && Objects.equals(current.getItemAt(0).getText(), ourClip.getItemAt(0).getText()))
        cm.clearPrimaryClip();
    } catch (Exception ignored) {
    }
    ourClip = null;
    handler.removeCallbacks(clearClipboard);
  }

  private void passwordGenerator() {
    LinearLayout c = column();
    add(c, text("ساخت پسورد تصادفی", 21, ink, true), 0);
    TextView p = text(newPassword(), 20, ink, true);
    p.setTextDirection(View.TEXT_DIRECTION_LTR);
    p.setTextIsSelectable(false);
    add(c, p, 18);
    add(c, text("۲۴ نویسه از حروف، عدد و نماد", 12, muted, false), 10);
    add(c, button("ساخت رمز دیگر", false, () -> p.setText(newPassword())), 12);
    add(c, button("کپی رمز", true, () -> copy(p.getText().toString())), 8);
    add(c, button("بستن", false, () -> activeDialog.dismiss()), 8);
    dialog(c, false);
  }

  private void settings() {
    LinearLayout appearance = card();
    add(appearance, text("ظاهر اپ", 18, ink, true), 0);
    add(
        appearance,
        button(
            dark ? "تغییر به حالت روشن" : "تغییر به حالت تاریک",
            false,
            () -> {
              ui.edit().putBoolean("dark", !dark).apply();
              home();
            }),
        14);
    for (int start = 0; start < 6; start += 3) {
      LinearLayout r = row();
      for (int i = start; i < start + 3; i++) {
        final int n = i;
        Button b =
            button(
                COLOR_NAMES[i],
                ui.getInt("color", 0) == i,
                () -> {
                  ui.edit().putInt("color", n).apply();
                  home();
                });
        int col = (dark ? DARK_COLORS : LIGHT_COLORS)[i];
        if (ui.getInt("color", 0) != i) {
          b.setTextColor(col);
          b.setBackground(shape(tint(col, .09f), 14, col));
        }
        weight(r, b);
      }
      add(appearance, r, 10);
    }
    add(body, appearance, 0);
    LinearLayout notifications = card();
    add(notifications, text("یادآورها", 18, ink, true), 0);
    add(notifications, text(ReminderEngine.status(this), 13, muted, false), 12);
    add(
        notifications,
        text(
            "۷، ۳ و ۱ روز قبل • حدود ساعت ۹ صبح تهران\n"
                + "پس از ثبت تمدید، یادآورها با تاریخ جدید تنظیم می‌شوند.",
            13,
            muted,
            false),
        10);
    add(
        notifications,
        button(
            "اجازه اعلان‌ها",
            false,
            () -> {
              if (Build.VERSION.SDK_INT >= 33
                  && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                      != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 101);
              else {
                try {
                  startActivity(
                      new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                          .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
                } catch (Exception e) {
                  toast("تنظیمات اعلان را از تنظیمات گوشی باز کن.");
                }
              }
            }),
        14);
    AlarmManager alarms = (AlarmManager) getSystemService(ALARM_SERVICE);
    add(
        notifications,
        text(
            (Build.VERSION.SDK_INT < 31 || alarms.canScheduleExactAlarms())
                ? "زمان‌بندی دقیق فعال است"
                : "زمان‌بندی عادی فعال است؛ اندروید ممکن است اعلان را با تأخیر نشان دهد.",
            12,
            muted,
            false),
        10);
    if (Build.VERSION.SDK_INT >= 31 && !alarms.canScheduleExactAlarms())
      add(
          notifications,
          button(
              "فعال‌کردن زمان‌بندی دقیق",
              false,
              () -> {
                try {
                  startActivity(
                      new Intent(
                          Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                          Uri.parse("package:" + getPackageName())));
                } catch (Exception e) {
                  toast("این تنظیم در گوشی در دسترس نیست.");
                }
              }),
          10);
    add(
        notifications,
        button(
            "تست اعلان",
            true,
            () -> {
              if (Build.VERSION.SDK_INT >= 33
                  && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                      != PackageManager.PERMISSION_GRANTED) {
                toast("ابتدا اجازه اعلان‌ها را فعال کن.");
                return;
              }
              ReminderEngine.test(this);
            }),
        10);
    add(body, notifications, 14);
    LinearLayout security = card();
    add(security, text("امنیت خزانه", 18, ink, true), 0);
    add(
        security,
        text("قفل خودکار هنگام خروج • رمزها مخفی • محافظت از اسکرین‌شات", 13, muted, false),
        10);
    add(
        security,
        button(
            BioKey.enabled(this) ? "غیرفعال‌کردن اثر انگشت" : "فعال‌کردن اثر انگشت",
            false,
            () -> {
              if (BioKey.enabled(this)) {
                BioKey.disable(this);
                home();
                toast("ورود با اثر انگشت غیرفعال شد.");
              } else if (!BioKey.available(this))
                toast("اثر انگشت قوی در گوشی تنظیم نشده یا در دسترس نیست.");
              else {
                final byte[] copy = vaultKey.clone();
                biometric =
                    BioKey.run(
                        this,
                        copy,
                        new BioKey.Callback() {
                          public void success(byte[] k) {
                            Crypto.wipe(copy);
                            biometric = null;
                            if (vaultKey != null) home();
                            toast("اثر انگشت فعال شد.");
                          }

                          public void error(String m) {
                            Crypto.wipe(copy);
                            biometric = null;
                            toast(m);
                          }
                        });
              }
            }),
        14);
    add(security, button("تغییر رمز اصلی", false, () -> changeMaster()), 10);
    add(body, security, 14);
    LinearLayout backup = card();
    add(backup, text("پشتیبان و بازیابی", 18, ink, true), 0);
    add(
        backup,
        text(
            "از اطلاعات یک فایل رمزگذاری‌شده با رمز مستقل بگیر و در جای امن نگه دار. برای انتقال به"
                + " گوشی جدید هم از همین فایل استفاده کن.",
            13,
            muted,
            false),
        10);
    add(backup, button("ساخت پشتیبان رمزگذاری‌شده", true, () -> backupDialog()), 14);
    add(
        backup,
        button(
            "بازیابی از فایل پشتیبان",
            false,
            () -> {
              Intent pick =
                  new Intent(Intent.ACTION_OPEN_DOCUMENT)
                      .setType("*/*")
                      .addCategory(Intent.CATEGORY_OPENABLE);
              try {
                startActivityForResult(pick, 202);
              } catch (Exception e) {
                toast("فایل‌منیجر در دسترس نیست.");
              }
            }),
        10);
    add(body, backup, 14);
    LinearLayout about = card();
    add(about, text("اکانت‌یار ۱.۰", 17, ink, true), 0);
    add(
        about,
        text(
            "ساخته‌شده برای حمید سعیدی\n"
                + "کاملاً آفلاین؛ اپ مجوز اینترنت ندارد.\n"
                + "رمز اصلی و رمز پشتیبان قابل بازیابی نیستند.\n"
                + "یادآورها به اجازه اعلان‌ها و تنظیمات باتری گوشی وابسته‌اند.",
            12,
            muted,
            false),
        10);
    add(body, about, 14);
  }

  private boolean strong(EditText first, EditText second, TextView status) {
    if (first.getText().length() < 12) {
      status.setText("حداقل ۱۲ نویسه وارد کن؛ یک عبارت عبور طولانی پیشنهاد می‌شود.");
      return false;
    }
    if (!first.getText().toString().equals(second.getText().toString())) {
      status.setText("تکرار رمز یکسان نیست.");
      return false;
    }
    return true;
  }

  private void backupDialog() {
    LinearLayout c = column();
    add(c, text("رمز پشتیبان", 22, ink, true), 0);
    add(
        c,
        text("این رمز مستقل از رمز اصلی است. برای بازیابی فایل به آن نیاز داری.", 13, muted, false),
        10);
    EditText first = field("رمز فایل پشتیبان", "", true),
        second = field("تکرار رمز پشتیبان", "", true);
    add(c, first, 16);
    add(c, second, 8);
    TextView status = text("", 12, accent, false);
    add(c, status, 8);
    add(
        c,
        button(
            "ساخت فایل",
            true,
            () -> {
              if (!strong(first, second, status)) return;
              char[] pw = first.getText().toString().toCharArray();
              byte[] key = vaultKey.clone();
              first.setText("");
              second.setText("");
              busy = true;
              status.setText("در حال رمزگذاری پشتیبان…");
              int token = generation;
              worker.execute(
                  () -> {
                    byte[] file = null;
                    Exception problem = null;
                    try {
                      file = store.backup(key, pw);
                    } catch (Exception e) {
                      problem = e;
                    } finally {
                      Crypto.wipe(key);
                      Crypto.wipe(pw);
                    }
                    final byte[] out = file;
                    final Exception failure = problem;
                    handler.post(
                        () -> {
                          busy = false;
                          if (token != generation) {
                            Crypto.wipe(out);
                            return;
                          }
                          if (failure != null) {
                            status.setText("پشتیبان ساخته نشد.");
                            return;
                          }
                          pendingBackup = out;
                          if (activeDialog != null) activeDialog.dismiss();
                          Intent create =
                              new Intent(Intent.ACTION_CREATE_DOCUMENT)
                                  .setType("application/octet-stream")
                                  .addCategory(Intent.CATEGORY_OPENABLE)
                                  .putExtra(
                                      Intent.EXTRA_TITLE,
                                      "AccountYar-"
                                          + Jalali.from(Jalali.today()).numeric().replace('/', '-')
                                          + ".aybackup");
                          try {
                            startActivityForResult(create, 201);
                          } catch (Exception e) {
                            Crypto.wipe(pendingBackup);
                            pendingBackup = null;
                            error("امکان انتخاب محل ذخیره وجود ندارد.");
                          }
                        });
                  });
            }),
        14);
    add(c, button("انصراف", false, () -> activeDialog.dismiss()), 8);
    dialog(c, false);
  }

  @Override
  protected void onActivityResult(int request, int result, Intent intent) {
    super.onActivityResult(request, result, intent);
    if (request == 201) {
      byte[] bytes = pendingBackup;
      pendingBackup = null;
      if (result != RESULT_OK || intent == null || intent.getData() == null) {
        Crypto.wipe(bytes);
        toast("ذخیره پشتیبان لغو شد.");
        return;
      }
      Uri uri = intent.getData();
      worker.execute(
          () -> {
            boolean ok = false;
            try (OutputStream out = getContentResolver().openOutputStream(uri, "wt")) {
              if (out == null || bytes == null) throw new IOException();
              out.write(bytes);
              out.flush();
              ok = true;
            } catch (Exception e) {
            } finally {
              Crypto.wipe(bytes);
            }
            boolean success = ok;
            handler.post(
                () ->
                    toast(
                        success
                            ? "فایل پشتیبان رمزگذاری‌شده ذخیره شد."
                            : "فایل پشتیبان ذخیره نشد؛ دوباره تلاش کن."));
          });
    } else if (request == 202
        && result == RESULT_OK
        && intent != null
        && intent.getData() != null) {
      Uri uri = intent.getData();
      worker.execute(
          () -> {
            byte[] bytes = null;
            try (InputStream in = getContentResolver().openInputStream(uri)) {
              if (in == null) throw new IOException();
              bytes = VaultStore.readLimited(in, 16 * 1024 * 1024);
            } catch (Exception e) {
            }
            byte[] file = bytes;
            handler.post(
                () -> {
                  Crypto.wipe(pendingRestore);
                  pendingRestore = file;
                  if (file == null) {
                    toast("فایل پشتیبان قابل خواندن نیست.");
                    return;
                  }
                  if (vaultKey == null) toast("برای ادامه بازیابی، ابتدا خزانه را باز کن.");
                  else restorePasswordDialog();
                });
          });
    }
  }

  private void restorePasswordDialog() {
    if (pendingRestore == null || vaultKey == null) return;
    LinearLayout c = column();
    add(c, text("بازکردن پشتیبان", 22, ink, true), 0);
    add(
        c,
        text(
            "رمز همان فایل پشتیبان را وارد کن. اطلاعات فعلی تا تأیید نهایی تغییر نمی‌کند.",
            13,
            muted,
            false),
        10);
    EditText pass = field("رمز فایل پشتیبان", "", true);
    add(c, pass, 16);
    TextView status = text("", 12, accent, false);
    add(c, status, 8);
    add(
        c,
        button(
            "بررسی فایل",
            true,
            () -> {
              char[] pw = pass.getText().toString().toCharArray();
              pass.setText("");
              byte[] encrypted = pendingRestore.clone();
              busy = true;
              int token = generation;
              status.setText("در حال بررسی رمز و سلامت فایل…");
              worker.execute(
                  () -> {
                    JSONObject restored = null;
                    try {
                      restored = VaultStore.restore(encrypted, pw);
                    } catch (Exception e) {
                    } finally {
                      Crypto.wipe(pw);
                      Crypto.wipe(encrypted);
                    }
                    JSONObject out = restored;
                    handler.post(
                        () -> {
                          busy = false;
                          if (token != generation) return;
                          if (out == null) {
                            status.setText("رمز درست نیست یا فایل معتبر نیست.");
                            return;
                          }
                          if (activeDialog != null) activeDialog.dismiss();
                          confirmRestore(out);
                        });
                  });
            }),
        14);
    add(
        c,
        button(
            "انصراف",
            false,
            () -> {
              Crypto.wipe(pendingRestore);
              pendingRestore = null;
              activeDialog.dismiss();
            }),
        8);
    dialog(c, false);
  }

  private void confirmRestore(JSONObject restored) {
    LinearLayout c = column();
    add(c, text("تأیید بازیابی", 22, ink, true), 0);
    add(
        c,
        text(
            Jalali.fa(
                restored.optJSONArray("accounts").length()
                    + " اکانت در فایل پیدا شد.\n"
                    + "با تأیید، اکانت‌های فعلی با محتویات فایل جایگزین می‌شوند. قبل از ادامه"
                    + " می‌توانی از اطلاعات فعلی پشتیبان بگیری."),
            13,
            muted,
            false),
        12);
    add(
        c,
        button(
            "جایگزینی و بازیابی",
            true,
            () -> {
              JSONObject old = data;
              data = restored;
              if (persist()) {
                Crypto.wipe(pendingRestore);
                pendingRestore = null;
                activeDialog.dismiss();
                home();
                toast("بازیابی انجام شد و یادآورها تنظیم شدند.");
              } else data = old;
            }),
        18);
    add(
        c,
        button(
            "انصراف",
            false,
            () -> {
              Crypto.wipe(pendingRestore);
              pendingRestore = null;
              activeDialog.dismiss();
            }),
        8);
    dialog(c, false);
  }

  private void changeMaster() {
    LinearLayout c = column();
    add(c, text("تغییر رمز اصلی", 22, ink, true), 0);
    EditText current = field("رمز اصلی فعلی", "", true),
        first = field("رمز اصلی جدید", "", true),
        second = field("تکرار رمز جدید", "", true);
    add(c, current, 16);
    add(c, first, 8);
    add(c, second, 8);
    TextView status = text("", 12, accent, false);
    add(c, status, 8);
    add(
        c,
        button(
            "ذخیره رمز جدید",
            true,
            () -> {
              if (!strong(first, second, status)) return;
              char[] old = current.getText().toString().toCharArray(),
                  next = first.getText().toString().toCharArray();
              byte[] key = vaultKey.clone();
              current.setText("");
              first.setText("");
              second.setText("");
              busy = true;
              int token = generation;
              status.setText("در حال تغییر رمز…");
              worker.execute(
                  () -> {
                    boolean ok = false;
                    byte[] check = null;
                    byte[] rotated = null;
                    try {
                      check = store.unlock(old);
                      if (!java.security.MessageDigest.isEqual(key, check)) throw new Exception();
                      rotated = store.changeMaster(key, next);
                      ok = true;
                    } catch (Exception e) {
                    } finally {
                      Crypto.wipe(old);
                      Crypto.wipe(next);
                      Crypto.wipe(key);
                      Crypto.wipe(check);
                    }
                    boolean success = ok;
                    final byte[] newVaultKey = rotated;
                    handler.post(
                        () -> {
                          busy = false;
                          if (success) BioKey.disable(this);
                          if (token != generation) {
                            Crypto.wipe(newVaultKey);
                            return;
                          }
                          if (!success) {
                            status.setText("رمز فعلی درست نیست یا تغییر انجام نشد.");
                            return;
                          }
                          Crypto.wipe(vaultKey);
                          vaultKey = newVaultKey;
                          activeDialog.dismiss();
                          toast("رمز اصلی تغییر کرد. در صورت نیاز اثر انگشت را دوباره فعال کن.");
                          home();
                        });
                  });
            }),
        16);
    add(c, button("انصراف", false, () -> activeDialog.dismiss()), 8);
    dialog(c, false);
  }

  @Override
  public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
    super.onRequestPermissionsResult(code, permissions, results);
    if (code == 101) {
      if (vaultKey != null) home();
      toast(
          results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED
              ? "اعلان‌ها فعال شدند."
              : "اجازه اعلان داده نشد؛ از تنظیمات گوشی می‌توانی فعالش کنی.");
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    generation++;
    if (biometric != null) biometric.cancel();
    Crypto.wipe(vaultKey);
    Crypto.wipe(pendingBackup);
    Crypto.wipe(pendingRestore);
    worker.shutdown();
  }
}
