# Smallest Android App — Modern (2026)

Versi modern dari project "smallest android app" yang dikonversi ke standar 2026.

## Perubahan dari versi lama (2016)

| | Lama | Baru |
|---|---|---|
| Build system | Ant / manual | Gradle 8.6 |
| Android Gradle Plugin | — | 8.3.0 |
| compileSdk | API 1 | API 34 |
| minSdk | API 1 | API 21 |
| Java | Java 7 | Java 17 |
| Manifest `exported` | tidak ada | wajib di API 31+ |
| namespace | di manifest | di build.gradle |
| CI/CD | — | GitHub Actions |

## Cara build lokal

### Syarat
- Android Studio Hedgehog (2023.1) atau lebih baru
- JDK 17

### Langkah
```bash
# Clone repo
git clone https://github.com/username/myapp.git
cd myapp

# Build debug APK
./gradlew assembleDebug

# APK ada di:
# app/build/outputs/apk/debug/app-debug.apk
```

## Cara build via GitHub Actions

1. Push project ini ke GitHub repo kamu
2. Buka tab **Actions** di repo
3. Build akan otomatis jalan saat push ke `main`
4. Setelah selesai, download APK di bagian **Artifacts**

Atau trigger manual: Actions → "Build Android APK" → **Run workflow**

## Install APK ke HP

Setelah download `app-debug.apk` dari GitHub Actions:
1. Transfer ke HP (via kabel / Google Drive / email)
2. Buka file manager, tap file APK
3. Izinkan install dari sumber tidak dikenal jika diminta
4. Install ✓

## Struktur project

```
myapp/
├── .github/
│   └── workflows/
│       └── build.yml        ← GitHub Actions CI
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/myapp/
│       │   └── MainActivity.java
│       └── res/
│           ├── layout/activity_main.xml
│           └── values/strings.xml
├── build.gradle
├── settings.gradle
└── gradlew
```
