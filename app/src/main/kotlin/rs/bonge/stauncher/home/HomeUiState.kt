package rs.bonge.stauncher.home

import rs.bonge.stauncher.apps.InstalledApp

data class HomeUiState(
    val apps: List<InstalledApp> = emptyList(),
)
