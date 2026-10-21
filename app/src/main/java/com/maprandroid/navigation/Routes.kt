package com.maprandroid.navigation

import kotlinx.serialization.Serializable
import androidx.navigation3.runtime.NavKey

@Serializable
data object PresetsRoute : NavKey

@Serializable
data object GenerateRoute : NavKey

@Serializable
data object LibraryListRoute : NavKey

@Serializable
data class LibraryDetailRoute(val gameId: Int) : NavKey

@Serializable
data object SettingsRoute : NavKey
