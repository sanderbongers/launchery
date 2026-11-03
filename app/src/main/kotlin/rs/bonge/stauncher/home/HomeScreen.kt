package rs.bonge.stauncher.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import rs.bonge.stauncher.R
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.ui.ScreenMargin

private val HeadingSize = 32.sp

// The stock launcher keeps about 34 dp between the base of its heading and the icons; the row itself has 16 dp.
private val HeadingGap = 11.dp

/** What the home screen asks of its host. */
class HomeCallbacks(
    val onLaunch: (InstalledApp) -> Unit,
    val onOpenActions: (InstalledApp) -> Unit,
    /**
     * Closes the settings or the actions screen, or puts a picked-up app back. It runs on Back, and before App info and
     * Uninstall.
     */
    val onDismiss: () -> Unit,
    val onStartMove: () -> Unit,
    val onMoveBy: (Int) -> Unit,
    val onDrop: () -> Unit,
    val onHide: () -> Unit,
    val onAppInfo: (InstalledApp) -> Unit,
    val onUninstall: (InstalledApp) -> Unit,
    val onOpenSettings: () -> Unit,
    val onToggleAppStyle: () -> Unit,
    val onUnhide: (InstalledApp) -> Unit,
    val onOpenSystemSettings: () -> Unit,
)

/** The keys in the text colour and what they do in the muted one, so that the keys stand out. */
@Composable
private fun MoveHint(modifier: Modifier = Modifier) {
    val keyColor = MaterialTheme.colorScheme.onSurface
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val steps =
        listOf(
            stringResource(R.string.hint_key_move) to stringResource(R.string.hint_label_move),
            stringResource(R.string.hint_key_place) to stringResource(R.string.hint_label_place),
            stringResource(R.string.hint_key_cancel) to stringResource(R.string.hint_label_cancel),
        )
    val text =
        buildAnnotatedString {
            steps.forEachIndexed { index, (key, label) ->
                if (index > 0) withStyle(SpanStyle(color = labelColor)) { append(" · ") }
                withStyle(SpanStyle(color = keyColor)) { append(key) }
                withStyle(SpanStyle(color = labelColor)) { append(" $label") }
            }
        }
    Text(text = text, fontSize = 16.sp, modifier = modifier)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    time: String,
    bannerLoader: BannerLoader,
    callbacks: HomeCallbacks,
    modifier: Modifier = Modifier,
) {
    // Back puts away what is open, and otherwise does nothing: there is nowhere further back than the home screen.
    BackHandler { if (state.mode != HomeMode.Browsing) callbacks.onDismiss() }

    // With no app to focus, the settings button is the way in, so it takes focus.
    val settingsFocus = remember { FocusRequester() }
    val rowEmpty = state.loaded && state.apps.isEmpty()
    LaunchedEffect(rowEmpty) { if (rowEmpty) settingsFocus.requestFocus() }

    // The settings sheet takes focus while it is open. When it closes the button that opened it gets focus back. This
    // comes before the row's effects, so that the first app still wins when the Home button closes the sheet.
    val settingsOpen = state.mode is HomeMode.Settings
    val settingsWereOpen = remember { booleanArrayOf(false) }
    LaunchedEffect(settingsOpen) {
        if (settingsWereOpen[0] && !settingsOpen) settingsFocus.requestFocus()
        settingsWereOpen[0] = settingsOpen
    }

    val glow = rememberGlowState()
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).glow(glow)) {
        TopBar(
            time = time,
            onOpenSettings = callbacks.onOpenSettings,
            onOpenSystemSettings = callbacks.onOpenSystemSettings,
            settingsFocus = settingsFocus,
            focusable = state.loaded,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        if (rowEmpty) {
            Text(
                text = stringResource(R.string.no_apps),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 20.sp,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            Column(Modifier.align(Alignment.Center).fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.apps),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = HeadingSize,
                    maxLines = 1,
                    modifier = Modifier.padding(start = ScreenMargin, bottom = HeadingGap),
                )
                AppRow(
                    apps = state.apps,
                    style = state.appStyle,
                    mode = state.mode,
                    homeRequests = state.homeRequests,
                    bannerLoader = bannerLoader,
                    onLaunch = callbacks.onLaunch,
                    onOpenActions = callbacks.onOpenActions,
                    onMoveBy = callbacks.onMoveBy,
                    onDrop = callbacks.onDrop,
                    modifier = Modifier.fillMaxWidth(),
                    glow = glow,
                )
            }
        }
        if (state.mode is HomeMode.Moving) {
            MoveHint(Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp))
        }
        (state.mode as? HomeMode.Actions)?.let { actions ->
            ActionsScreen(
                app = actions.app,
                canMove = state.apps.size > 1,
                bannerLoader = bannerLoader,
                onMove = callbacks.onStartMove,
                onHide = callbacks.onHide,
                onAppInfo = {
                    callbacks.onDismiss()
                    callbacks.onAppInfo(actions.app)
                },
                onUninstall = {
                    callbacks.onDismiss()
                    callbacks.onUninstall(actions.app)
                },
            )
        }
        if (settingsOpen) {
            SettingsSheet(
                hidden = state.hidden,
                appStyle = state.appStyle,
                bannerLoader = bannerLoader,
                onToggleAppStyle = callbacks.onToggleAppStyle,
                onUnhide = callbacks.onUnhide,
                onOpenSystemSettings = callbacks.onOpenSystemSettings,
            )
        }
    }
}
