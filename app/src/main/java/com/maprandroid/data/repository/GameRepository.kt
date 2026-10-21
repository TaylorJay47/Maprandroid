package com.maprandroid.data.repository

import com.maprandroid.data.local.GameDao
import com.maprandroid.data.local.GameEntity
import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    val allGames: Flow<List<GameEntity>> = gameDao.getAllGames()

    suspend fun insert(game: GameEntity) {
        gameDao.insertGame(game)
    }

    suspend fun getGameById(gameId: Int): GameEntity? {
        return gameDao.getGameById(gameId)
    }

    suspend fun delete(gameId: Int) {
        gameDao.deleteGame(gameId)
    }

    suspend fun setGameCompleted(gameId: Int, isCompleted: Boolean) {
        gameDao.updateGameCompletion(gameId, isCompleted)
    }
}
