# Roadmap | نقشه راه

These are proposed directions, not delivery dates or existing features. Priorities may change based on reproducible issues and available maintenance capacity.

| Priority | Proposed work | Acceptance direction |
| --- | --- | --- |
| Near-term | Real-device reminders and strong biometric checks | Record behavior on supported Android versions and Samsung battery modes |
| Near-term | Automated backup/import and lifecycle tests | Wrong password, corrupt input, cancel, replacement and process recreation |
| Near-term | Accessibility and responsive UI review | Large text, screen readers, focus order, RTL and all six light/dark palettes |
| Near-term | Wire device tests into Gradle | Reproducible instrumentation setup without personal data |
| Later | Desktop companion | Read/write the versioned encrypted backup with compatible KDF/AAD and explicit import |
| Later | Vault architecture and list performance | Separate UI/state/storage responsibilities and support larger account lists |
| Later | Independent security review | Document findings and fixes without overstating certification |

Internet synchronization is not implemented and should not be introduced without a separate threat model, user control and a compatible migration plan.

## فارسی

اولویت نزدیک، بررسی گوشی واقعی، آزمون پشتیبان و چرخه عمر، دسترس‌پذیری و اتصال آزمون دستگاه به Gradle است. نسخه دسکتاپ با پشتیبان رمزگذاری‌شده سازگار در مرحله بعد قرار دارد. زمان قطعی یا وجود این امکانات در نسخه فعلی ادعا نمی‌شود. همگام‌سازی اینترنتی به طراحی امنیتی مستقل نیاز دارد.
