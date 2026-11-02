package rs.bonge.stauncher.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.ui.FocusStyle
import rs.bonge.stauncher.ui.FocusSurface

internal val CardWidth = 192.dp
internal val CardHeight = 108.dp
private val CardCornerRadius = 12.dp
private val IconSize = 48.dp
private val LabelAreaHeight = 32.dp
private val LabelTopGap = 12.dp
private const val FOCUSED_SCALE = 1.1f
private const val LIFTED_SCALE = 1.2f

/**
 * A banner card shows its name below it while focused. An icon-only card, for an app without a banner, shows its name
 * inside the card at all times. A [lifted] card, one that is being moved, grows a little further than a focused one.
 * [itemModifier] goes on the outermost layout, where a lazy list reads its item modifiers; [modifier] goes on the
 * focusable card.
 */
@Composable
fun AppCard(
    app: InstalledApp,
    bannerLoader: BannerLoader,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    lifted: Boolean,
    modifier: Modifier = Modifier,
    itemModifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }

    Column(
        // A lifted card grows past the gap to its neighbours, so it is drawn over them.
        modifier = itemModifier.width(CardWidth).zIndex(if (lifted) 1f else 0f),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FocusSurface(
            onClick = onClick,
            onLongClick = onLongClick,
            shape = RoundedCornerShape(CardCornerRadius),
            style = FocusStyle.Ring,
            modifier =
                modifier
                    .size(CardWidth, CardHeight)
                    .onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = app.label },
            focusedScale = if (lifted) LIFTED_SCALE else FOCUSED_SCALE,
        ) {
            if (app.hasBanner) {
                BannerArt(app, bannerLoader)
            } else {
                IconAndName(app, bannerLoader)
            }
        }
        Box(
            modifier = Modifier.height(LabelAreaHeight).padding(top = LabelTopGap),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (app.hasBanner && focused) {
                Text(
                    text = app.label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BannerArt(
    app: InstalledApp,
    bannerLoader: BannerLoader,
) {
    val density = LocalDensity.current
    val widthPx = with(density) { CardWidth.roundToPx() }
    val heightPx = with(density) { CardHeight.roundToPx() }
    val art = rememberArt(app, bannerLoader, widthPx, heightPx)
    art?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            modifier = Modifier.size(CardWidth, CardHeight),
            contentScale = ContentScale.FillBounds,
        )
    }
}

@Composable
private fun IconAndName(
    app: InstalledApp,
    bannerLoader: BannerLoader,
) {
    val iconPx = with(LocalDensity.current) { IconSize.roundToPx() }
    val icon = rememberArt(app, bannerLoader, iconPx, iconPx)
    Column(
        modifier = Modifier.size(CardWidth, CardHeight).padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(IconSize)) {
            icon?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.size(IconSize)) }
        }
        Text(
            text = app.label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
internal fun rememberArt(
    app: InstalledApp,
    bannerLoader: BannerLoader,
    widthPx: Int,
    heightPx: Int,
    iconOnly: Boolean = false,
): ImageBitmap? {
    val bitmap by produceState(
        initialValue = bannerLoader.peek(app, widthPx, heightPx, iconOnly),
        app.packageName,
        app.lastUpdateTime,
        widthPx,
        heightPx,
        iconOnly,
    ) {
        value = bannerLoader.load(app, widthPx, heightPx, iconOnly)
    }
    return remember(bitmap) { bitmap?.asImageBitmap() }
}
