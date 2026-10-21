package com.maprandroid.util

import java.security.MessageDigest

data class RomValidationResult(
    val isValid: Boolean,
    val statusMessage: String,
    val sha1: String = "",
    val md5: String = "",
    val isHeadered: Boolean = false,
    val headerlessBytes: ByteArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RomValidationResult

        if (isValid != other.isValid) return false
        if (statusMessage != other.statusMessage) return false
        if (sha1 != other.sha1) return false
        if (md5 != other.md5) return false
        if (isHeadered != other.isHeadered) return false
        if (headerlessBytes != null) {
            if (other.headerlessBytes == null) return false
            if (!headerlessBytes.contentEquals(other.headerlessBytes)) return false
        } else if (other.headerlessBytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isValid.hashCode()
        result = 31 * result + statusMessage.hashCode()
        result = 31 * result + sha1.hashCode()
        result = 31 * result + md5.hashCode()
        result = 31 * result + isHeadered.hashCode()
        result = 31 * result + (headerlessBytes?.contentHashCode() ?: 0)
        return result
    }
}

object BaseRomValidator {

    // Standard Super Metroid (JU) Checksums for headerless ROM (3,145,728 bytes)
    const val SUPER_METROID_JU_SHA1 = "da957f0d6323daab28af8d24db85e5792d0003c8"
    const val SUPER_METROID_JU_MD5 = "21f3e98df4780ee1c667b84e57d88675"

    fun validate(romBytes: ByteArray): RomValidationResult {
        if (romBytes.isEmpty()) {
            return RomValidationResult(
                isValid = false,
                statusMessage = "Empty ROM file"
            )
        }

        var isHeadered = false
        var cleanBytes = romBytes

        // Check for 512-byte SMC header
        if ((romBytes.size % 1024 == 512) || romBytes.size == 3146240) {
            isHeadered = true
            cleanBytes = romBytes.copyOfRange(512, romBytes.size)
        }

        val sha1 = computeHash(cleanBytes, "SHA-1")
        val md5 = computeHash(cleanBytes, "MD5")

        val isJuMatch = sha1.equals(SUPER_METROID_JU_SHA1, ignoreCase = true) ||
                md5.equals(SUPER_METROID_JU_MD5, ignoreCase = true)

        if (isJuMatch) {
            return RomValidationResult(
                isValid = true,
                statusMessage = "Valid Super Metroid JU ROM",
                sha1 = sha1,
                md5 = md5,
                isHeadered = isHeadered,
                headerlessBytes = cleanBytes
            )
        }

        // Check if size is roughly 3MB and contains SUPER METROID string in internal title
        val hasSuperMetroidTitle = cleanBytes.size >= 0x8000 && (
                containsAscii(cleanBytes, 0x7FC0, "SUPER METROID") ||
                        containsAscii(cleanBytes, 0xFFC0, "SUPER METROID")
                )

        if (hasSuperMetroidTitle) {
            return RomValidationResult(
                isValid = false,
                statusMessage = "Super Metroid ROM found, but hash mismatch",
                sha1 = sha1,
                md5 = md5,
                isHeadered = isHeadered,
                headerlessBytes = cleanBytes
            )
        }

        return RomValidationResult(
            isValid = false,
            statusMessage = "Invalid ROM (Expected Super Metroid JU)",
            sha1 = sha1,
            md5 = md5,
            isHeadered = isHeadered,
            headerlessBytes = cleanBytes
        )
    }

    private fun containsAscii(bytes: ByteArray, offset: Int, search: String): Boolean {
        if (offset + search.length > bytes.size) return false
        val searchBytes = search.toByteArray(Charsets.US_ASCII)
        for (i in searchBytes.indices) {
            if (bytes[offset + i] != searchBytes[i]) return false
        }
        return true
    }

    private fun computeHash(bytes: ByteArray, algorithm: String): String {
        val digest = MessageDigest.getInstance(algorithm)
        val hashBytes = digest.digest(bytes)
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
