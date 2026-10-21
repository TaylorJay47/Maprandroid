package com.maprandroid.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConfigProfileDao {
    @Query("SELECT * FROM config_profiles ORDER BY updatedAt DESC")
    fun getAllProfiles(): Flow<List<ConfigProfileEntity>>

    @Query("SELECT * FROM config_profiles WHERE id = :id")
    suspend fun getProfileById(id: Int): ConfigProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ConfigProfileEntity): Long

    @Update
    suspend fun updateProfile(profile: ConfigProfileEntity)

    @Delete
    suspend fun deleteProfile(profile: ConfigProfileEntity)

    @Query("DELETE FROM config_profiles WHERE id = :id")
    suspend fun deleteProfileById(id: Int)
}
