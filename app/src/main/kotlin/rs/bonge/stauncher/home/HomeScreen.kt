package rs.bonge.stauncher.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import rs.bonge.stauncher.R
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.InstalledApp

/** What the home screen asks of its host. */
class HomeCallbacks(
    val onLaunch: (InstalledApp) -> Unit,
    val onOpenActions: (InstalledApp) -> Unit,
    /** Back: closes the actions panel, or puts a picked-up app back. */
    val onDismiss: () -> Unit,
    val onStartMove: () -> Unit,
    val onMoveBy: (Int) -> Unit,
    val onDrop: () -> Unit,
    val onHide: () -> Unit,
    val onAppInfo: (InstalledApp) -> Unit,
    val onUninstall: (InstalledApp) -> Unit,
    val onOpenSettings: () -> Unit,
    val onOpenSystemSettings: () -> Unit,
)

@Composable
fun HomeScreen(
    state: HomeUiState,
    bannerLoader: BannerLoader,
    callbacks: HomeCallbacks,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = state.mode != HomeMode.Browsing, onBack = callbacks.onDismiss)

    // With no app to focus, the settings button is the way in, so it takes focus.
    val settingsFocus = remember { FocusRequester() }
    val rowEmpty = state.loaded && state.apps.isEmpty()
    LaunchedEffect(rowEmpty) { if (rowEmpty) settingsFocus.requestFocus() }

    Box(modifier = modifier.fillMaxSize().background(colorResource(R.color.background))) {
        TopBar(
            onOpenSettings = callbacks.onOpenSettings,
            onOpenSystemSettings = callbacks.onOpenSystemSettings,
            settingsFocus = settingsFocus,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        if (rowEmpty) {
            Text(
                text = stringResource(R.string.no_apps),
                color = colorResource(R.color.text_secondary),
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            AppRow(
                apps = state.apps,
                mode = state.mode,
                bannerLoader = bannerLoader,
                onLaunch = callbacks.onLaunch,
                onOpenActions = callbacks.onOpenActions,
                onMoveBy = callbacks.onMoveBy,
                onDrop = callbacks.onDrop,
                modifier = Modifier.align(Alignment.Center).fillMaxWidth(),
            )
        }
        if (state.mode is HomeMode.Moving) {
            Text(
                text = stringResource(R.string.move_hint),
                color = colorResource(R.color.text_secondary),
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp),
            )
        }
        (state.mode as? HomeMode.Actions)?.let { actions ->
            ActionsPanel(
                app = actions.app,
                canMove = state.apps.size > 1,
                bannerLoader = bannerLoader,
                onMove = callbacks.onStartMove,
                onHide = callbacks.onHide,
                onAppInfo = { callbacks.onAppInfo(actions.app) },
                onUninstall = { callbacks.onUninstall(actions.app) },
            )
        }
    }
}
