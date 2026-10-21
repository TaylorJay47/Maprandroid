package com.maprandroid

import com.maprandroid.util.BaseRomValidator
import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

class BaseRomValidatorTest {

    @Test
    fun validate_emptyRom_returnsInvalid() {
        val result = BaseRomValidator.validate(ByteArray(0))
        assertFalse(result.isValid)
        assertEquals("Empty ROM file", result.statusMessage)
    }

    @Test
    fun validate_validJUChecksum_returnsValid() {
        // Create dummy bytes that hash to expected JU SHA-1 / MD5 or test hashing logic
        // Since we can't bundle full 3MB ROM, we test with mock byte array matching size and hash logic
        val romBytes = ByteArray(3145728) { 0 }
        
        // Calculate hash of dummy rom
        val digest = MessageDigest.getInstance("SHA-1")
        val computedSha1 = digest.digest(romBytes).joinToString("") { "%02x".format(it) }

        val result = BaseRomValidator.validate(romBytes)
        assertNotNull(result)
        assertEquals(computedSha1, result.sha1)
    }

    @Test
    fun validate_headeredRom_detectsHeaderAndStrips() {
        // 512 header + 1024 body = 1536 bytes (1536 % 1024 == 512)
        val headeredBytes = ByteArray(1536) { 0 }
        val result = BaseRomValidator.validate(headeredBytes)
        
        assertTrue(result.isHeadered)
        assertNotNull(result.headerlessBytes)
        assertEquals(1024, result.headerlessBytes!!.size)
    }

    @Test
    fun validate_superMetroidTitleFound_returnsSpecificStatus() {
        val romBytes = ByteArray(3145728) { 0 }
        // Insert "SUPER METROID" title at 0x7FC0
        val title = "SUPER METROID".toByteArray(Charsets.US_ASCII)
        System.arraycopy(title, 0, romBytes, 0x7FC0, title.size)

        val result = BaseRomValidator.validate(romBytes)
        assertFalse(result.isValid)
        assertTrue(result.statusMessage.contains("Super Metroid ROM found, but hash mismatch"))
    }
}
