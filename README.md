# Pulnoma

O'zbek tilidagi shaxsiy budjet ilovasi (Android). Hamyonlar, kirim-chiqim, byudjet, qarz daftari, doimiy to'lovlar, hisobot, zaxira nusxa.

- Interfeys: `app/src/main/assets/index.html`
- Android qismi: `app/src/main/java/uz/mojal/pulnoma/`
- APK har push'da GitHub Actions orqali yig'iladi: **Actions → oxirgi ishga tushirish → Artifacts → Pulnoma-APK**

Doimiy imzo uchun repozitoriy sozlamalarida (Settings → Secrets and variables → Actions) ikkita secret qo'shing:
`PULNOMA_KEYSTORE_BASE64` (kalit fayli base64 ko'rinishida) va `PULNOMA_KEY_PASSWORD`. Ular bo'lmasa, APK vaqtinchalik kalit bilan imzolanadi.
