package com.maprandroid.data.model

object SpriteCatalog {
    val DEFAULT_SPRITES: List<String> = listOf(
        "samus_vanilla",
        "samus_fusion",
        "samus_dread",
        "samus_pixel"
    )

    private val dynamicSpritesList = mutableListOf<String>()

    fun updateSprites(sprites: List<String>) {
        val cleanList = sprites
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (cleanList.isEmpty()) return

        val orderedList = mutableListOf<String>()
        if (cleanList.contains("samus_vanilla")) {
            orderedList.add("samus_vanilla")
            orderedList.addAll(cleanList.filter { it != "samus_vanilla" })
        } else {
            orderedList.add("samus_vanilla")
            orderedList.addAll(cleanList)
        }

        synchronized(dynamicSpritesList) {
            dynamicSpritesList.clear()
            dynamicSpritesList.addAll(orderedList.distinct())
        }
    }

    val availableSprites: List<String>
        get() {
            synchronized(dynamicSpritesList) {
                if (dynamicSpritesList.isNotEmpty()) {
                    return dynamicSpritesList.toList()
                }
            }
            return DEFAULT_SPRITES
        }
}
