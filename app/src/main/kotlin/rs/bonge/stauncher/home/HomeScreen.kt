package rs.bonge.stauncher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import rs.bonge.stauncher.R
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.InstalledApp

@Composable
fun HomeScreen(
    state: HomeUiState,
    bannerLoader: BannerLoader,
    onLaunch: (InstalledApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().background(colorResource(R.color.background)),
        contentAlignment = Alignment.Center,
    ) {
        AppRow(
            apps = state.apps,
            bannerLoader = bannerLoader,
            onLaunch = onLaunch,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
