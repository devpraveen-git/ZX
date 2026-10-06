package com.example.callscribe.ui

sealed class Screen {
    data object Home : Screen()
    data object Permissions : Screen()
    data class CallDetail(val recordingId: Long) : Screen()
    data object Search : Screen()
    data object Settings : Screen()
    data object ModelManager : Screen()
}
