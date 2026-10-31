package rs.bonge.stauncher.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import rs.bonge.stauncher.R
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.home.rememberArt

private val ScreenHorizontalPadding = 48.dp
private val ScreenVerticalPadding = 27.dp
private val RowHeight = 56.dp
private val RowCornerRadius = 8.dp
private val RowIconSize = 40.dp
private val FocusBorderWidth = 3.dp

/**
 * The settings: a link to the system Settings, and the hidden apps, which select puts back on the home row. After an
 * app is unhidden focus stays at the same place in the list, or moves to the link when the list is empty.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    bannerLoader: BannerLoader,
    onUnhide: (InstalledApp) -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val systemSettingsFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    var unhiddenIndex by remember { mutableStateOf<Int?>(null) }
    var pendingFocus by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) { systemSettingsFocus.requestFocus() }
    LaunchedEffect(state.hidden) {
        val index = unhiddenIndex ?: return@LaunchedEffect
        unhiddenIndex = null
        if (state.hidden.isEmpty()) {
            systemSettingsFocus.requestFocus()
        } else {
            pendingFocus = index.coerceAtMost(state.hidden.lastIndex)
        }
    }
    // An off-screen target has to be composed before it can take focus.
    LaunchedEffect(pendingFocus) { pendingFocus?.let { listState.scrollToItem(it) } }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(colorResource(R.color.background))
                .padding(horizontal = ScreenHorizontalPadding, vertical = ScreenVerticalPadding),
    ) {
        Text(
            text = stringResource(R.string.stauncher_settings),
            color = colorResource(R.color.text_primary),
            fontSize = 28.sp,
        )
        Spacer(Modifier.height(24.dp))
        SettingsRow(onClick = onOpenSystemSettings, modifier = Modifier.focusRequester(systemSettingsFocus)) {
            Text(
                text = stringResource(R.string.system_settings),
                color = colorResource(R.color.text_primary),
                fontSize = 18.sp,
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.hidden_apps),
            color = colorResource(R.color.text_secondary),
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(8.dp))
        if (state.loaded && state.hidden.isEmpty()) {
            Text(
                text = stringResource(R.string.no_hidden_apps),
                color = colorResource(R.color.text_secondary),
                fontSize = 18.sp,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                color = colorResource(R.color.text_primary),
                fontSize = 18.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            )
            Text(
                text = stringResource(R.string.unhide),
                color = colorResource(R.color.text_secondary),
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
    val shape = RoundedCornerShape(RowCornerRadius)
    val surfaceColor = colorResource(R.color.card_surface)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(RowHeight),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = surfaceColor,
                focusedContainerColor = surfaceColor,
                pressedContainerColor = surfaceColor,
            ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border =
            ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(FocusBorderWidth, Color.White), shape = shape),
            ),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
            content()
        }
    }
}
