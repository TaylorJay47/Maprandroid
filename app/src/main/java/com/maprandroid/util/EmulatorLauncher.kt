package com.maprandroid.util

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

data class EmulatorInfo(
    val name: String,
    val packageName: String
)

object EmulatorLauncher {

    private const val TAG = "EmulatorLauncher"
    private const val FILE_PROVIDER_AUTHORITY = "com.maprandroid.fileprovider"

    val RETROARCH_PACKAGES = listOf(
        "com.retroarch.aarch64",
        "com.retroarch",
        "com.retroarch.ra",
        "com.retroarch.armv7",
        "org.retroarch"
    )

    val RETROARCH_CORES = listOf(
        "snes9x_libretro_android.so",
        "snes9x2010_libretro_android.so",
        "snes9x2005_libretro_android.so",
        "bsnes_libretro_android.so"
    )

    fun isRetroArchPackage(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        return RETROARCH_PACKAGES.any { it.equals(packageName.trim(), ignoreCase = true) }
    }

    fun getRetroArchCorePath(context: Context?, packageName: String): String {
        if (context != null) {
            val storageRoot = try {
                android.os.Environment.getExternalStorageDirectory()
            } catch (_: Exception) {
                File("/storage/emulated/0")
            }
            val possibleCoreDirs = listOf(
                File("/data/data/$packageName/cores"),
                File(storageRoot, "Android/data/$packageName/files/cores"),
                File(storageRoot, "RetroArch/cores")
            )
            for (coreDir in possibleCoreDirs) {
                if (coreDir.exists() && coreDir.isDirectory) {
                    for (coreName in RETROARCH_CORES) {
                        val coreFile = File(coreDir, coreName)
                        if (coreFile.exists() && coreFile.isFile) {
                            return coreFile.absolutePath
                        }
                    }
                }
            }
        }
        return "/data/data/$packageName/cores/snes9x_libretro_android.so"
    }

    fun getRetroArchCorePath(packageName: String): String {
        return getRetroArchCorePath(null, packageName)
    }

    fun getRetroArchConfigPath(packageName: String): String {
        try {
            val storageRoot = android.os.Environment.getExternalStorageDirectory() ?: File("/storage/emulated/0")
            val candidateConfigs = listOf(
                File(storageRoot, "Android/data/$packageName/files/retroarch.cfg"),
                File(storageRoot, "RetroArch/config/retroarch.cfg"),
                File(storageRoot, "RetroArch/retroarch.cfg")
            )
            for (cfg in candidateConfigs) {
                if (cfg.exists() && cfg.isFile) {
                    return cfg.absolutePath
                }
            }
        } catch (_: Exception) {
        }
        return "/storage/emulated/0/Android/data/$packageName/files/retroarch.cfg"
    }

    /**
     * Resolves the RetroArch activity class name.
     */
    fun resolveRetroArchActivity(context: Context?, retroArchPkg: String): String {
        return "com.retroarch.browser.retroactivity.RetroActivityFuture"
    }

    /**
     * Generates a valid FileProvider content URI with a fallback content URI for testing/edge cases.
     */
    fun getUriForRom(context: Context, romFile: File): Uri? {
        return try {
            FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, romFile)
        } catch (_: Throwable) {
            try {
                Uri.parse("content://$FILE_PROVIDER_AUTHORITY/${romFile.name}")
            } catch (_: Throwable) {
                null
            }
        }
    }

    /**
     * Builds an Intent specifically configured for launching RetroArch directly into the ROM with snes9x core.
     */
    fun createRetroArchIntent(
        context: Context,
        retroArchPkg: String,
        romFile: File
    ): Intent {
        val corePath = getRetroArchCorePath(context, retroArchPkg)
        val configPath = getRetroArchConfigPath(retroArchPkg)
        val activityClass = resolveRetroArchActivity(context, retroArchPkg)

        return Intent().apply {
            setClassName(retroArchPkg, activityClass)
            putExtra("ROM", romFile.absolutePath)
            putExtra("PATH", romFile.absolutePath)
            putExtra("LIBRETRO", corePath)
            putExtra("CONFIGFILE", configPath)
            putExtra("QUITFOCUS", "")
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NO_HISTORY
            )
        }
    }

    /**
     * Finds installed RetroArch package name if present on system by checking known packages.
     */
    fun findInstalledRetroArchPackage(context: Context): String? {
        val pm = try {
            context.packageManager
        } catch (_: Exception) {
            return null
        }

        for (pkg in RETROARCH_PACKAGES) {
            val installed = try {
                pm.getLaunchIntentForPackage(pkg) != null
            } catch (_: Exception) {
                false
            } || try {
                pm.getPackageInfo(pkg, 0)
                true
            } catch (_: Exception) {
                false
            }
            if (installed) {
                return pkg
            }
        }
        return null
    }

    /**
     * Detects installed SNES emulators on the device using [PackageManager].
     */
    fun getAvailableEmulators(context: Context): List<EmulatorInfo> {
        val results = mutableListOf<EmulatorInfo>()
        val addedPackages = mutableSetOf<String>()

        results.add(EmulatorInfo(name = "System Default (Chooser)", packageName = ""))
        addedPackages.add("")

        val pm = try {
            context.packageManager
        } catch (_: Exception) {
            null
        }

        if (pm != null) {
            val retroArchPkg = findInstalledRetroArchPackage(context)
            if (retroArchPkg != null && addedPackages.add(retroArchPkg)) {
                results.add(EmulatorInfo(name = "RetroArch", packageName = retroArchPkg))
            }

            val knownPackages = listOf(
                "com.retroarch.aarch64" to "RetroArch 64-bit",
                "com.retroarch" to "RetroArch",
                "com.retroarch.ra" to "RetroArch RA",
                "com.retroarch.armv7" to "RetroArch 32-bit",
                "org.retroarch" to "RetroArch",
                "com.explusalpha.snes9xplus" to "Snes9x EX+",
                "com.swordfish.lemuroid" to "Lemuroid"
            )

            for ((pkg, defaultLabel) in knownPackages) {
                if (!addedPackages.contains(pkg)) {
                    try {
                        val isInstalled = pm.getLaunchIntentForPackage(pkg) != null
                        if (isInstalled) {
                            addedPackages.add(pkg)
                            results.add(EmulatorInfo(name = defaultLabel, packageName = pkg))
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }

        results.add(EmulatorInfo(name = "Custom Package...", packageName = "custom"))
        return results
    }

    /**
     * Resolves a ROM path or content URI into an accessible local [File] in public Downloads or app storage.
     */
    fun resolveRomFile(context: Context, romPath: String): File? {
        if (romPath.isBlank()) return null

        fun ensurePublicCopyIfNeeded(file: File): File {
            if (file.absolutePath.contains("/Android/data/", ignoreCase = true)) {
                try {
                    val publicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    if (publicDir != null && (publicDir.exists() || publicDir.mkdirs())) {
                        val publicFile = File(publicDir, file.name)
                        if (!publicFile.exists() || publicFile.length() != file.length()) {
                            file.inputStream().use { input ->
                                publicFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                        if (publicFile.exists() && publicFile.length() > 0) {
                            return publicFile
                        }
                    }
                } catch (_: Exception) {
                }
            }
            return file
        }

        val directFile = File(romPath)
        if (directFile.exists() && directFile.isFile) {
            return ensurePublicCopyIfNeeded(directFile)
        }

        if (romPath.startsWith("file://", ignoreCase = true)) {
            try {
                val cleanPath = romPath.removePrefix("file://").removePrefix("localhost")
                val uriFile = File(cleanPath)
                if (uriFile.exists() && uriFile.isFile) {
                    return ensurePublicCopyIfNeeded(uriFile)
                }
            } catch (_: Exception) {
            }
        }

        if (romPath.startsWith("content://")) {
            try {
                val uri = Uri.parse(romPath)
                var fileName = ""

                try {
                    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME, "_data"), null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        val dataIndex = cursor.getColumnIndex("_data")
                        if (cursor.moveToFirst()) {
                            if (dataIndex >= 0) {
                                val dataPath = cursor.getString(dataIndex)
                                if (!dataPath.isNullOrBlank()) {
                                    val f = File(dataPath)
                                    if (f.exists() && f.isFile) {
                                        return f
                                    }
                                }
                            }
                            if (nameIndex >= 0) {
                                val displayName = cursor.getString(nameIndex)
                                if (!displayName.isNullOrBlank()) {
                                    fileName = displayName
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to query content URI metadata: ${e.message}")
                }

                if (fileName.isNotBlank()) {
                    val publicDownloadDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                    val directDownloadFile = File(publicDownloadDir, fileName)
                    if (directDownloadFile.exists() && directDownloadFile.isFile) {
                        return directDownloadFile
                    }
                }

                val targetName = if (fileName.isNotBlank()) fileName else "SM_MapRando_${System.currentTimeMillis()}.sfc"
                val publicDownloadDir = try {
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                } catch (_: Exception) {
                    null
                }

                val romsDir = if (publicDownloadDir != null && (publicDownloadDir.exists() || publicDownloadDir.mkdirs())) {
                    publicDownloadDir
                } else {
                    context.getExternalFilesDir("ROMs") ?: File(context.filesDir, "ROMs")
                }

                if (!romsDir.exists()) {
                    romsDir.mkdirs()
                }

                val localFile = File(romsDir, targetName)
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        localFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to open input stream for $romPath: ${e.message}")
                }

                if (localFile.exists() && localFile.length() > 0) {
                    return localFile
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error resolving content URI $romPath: ${e.message}")
            }
        }

        return null
    }

    /**
     * Launches the emulator with the specified ROM, booting RetroArch directly into the ROM using the snes9x core.
     */
    fun launchEmulator(context: Context, romFile: File, emulatorPackage: String?): Boolean {
        if (!romFile.exists() || !romFile.isFile) {
            Log.w(TAG, "ROM file does not exist or is not a file: ${romFile.absolutePath}")
            return false
        }

        val trimmedPackage = emulatorPackage?.trim()
        val isSystemDefault = trimmedPackage.isNullOrEmpty() ||
                trimmedPackage.equals("System Default", ignoreCase = true) ||
                trimmedPackage.equals("System Default (Chooser)", ignoreCase = true)

        val installedRetroArch = findInstalledRetroArchPackage(context)
        val targetPackage = if (isSystemDefault) {
            installedRetroArch
        } else {
            trimmedPackage
        }

        val isRetroArch = isRetroArchPackage(targetPackage) || (isSystemDefault && installedRetroArch != null)

        if (isRetroArch && !targetPackage.isNullOrEmpty()) {
            val activityClasses = listOf(
                "com.retroarch.browser.retroactivity.RetroActivityFuture",
                "$targetPackage.browser.retroactivity.RetroActivityFuture",
                "com.retroarch.browser.retroactivity.RetroActivity",
                "$targetPackage.browser.retroactivity.RetroActivity"
            )

            for (activityClass in activityClasses) {
                try {
                    try {
                        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                        am?.killBackgroundProcesses(targetPackage)
                    } catch (_: Exception) {
                    }

                    val corePath = getRetroArchCorePath(targetPackage)
                    val configPath = getRetroArchConfigPath(targetPackage)

                    val intent = Intent().apply {
                        setClassName(targetPackage, activityClass)
                        putExtra("ROM", romFile.absolutePath)
                        putExtra("PATH", romFile.absolutePath)
                        putExtra("LIBRETRO", corePath)
                        putExtra("CONFIGFILE", configPath)
                        putExtra("QUITFOCUS", "")
                        addFlags(
                            Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_NO_HISTORY
                        )
                    }
                    try {
                        Toast.makeText(context, "Launching RetroArch...", Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                    }
                    context.startActivity(intent)
                    return true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to launch RetroArch with activity $activityClass: ${e.message}")
                }
            }
        }

        // Handle other specific emulator packages
        val pm = try {
            context.packageManager
        } catch (_: Exception) {
            null
        }

        if (pm != null && !targetPackage.isNullOrEmpty() && !isSystemDefault) {
            try {
                val fileUri = getUriForRom(context, romFile)
                val launchIntent = (pm.getLaunchIntentForPackage(targetPackage)
                    ?: Intent(Intent.ACTION_VIEW).apply { setPackage(targetPackage) }).apply {
                    if (fileUri != null) {
                        setDataAndType(fileUri, "application/octet-stream")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    putExtra("ROM", romFile.absolutePath)
                    putExtra("PATH", romFile.absolutePath)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    Toast.makeText(context, "Launching emulator...", Toast.LENGTH_SHORT).show()
                } catch (_: Exception) {
                }
                context.startActivity(launchIntent)
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Failed to launch package $targetPackage: ${e.message}")
            }
        }

        // Fallback generic chooser / view intent
        try {
            val fileUri = getUriForRom(context, romFile)
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                if (fileUri != null) {
                    setDataAndType(fileUri, "application/octet-stream")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                putExtra("ROM", romFile.absolutePath)
                putExtra("PATH", romFile.absolutePath)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(viewIntent, "Open ROM with...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                Toast.makeText(context, "Opening ROM with available app...", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
            }
            context.startActivity(chooser)
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Fallback launch failed: ${e.message}")
        }

        return false
    }

    /**
     * Overload accepting romPath string for backward compatibility with existing code and tests.
     */
    fun launchEmulator(context: Context, romPath: String, emulatorPackage: String?): Boolean {
        val romFile = resolveRomFile(context, romPath) ?: run {
            Log.w(TAG, "Failed to resolve ROM file from path: $romPath")
            return false
        }
        return launchEmulator(context, romFile, emulatorPackage)
    }
}
