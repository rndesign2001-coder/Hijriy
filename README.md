# Hijriy Taqvim — namozxonlar uchun Android ilova

Kotlin + Jetpack Compose. APK har `main` ga push qilinganda GitHub Actions'da avtomatik yig'iladi va **Releases** bo'limiga chiqariladi.

## Imkoniyatlar

- **Milodiy ⇄ Hijriy konvertor** — Ummul-Quro rasmiy jadvali (1924–2077), tashqarisida arifmetik hisob. Misol: 21-mart 2001 → 26 Zulhijja 1421. Mahalliy e'longa moslash uchun ±2 kun tuzatish.
- **Ikki kalendar** — milodiy oy (ostida hijriy kunlar) va hijriy oy (ostida milodiy kunlar), muhim islomiy kunlar belgilangan.
- **Namoz vaqtlari** — O'zbekiston musulmonlari idorasi (muslim.uz) vaqtlariga kalibrlangan: bomdod/xufton 15.5°, asr Hanafiy, shom = quyosh botishi + 4 daq. Boshqa usullar ham bor (MWL, ISNA, Misr, Karachi, Ummul-Quro, Turkiya, qo'lda burchak).
- **Qo'lda sozlash** — har bir vaqt uchun: `+N daqiqa`, `o'zingiz bilgan vaqtni kiritish` (farq saqlanadi va har kuni hisobiy vaqt bilan birga siljiydi) yoki `doimiy vaqt`.
- **Oylik jadval**, keyingi namozgacha sanoq, eslatmalar (bildirishnoma, oldindan 5–30 daqiqa).
- **Qibla kompasi** — magnit og'ishi hisobga olinadi.
- **Ob-havo** — hozirgi holat, namlik, shamol, bosim, UV, 7 kunlik prognoz, quyosh chiqishi/botishi (Open-Meteo, kalitsiz).
- **O'zbekistonning 14 hududi, 209 tuman/shahri** + GPS orqali aniqlash.
- **Qur'oni Karim** — oflayn, 2 variant: tajvid ranglari bilan va oddiy; shrift o'lchami, yaxlit/oyatma-oyat rejim, to'liq ekran, ekranni aylantirish, oxirgi o'qilgan joy.
- **Dizayn** — kunduzgi/tungi/tizim rejimi, 4 mavzu rangi (Zumrad, Firuza, Oltin, Binafsha).

## Ma'lumot manbalari

| Ma'lumot | Manba |
|---|---|
| Qur'on matni | Tanzil.net Usmoniy matni (`quran` npm paketi orqali) |
| Tajvid belgilari | [cpfair/quran-tajweed](https://github.com/cpfair/quran-tajweed) (CC BY 4.0) — 60 057 belgi, matn bilan 100% mosligi tekshirilgan |
| Shrift | Amiri Quran (SIL OFL) |
| Hijriy jadval | Ummul-Quro (hijri-converter) |
| Hududlar | `hududlar` (rasmiy ro'yxat) + tuman markazlari koordinatalari |
| Ob-havo | Open-Meteo.com |

## Testlar

`app/src/test` — Hijriy konvertor 608 ta Ummul-Quro sanasida ikki tomonlama tekshiriladi, namoz vaqtlari Toshkent rasmiy vaqtlari bilan solishtiriladi. CI har build'da testlarni ishga tushiradi.

## Doimiy imzolash kaliti (ixtiyoriy)

Kalit bo'lmasa APK har safar vaqtinchalik kalit bilan imzolanadi (yangi versiyani o'rnatishdan oldin eskisini o'chirish kerak bo'ladi). Doimiy kalit uchun repo **Settings → Secrets and variables → Actions** bo'limiga qo'shing:

- `HIJRIY_KEYSTORE_B64` — `hijriy.jks` faylining base64 ko'rinishi
- `HIJRIY_KEYSTORE_PASSWORD` — kalit paroli

## Lokal build

```bash
./gradlew testReleaseUnitTest assembleRelease
```
