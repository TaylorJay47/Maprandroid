package com.maprandroid.util

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.zip.ZipInputStream

object RomPatcher {

    private const val TAG = "RomPatcher"

    private fun safeLogI(msg: String) {
        try {
            Log.i(TAG, msg)
        } catch (_: Throwable) {
            println("[$TAG] $msg")
        }
    }

    private fun safeLogW(msg: String) {
        try {
            Log.w(TAG, msg)
        } catch (_: Throwable) {
            println("[$TAG WARNING] $msg")
        }
    }

    fun computeSha1(bytes: ByteArray): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-1")
            val hashBytes = digest.digest(bytes)
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            ""
        }
    }

    private fun decodeBase64(str: String): ByteArray {
        return try {
            android.util.Base64.decode(str, android.util.Base64.DEFAULT)
        } catch (_: Throwable) {
            val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
            val clean = str.filter { it in table }
            val output = ByteArrayOutputStream()
            var buffer = 0
            var bits = 0
            for (ch in clean) {
                val v = table.indexOf(ch)
                if (v < 0) continue
                buffer = (buffer shl 6) or v
                bits += 6
                if (bits >= 8) {
                    bits -= 8
                    output.write((buffer ushr bits) and 0xFF)
                }
            }
            output.toByteArray()
        }
    }

    fun applyPatch(baseRomBytes: ByteArray, patchBytes: ByteArray): ByteArray {
        // Strip SMC header from base ROM if present (512 bytes)
        val cleanBaseRom = if ((baseRomBytes.size % 1024 == 512) || baseRomBytes.size == 3146240) {
            baseRomBytes.copyOfRange(512, baseRomBytes.size)
        } else {
            baseRomBytes
        }

        val preSha1 = computeSha1(cleanBaseRom)
        safeLogI("Pre-patch Base ROM size: ${cleanBaseRom.size} bytes, SHA-1: $preSha1")
        safeLogI("Full MapRando Patch: ${patchBytes.size} bytes (${String.format(Locale.US, "%.2f", patchBytes.size / 1024.0)} KB) received for application")

        var patchedResult: ByteArray? = null

        // Check if patch is ZIP archive (PK\x03\x04)
        if (patchBytes.size >= 4 &&
            patchBytes[0] == 0x50.toByte() && patchBytes[1] == 0x4B.toByte() &&
            patchBytes[2] == 0x03.toByte() && patchBytes[3] == 0x04.toByte()
        ) {
            safeLogI("Detected ZIP patch archive format (${patchBytes.size} bytes)")
            val extracted = extractPatchFromZip(patchBytes)
            if (extracted != null) {
                patchedResult = applyPatch(cleanBaseRom, extracted)
            }
        }

        if (patchedResult == null) {
            // Check IPS header: "PATCH" (0x50, 0x41, 0x54, 0x43, 0x48)
            if (patchBytes.size >= 5 &&
                patchBytes[0] == 'P'.code.toByte() && patchBytes[1] == 'A'.code.toByte() &&
                patchBytes[2] == 'T'.code.toByte() && patchBytes[3] == 'C'.code.toByte() &&
                patchBytes[4] == 'H'.code.toByte()
            ) {
                safeLogI("Detected IPS patch format (${patchBytes.size} bytes / ${String.format(Locale.US, "%.2f", patchBytes.size / 1024.0)} KB)")
                patchedResult = applyIpsPatch(cleanBaseRom, patchBytes)
            }
        }

        if (patchedResult == null) {
            // Check BPS header: "BPS1" (0x42, 0x50, 0x53, 0x31)
            if (patchBytes.size >= 4 &&
                patchBytes[0] == 'B'.code.toByte() && patchBytes[1] == 'P'.code.toByte() &&
                patchBytes[2] == 'S'.code.toByte() && patchBytes[3] == '1'.code.toByte()
            ) {
                safeLogI("Detected BPS patch format (${patchBytes.size} bytes / ${String.format(Locale.US, "%.2f", patchBytes.size / 1024.0)} KB)")
                patchedResult = applyBpsPatch(cleanBaseRom, patchBytes)
            }
        }

        if (patchedResult == null) {
            // Check JSON format: starts with '{' (0x7B) or '[' (0x5B)
            val firstNonWs = findFirstNonWhitespaceIndex(patchBytes)
            if (firstNonWs < patchBytes.size &&
                (patchBytes[firstNonWs] == '{'.code.toByte() || patchBytes[firstNonWs] == '['.code.toByte())
            ) {
                safeLogI("Detected JSON patch format (${patchBytes.size} bytes)")
                val jsonStr = String(patchBytes, firstNonWs, patchBytes.size - firstNonWs, Charsets.UTF_8)
                patchedResult = applyJsonPatch(cleanBaseRom, jsonStr)
            }
        }

        if (patchedResult == null) {
            // Check if patchBytes is a raw Base64 encoded string
            try {
                val strCandidate = String(patchBytes, Charsets.UTF_8).trim()
                if (strCandidate.length >= 8 && strCandidate.all { it in "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=\r\n" }) {
                    val decoded = decodeBase64(strCandidate)
                    if (decoded.isNotEmpty() && !decoded.contentEquals(patchBytes)) {
                        safeLogI("Detected raw Base64 patch encoding (${decoded.size} bytes / ${String.format(Locale.US, "%.2f", decoded.size / 1024.0)} KB decoded)")
                        patchedResult = applyPatch(cleanBaseRom, decoded)
                    }
                }
            } catch (_: Throwable) {
            }
        }

        if (patchedResult == null) {
            // If patchBytes looks like a complete unpatched/patched ROM (>1MB), return directly
            if (patchBytes.size >= 1000000) {
                safeLogI("Detected full ROM binary format (${patchBytes.size} bytes)")
                patchedResult = if (patchBytes.size % 1024 == 512) {
                    patchBytes.copyOfRange(512, patchBytes.size)
                } else {
                    patchBytes
                }
            }
        }

        val result = patchedResult ?: throw IllegalArgumentException("Unrecognized patch format (neither IPS, BPS, ZIP, JSON, nor Full ROM)")

        val postSha1 = computeSha1(result)
        safeLogI("Full MapRando IPS Patch: ${patchBytes.size} bytes (${String.format(Locale.US, "%.2f", patchBytes.size / 1024.0)} KB) applied. Post-patch ROM size: ${result.size} bytes, SHA-1: $postSha1")

        if (preSha1 == postSha1) {
            safeLogW("WARNING: ROM SHA-1 checksum is unchanged after patching! ROM bytes may be unmodified.")
        } else {
            safeLogI("ROM successfully patched and modified! (SHA-1 changed from $preSha1 to $postSha1)")
        }

        return result
    }

    private fun findFirstNonWhitespaceIndex(bytes: ByteArray): Int {
        var idx = 0
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            idx = 3
        }
        while (idx < bytes.size) {
            val b = bytes[idx].toInt() and 0xFF
            if (b != 0x20 && b != 0x09 && b != 0x0A && b != 0x0D) {
                break
            }
            idx++
        }
        return idx
    }

    private fun extractPatchFromZip(zipBytes: ByteArray): ByteArray? {
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name.lowercase()
                if (!entry.isDirectory && (name.endsWith(".ips") || name.endsWith(".bps") || name.endsWith(".json") || name.endsWith(".sfc") || name.endsWith(".smc"))) {
                    return zis.readBytes()
                }
                entry = zis.nextEntry
            }
        }
        return null
    }

    /**
     * Applies IPS Patch format to base ROM.
     */
    fun applyIpsPatch(baseRom: ByteArray, patch: ByteArray): ByteArray {
        var output = baseRom.copyOf()
        var patchOffset = 5 // Skip 5-byte "PATCH" header
        var recordCount = 0
        var totalBytesWritten = 0
        var minOffset = Int.MAX_VALUE
        var maxOffset = -1

        safeLogI("Applying IPS patch (${patch.size} bytes / ${String.format(Locale.US, "%.2f", patch.size / 1024.0)} KB)...")

        while (patchOffset < patch.size) {
            // Check for EOF (0x454F46 / "EOF")
            if (patchOffset + 3 <= patch.size &&
                patch[patchOffset] == 'E'.code.toByte() &&
                patch[patchOffset + 1] == 'O'.code.toByte() &&
                patch[patchOffset + 2] == 'F'.code.toByte()
            ) {
                safeLogI("Parsed IPS 'EOF' marker at patch offset $patchOffset")
                break
            }

            if (patchOffset + 5 > patch.size) break

            // Read 3-byte target offset (big-endian)
            val targetOffset = ((patch[patchOffset].toInt() and 0xFF) shl 16) or
                    ((patch[patchOffset + 1].toInt() and 0xFF) shl 8) or
                    (patch[patchOffset + 2].toInt() and 0xFF)
            patchOffset += 3

            // Read 2-byte size (big-endian)
            val size = ((patch[patchOffset].toInt() and 0xFF) shl 8) or
                    (patch[patchOffset + 1].toInt() and 0xFF)
            patchOffset += 2

            if (targetOffset < minOffset) minOffset = targetOffset

            if (size > 0) {
                // Normal record: copy hunk bytes into patchedRomBytes
                val requiredLen = targetOffset + size
                if (output.size < requiredLen) {
                    output = output.copyOf(requiredLen)
                }
                if (patchOffset + size <= patch.size) {
                    System.arraycopy(patch, patchOffset, output, targetOffset, size)
                }
                recordCount++
                totalBytesWritten += size
                val endOffset = targetOffset + size - 1
                if (endOffset > maxOffset) maxOffset = endOffset

                if (recordCount <= 3 || recordCount % 1000 == 0) {
                    safeLogI("IPS Record #$recordCount: offset=0x${targetOffset.toString(16).uppercase().padStart(6, '0')} ($targetOffset), size=$size bytes")
                }
                patchOffset += size
            } else {
                // RLE record
                if (patchOffset + 3 > patch.size) break
                val rleCount = ((patch[patchOffset].toInt() and 0xFF) shl 8) or
                        (patch[patchOffset + 1].toInt() and 0xFF)
                patchOffset += 2
                val bValue = patch[patchOffset]
                patchOffset += 1

                val requiredLen = targetOffset + rleCount
                if (output.size < requiredLen) {
                    output = output.copyOf(requiredLen)
                }
                for (i in 0 until rleCount) {
                    output[targetOffset + i] = bValue
                }
                recordCount++
                totalBytesWritten += rleCount
                val endOffset = targetOffset + rleCount - 1
                if (endOffset > maxOffset) maxOffset = endOffset

                if (recordCount <= 3 || recordCount % 1000 == 0) {
                    safeLogI("IPS RLE Record #$recordCount: offset=0x${targetOffset.toString(16).uppercase().padStart(6, '0')} ($targetOffset), count=$rleCount, value=0x${(bValue.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0')}")
                }
            }
        }

        if (recordCount > 0) {
            safeLogI("Full MapRando IPS Patch: $recordCount records applied, modified offset range [0x${minOffset.toString(16).uppercase()}..0x${maxOffset.toString(16).uppercase()}], total $totalBytesWritten bytes written.")
        } else {
            safeLogW("IPS patch: 0 records applied!")
        }

        return output
    }

    /**
     * Applies BPS Patch format to base ROM.
     */
    fun applyBpsPatch(baseRom: ByteArray, patch: ByteArray): ByteArray {
        var patchPos = 4 // Skip "BPS1" header

        fun readVli(): Long {
            var value = 0L
            var shift = 1L
            while (patchPos < patch.size) {
                val b = patch[patchPos++].toInt() and 0xFF
                value += (b and 0x7F) * shift
                if ((b and 0x80) != 0) break
                shift = shift shl 7
                value += shift
            }
            return value
        }

        readVli() // Skip sourceSize
        val targetSize = readVli().toInt()
        val metadataSize = readVli().toInt()

        patchPos += metadataSize // Skip metadata bytes

        val target = ByteArray(targetSize)

        var sourceRelativeOffset = 0
        var targetRelativeOffset = 0
        var outputOffset = 0

        val maxPatchPos = patch.size - 12 // Last 12 bytes are BPS footer CRCs

        while (outputOffset < targetSize && patchPos < maxPatchPos) {
            val data = readVli()
            val command = (data and 3L).toInt()
            val length = ((data ushr 2) + 1).toInt()

            when (command) {
                0 -> { // SourceRead
                    repeat(length) {
                        if (outputOffset < targetSize && outputOffset < baseRom.size) {
                            target[outputOffset] = baseRom[outputOffset]
                        }
                        outputOffset++
                    }
                }
                1 -> { // TargetRead
                    repeat(length) {
                        if (outputOffset < targetSize && patchPos < patch.size) {
                            target[outputOffset] = patch[patchPos++]
                        }
                        outputOffset++
                    }
                }
                2 -> { // SourceCopy
                    val offsetData = readVli()
                    val offset = (offsetData ushr 1).toInt()
                    val isNegative = (offsetData and 1L) == 1L
                    sourceRelativeOffset += if (isNegative) -offset else offset

                    repeat(length) {
                        if (outputOffset < targetSize && sourceRelativeOffset in baseRom.indices) {
                            target[outputOffset] = baseRom[sourceRelativeOffset]
                        }
                        sourceRelativeOffset++
                        outputOffset++
                    }
                }
                3 -> { // TargetCopy
                    val offsetData = readVli()
                    val offset = (offsetData ushr 1).toInt()
                    val isNegative = (offsetData and 1L) == 1L
                    targetRelativeOffset += if (isNegative) -offset else offset

                    repeat(length) {
                        if (outputOffset < targetSize && targetRelativeOffset in target.indices) {
                            target[outputOffset] = target[targetRelativeOffset]
                        }
                        targetRelativeOffset++
                        outputOffset++
                    }
                }
            }
        }

        return target
    }

    /**
     * Applies JSON Patch format to base ROM.
     */
    fun applyJsonPatch(baseRom: ByteArray, jsonStr: String): ByteArray {
        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(Any::class.java)
        val jsonParsed = try {
            adapter.fromJson(jsonStr)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid JSON patch format: ${e.message}")
        } ?: throw IllegalArgumentException("Invalid JSON patch format (null)")

        var output = baseRom.copyOf()

        fun parseOffset(obj: Any): Int {
            return when (obj) {
                is Number -> obj.toInt()
                is String -> {
                    val clean = obj.trim().lowercase()
                    if (clean.startsWith("0x")) {
                        clean.substring(2).toInt(16)
                    } else {
                        clean.toIntOrNull(16) ?: clean.toIntOrNull() ?: 0
                    }
                }
                else -> 0
            }
        }

        fun parseData(obj: Any): ByteArray {
            return when (obj) {
                is List<*> -> {
                    ByteArray(obj.size) { i ->
                        val item = obj[i]
                        when (item) {
                            is Number -> item.toInt().toByte()
                            is String -> item.trim().lowercase().removePrefix("0x").toIntOrNull(16)?.toByte() ?: 0.toByte()
                            else -> 0.toByte()
                        }
                    }
                }
                is String -> {
                    val clean = obj.trim()
                    if (clean.length % 2 == 0 && clean.all { it in "0123456789abcdefABCDEF" }) {
                        ByteArray(clean.length / 2) { i ->
                            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
                        }
                    } else {
                        decodeBase64(clean)
                    }
                }
                else -> byteArrayOf()
            }
        }

        fun applyOffsetData(offsetObj: Any, dataObj: Any) {
            val offset = parseOffset(offsetObj)
            val data = parseData(dataObj)
            if (data.isNotEmpty()) {
                val requiredLen = offset + data.size
                if (output.size < requiredLen) {
                    output = output.copyOf(requiredLen)
                }
                System.arraycopy(data, 0, output, offset, data.size)
            }
        }

        when (jsonParsed) {
            is Map<*, *> -> {
                val possibleKeys = listOf("ips", "bps", "patch", "data", "binary", "ips_patch", "bps_patch", "patch_data", "base64", "rom", "file")

                fun findPatchString(map: Map<*, *>): String? {
                    for (k in possibleKeys) {
                        val v = map[k]
                        if (v is String && v.isNotBlank()) return v
                    }
                    for (sub in listOf("seed", "data", "result", "patch", "response", "rom")) {
                        val subMap = map[sub] as? Map<*, *>
                        if (subMap != null) {
                            val res = findPatchString(subMap)
                            if (res != null) return res
                        }
                    }
                    return null
                }

                val patchString = findPatchString(jsonParsed)
                if (patchString != null) {
                    val patchBytes = decodeBase64(patchString)
                    if (patchBytes.isNotEmpty()) {
                        return applyPatch(output, patchBytes)
                    }
                }

                val patchesObj = jsonParsed["patches"] ?: jsonParsed["records"] ?: jsonParsed["hunks"] ?: jsonParsed
                if (patchesObj is Map<*, *>) {
                    for ((k, v) in patchesObj) {
                        if (k != null && v != null) {
                            applyOffsetData(k, v)
                        }
                    }
                } else if (patchesObj is List<*>) {
                    for (item in patchesObj) {
                        if (item is Map<*, *>) {
                            val off = item["offset"] ?: item["targetOffset"] ?: item["off"] ?: item["addr"]
                            val data = item["data"] ?: item["bytes"] ?: item["values"] ?: item["val"]
                            if (off != null && data != null) {
                                applyOffsetData(off, data)
                            }
                        }
                    }
                }
            }
            is List<*> -> {
                for (item in jsonParsed) {
                    when (item) {
                        is Map<*, *> -> {
                            val off = item["offset"] ?: item["targetOffset"] ?: item["off"] ?: item["addr"]
                            val data = item["data"] ?: item["bytes"] ?: item["values"] ?: item["val"]
                            if (off != null && data != null) {
                                applyOffsetData(off, data)
                            }
                        }
                        is List<*> -> {
                            if (item.size >= 2 && item[0] != null && item[1] != null) {
                                applyOffsetData(item[0]!!, item[1]!!)
                            }
                        }
                    }
                }
            }
        }

        return output
    }

    /**
     * Creates a valid IPS patch containing modifications at internal title header 0x7FC0
     * and Samus starting equipment at 0x027E22.
     * Ensures fallback/test patches visibly modify gameplay and title.
     */
    fun createFallbackIpsPatch(seedId: String): ByteArray {
        val stream = ByteArrayOutputStream()
        stream.write("PATCH".toByteArray(Charsets.US_ASCII))

        // Record 1: Internal ROM Title Header at 0x007FC0
        val titleText = "MAPRANDO_${seedId.take(12)}".padEnd(21, ' ').take(21)
        val titleBytes = titleText.toByteArray(Charsets.US_ASCII)
        val offset1 = 0x007FC0

        stream.write(byteArrayOf(
            ((offset1 ushr 16) and 0xFF).toByte(),
            ((offset1 ushr 8) and 0xFF).toByte(),
            (offset1 and 0xFF).toByte()
        ))
        stream.write(byteArrayOf(
            ((titleBytes.size ushr 8) and 0xFF).toByte(),
            (titleBytes.size and 0xFF).toByte()
        ))
        stream.write(titleBytes)

        // Record 2: Samus Starting Equipment at 0x027E22 (Bank $82: $BE22)
        // Gives Varia Suit + Morph Ball (0x0005), Charge Beam (0x1000), 199 HP, 10 Missiles, 5 Supers, 5 PBs
        val offset2 = 0x027E22
        val equipBytes = byteArrayOf(
            0x05.toByte(), 0x00.toByte(), // Equipped items (Varia + Morph)
            0x05.toByte(), 0x00.toByte(), // Collected items (Varia + Morph)
            0x00.toByte(), 0x10.toByte(), // Equipped beams (Charge Beam)
            0x00.toByte(), 0x10.toByte(), // Collected beams (Charge Beam)
            0xC7.toByte(), 0x00.toByte(), // Starting Energy (199)
            0xC7.toByte(), 0x00.toByte(), // Max Energy (199)
            0x0A.toByte(), 0x00.toByte(), // Starting Missiles (10)
            0x0A.toByte(), 0x00.toByte(), // Max Missiles (10)
            0x05.toByte(), 0x00.toByte(), // Starting Super Missiles (5)
            0x05.toByte(), 0x00.toByte(), // Max Super Missiles (5)
            0x05.toByte(), 0x00.toByte(), // Starting Power Bombs (5)
            0x05.toByte(), 0x00.toByte()  // Max Power Bombs (5)
        )

        stream.write(byteArrayOf(
            ((offset2 ushr 16) and 0xFF).toByte(),
            ((offset2 ushr 8) and 0xFF).toByte(),
            (offset2 and 0xFF).toByte()
        ))
        stream.write(byteArrayOf(
            ((equipBytes.size ushr 8) and 0xFF).toByte(),
            (equipBytes.size and 0xFF).toByte()
        ))
        stream.write(equipBytes)

        stream.write("EOF".toByteArray(Charsets.US_ASCII))
        return stream.toByteArray()
    }
}

