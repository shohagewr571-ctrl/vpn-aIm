# VIP Cool Wire — Native Android Downloader

Production-oriented Kotlin + Jetpack Compose downloader app.

## Identity

- **App name**: VIP Cool Wire (configurable via Firebase)
- **Package**: `com.aim.vpn`
- **Min SDK**: 26 (Android 8)
- **Target SDK**: 36 (Android 16)
- **Architecture**: MVVM + Repository + Hilt + Coroutines/Flow

## What this is

A native Android downloader with:

1. Access-key login (server-validated via Cloud Function)
2. Screenshot-matched main screen (VIP Cool Wire style)
3. Particle background
4. Floating overlay icon + configurable menu
5. Admin-driven switch list from Firestore (no APK rebuild to add switches)
6. HTTPS download engine (GitHub / MediaFire host allow-list)
7. Safe ZIP extraction (path-traversal protected)
8. Verified success/failure only after real file operations

This is **not** a game cheat, aimbot, or anti-cheat bypass. Switches download and extract archives to authorized destinations only.

## Screenshots mapping

| Screen | Reference |
|--------|-----------|
| Login | Black Mirror style (password field + UNLOCK) |
| Home | VIP COOL WIRE / START / EXIT APP |
| Floating menu | Switch list + permanent Transparency seekbar |

## Project structure

```
app/src/main/java/com/aim/vpn/
  core/          security, network, storage, overlay
  data/          auth, config, download, files
  domain/        models
  presentation/  login, home, permissions, floating
  ui/theme/
```

## Firebase setup

1. Create a Firebase project (or use existing `aim-a2082`).
2. Android app package in Firebase = `com.aim.vpn` (already matches `google-services.json`).
3. `google-services.json` is already in `app/`.
4. Enable:
   - Cloud Firestore
   - Cloud Functions
   - (Optional) App Check with Play Integrity
5. Deploy the Cloud Function:

```bash
cd functions
npm install
firebase deploy --only functions:validateAccessKey
```

6. Create collections (examples below).

### accessKeys (admin write only)

```
{
  "keyHash": "<sha256 hex of plaintext key>",
  "active": true,
  "expiresAt": null,
  "revokedAt": null,
  "createdAt": <timestamp>
}
```

Generate hash: `echo -n "YOUR_KEY" | sha256sum`

### appConfig / main

```
{
  "appName": "VIP COOL WIRE",
  "creditText": "CREDIT BY @MODDERSUNNY©",
  "startLabel": "START",
  "exitLabel": "EXIT APP",
  "particleAnimationEnabled": true,
  "configVersion": 1,
  "minimumSupportedVersion": 1
}
```

### switchConfigs

```
{
  "name": "ESP Box",
  "active": true,
  "sortOrder": 0,
  "onDownloadUrl": "https://github.com/.../releases/download/.../on.zip",
  "offDownloadUrl": "https://github.com/.../releases/download/.../off.zip",
  "destinationId": "app_private",
  "expectedSha256On": null,
  "expectedSha256Off": null,
  "version": 1
}
```

### Firestore Security Rules (starter)

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /appConfig/{doc} {
      allow read: if true;
      allow write: if false; // admin SDK only
    }
    match /switchConfigs/{doc} {
      allow read: if true;
      allow write: if false;
    }
    match /destinationConfigs/{doc} {
      allow read: if true;
      allow write: if false;
    }
    match /accessKeys/{doc} {
      allow read, write: if false; // only Cloud Functions / Admin SDK
    }
    match /rateLimits/{doc} {
      allow read, write: if false;
    }
    match /operationLogs/{doc} {
      allow read, write: if false;
    }
  }
}
```

## Build (GitHub Actions - no extra steps)

Upload the contents of this folder to a GitHub repo (branch `main`). The workflow in
`.github/workflows/build.yml` builds automatically. Download the APK from
**Actions -> latest run -> Artifacts -> VipCoolWire-debug-apk**.

## Build locally

```bash
# From project root
gradle assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk

gradle assembleRelease
```

Requires Android SDK 36, JDK 17+, and a valid `google-services.json`.

## Permissions

| Permission | Why |
|------------|-----|
| INTERNET | Downloads + Firebase |
| SYSTEM_ALERT_WINDOW | Floating icon |
| FOREGROUND_SERVICE | Keep overlay alive |
| POST_NOTIFICATIONS | Foreground service notification |

No `MANAGE_EXTERNAL_STORAGE`. Destinations use app-private / SAF.

## Shizuku

Optional. Detection toast can be added; ordinary downloads never require Shizuku.

## Tests

```bash
gradle test
```

Includes ZIP path-traversal rejection and valid extraction.

## Known limitations

- Cannot build APK inside this sandbox (no full Android SDK / emulator).
- MediaFire links that require CAPTCHA or interactive browser flows will fail with a clear error; user can open manually.
- Floating menu uses classic Views for overlay reliability; main UI is Compose.

## Delivery checklist

- [x] Native Kotlin + Compose
- [x] MVVM + Hilt + Repository
- [x] Access-key login via Cloud Function
- [x] Screenshot-matched login + home
- [x] Floating icon service
- [x] Floating menu with remote switches + permanent seekbar
- [x] Download engine with host allow-list + SHA-256
- [x] Safe ZIP extraction
- [x] Firebase config model
- [x] Unit tests for ZIP safety
- [x] Cloud Function sample
- [x] Security rules sample
- [x] Build instructions
