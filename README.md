# OpenList Android Client

A modern Android client for [OpenList](https://github.com/OpenListTeam/OpenList) — the open-source file list server.

## Features

- 🗂 Browse files and folders with a clean white/modern Material Design UI
- 🎬 Video playback via ExoPlayer (MP4, MKV, AVI, WebM, etc.)
- 🎵 Audio playback with player controls (MP3, FLAC, AAC, OGG, etc.)
- 🖼 Image viewer
- ⬇️ File download with progress notification
- 🔍 Search files
- 📋 Breadcrumb navigation
- 📱 Tablet landscape support with persistent side navigation
- 🌙 Dark mode support
- 🔒 Login / Guest access

## Architecture

- **Language**: Kotlin
- **UI**: Material Design 3, ViewBinding
- **Network**: Retrofit 2 + OkHttp
- **Media**: AndroidX Media3 (ExoPlayer)
- **Image loading**: Glide
- **Database**: Room (server list)
- **Preferences**: DataStore
- **Architecture**: MVVM (ViewModel + LiveData)
- **ABI**: ARM64-v8a only

---

## Build with GitHub Actions

### Quick start (automated build)

1. **Fork or push** this repository to your GitHub account
2. Go to **Actions** tab
3. The workflow runs automatically on every push
4. Download the APK from **Artifacts** after the build completes

### Release build (signed APK)

To produce a signed release APK, add these **repository secrets** in  
`Settings → Secrets and variables → Actions`:

| Secret | Description |
|--------|-------------|
| `SIGNING_KEY` | Base64-encoded `.jks` keystore file |
| `KEY_ALIAS` | Key alias inside the keystore |
| `KEY_STORE_PASSWORD` | Keystore password |
| `KEY_PASSWORD` | Key password |

Generate a keystore and encode it:
```bash
keytool -genkey -v -keystore openlist.jks -alias openlist \
  -keyalg RSA -keysize 2048 -validity 10000

base64 -i openlist.jks | pbcopy   # macOS — paste as SIGNING_KEY secret
```

### Create a release

Tag a commit to trigger a GitHub Release with attached APK:
```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## Local build

Requirements: JDK 17, Android SDK

```bash
git clone https://github.com/<you>/OpenListAndroid
cd OpenListAndroid
./gradlew assembleArm64Debug      # debug APK
./gradlew assembleArm64Release    # release APK (requires signing config)
```

Output: `app/build/outputs/apk/arm64/debug/` or `.../release/`

---

## Usage

1. Install the APK on your ARM64 Android device (API 26+)
2. Open **OpenList**
3. Enter your OpenList server address, e.g. `http://192.168.1.100:5244`
4. Tap **Connect** — the app verifies the server is reachable
5. Enter username / password and tap **Login**, or tap **Browse as Guest**
6. Start browsing!

### Tablet landscape

On tablets ≥600dp width in landscape orientation, the navigation drawer is  
always visible on the left, giving you a two-panel layout.

---

## API compatibility

This client uses the OpenList / AList v3 REST API:

| Endpoint | Usage |
|---|---|
| `POST /api/auth/login` | Login |
| `POST /api/fs/list` | List directory |
| `POST /api/fs/get` | Get file detail + raw URL |
| `POST /api/fs/search` | Search |
| `GET /api/public/settings` | Server info |
| `GET /d/<path>` | Direct download |

---

## License

Apache 2.0
