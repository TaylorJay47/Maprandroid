package com.maprandroid

import com.maprandroid.util.RomPatcher
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class RomPatcherTest {

    @Test
    fun applyIpsPatch_modifiesBaseRomCorrectly() {
        // Base ROM of 16 bytes filled with 0x00
        val baseRom = ByteArray(16) { 0x00.toByte() }

        val patchStream = ByteArrayOutputStream()
        patchStream.write("PATCH".toByteArray(Charsets.US_ASCII))

        // Record 1
        patchStream.write(byteArrayOf(0x00, 0x00, 0x02)) // Offset 2
        patchStream.write(byteArrayOf(0x00, 0x03))       // Size 3
        patchStream.write(byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte()))

        // Record 2 (RLE)
        patchStream.write(byteArrayOf(0x00, 0x00, 0x08)) // Offset 8
        patchStream.write(byteArrayOf(0x00, 0x00))       // Size 0 (RLE)
        patchStream.write(byteArrayOf(0x00, 0x04))       // RLE Count 4
        patchStream.write(0xFF)                          // Value 0xFF

        // EOF
        patchStream.write("EOF".toByteArray(Charsets.US_ASCII))

        val patch = patchStream.toByteArray()

        val preSha1 = RomPatcher.computeSha1(baseRom)
        val patched = RomPatcher.applyIpsPatch(baseRom, patch)
        val postSha1 = RomPatcher.computeSha1(patched)

        assertNotEquals(preSha1, postSha1)
        assertEquals(16, patched.size)
        assertEquals(0x00.toByte(), patched[0])
        assertEquals(0x00.toByte(), patched[1])
        assertEquals(0xAA.toByte(), patched[2])
        assertEquals(0xBB.toByte(), patched[3])
        assertEquals(0xCC.toByte(), patched[4])
        assertEquals(0x00.toByte(), patched[5])
        assertEquals(0xFF.toByte(), patched[8])
        assertEquals(0xFF.toByte(), patched[9])
        assertEquals(0xFF.toByte(), patched[10])
        assertEquals(0xFF.toByte(), patched[11])
        assertEquals(0x00.toByte(), patched[12])
    }

    @Test
    fun applyJsonPatch_modifiesBaseRomCorrectly() {
        val baseRom = ByteArray(16) { 0x00.toByte() }
        val jsonPatch = """
            {
                "0x000002": [170, 187, 204],
                "0x000008": "FFFFFFFF"
            }
        """.trimIndent()

        val preSha1 = RomPatcher.computeSha1(baseRom)
        val patched = RomPatcher.applyPatch(baseRom, jsonPatch.toByteArray(Charsets.UTF_8))
        val postSha1 = RomPatcher.computeSha1(patched)

        assertNotEquals(preSha1, postSha1)
        assertEquals(16, patched.size)
        assertEquals(0xAA.toByte(), patched[2])
        assertEquals(0xBB.toByte(), patched[3])
        assertEquals(0xCC.toByte(), patched[4])
        assertEquals(0xFF.toByte(), patched[8])
        assertEquals(0xFF.toByte(), patched[9])
        assertEquals(0xFF.toByte(), patched[10])
        assertEquals(0xFF.toByte(), patched[11])
    }

    @Test
    fun applyPatch_nestedJsonWithBase64Ips_extractsAndPatches() {
        val baseRom = ByteArray(16) { 0x00.toByte() }

        val ipsStream = ByteArrayOutputStream()
        ipsStream.write("PATCH".toByteArray(Charsets.US_ASCII))
        ipsStream.write(byteArrayOf(0x00, 0x00, 0x01)) // Offset 1
        ipsStream.write(byteArrayOf(0x00, 0x02))       // Size 2
        ipsStream.write(byteArrayOf(0x11.toByte(), 0x22.toByte()))
        ipsStream.write("EOF".toByteArray(Charsets.US_ASCII))

        val b64Ips = java.util.Base64.getEncoder().encodeToString(ipsStream.toByteArray())
        val jsonNested = """
            {
                "seed": {
                    "patch_data": "$b64Ips"
                }
            }
        """.trimIndent()

        val patched = RomPatcher.applyPatch(baseRom, jsonNested.toByteArray(Charsets.UTF_8))
        assertEquals(0x11.toByte(), patched[1])
        assertEquals(0x22.toByte(), patched[2])
    }

    @Test
    fun applyPatch_rawBase64IpsPatch_decodesAndPatches() {
        val baseRom = ByteArray(16) { 0x00.toByte() }

        val ipsStream = ByteArrayOutputStream()
        ipsStream.write("PATCH".toByteArray(Charsets.US_ASCII))
        ipsStream.write(byteArrayOf(0x00, 0x00, 0x03)) // Offset 3
        ipsStream.write(byteArrayOf(0x00, 0x01))       // Size 1
        ipsStream.write(byteArrayOf(0x88.toByte()))
        ipsStream.write("EOF".toByteArray(Charsets.US_ASCII))

        val b64PatchText = java.util.Base64.getEncoder().encodeToString(ipsStream.toByteArray())
        val patched = RomPatcher.applyPatch(baseRom, b64PatchText.toByteArray(Charsets.UTF_8))
        assertEquals(0x88.toByte(), patched[3])
    }

    @Test
    fun createFallbackIpsPatch_actuallyModifiesRomBytes() {
        // Create 32KB base ROM
        val baseRom = ByteArray(0x8000) { 0x00.toByte() }
        val preSha1 = RomPatcher.computeSha1(baseRom)

        val fallbackPatch = RomPatcher.createFallbackIpsPatch("SEED_9999")
        val patched = RomPatcher.applyPatch(baseRom, fallbackPatch)
        val postSha1 = RomPatcher.computeSha1(patched)

        assertNotEquals(preSha1, postSha1)
        val headerTitle = String(patched, 0x007FC0, 21, Charsets.US_ASCII)
        assertTrue(headerTitle.startsWith("MAPRANDO_SEED_9999"))
    }

    @Test
    fun applyPatch_zipContainingIps_extractsAndPatches() {
        val baseRom = ByteArray(16) { 0x00.toByte() }

        val ipsStream = ByteArrayOutputStream()
        ipsStream.write("PATCH".toByteArray(Charsets.US_ASCII))
        ipsStream.write(byteArrayOf(0x00, 0x00, 0x00)) // Offset 0
        ipsStream.write(byteArrayOf(0x00, 0x02))       // Size 2
        ipsStream.write(byteArrayOf(0x77.toByte(), 0x88.toByte()))
        ipsStream.write("EOF".toByteArray(Charsets.US_ASCII))

        val zipOutputStream = ByteArrayOutputStream()
        ZipOutputStream(zipOutputStream).use { zos ->
            zos.putNextEntry(ZipEntry("patch.ips"))
            zos.write(ipsStream.toByteArray())
            zos.closeEntry()
        }

        val zipBytes = zipOutputStream.toByteArray()
        val patched = RomPatcher.applyPatch(baseRom, zipBytes)

        assertEquals(0x77.toByte(), patched[0])
        assertEquals(0x88.toByte(), patched[1])
    }

    @Test
    fun applyPatch_headeredBaseRom_stripsHeaderBeforePatching() {
        val headeredBase = ByteArray(1536) { 0x11.toByte() }
        for (i in 512 until 1536) {
            headeredBase[i] = 0x00.toByte()
        }

        val ipsStream = ByteArrayOutputStream()
        ipsStream.write("PATCH".toByteArray(Charsets.US_ASCII))
        ipsStream.write(byteArrayOf(0x00, 0x00, 0x00)) // Offset 0
        ipsStream.write(byteArrayOf(0x00, 0x01))       // Size 1
        ipsStream.write(byteArrayOf(0x99.toByte()))
        ipsStream.write("EOF".toByteArray(Charsets.US_ASCII))

        val patched = RomPatcher.applyPatch(headeredBase, ipsStream.toByteArray())

        assertEquals(1024, patched.size)
        assertEquals(0x99.toByte(), patched[0])
    }

    @Test
    fun applyPatch_smcHeaderedSuperMetroidRom_stripsHeaderAndAlignsOffsets() {
        // 3,146,240 bytes (3MB Super Metroid ROM + 512-byte SMC header)
        val smcHeaderedRom = ByteArray(3146240) { 0x00.toByte() }
        for (i in 0 until 512) {
            smcHeaderedRom[i] = 0xFF.toByte()
        }

        val ipsStream = ByteArrayOutputStream()
        ipsStream.write("PATCH".toByteArray(Charsets.US_ASCII))
        // Target unheadered offset 0x007FC0
        ipsStream.write(byteArrayOf(0x00, 0x7F.toByte(), 0xC0.toByte()))
        ipsStream.write(byteArrayOf(0x00, 0x04)) // Size 4
        ipsStream.write("TEST".toByteArray(Charsets.US_ASCII))
        ipsStream.write("EOF".toByteArray(Charsets.US_ASCII))

        val patched = RomPatcher.applyPatch(smcHeaderedRom, ipsStream.toByteArray())

        // Patched ROM should be unheadered (3,145,728 bytes)
        assertEquals(3145728, patched.size)
        val writtenTitle = String(patched, 0x007FC0, 4, Charsets.US_ASCII)
        assertEquals("TEST", writtenTitle)
    }

    @Test(expected = IllegalArgumentException::class)
    fun applyPatch_invalidFormat_throwsException() {
        val baseRom = ByteArray(16)
        val invalidPatch = ByteArray(10) { 0x12 }
        RomPatcher.applyPatch(baseRom, invalidPatch)
    }
}
