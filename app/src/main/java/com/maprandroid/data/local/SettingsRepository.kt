package com.maprandroid.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.maprandroid.util.EmulatorInfo
import com.maprandroid.util.EmulatorLauncher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

open class SettingsRepository(private val context: Context) {
    companion object {
        val EMULATOR_PACKAGE_NAME = stringPreferencesKey("emulator_package_name")
        val BASE_ROM_URI = stringPreferencesKey("base_rom_uri")
        val BASE_ROM_NAME = stringPreferencesKey("base_rom_name")
        val BASE_ROM_STATUS = stringPreferencesKey("base_rom_status")
        val IS_BASE_ROM_VALID = booleanPreferencesKey("is_base_rom_valid")
        val SAVED_SPRITES = stringPreferencesKey("saved_sprites")
        val FAVORITE_SPRITE = stringPreferencesKey("favorite_sprite")
        val DEFAULT_CONTROL_SHOT = stringPreferencesKey("default_control_shot")
        val DEFAULT_CONTROL_JUMP = stringPreferencesKey("default_control_jump")
        val DEFAULT_CONTROL_DASH = stringPreferencesKey("default_control_dash")
        val DEFAULT_CONTROL_ITEM_SELECT = stringPreferencesKey("default_control_item_select")
        val DEFAULT_CONTROL_ITEM_CANCEL = stringPreferencesKey("default_control_item_cancel")
        val DEFAULT_CONTROL_ANGLE_UP = stringPreferencesKey("default_control_angle_up")
        val DEFAULT_CONTROL_ANGLE_DOWN = stringPreferencesKey("default_control_angle_down")
    }

    open val emulatorPackageName: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[EMULATOR_PACKAGE_NAME]
        }

    open val baseRomUri: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[BASE_ROM_URI]
        }

    open val baseRomName: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[BASE_ROM_NAME]
        }

    open val baseRomStatus: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[BASE_ROM_STATUS]
        }

    open val isBaseRomValid: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_BASE_ROM_VALID] ?: false
        }

    open val savedSprites: Flow<List<String>> = context.dataStore.data
        .map { preferences ->
            val raw = preferences[SAVED_SPRITES]
            if (!raw.isNullOrBlank()) {
                raw.split(",").map { it.trim() }.filter { it.isNotBlank() }
            } else {
                emptyList()
            }
        }

    open val favoriteSprite: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[FAVORITE_SPRITE] ?: "samus_vanilla"
        }

    open val defaultControls: Flow<DefaultControls> = context.dataStore.data
        .map { preferences ->
            DefaultControls(
                shot = preferences[DEFAULT_CONTROL_SHOT] ?: "X",
                jump = preferences[DEFAULT_CONTROL_JUMP] ?: "A",
                dash = preferences[DEFAULT_CONTROL_DASH] ?: "B",
                itemSelect = preferences[DEFAULT_CONTROL_ITEM_SELECT] ?: "Select",
                itemCancel = preferences[DEFAULT_CONTROL_ITEM_CANCEL] ?: "Y",
                angleUp = preferences[DEFAULT_CONTROL_ANGLE_UP] ?: "R",
                angleDown = preferences[DEFAULT_CONTROL_ANGLE_DOWN] ?: "L"
            )
        }

    open fun getAvailableEmulators(): List<EmulatorInfo> {
        return EmulatorLauncher.getAvailableEmulators(context)
    }

    open suspend fun saveEmulatorPackageName(packageName: String) {
        context.dataStore.edit { preferences ->
            preferences[EMULATOR_PACKAGE_NAME] = packageName
        }
    }

    open suspend fun saveBaseRomInfo(uri: String, name: String, status: String, isValid: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BASE_ROM_URI] = uri
            preferences[BASE_ROM_NAME] = name
            preferences[BASE_ROM_STATUS] = status
            preferences[IS_BASE_ROM_VALID] = isValid
        }
    }

    open suspend fun clearBaseRom() {
        context.dataStore.edit { preferences ->
            preferences.remove(BASE_ROM_URI)
            preferences.remove(BASE_ROM_NAME)
            preferences.remove(BASE_ROM_STATUS)
            preferences[IS_BASE_ROM_VALID] = false
        }
    }

    open suspend fun saveSprites(sprites: List<String>) {
        context.dataStore.edit { preferences ->
            preferences[SAVED_SPRITES] = sprites.joinToString(",")
        }
    }

    open suspend fun saveFavoriteSprite(sprite: String) {
        context.dataStore.edit { preferences ->
            preferences[FAVORITE_SPRITE] = sprite
        }
    }

    open suspend fun saveDefaultControls(controls: DefaultControls) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_CONTROL_SHOT] = controls.shot
            preferences[DEFAULT_CONTROL_JUMP] = controls.jump
            preferences[DEFAULT_CONTROL_DASH] = controls.dash
            preferences[DEFAULT_CONTROL_ITEM_SELECT] = controls.itemSelect
            preferences[DEFAULT_CONTROL_ITEM_CANCEL] = controls.itemCancel
            preferences[DEFAULT_CONTROL_ANGLE_UP] = controls.angleUp
            preferences[DEFAULT_CONTROL_ANGLE_DOWN] = controls.angleDown
        }
    }
}
