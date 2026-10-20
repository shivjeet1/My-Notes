# My Notes

**My Notes** is a lightweight, offline-first Android note-taking app written in Java with the native Android SDK. Notes are stored locally with Room, so the app works without an account, a network connection, or a cloud service.

The project is intentionally small: it is a practical reference for Android fundamentals (activities, XML layouts, RecyclerView, Room, and Material components) and a usable personal notes app.

---

## Features

- **Create notes** with an optional title and a body; blank bodies are blocked and titles fall back to “Untitled”.
- **Edit existing notes** by tapping a card, with unsaved-changes protection on back.
- **Search** titles and bodies with a short debounce, a clear affordance, result counts, and a dedicated no-results state.
- **Pin, unpin, and delete** from each card’s overflow menu or long press. Pinned notes sort first, deletes can be undone from the Snackbar, and pin changes are announced.
- **Explicit states** for loading, empty, search-empty, error, and content, with a scrollable empty state that survives large font scales.
- **Local persistence** with Room — no account and no network required.
- **Material 3 light/dark theme** built from semantic colour, type, spacing, and component tokens. The interface uses warm neutrals and one indigo accent, not random card colours.
- **Responsive layout** with a 2/3/4-column staggered grid, tablet-aware editor margins, and edge-to-edge system bar/IME handling on API 30+.

> The UI/UX implementation is complete through the build-and-test phase. On-device visual and TalkBack validation is still pending because no emulator or physical device was attached during implementation. See [`docs/UI_UX_PLAN.md`](docs/UI_UX_PLAN.md) for the design rationale and validation protocol.

---

## Screenshots

<p>
  <img src="static/mynotes/ui-demo1.jpg" width="45%" alt="Pre-refactor notes list with a search field and a note card" />
  <img src="static/mynotes/ui-demo2.jpg" width="45%" alt="Pre-refactor new note editor with title and body fields" />
  <img src="static/mynotes/ui-demo3.jpg" width="45%" alt="Pre-refactor notes list with multiple cards" />
  <img src="static/mynotes/ui-demo4.jpg" width="45%" alt="Pre-refactor search results filtered by a query" />
</p>

> These are **archived pre-refactor screenshots**. The current build uses the quiet-paper design system, adaptive cards, explicit states, and the editor toolbar described below. New after screenshots will be captured during on-device validation, when a device or emulator is available.

---

## Project structure

```plaintext
My-Notes/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/shivjeet1/mynotes/
│       │   ├── MainActivity.java              ← List, search, states, pin/delete/undo
│       │   ├── NotesTakeActivity.java         ← Create/edit, dirty check, save validation
│       │   ├── Adapter/NoteListAdapter.java   ← ListAdapter/DiffUtil and card actions
│       │   ├── Database/MainDAO.java          ← Room queries and pinned ordering
│       │   ├── Database/RoomDb.java           ← Room database instance
│       │   ├── Interface/NotesClickListener.java
│       │   ├── Model/Notes.java               ← Note entity
│       │   └── util/WindowInsetsHelper.java   ← System bar, cutout, and IME insets
│       └── res/
│           ├── color/                         ← Stateful stroke and disabled-text selectors
│           ├── layout/                        ← List, editor, note card
│           ├── menu/                          ← Overflow and editor actions
│           ├── values/                        ← Tokens, themes, dimensions, strings
│           ├── values-night/                  ← Dark semantic colours and status bar theme
│           ├── values-sw600dp/                ← Tablet spacing and 3-column grid
│           ├── values-sw840dp/                ← Wide-tablet spacing and 4-column grid
│           └── values-land/                   ← Landscape grid
├── docs/UI_UX_PLAN.md                         ← Design plan, decisions, validation protocol
├── static/mynotes/                            ← Archived pre-refactor screenshots
├── gradle/libs.versions.toml                  ← Dependency versions
├── Makefile                                   ← Command-line build helpers
├── build.gradle.kts
├── settings.gradle.kts
├── gradlew
└── README.md
```

---

## Requirements

- Android 7.0 (API 24) or newer on the device.
- **JDK 17** (AGP 8.7.2 and Gradle 8.9 do not support newer JDKs such as JDK 27).
- Android SDK Platform **34** and Build-Tools **34.0.0**.
- `adb` for installing on a physical device.

The Gradle wrapper downloads Gradle 8.9 automatically; you do not need to install Gradle separately.

---

## Build on Arch Linux without Android Studio

The entire app can be built and installed from a terminal. The commands below use the upstream Android command-line tools; no IDE is required.

### 1. Install the system packages

```bash
sudo pacman -S --needed base-devel curl unzip jdk17-openjdk android-tools android-udev
sudo archlinux-java set java-17-openjdk
java -version
```

`java -version` should report **17.x**. The `android-tools` package provides `adb`; `android-udev` provides device permissions. If you prefer a different JDK path, pass it explicitly to Make: `make debug JAVA_HOME=/path/to/jdk17`.

### 2. Set up the Android SDK

The Makefile can bootstrap the SDK in one step:

```bash
make setup
```

This downloads the Android command-line tools into `~/Android/Sdk`, accepts the SDK licences, installs `platform-tools`, `platforms;android-34`, and `build-tools;34.0.0`, and writes `local.properties`.

Use a different SDK location if you prefer:

```bash
make setup ANDROID_HOME="$HOME/.local/share/android-sdk"
```

Or point the build at an SDK that is already installed:

```bash
make local-properties ANDROID_HOME=/opt/android-sdk FORCE=1
```

#### Manual SDK setup (without Make)

If you want to see or control every step:

```bash
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
mkdir -p "$ANDROID_HOME/cmdline-tools"

# Check https://developer.android.com/studio#command-line-tools-only for the
# current commandlinetools-linux-*_latest.zip URL if this one has moved.
curl -fL -o /tmp/cmdline-tools.zip \
  "https://dl.google.com/android/repository/commandlinetools-linux-15859902_latest.zip"

unzip -q /tmp/cmdline-tools.zip -d "$ANDROID_HOME/cmdline-tools"
mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" \
   "$ANDROID_HOME/cmdline-tools/latest"

export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

sdkmanager --licenses
sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"

printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
```

Add the environment variables to your shell startup file so later terminals can find the SDK:

```bash
# ~/.zshrc or ~/.bashrc
export ANDROID_HOME="$HOME/Android/Sdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

> If the command-line tools URL returns 404, copy the current link from the Android Studio download page and pass it to Make: `make sdk-bootstrap CMDLINE_TOOLS_URL="https://..."`.

### 3. Verify the toolchain

```bash
make doctor
```

This checks the JDK version, `sdkmanager`, Platform 34, Build-Tools 34.0.0, `adb`, and `local.properties`. Fix anything marked `[fail]` before continuing.

### 4. Build

```bash
make debug
```

Equivalent Gradle command:

```bash
./gradlew assembleDebug
```

The APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### 5. Install and run on a device

Enable **Developer options → USB debugging** on the phone, connect it, and authorise the computer when prompted.

```bash
make devices     # confirm the device is visible
make install     # build if needed and install the debug APK
make run         # install and launch the app
```

Or manually:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.shivjeet1.mynotes/.MainActivity
```

If `adb devices` shows `no permissions`, reinstall/refresh `android-udev`, reconnect the cable, and make sure your user is in the `adbusers` group:

```bash
sudo usermod -aG adbusers "$USER"
# log out and back in, then reconnect the device
```

### Make targets

Run `make help` for the full list.

| Command | What it does |
| :--- | :--- |
| `make setup` | One-time SDK bootstrap, licence acceptance, and `local.properties` |
| `make doctor` | Check JDK, SDK packages, `adb`, and project configuration |
| `make debug` | Build the debug APK |
| `make install` | Install the debug APK on the connected device |
| `make run` | Install and launch the app |
| `make test` | Run JVM unit tests |
| `make lint` | Run Android lint |
| `make release` | Build the unsigned release APK |
| `make bundle` | Build the unsigned release App Bundle |
| `make clean` | Delete Gradle build outputs |

Override toolchain paths on the command line:

```bash
make debug JAVA_HOME=/usr/lib/jvm/java-17-openjdk
make install ANDROID_HOME=/path/to/android-sdk
make local-properties FORCE=1
```

---

## Troubleshooting

### `Unsupported class file major version` or “Android Gradle plugin requires Java 17”

The default JDK on Arch may be newer than the one AGP 8.7 supports. Install and select JDK 17:

```bash
sudo pacman -S jdk17-openjdk
sudo archlinux-java set java-17-openjdk
java -version
```

Alternatively, keep your default JDK and select 17 only for the build:

```bash
make debug JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

### `SDK location not found`

Set `ANDROID_HOME`, or write `local.properties` at the repository root:

```bash
printf 'sdk.dir=%s\n' "$HOME/Android/Sdk" > local.properties
```

`local.properties` is machine-specific and is already ignored by Git.

### `Failed to install the following Android SDK packages ... licenses`

Accept the licences explicitly:

```bash
"$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" --licenses
```

### `sdkmanager: command not found`

Check the path and add it to your shell:

```bash
ls "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"
```

### `Warning: SDK processing. This version only understands SDK XML versions up to 3`

This is a version-skew warning between a very new command-line-tools release and AGP 8.7.2. It is non-fatal in the tested build. If it becomes an error, either update `agp`/Gradle together in [`gradle/libs.versions.toml`](gradle/libs.versions.toml), or install a command-line-tools version contemporaneous with AGP 8.7.

### `adb: no permissions` / device not listed

```bash
sudo pacman -S android-udev
sudo usermod -aG adbusers "$USER"
adb kill-server
adb devices
```

Log out and back in after changing groups, then reconnect the USB cable. On some devices you also need to accept the “Allow USB debugging?” prompt again.

### First launch shows an empty screen

The empty state is intentional when Room has no notes. Tap **Create first note** or the **+** button to open the editor. If the spinner never resolves, check for a Room/database error:

```bash
adb logcat -s MainActivity
```

The debug build logs database failures under the `MainActivity` tag.

### Where are build outputs?

```text
app/build/outputs/apk/debug/app-debug.apk       # debug APK
app/build/outputs/apk/release/                  # unsigned release APK
app/build/outputs/bundle/release/               # unsigned release AAB
app/build/reports/lint-results-debug.html       # lint report
```

---

## UI/UX implementation status

| Area | Status |
| :--- | :--- |
| Semantic colour/type/spacing tokens and Material 3 light/dark themes | Implemented |
| Adaptive note grid, search, loading/empty/search-empty/error states | Implemented |
| Pin-first sorting, overflow menu, long press, delete undo, TalkBack announcements | Implemented |
| Editor toolbar, save validation, dirty-state confirmation, edited-date metadata | Implemented |
| Edge-to-edge system bars on API 30+, fitted windows below API 30, IME padding | Implemented |
| `ListAdapter`/`DiffUtil`, stable IDs, saved scroll/search state | Implemented |
| JVM tests and lint | Passing (`make test`, `make lint`) |
| Device/emulator screenshots, TalkBack pass, 200% font scale, light/dark visual QA | Pending — no device attached |

Room keeps the existing `notes` schema and database version 2. The DAO now persists edited dates and sorts pinned notes first; destructive fallback migration was removed so a future schema change must ship a real migration rather than silently wiping notes.

See [`docs/UI_UX_PLAN.md`](docs/UI_UX_PLAN.md) for the audits, wireframes, component specs, file map, and the remaining validation checklist.

---

## Download

[Download the latest APK](https://github.com/shivjeet1/My-Notes/releases)

---

## Conclusion

**My Notes** is a compact local-first Android project with a clear core: create and edit notes, search quickly, pin what matters, delete with undo, and keep everything on the device. The Java/XML codebase, Room storage, and command-line build workflow remain intact while the interface now has a coherent design system, explicit states, and accessibility-focused interactions.

The remaining work is validation, not architecture: capture after screenshots, run TalkBack and large-font checks on hardware, and make any final spacing or contrast adjustments those checks reveal.
