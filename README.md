# Phonix

A simple, clean Android dialer. Based on [Koler](https://github.com/Chooloo/koler) by Chooloo (copyleft; see the original project for its license).

- Package: `app.mrm.phonix`
- Android 8.0+ (API 26), targets Android 13 (API 33)
- Single module: `apk/`

## Build a debug APK

Requires JDK 17 and the Android SDK (platform 33).

```sh
./gradlew :apk:assembleDebug
# -> apk/build/outputs/apk/debug/apk-debug.apk
```

CI: `.github/workflows/debug-apk.yml` builds the debug APK on every push / PR and uploads it as a
workflow artifact (`phonix-debug-apk`). It can also be started manually from the Actions tab.

## Contact editor

Adding and editing contacts happens inside the app (`lib/ui/contacteditor/`), not in the system contacts app.
It supports photo (with a pinch-to-zoom square/circle cropper), name parts (prefix / first / middle / last / suffix), nickname, phonetic pronunciation, favorite star, delete, phones, emails, postal
addresses, "file as", organization (company / job title / department), birthday & other dates, websites, messaging
accounts, related people, SIP addresses, notes and free-form custom fields. Every typed field accepts a
custom label. Custom fields use a mimetype private to this app, so other contacts apps don't show them.
