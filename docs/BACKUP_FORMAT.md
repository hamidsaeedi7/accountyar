# Backup format v1 | قالب پشتیبان نسخه ۱

This document describes the current implementation for future interoperability. It is not a claim that a desktop importer is shipped. Always authenticate before accepting decrypted data; a damaged or wrong-passphrase backup must not partially replace the current vault.

## Outer envelope

UTF-8 JSON, extension `.aybackup`:

```json
{
  "format": "accountyar-backup",
  "version": 1,
  "iterations": 600000,
  "salt": "<standard Base64 of 32 random bytes>",
  "payload": "<standard Base64 of IV || ciphertext || GCM tag>"
}
```

The values in angle brackets are explanations, not a valid backup.

- KDF: PBKDF2-HMAC-SHA256 with a 32-byte random salt and a 256-bit output.
- Writer iteration count: 600,000. Reader accepts 600,000 through 2,000,000.
- Cipher: AES-256-GCM; the first 12 decoded payload bytes are the fresh IV. The remaining bytes are ciphertext followed by the 16-byte authentication tag.
- AAD: exact UTF-8 bytes of `accountyar/backup/v1`.
- Base64: standard RFC 4648 alphabet, padded, without line wrapping.
- Payload plaintext: UTF-8 JSON containing an `accounts` array and the application's payload version.

The account records carry UUID identifiers, names, optional credential/notes/website/cost fields, ISO Gregorian `due` strings or an empty string, recurrence (`none`, `monthly`, `yearly`), an anchored Solar Hijri day (`anchor`), a reminder toggle, and optional renewal history. The UI displays Solar Hijri dates, while stored due strings are Gregorian `YYYY-MM-DD`.

Changing the master passphrase does not change old exports. Each export remains protected by its own backup passphrase. Biometric wrapping, device Keystore keys, Android permissions and the live master passphrase are not exported.

Implementation authority: `VaultStore.backup`, `VaultStore.restore`, `VaultStore.validateData`, and `Crypto`. Preserve envelope versioning and authenticated-data contexts when proposing migrations. Do not silently rewrite or weaken the KDF on import. The live vault and reminder-index envelopes use separate formats and are not this portable backup format.

## فارسی

فایل پشتیبان، JSON با پسوند `.aybackup` و پوشش نسخه ۱ است. رمز مستقل پشتیبان با PBKDF2-HMAC-SHA256 به کلید ۲۵۶ بیتی تبدیل می‌شود و اطلاعات با AES-GCM رمزگذاری می‌شوند. ۱۲ بایت اول payload رمزگشایی‌شده از Base64، IV است؛ بقیه ciphertext و تگ ۱۶ بایتی هستند. AAD باید دقیقاً `accountyar/backup/v1` باشد.

تاریخ‌های ذخیره‌شده ISO میلادی هستند، ولی نمایش و دوره تمدید شمسی‌اند. کلید اثر انگشت، مجوزهای اندروید و رمز اصلی زنده صادر نمی‌شوند. تغییر رمز اصلی، پشتیبان قبلی را تغییر نمی‌دهد. در نسخه دسکتاپ آینده باید اصالت و صحت کل فایل پیش از جایگزینی داده بررسی شود.
