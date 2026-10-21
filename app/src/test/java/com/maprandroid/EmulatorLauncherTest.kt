package com.maprandroid

import android.content.Context
import android.content.ContextWrapper
import com.maprandroid.util.EmulatorLauncher
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class EmulatorLauncherTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun testContext(): Context {
        return object : ContextWrapper(null) {}
    }

    @Test
    fun getRetroArchCorePath_returnsDefaultSnes9xCorePath() {
        val path = EmulatorLauncher.getRetroArchCorePath("com.retroarch.aarch64")
        assertEquals("/data/data/com.retroarch.aarch64/cores/snes9x_libretro_android.so", path)
    }

    @Test
    fun getRetroArchConfigPath_returnsDefaultConfigPath() {
        val path = EmulatorLauncher.getRetroArchConfigPath("com.retroarch.aarch64")
        assertEquals("/storage/emulated/0/Android/data/com.retroarch.aarch64/files/retroarch.cfg", path)
    }

    @Test
    fun resolveRomFile_blankPath_returnsNull() {
        assertNull(EmulatorLauncher.resolveRomFile(testContext(), ""))
        assertNull(EmulatorLauncher.resolveRomFile(testContext(), "   "))
    }

    @Test
    fun resolveRomFile_existingDirectFile_returnsFile() {
        val tempFile = tempFolder.newFile("test_rom.sfc")
        tempFile.writeText("dummy content")

        val resolved = EmulatorLauncher.resolveRomFile(testContext(), tempFile.absolutePath)
        assertNotNull(resolved)
        assertEquals(tempFile.absolutePath, resolved?.absolutePath)
    }

    @Test
    fun resolveRomFile_existingFileUri_returnsFile() {
        val tempFile = tempFolder.newFile("test_rom2.sfc")
        tempFile.writeText("dummy content 2")

        val fileUri = "file://" + tempFile.absolutePath
        val resolved = EmulatorLauncher.resolveRomFile(testContext(), fileUri)
        assertNotNull(resolved)
        assertEquals(tempFile.absolutePath, resolved?.absolutePath)
    }

    @Test
    fun resolveRomFile_nonExistentPath_returnsNull() {
        val nonExistent = File(tempFolder.root, "does_not_exist.sfc").absolutePath
        assertNull(EmulatorLauncher.resolveRomFile(testContext(), nonExistent))
    }

    @Test
    fun launchEmulator_invalidRomFile_returnsFalse() {
        val result = EmulatorLauncher.launchEmulator(testContext(), "non_existent_rom.sfc", "com.retroarch")
        assertFalse(result)
    }

    @Test
    fun getAvailableEmulators_withTestContext_returnsSafelyWithoutException() {
        val context = object : ContextWrapper(null) {
            override fun getPackageName(): String = "com.maprandroid"
        }
        val emulators = EmulatorLauncher.getAvailableEmulators(context)
        assertNotNull(emulators)
        assertTrue(emulators.any { it.name == "System Default (Chooser)" })
        assertTrue(emulators.any { it.name == "Custom Package..." })
    }

    @Test
    fun findInstalledRetroArchPackage_withTestContext_returnsNullSafely() {
        val context = testContext()
        val result = EmulatorLauncher.findInstalledRetroArchPackage(context)
        assertNull(result)
    }

    @Test
    fun isRetroArchPackage_identifiesRetroArchPackagesCorrectly() {
        assertTrue(EmulatorLauncher.isRetroArchPackage("com.retroarch.aarch64"))
        assertTrue(EmulatorLauncher.isRetroArchPackage("com.retroarch"))
        assertTrue(EmulatorLauncher.isRetroArchPackage("org.retroarch"))
        assertFalse(EmulatorLauncher.isRetroArchPackage("com.explusalpha.snes9xplus"))
        assertFalse(EmulatorLauncher.isRetroArchPackage(null))
    }

    @Test
    fun createRetroArchIntent_returnsNonNullIntentWithRetroArchExtras() {
        val tempFile = tempFolder.newFile("test_game.sfc")
        tempFile.writeText("sfc content")

        val intent = EmulatorLauncher.createRetroArchIntent(
            context = testContext(),
            retroArchPkg = "com.retroarch.aarch64",
            romFile = tempFile,
        )
        assertNotNull(intent)
    }

    @Test
    fun resolveRetroArchActivity_returnsActivityClassForAarch64() {
        val activityClass = EmulatorLauncher.resolveRetroArchActivity(testContext(), "com.retroarch.aarch64")
        assertNotNull(activityClass)
        assertTrue(activityClass.contains("RetroActivity"))
    }

    @Test
    fun launchEmulator_validRomUninstalledPackage_handlesGracefully() {
        val tempFile = tempFolder.newFile("test_game_valid.sfc")
        tempFile.writeText("sfc content")

        val result = EmulatorLauncher.launchEmulator(testContext(), tempFile, "com.nonexistent.emulator")
        // Verifies no uncaught exception is thrown when package is not installed
        assertFalse(result)
    }

    @Test
    fun launchEmulator_noResolvedActivities_returnsFalse() {
        val tempFile = tempFolder.newFile("test_game_norouter.sfc")
        tempFile.writeText("sfc content")

        val result = EmulatorLauncher.launchEmulator(testContext(), tempFile, null)
        assertFalse(result)
    }
}
