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

## AI Usage & Transparency Disclosure

Some of the code, documentation, and assets within this repository were created or optimized with the assistance of artificial intelligence tools (e.g., GitHub Copilot, ChatGPT, Claude).

### Human Oversight & Review
To ensure project quality and security, all AI-assisted outputs undergo a rigorous review process:
- **Verification:** Every line of code is human-reviewed, customized, and validated before merging.
- **Testing:** Functionality is tested locally to minimize bugs, hallucinated dependencies, or performance drops.

### No-Liability & Accountability Disclaimer
- **As-Is Basis:** This software is provided "as is" without warranty of any kind. 
- **User Responsibility:** AI models can occasionally produce insecure, inefficient, or flawed logic. Users are strongly advised to audit and test the code independently before deployment in production environments.
- **Accountability:** The human maintainers retain final accountability for the codebase, architecture, and fitness for purpose.
