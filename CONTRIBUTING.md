# Contributing | مشارکت

## English

Thank you for helping improve AccountYar. English and Persian issues and pull requests are welcome.

1. Search existing issues before opening a new one. Use the bug or feature template and test data only.
2. For larger behavior changes, describe the proposal in an issue before implementing it. Focused documentation and bug fixes can go straight to a PR.
3. Fork the repository and create a short-lived branch from `main`.
4. Use JDK 17 and the Android SDK described in the README. Run `./scripts/test-core.sh`, `node scripts/check-jalali.mjs`, and `./gradlew assembleDebug` when relevant to your change.
5. Keep PRs focused. Explain the concrete problem, the resulting behavior, and what you checked. Include screenshots only when the UI changed and all content is synthetic.
6. Preserve Persian RTL layout, date semantics, accessibility, and every palette/mode when changing the UI.
7. Changes to cryptography, key storage, authentication, backups, or lock behavior need focused tests and a clear migration/compatibility explanation. Do not invent a new cryptographic primitive.

### Repository hygiene

Never commit real credentials, vault files, `.aybackup` exports, signing keys, signing passwords, local SDK paths, or logs containing secrets. Use obviously fake examples. The existing test passphrases are synthetic fixtures, not production credentials. Do not add the private signing bundle distributed separately from this project.

Use readable Java and descriptive names; avoid unrelated formatting churn. No CLA is required. By submitting a contribution, you agree that it may be distributed under the project's MIT license. Existing third-party licenses must remain intact.

Security vulnerabilities follow [SECURITY.md](SECURITY.md), not a public bug report. See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for community expectations.

## فارسی

Issue و Pull Request فارسی و انگلیسی پذیرفته می‌شوند. ابتدا موارد موجود را بررسی کنید، از قالب گزارش باگ یا پیشنهاد استفاده کنید و فقط اطلاعات آزمایشی بنویسید.

برای تغییرات بزرگ، ابتدا پیشنهاد را در Issue توضیح دهید. مخزن را Fork کنید، از `main` یک شاخه بسازید و تغییر را متمرکز نگه دارید. نتیجه مورد انتظار، تغییر انجام‌شده و بررسی‌های اجراشده را در PR بنویسید. بررسی‌های مربوط به تغییر را با اسکریپت‌های README و ساخت اپ اجرا کنید.

راست‌چین بودن، فارسی، تقویم شمسی، دسترس‌پذیری و همه رنگ‌ها و حالت‌ها را حفظ کنید. تغییر رمزگذاری، قفل، پشتیبان یا کلیدها به آزمون و توضیح سازگاری یا مهاجرت نیاز دارد.

رمز واقعی، خزانه، پشتیبان، کلید یا رمز امضا، مسیر SDK شخصی و لاگ حاوی اطلاعات حساس را وارد Git نکنید. رمزهای موجود در آزمون‌ها ساختگی هستند. مجوزهای وابستگی‌ها را حفظ کنید؛ مشارکت‌ها تحت MIT توزیع می‌شوند. آسیب‌پذیری را مطابق سیاست امنیت گزارش دهید.
