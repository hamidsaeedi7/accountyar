# Architecture | معماری

## Components

| Component | Responsibility |
| --- | --- |
| MainActivity | Native UI, in-memory unlocked vault, CRUD, palette and mode selection, import/export orchestration |
| VaultStore | Atomic private-file storage and versioned encrypted envelopes |
| Crypto | JCA PBKDF2 and AES-GCM primitives, randomness, Base64 and byte-array clearing |
| BioKey | Authentication-bound Keystore wrapping of the random vault key |
| ReminderEngine | Separate encrypted scheduling index, notification channel and 7/3/1 offsets |
| ReminderReceiver | Validate scheduled conditions and reschedule after relevant system events |
| Jalali | Gregorian/Solar Hijri conversion, Tehran date boundary and anchored renewals |

## Unlock, storage and reminders

The master passphrase derives a wrapping key. That key decrypts a random vault key; the vault key decrypts the account payload. The passphrase is not persisted. The UI holds unlocked data in memory until locking. Persistence uses authenticated encryption and AtomicFile in private app storage.

Strong biometric access is optional and wraps the same vault key with an authentication-bound Android Keystore key. It does not replace the master-passphrase recovery path. Changing the master passphrase rotates the vault key and disables prior biometric wrapping.

Scheduling has a different encrypted index containing only account identifiers, names, and due dates. Its Keystore key does not require vault authentication. Account saves resynchronize the index and scheduled alarms. A sent-token ledger avoids re-notifying the same account/date/offset after rescheduling. Native alarms use exact timing when permitted and an inexact fallback otherwise. Renewal dates advance only when the user confirms renewal.

## Boundaries

There is no Internet permission or synchronization service. Opening a website delegates to an external browser after URL validation. Export/import uses Android's document picker and an independent backup passphrase. Private signing keys are a distributor concern and do not decrypt vault contents.

The UI is currently implemented as programmatic native Android views in one Activity. Larger-scale account-list virtualization, architectural separation and complete instrumentation wiring are potential future improvements; they are not claimed in the current implementation.

## فارسی

رمز اصلی، کلید بازکننده را مشتق می‌کند؛ این کلید، کلید تصادفی خزانه را باز می‌کند و کلید خزانه اطلاعات اکانت‌ها را رمزگشایی می‌کند. داده بازشده تا قفل در حافظه می‌ماند. ذخیره فایل خصوصی و اتمیک است و تغییر رمز، کلید خزانه را هم عوض می‌کند.

فهرست یادآورها از خزانه جدا است و فقط شناسه، نام و تاریخ دارد؛ کلید جداگانه آن به ورود خزانه وابسته نیست. پس از ذخیره یا تمدید، فهرست و زمان‌بندی بازسازی می‌شوند. پشتیبان با رمز مستقل و انتخاب‌گر فایل اندروید انجام می‌شود. اپ سرویس اینترنت یا همگام‌سازی ندارد.
