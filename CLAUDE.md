# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Aurora Updater is a simplified fork of Aurora Store focused exclusively on app updates. It provides anonymous login with device spoofing for Google Play authentication, password-protected blacklist management, and smart installer selection with root install as default when available.

**Key distinction from upstream**: This fork disables app browsing/discovery entirely - it only handles updating existing apps. External Google Play links are disabled to prevent app discovery.

## Build Configuration

### Requirements
- Java 21 (JVM toolchain required)
- Android SDK with API 36
- Gradle 9.1.0+

### Build Commands
```bash
# Build debug APK (vanilla flavor)
./gradlew assembleVanillaDebug

# Build release APK
./gradlew assembleVanillaRelease

# Build all flavors
./gradlew assembleDebug
./gradlew assembleRelease

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Lint
./gradlew ktlintCheck
./gradlew ktlintFormat
```

### Product Flavors
- **vanilla** (default): Standard build for normal Android devices
- **huawei**: Build for Huawei devices (includes HMS Core Service)
- **preload**: Build for preloaded/system apps (no nightly variant)

### Build Types
- **debug**: Development build with AOSP testkey signing
- **release**: Production build with ProGuard/R8 minification
- **nightly**: Release variant with commit hash in version name

Note: Release builds require `signing.properties` file for custom signing, otherwise uses default AOSP testkey.

## Architecture

### Dependency Injection (Hilt)
All dependency injection uses Dagger Hilt. Key modules:
- `OkHttpClientModule` - HTTP client configuration
- `IHttpClientModule` - GPlayAPI HTTP client
- `RoomModule` - Database configuration
- `HelperModule` - Helper classes (UpdateHelper, DownloadHelper)
- `CommonModule` - Common providers (AuthProvider, SpoofProvider, etc.)
- `ExodusModule` - Privacy/tracker analysis

### Database (Room)
Three main entities in `AuroraDatabase`:
- **Download**: Tracks download queue and status (app\src\main\java\com\aurora\store\data\room\download)
- **Update**: Stores available updates (app\src\main\java\com\aurora\store\data\room\update)
- **Favourite**: User favorites (app\src\main\java\com\aurora\store\data\room\favourite)

Database version: 5, schema exported to `app/schemas/`

### App Installers
Six installer implementations (app\src\main\java\com\aurora\store\data\installer):
1. **SessionInstaller**: Android PackageInstaller API (default fallback)
2. **NativeInstaller**: Manual APK intent
3. **RootInstaller**: Root access via LibSU
4. **ServiceInstaller**: Aurora Services privileged extension
5. **AMInstaller**: AppManager integration
6. **ShizukuInstaller**: Shizuku framework integration

Selection logic in `AppInstaller.kt`:
- Smart detection based on available permissions/services
- Root installer selected by default if root access available
- Falls back to SessionInstaller if preferred installer unavailable
- Silent install capability checked per installer in `canInstallSilently()`

### Background Workers (WorkManager)
Two primary workers:
- **DownloadWorker** (app\src\main\java\com\aurora\store\data\work\DownloadWorker.kt): Handles APK downloads with exponential backoff retry
- **UpdateWorker** (app\src\main\java\com\aurora\store\data\work\UpdateWorker.kt): Periodic update checks with configurable interval

Managed by:
- `DownloadHelper` - Download queue management, automatically triggers next queued download
- `UpdateHelper` - Update scheduling with user-defined constraints (metered network, battery, idle)

### Authentication Flow
Handled by `AuthProvider` (app\src\main\java\com\aurora\store\data\providers\AuthProvider.kt):
- **Anonymous login**: Uses token dispensers to obtain temporary Google accounts
- **Google login**: Direct authentication with user's Google account
- **Device spoofing**: `SpoofProvider` provides fake device properties for Play Store API
- AuthData cached in SharedPreferences as JSON

Fallback: Spoof Manager accessible from login screen when dispensers fail.

### Navigation Structure
Uses Jetpack Navigation with both traditional fragments and Compose screens:
- Main activity: `MainActivity` with bottom navigation (Updates tab only as top-level)
- Compose screens: Hosted in `ComposeActivity`
- Fragment-based: Legacy screens in `app\src\main\java\com\aurora\store\view\ui`
- Compose UI: Modern screens in `app\src\main\java\com\aurora\store\compose\ui`

Top-level destination: `R.id.updatesFragment` (default tab)

### Event System
Global event bus via `AuroraApp.events` (EventFlow):
- `InstallerEvent`: Package install/uninstall notifications
- `BusEvent`: App-wide events (blacklist changes, etc.)

Flows are collected in `AuroraApp.scope` (MainScope).

## Key Components

### Password-Protected Blacklist
Blacklist Manager requires alphanumeric password verification. Security enhancement in recent commits requires entering previous password before making changes (see commit: "Enhance blacklist password security...").

### Update Detection
`UpdateHelper.checkUpdatesNow()` triggers expedited update check. Updates filtered by certificate validity unless extended updates enabled (`PREFERENCE_UPDATES_EXTENDED`).

### Download Pipeline
1. Enqueue: `DownloadHelper.enqueueApp()` or `enqueueUpdate()`
2. Queue: Download added to Room database with QUEUED status
3. Trigger: `DownloadHelper` observes queue, triggers `DownloadWorker`
4. Download: Worker fetches APK files, updates status to DOWNLOADING
5. Install: On completion, hands off to selected installer
6. Cleanup: Download entry cleared after installation

### Network Monitoring
`NetworkProvider` tracks connectivity. `NetworkDialogSheet` shown when network unavailable (after intro completion).

## Code Style

### Kotlin
- Target: JVM 21
- Compiler options: `@OptIn` for experimental Compose Material3/Adaptive APIs
- Parcelize plugin enabled for data classes
- Kotlinx Serialization for JSON

### Compose
Material3 with adaptive layout support. Theme system in `app\src\main\java\com\aurora\store\compose\theme`.

## Testing

### Unit Tests
Location: `app\src\test\java\com\aurora\store`
Framework: JUnit + Google Truth

### Instrumentation Tests
Location: `app\src\androidTest\java\com\aurora\store`
Runner: `HiltInstrumentationTestRunner` (custom Hilt test runner)
Key tests:
- `SpoofProviderTest`: Device spoofing functionality
- `ExodusModuleTest`: Privacy tracker database

## Debugging

- LeakCanary enabled in debug builds for memory leak detection
- BuildConfig.DEBUG flag controls logging verbosity
- WorkManager minimum logging level: DEBUG in debug builds, INFO in release

## Important Paths

- Downloads: Managed by `PathUtil.getDownloadDirectory()` and `PathUtil.getAppDownloadDir()`
- Database: Room database at default location, migrations handled by `MigrationHelper`
- Shared Preferences: All preferences use `Preferences` utility class with typed constants
