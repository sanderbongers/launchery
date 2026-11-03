package rs.bonge.stauncher.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import rs.bonge.stauncher.R
import rs.bonge.stauncher.apps.ArtKind
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.ui.FocusStyle
import rs.bonge.stauncher.ui.FocusSurface

private val PanelWidth = 360.dp
private val PanelPadding = 24.dp
private val PanelCornerRadius = 16.dp
private val TitleIconSize = 48.dp
private val ActionHeight = 48.dp
private val ActionCornerRadius = 8.dp

/**
 * The actions for one app, centred over the home screen. Focus stays inside it until it closes. App info comes first
 * because Force stop, which only the system can do, is one step further in. Uninstall is offered only for apps the
 * user installed, and Move only when there is another app to swap places with.
 */
@Composable
fun ActionsPanel(
    app: InstalledApp,
    canMove: Boolean,
    bannerLoader: BannerLoader,
    onMove: () -> Unit,
    onHide: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstAction = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstAction.requestFocus() }

    // The select press that opened the panel may still be held. Its repeats and its release must not reach the first
    // action, so select is swallowed until a fresh press begins in the panel.
    var armed by remember { mutableStateOf(false) }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
                .onPreviewKeyEvent { event ->
                    if (!event.isSelect()) return@onPreviewKeyEvent false
                    if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) armed = true
                    !armed
                },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .width(PanelWidth)
                    .clip(RoundedCornerShape(PanelCornerRadius))
                    .background(colorResource(R.color.panel_surface))
                    .padding(PanelPadding)
                    .focusProperties { onExit = { cancelFocusChange() } }
                    .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Title(app, bannerLoader)
            Spacer(Modifier.height(8.dp))
            val actions =
                buildList {
                    add(stringResource(R.string.action_app_info) to onAppInfo)
                    if (canMove) add(stringResource(R.string.action_move) to onMove)
                    add(stringResource(R.string.action_hide) to onHide)
                    if (!app.system) add(stringResource(R.string.action_uninstall) to onUninstall)
                }
            actions.forEachIndexed { index, (text, onClick) ->
                ActionItem(
                    text = text,
                    onClick = onClick,
                    modifier = if (index == 0) Modifier.focusRequester(firstAction) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun Title(
    app: InstalledApp,
    bannerLoader: BannerLoader,
) {
    val iconPx = with(LocalDensity.current) { TitleIconSize.roundToPx() }
    val icon = rememberArt(app, bannerLoader, iconPx, iconPx, ArtKind.Icon)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(TitleIconSize)) {
            icon?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.size(TitleIconSize)) }
        }
        Text(
            text = app.label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp),
        )
    }
}

@Composable
private fun ActionItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusSurface(
        onClick = onClick,
        shape = RoundedCornerShape(ActionCornerRadius),
        style = FocusStyle.Fill,
        modifier = modifier.fillMaxWidth().height(ActionHeight),
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentAlignment = Alignment.CenterStart) {
            Text(text = text, fontSize = 18.sp)
        }
    }
}

private fun KeyEvent.isSelect() = key == Key.DirectionCenter || key == Key.Enter || key == Key.NumPadEnter
