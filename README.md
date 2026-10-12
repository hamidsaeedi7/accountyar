# AccountYar | اکانت‌یار

**Offline Android subscription reminders and an encrypted account vault, with a Persian-first interface.**

[فارسی](README-fa.md) · [User guide / راهنمای استفاده](docs/USER_GUIDE.fa.md) · [Contributing](CONTRIBUTING.md) · [Security](SECURITY.md) · [MIT license](LICENSE)

AccountYar helps you keep subscription renewal dates and account credentials in one local app. Add your own accounts, see which subscriptions are approaching expiry, and receive reminders **7, 3, and 1 days before** the saved expiration date. The initial account list is empty.

Created and maintained by **Hamid Saeedi | AI & Automation**.

> **Early-stage project:** the implementation has passed focused emulator and core tests, but has not received an independent security audit. Evaluate it with test data before relying on it for important credentials. A future desktop app is planned; desktop support and synchronization are not available in this version.

## Features

| Area | Current behavior |
| --- | --- |
| Account management | Add, edit, delete, search by name, and filter expiring or expired accounts |
| Subscription reminders | Local notifications 7, 3, and 1 days before expiry, around 09:00 Asia/Tehran |
| Solar Hijri dates | Persian date picker, remaining-day display, monthly and yearly renewal cycles |
| Renewal workflow | Confirm or edit the new expiry date with “Renewed”; retain up to 100 renewal records |
| Credentials | Optional username, password, website, category, cost, currency, and notes |
| Vault access | Master passphrase and optional Android strong biometric authentication |
| Privacy controls | Lock on leaving the app, hidden passwords, protected sensitive windows, sensitive clipboard flag |
| Password generator | 24-character random passwords with letters, digits, and symbols |
| Backup | Password-protected encrypted export and explicit replacement on import |
| Appearance | Green, blue, purple, orange, red, and yellow palettes, with light and dark modes |
| Localization | Persian, RTL layout, and bundled Vazirmatn fonts |
| Connectivity | No Internet permission, app server, analytics, or ads |

### Renewal dates

Monthly and yearly cycles use the **Solar Hijri calendar**. A day that does not exist in a shorter month is clamped to the final valid day; the original renewal day is retained for later months. The supported date-picker range is **1300–1500 SH**.

A subscription does not automatically become renewed when it expires. Use **“تمدید کردم” / “Renewed”** and confirm the actual new date. This reschedules its reminders. Reminders depend on notification permission, the notification channel, Android scheduling permissions, and device battery settings; delivery is not guaranteed while the phone is off or the app is force-stopped.

## Install and first use

1. Use **Android 11 / API 30 or newer**.
2. Build a debug APK with the instructions below. Maintainer-signed downloads, when published, should come from this repository’s Releases page; source checkout does not include a private signing key.
3. Create a master passphrase of at least 12 characters. Prefer a long, unique passphrase.
4. Add your own accounts. Credentials and an expiry date are optional.
5. Open Settings, grant notification permission, and use **Test notification**. Enable **Alarms & reminders** access if you need more precise timing.
6. Export an encrypted backup and keep its passphrase separately. Losing the vault and all backups, or forgetting the necessary passphrases, can make the data unrecoverable.

On Samsung devices, check battery restrictions and Deep sleeping apps if reminders are delayed. Strong biometric availability and notification behavior must be verified on your actual phone.

## Build from source

### Android Studio / Gradle

Requirements: **JDK 17**, Android SDK **platform 35**, and SDK **build-tools 35.0.0**. The project uses Android Gradle Plugin 8.7.3 and Gradle 8.9. The initial dependency and SDK downloads require Internet access; the installed app works offline.

```bash
git clone https://github.com/hamidsaeedi7/accountyar.git
cd accountyar
chmod +x gradlew
./gradlew assembleDebug
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

On Windows, use `gradlew.bat assembleDebug`. In Android Studio, open the repository folder and let the IDE locate your Android SDK. Do not commit `local.properties`.

### Local build without Gradle

Linux/macOS: JDK 17, SDK platform 35, build-tools 35.0.0, Python 3, and `zip` are required.

```bash
export ANDROID_SDK_ROOT=/absolute/path/to/android-sdk
./build-local.sh
```

Without signing configuration, this produces `build/aligned.apk`, which is **unsigned**. Configure the existing script with `ACCOUNTYAR_KEYSTORE` and `ACCOUNTYAR_SIGNING_PASS` in your local environment to produce `build/AccountYar.apk`. The signing alias is `accountyar`. Keep signing material out of Git and command history.

### Signing and updates

Use your own signing key for your distribution. An APK with a different key cannot update an existing installation in place. Changing from a maintainer build to your own build may require exporting an encrypted backup before uninstalling and importing it into a fresh vault afterward. **The repository does not include the original private key or its password.**

## Tests and CI

```bash
./scripts/test-core.sh
node scripts/check-jalali.mjs
```

The core suite checks calendar conversion and renewal edge cases, random IVs, encryption round trips, wrong-key rejection, tamper detection, and AAD separation. The Node script independently compares dates with the ICU/Intl Persian calendar.

A GitHub Actions workflow is provided to run these checks and build a debug APK. Its uploaded APK is a **CI debug artifact**, not a maintainer-signed production release. Workflow runs must be checked on GitHub after publication; their success is not assumed.

The `tests/` folder also contains standalone Android instrumentation test sources. They are not wired into `connectedAndroidTest` in this initial project. The prior focused device checks and remaining limitations are documented in [Validation](docs/VALIDATION.md).

## Security and privacy

- Vault contents use **AES-256-GCM**, fresh 96-bit IVs, and context-specific authenticated data.
- Master and backup passphrases use **PBKDF2-HMAC-SHA256**, random 32-byte salts, and 600,000 iterations.
- Changing the master passphrase generates a new random vault key and re-encrypts the data.
- Optional strong biometric access wraps the vault key with an authentication-bound **Android Keystore** key.
- The encrypted reminder index contains only identifiers, names, and dates, with a separate Keystore key. It excludes usernames, passwords, and notes so reminders can work while the vault is locked.
- Sensitive files live in private app storage and `noBackupFilesDir`; cloud backup and automatic device transfer are excluded.
- Only account names and expiry dates appear in reminder content; a generic public notification is supplied for the lock screen.

Android or Java cannot guarantee erasure of every in-memory string. Clipboard cleanup is best-effort and cannot guarantee deletion from third-party keyboard histories. A compromised device, an untrusted keyboard, accessibility abuse, or an unlocked vault can undermine confidentiality. See [SECURITY.md](SECURITY.md) for the threat model and reporting process. These controls are not a security certification.

## Project layout

| Path | Purpose |
| --- | --- |
| `MainActivity.java` | Native Android UI, account CRUD, themes, vault and backup flows |
| `VaultStore.java` / `Crypto.java` | Encrypted envelopes, persistence, passphrase derivation and key rotation |
| `BioKey.java` | Strong biometric authentication and Keystore key wrapping |
| `ReminderEngine.java` / `ReminderReceiver.java` | Encrypted reminder metadata, scheduling and notifications |
| `Jalali.java` | Solar Hijri conversion and anchored renewal dates |
| `scripts/` / `tests/` | Core checks, independent calendar comparison, and device test sources |
| `docs/` | User guide, backup format, architecture, validation and roadmap |

## Contribute and roadmap

Bug reports, documentation improvements, Persian localization fixes, accessibility improvements, and focused pull requests are welcome. Read [CONTRIBUTING.md](CONTRIBUTING.md) and the [Code of conduct](CODE_OF_CONDUCT.md). Never include real account credentials, vaults, backups, or signing keys in an issue or PR.

Planned work includes real-device reminder/biometric testing, automated backup/import tests, accessibility review, and a desktop companion that can read the portable encrypted backup format. These are plans, not shipped features. See [Roadmap](docs/ROADMAP.md).

## License and attribution

Application code and project documentation are licensed under **MIT**, copyright © 2026 Hamid Saeedi. Bundled Vazirmatn fonts retain **SIL Open Font License 1.1**. The Gradle wrapper is distributed under the Apache License 2.0. See [NOTICE.md](NOTICE.md), [FONT-OFL.txt](FONT-OFL.txt), and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## خلاصه فارسی

اکانت‌یار یک اپ آفلاین اندروید برای مدیریت اشتراک‌ها، تاریخ تمدید شمسی و خزانه رمزگذاری‌شده اطلاعات ورود است. اکانت‌ها را خودتان اضافه می‌کنید؛ یادآورهای ۷، ۳ و ۱ روز قبل، شش رنگ، حالت روشن و تاریک، قفل، اثر انگشت قوی و پشتیبان رمزگذاری‌شده دارد. این نسخه برای اندروید ۱۱ به بالا است؛ نسخه دسکتاپ هنوز ساخته نشده و بررسی امنیتی مستقل هم انجام نشده است. [توضیحات کامل فارسی](README-fa.md).
