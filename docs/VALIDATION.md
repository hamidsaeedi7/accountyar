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

The new public-repository workflow and Gradle build should be verified on the actual GitHub runner after publication. A configured workflow does not establish a successful CI run. Tests are evidence of the behaviors exercised, not a blanket security or compatibility guarantee.

## فارسی

آزمون‌های تاریخ، رمزگذاری، ذخیره‌سازی متمرکز اندروید و مسیرهای رابط ذکرشده انجام شده‌اند. ممیزی مستقل، اثر انگشت روی گوشی واقعی، عملکرد طولانی‌مدت اعلان در پس‌زمینه، رفتار باتری سامسونگ و انتخاب فایل پشتیبان روی گوشی واقعی هنوز تأیید نشده‌اند. نتیجه گردش‌کار جدید گیت‌هاب باید پس از انتشار بررسی شود.
