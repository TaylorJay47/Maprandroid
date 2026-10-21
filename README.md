# Maprandroid

**Maprandroid** is a native Android application designed as a full-featured Super Metroid Randomizer companion app, randomizer generator, ROM patcher, and emulator launcher. Built with modern Android development best practices and Jetpack Compose, it brings the Super Metroid Randomizer ecosystem directly to your Android device.
This application has only been tested using RetroArch with the Snes9x core installed. Any other combinations or cores are not guaranteed to function properly.

---

## Key Features

- **Randomized Seed Generation**: Configure game parameters, logic, goals, items, and settings to generate customized Super Metroid randomizer seeds via integration with randomizer backend APIs (`MapRandoApiService`).
- **Presets & Profiles**: Browse pre-configured presets, customize configuration profiles, and manage tech and sprite catalogs.
- **ROM Validation & Patching**: Securely validate base ROMs (`BaseRomValidator`) and apply ips/bps/ups patches (`RomPatcher`) directly on device.
- **Emulator Integration**: Seamlessly detect installed SNES emulators (such as RetroArch, Snes9x Plus, Lemuroid, John SNES, etc.) and launch patched ROMs instantly (`EmulatorLauncher`).
- **Game Library**: Track your generated seeds, configuration profiles, and played games locally using a robust Room database.
- **Modern Adaptive UI**: Built entirely with Jetpack Compose and Material 3, featuring Navigation 3, adaptive list-detail layouts for tablets and foldables, dark/light theme support, and edge-to-edge display rendering.

---

## Building

Prerequisites:
- Android Studio Ladybug or newer
- JDK 11 or higher
- Android SDK (compileSdk 37, minSdk 24)

To build a debug APK:
```bash
./gradlew assembleDebug
```

To build a release APK:
```bash
./gradlew assembleRelease
```

---

## Testing

The project includes comprehensive unit tests and instrumentation tests:
- **Unit Tests**: Test logic in ViewModels (`ConfigViewModelTest`, `LibraryViewModelTest`, `SettingsViewModelTest`), repositories (`MapRandoRepositoryTest`), utility tools (`RomPatcherTest`, `BaseRomValidatorTest`, `EmulatorLauncherTest`), and API interactions (`ApiTest`, `ApiRealTest`).
- **Instrumented Tests**: UI and integration tests (`ExampleInstrumentedTest`).

To run unit tests:
```bash
./gradlew testDebugUnitTest
```

To run instrumented tests:
```bash
./gradlew connectedAndroidTest
```
---

## License

This project is open-source and available under the terms of the [MIT License](LICENSE).
