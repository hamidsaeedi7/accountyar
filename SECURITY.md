# Security policy | سیاست امنیت

## Project status and support

AccountYar is an early-stage offline Android project. Only the current `main` development line is maintained on a best-effort basis. The project has **not undergone an independent security audit** and offers no guaranteed incident-response timeline, recovery service, or bug bounty.

Treat credentials stored in this app as sensitive. Evaluate backups and recovery with test data before depending on them. Losing the necessary passphrase or deleting the only vault copy can make data unrecoverable.

## Threat model

The implementation aims to protect credential confidentiality when app files or encrypted backups are obtained without the corresponding passphrase, and to reject modified authenticated-encryption envelopes. It also locks the UI when the app leaves the foreground, protects sensitive windows, and keeps credentials out of reminder metadata and notifications.

It does not claim protection from a compromised or rooted operating system, a malicious keyboard, privileged memory inspection, abusive accessibility services, or someone using an already unlocked vault. Android/Java strings and third-party clipboard histories cannot be completely erased by this app. Screenshot flags are a platform control, not protection against cameras or a compromised system.

## Important design boundaries

- Vault, backup, biometric wrapping, and reminder encryption have distinct authenticated-data contexts.
- Master passphrases are not stored as plaintext; changing the passphrase also rotates the vault key.
- The reminder index uses a separate non-authentication-bound Keystore key so scheduling can work while the vault UI is locked. It contains names and dates, never credentials or notes.
- A backup passphrase is independent of the master passphrase. Old exported backups remain protected by their own original passphrases; changing the live vault does not rewrite existing backup files.
- Sensitive app files are excluded from automatic cloud backup and device transfer. Manual encrypted exports may be stored wherever the user chooses.
- Biometric enrollment changes can invalidate the biometric key; master-passphrase access remains the fallback.

## Report a vulnerability

Use GitHub's **Report a vulnerability** option in this repository's Security → Advisories area **if private vulnerability reporting is enabled**. If it is unavailable, open an issue that only requests a private disclosure channel; do not publish exploit details or confidential data there. No private-reporting setting or response deadline is implied by this file.

Provide affected versions, Android/device details, impact, reproduction steps using synthetic credentials, and suggested mitigations. Never upload your real vault, backup, passwords, private signing key, or another person's information. Coordinate public disclosure after a safe private channel and an assessment are established.

## فارسی

این پروژه در مرحله اولیه است و ممیزی امنیتی مستقل نشده است. پشتیبانی از خط توسعه فعلی به‌صورت best-effort انجام می‌شود و زمان پاسخ یا برنامه جایزه تضمین نمی‌شود.

هدف، محافظت از فایل رمزگذاری‌شده بدون رمز لازم و تشخیص دست‌کاری آن است. سیستم عامل آلوده، صفحه‌کلید نامطمئن، سوءاستفاده از دسترس‌پذیری، بررسی حافظه با دسترسی بالا و خزانه‌ای که از قبل باز است، خارج از تضمین‌های پروژه هستند. پاک‌کردن همه رشته‌های حافظه و تاریخچه کلیپ‌بورد سایر برنامه‌ها تضمین نمی‌شود.

فهرست یادآورها با کلید جداگانه رمزگذاری می‌شود تا هنگام قفل خزانه کار کند؛ اطلاعات ورود در آن نیست. پشتیبان رمز مستقل دارد؛ تغییر رمز اصلی، پشتیبان‌های قبلی را تغییر نمی‌دهد. گم‌شدن رمزها یا حذف تنها نسخه داده‌ها می‌تواند باعث از دست‌رفتن اطلاعات شود.

اگر گزینه گزارش خصوصی آسیب‌پذیری در Security → Advisories فعال بود، از آن استفاده کنید. اگر نبود، فقط درخواست کانال خصوصی بدهید و جزئیات فنی حساس را در Issue عمومی ننویسید. نسخه، مدل گوشی، اثر مشکل و مراحل بازتولید با داده ساختگی را آماده کنید. رمز، خزانه، پشتیبان یا کلید واقعی نفرستید.
