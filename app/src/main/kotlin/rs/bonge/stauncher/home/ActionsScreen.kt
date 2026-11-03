package rs.bonge.stauncher.home

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import rs.bonge.stauncher.R
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.ui.FocusStyle
import rs.bonge.stauncher.ui.FocusSurface

// The measurements are those of the "app held" screen of Google TV.
private val PreviewStart = 127.dp
private val ContentTop = 124.dp
private val PreviewWidth = 195.dp
private val PreviewHeight = 110.dp
private val PreviewCornerRadius = 12.dp
private val PreviewLabelGap = 14.dp
private val ActionsStart = 556.dp
private val ActionWidth = 268.dp
private val ActionHeight = 52.dp
private val ActionGap = 16.dp
private val ActionTextStart = 24.dp
private const val FOCUSED_SCALE = 1.05f
private val FocusedGlow = Glow(elevationColor = Color.Black.copy(alpha = 0.5f), elevation = 12.dp)

/**
 * The actions for one app, on a screen of their own: its banner, as Google TV shows it whatever the row looks like,
 * with its name below, and a column of buttons. Focus stays inside the buttons until the screen closes. App info comes first because Force stop, which
 * only the system can do, is one step further in. Uninstall is offered only for apps the user installed, and Move only
 * when there is another app to swap places with.
 */
@Composable
fun ActionsScreen(
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

    // The select press that opened the screen may still be held. Its repeats and its release must not reach the first
    // action, so select is swallowed until a fresh press begins here.
    var armed by remember { mutableStateOf(false) }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .onPreviewKeyEvent { event ->
                    if (!event.isSelect()) return@onPreviewKeyEvent false
                    if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) armed = true
                    !armed
                },
    ) {
        Column(
            modifier = Modifier.padding(start = PreviewStart, top = ContentTop).width(PreviewWidth),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Preview(app, bannerLoader)
            Text(
                text = app.label,
                color = colorResource(R.color.preview_label),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = PreviewLabelGap),
            )
        }
        Column(
            modifier =
                Modifier
                    .padding(start = ActionsStart, top = ContentTop)
                    .width(ActionWidth)
                    .focusProperties { onExit = { cancelFocusChange() } }
                    .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(ActionGap),
        ) {
            val actions =
                buildList {
                    add(stringResource(R.string.action_app_info) to onAppInfo)
                    if (canMove) add(stringResource(R.string.action_move) to onMove)
                    add(stringResource(R.string.action_hide) to onHide)
                    if (!app.system) add(stringResource(R.string.action_uninstall) to onUninstall)
                }
            actions.forEachIndexed { index, (text, onClick) ->
                ActionButton(
                    text = text,
                    onClick = onClick,
                    modifier = if (index == 0) Modifier.focusRequester(firstAction) else Modifier,
                )
            }
        }
    }
}

/** The banner of the app, or its icon on a plain card when it has no banner. */
@Composable
private fun Preview(
    app: InstalledApp,
    bannerLoader: BannerLoader,
) {
    Box(
        Modifier
            .size(PreviewWidth, PreviewHeight)
            .clip(RoundedCornerShape(PreviewCornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (app.hasBanner) {
            BannerArt(app, bannerLoader, PreviewWidth, PreviewHeight)
        } else {
            FlatIconArt(app, bannerLoader, PreviewWidth, PreviewHeight)
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusSurface(
        onClick = onClick,
        shape = CircleShape,
        style = FocusStyle.Fill,
        modifier = modifier.fillMaxWidth().height(ActionHeight),
        containerColor = colorResource(R.color.action_pill),
        focusedScale = FOCUSED_SCALE,
        focusedGlow = FocusedGlow,
    ) {
        Box(Modifier.fillMaxSize().padding(start = ActionTextStart), contentAlignment = Alignment.CenterStart) {
            Text(text = text, fontSize = 16.sp, maxLines = 1)
        }
    }
}

private fun KeyEvent.isSelect() = key == Key.DirectionCenter || key == Key.Enter || key == Key.NumPadEnter
