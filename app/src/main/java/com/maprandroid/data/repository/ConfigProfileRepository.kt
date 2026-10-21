package com.maprandroid.data.repository

import com.maprandroid.data.local.ConfigProfileDao
import com.maprandroid.data.local.ConfigProfileEntity
import kotlinx.coroutines.flow.Flow

class ConfigProfileRepository(private val dao: ConfigProfileDao) {

    val allProfiles: Flow<List<ConfigProfileEntity>> = dao.getAllProfiles()

    suspend fun getProfileById(id: Int): ConfigProfileEntity? {
        return dao.getProfileById(id)
    }

    suspend fun saveProfile(profile: ConfigProfileEntity): Long {
        return if (profile.id > 0) {
            dao.updateProfile(profile.copy(updatedAt = System.currentTimeMillis()))
            profile.id.toLong()
        } else {
            dao.insertProfile(profile.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteProfile(id: Int) {
        dao.deleteProfileById(id)
    }

    suspend fun seedDefaultProfilesIfEmpty(existingProfiles: List<ConfigProfileEntity>) {
        if (existingProfiles.isEmpty()) {
            val defaults = listOf(
                com.maprandroid.data.model.PresetCatalog.CASUAL_EXPLORER.profile,
                com.maprandroid.data.model.PresetCatalog.STANDARD.profile.copy(name = "Standard"),
                com.maprandroid.data.model.PresetCatalog.EXPERT_CHALLENGE.profile.copy(name = "Expert Challenge")
            )
            for (p in defaults) {
                dao.insertProfile(p)
            }
        }
    }
}
