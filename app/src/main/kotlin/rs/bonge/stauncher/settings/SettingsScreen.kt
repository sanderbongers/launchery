package rs.bonge.stauncher.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import rs.bonge.stauncher.R
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.home.rememberArt
import rs.bonge.stauncher.ui.FocusStyle
import rs.bonge.stauncher.ui.FocusSurface

private val ScreenHorizontalPadding = 48.dp
private val ScreenVerticalPadding = 27.dp
private val RowHeight = 56.dp
private val RowSpacing = 8.dp
private val RowCornerRadius = 8.dp
private val RowIconSize = 40.dp

// The row's secondary text keeps its contrast whether the row is dark or, focused, light.
private const val SECONDARY_ALPHA = 0.7f

private enum class Page { Main, HiddenApps }

/**
 * The settings: a main page with a link to the system settings and one to the hidden apps, and the hidden apps page,
 * where select puts an app back on the home row. Back on the hidden apps page returns to the main page, with focus on
 * the row that led there.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    bannerLoader: BannerLoader,
    onUnhide: (InstalledApp) -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by rememberSaveable { mutableStateOf(Page.Main) }
    var cameBack by remember { mutableStateOf(false) }
    BackHandler(enabled = page == Page.HiddenApps) {
        cameBack = true
        page = Page.Main
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = ScreenHorizontalPadding, vertical = ScreenVerticalPadding),
    ) {
        when (page) {
            Page.Main -> {
                MainPage(
                    focusHiddenApps = cameBack,
                    onOpenSystemSettings = onOpenSystemSettings,
                    onOpenHiddenApps = {
                        cameBack = false
                        page = Page.HiddenApps
                    },
                )
            }

            Page.HiddenApps -> {
                HiddenAppsPage(state, bannerLoader, onUnhide)
            }
        }
    }
}

@Composable
private fun MainPage(
    focusHiddenApps: Boolean,
    onOpenSystemSettings: () -> Unit,
    onOpenHiddenApps: () -> Unit,
) {
    val allSettingsFocus = remember { FocusRequester() }
    val hiddenAppsFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { (if (focusHiddenApps) hiddenAppsFocus else allSettingsFocus).requestFocus() }

    PageTitle(stringResource(R.string.stauncher_settings))
    Column(verticalArrangement = Arrangement.spacedBy(RowSpacing)) {
        SettingsRow(onClick = onOpenSystemSettings, modifier = Modifier.focusRequester(allSettingsFocus)) {
            RowLabel(stringResource(R.string.all_settings))
        }
        SettingsRow(onClick = onOpenHiddenApps, modifier = Modifier.focusRequester(hiddenAppsFocus)) {
            RowLabel(stringResource(R.string.hidden_apps))
        }
    }
}

/**
 * After an app is unhidden focus stays at the same place in the list. When the list is empty there is nothing left to
 * focus, and Back leaves the page.
 */
@Composable
private fun HiddenAppsPage(
    state: SettingsUiState,
    bannerLoader: BannerLoader,
    onUnhide: (InstalledApp) -> Unit,
) {
    val listState = rememberLazyListState()
    var unhiddenIndex by remember { mutableStateOf<Int?>(null) }
    var pendingFocus by remember { mutableStateOf<Int?>(if (state.hidden.isEmpty()) null else 0) }

    LaunchedEffect(state.hidden) {
        val index = unhiddenIndex ?: return@LaunchedEffect
        unhiddenIndex = null
        if (state.hidden.isNotEmpty()) pendingFocus = index.coerceAtMost(state.hidden.lastIndex)
    }
    // An off-screen target has to be composed before it can take focus.
    LaunchedEffect(pendingFocus) { pendingFocus?.let { listState.scrollToItem(it) } }

    PageTitle(stringResource(R.string.hidden_apps))
    if (state.loaded && state.hidden.isEmpty()) {
        Text(
            text = stringResource(R.string.no_hidden_apps),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 18.sp,
        )
    }
    LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(RowSpacing)) {
        itemsIndexed(state.hidden, key = { _, app -> app.packageName }) { index, app ->
            val focusRequester = remember { FocusRequester() }
            if (pendingFocus == index) {
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                    pendingFocus = null
                }
            }
            HiddenAppRow(
                app = app,
                bannerLoader = bannerLoader,
                onClick = {
                    unhiddenIndex = index
                    onUnhide(app)
                },
                modifier = Modifier.focusRequester(focusRequester),
            )
        }
    }
}

@Composable
private fun PageTitle(text: String) {
    Text(text = text, color = MaterialTheme.colorScheme.onSurface, fontSize = 28.sp)
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun RowLabel(text: String) {
    Text(text = text, fontSize = 18.sp)
}

@Composable
private fun HiddenAppRow(
    app: InstalledApp,
    bannerLoader: BannerLoader,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconPx = with(LocalDensity.current) { RowIconSize.roundToPx() }
    val icon = rememberArt(app, bannerLoader, iconPx, iconPx, iconOnly = true)
    SettingsRow(onClick = onClick, modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(RowIconSize)) {
                icon?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.size(RowIconSize)) }
            }
            Text(
                text = app.label,
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            )
            Text(
                text = stringResource(R.string.unhide),
                color = LocalContentColor.current.copy(alpha = SECONDARY_ALPHA),
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun SettingsRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FocusSurface(
        onClick = onClick,
        shape = RoundedCornerShape(RowCornerRadius),
        style = FocusStyle.Fill,
        modifier = modifier.fillMaxWidth().height(RowHeight),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
            content()
        }
    }
}
