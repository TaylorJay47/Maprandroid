package com.maprandroid

import com.maprandroid.data.model.SpriteCatalog
import org.junit.Assert.*
import org.junit.Test

class SpriteCatalogTest {

    @Test
    fun defaultSprites_containsExpectedDefaults() {
        val available = SpriteCatalog.availableSprites
        assertTrue(available.contains("samus_vanilla"))
        assertTrue(available.contains("samus_fusion"))
        assertTrue(available.contains("samus_dread"))
        assertTrue(available.contains("samus_pixel"))
    }

    @Test
    fun updateSprites_sanitizesAndEnsuresVanillaFirst() {
        SpriteCatalog.updateSprites(listOf("samus_dread", "samus_fusion", "samus_custom", "samus_dread"))
        val updated = SpriteCatalog.availableSprites

        assertEquals("samus_vanilla", updated.first())
        assertTrue(updated.contains("samus_custom"))
        assertEquals(updated.size, updated.distinct().size)
    }
}
