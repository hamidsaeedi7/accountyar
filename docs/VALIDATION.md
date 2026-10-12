# Validation scope | محدوده بررسی

## Recorded checks for the initial implementation

- Signed APK built with Android SDK 35, Java source level 11 and minimum API 30; signature and ZIP alignment verified, installation on an Android 15 emulator succeeded.
- 73,048 calendar round trips and 10,436 independent comparisons with ICU/Intl Persian calendar passed.
- Core JVM tests passed for encryption round trips, wrong-key rejection, tamper detection, AAD separation and fresh IVs.
- Focused Android tests passed for private encrypted storage, a Keystore-encrypted reminder index excluding credentials, master-key rotation, data preservation, old-key rejection, editing/deleting accounts and reminder-index cancellation.
- UI tests passed for unlocking with the rotated master passphrase, adding an account, hidden/revealed password behavior, six palettes, light/dark mode, renewal and manual UI lock.

The broad initial emulator test exceeded its time limit in an environment without hardware acceleration. Focused checks subsequently passed. The historical report is retained in [Persian](VALIDATION.fa.md).

## What is not established

There is no independent security audit or certification. Strong biometrics on physical devices, Samsung-specific battery handling, long-term background delivery and the document-picker backup/import flow on real phones require additional validation. The standalone Android instrumentation sources are retained but are not wired into the Gradle connected-test task.

## Public repository CI — 2026-10-12

[GitHub Actions run #9](https://github.com/hamidsaeedi7/accountyar/actions/runs/38236062291) passed on commit `be2c07849f9a3d4ad91e3377b48da263a0c657c9`. It ran the core JVM checks, independent Persian-calendar comparison, Android SDK setup, and `./gradlew assembleDebug`, then uploaded the debug APK artifact. The first run passed the core checks but failed while an SDK action requested the removed `tools` package; the workflow now explicitly installs `platform-tools`.

This CI run does not include emulator or physical-device instrumentation. Tests are evidence of the behaviors exercised, not a blanket security or compatibility guarantee.

## فارسی

آزمون‌های تاریخ، رمزگذاری، ذخیره‌سازی متمرکز اندروید و مسیرهای رابط ذکرشده انجام شده‌اند. اجرای شماره ۹ گیت‌هاب در ۱۲ اکتبر ۲۰۲۶، آزمون‌های پایه و تاریخ مستقل، ساخت Gradle و بارگذاری APK آزمایشی را با موفقیت انجام داد. این CI شامل آزمون دستگاه یا شبیه‌ساز نیست. ممیزی مستقل، اثر انگشت روی گوشی واقعی، عملکرد طولانی‌مدت اعلان در پس‌زمینه، رفتار باتری سامسونگ و انتخاب فایل پشتیبان روی گوشی واقعی هنوز تأیید نشده‌اند.
