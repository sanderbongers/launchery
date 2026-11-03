package rs.bonge.stauncher.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import rs.bonge.stauncher.apps.ArtKind
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.settings.AppStyle
import rs.bonge.stauncher.ui.FocusStyle
import rs.bonge.stauncher.ui.FocusSurface

private val BannerWidth = 192.dp
private val BannerHeight = 108.dp
private val BannerGap = 16.dp
private val BannerCornerRadius = 12.dp
private val FlatIconSize = 48.dp
private val RoundIconSize = 76.dp
private val RoundIconGap = 15.dp
private val LabelTopGap = 12.dp
private val BannerLabelAreaHeight = 32.dp
private val RoundLabelAreaHeight = 48.dp
private const val FOCUSED_SCALE = 1.1f
private const val LIFTED_SCALE = 1.2f

/** The width of one item in the row, and the gap to the next one. */
internal fun AppStyle.itemWidth(): Dp =
    when (this) {
        AppStyle.RoundIcons -> RoundIconSize
        AppStyle.Banners -> BannerWidth
    }

internal fun AppStyle.itemGap(): Dp =
    when (this) {
        AppStyle.RoundIcons -> RoundIconGap
        AppStyle.Banners -> BannerGap
    }

// A round icon's name is as wide as the pitch of the row, so that the names of neighbours meet but do not overlap.
private val RoundLabelWidth = RoundIconSize + RoundIconGap

/**
 * An app in the row. In the round style every app is an icon with its name below it. In the banner style the name shows
 * below the focused card only, and an app without a banner has its icon on a plain card. A [lifted] card, one that is
 * being moved, grows a little further than a focused one. [itemModifier] goes on the outermost layout, where a lazy list
 * reads its item modifiers; [modifier] goes on the focusable surface.
 */
@Composable
fun AppCard(
    app: InstalledApp,
    style: AppStyle,
    bannerLoader: BannerLoader,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    lifted: Boolean,
    modifier: Modifier = Modifier,
    itemModifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val round = style == AppStyle.RoundIcons
    val width = style.itemWidth()
    val height = if (round) RoundIconSize else BannerHeight

    Column(
        // A lifted card grows past the gap to its neighbours, so it is drawn over them.
        modifier = itemModifier.width(width).zIndex(if (lifted) 1f else 0f),
        horizontalAlignment = if (round) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        FocusSurface(
            onClick = onClick,
            onLongClick = onLongClick,
            shape = if (round) CircleShape else RoundedCornerShape(BannerCornerRadius),
            style = FocusStyle.Ring,
            modifier =
                modifier
                    .size(width, height)
                    .onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = app.label },
            focusedScale = if (lifted) LIFTED_SCALE else FOCUSED_SCALE,
        ) {
            AppArt(app, bannerLoader, width, height, artKindFor(app, style))
        }
        if (round) RoundLabel(app.label, focused) else BannerLabel(app.label, focused)
    }
}

@Composable
private fun RoundLabel(
    label: String,
    focused: Boolean,
) {
    Box(
        modifier = Modifier.width(RoundIconSize).height(RoundLabelAreaHeight).padding(top = LabelTopGap),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = label,
            color = if (focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            // Two lines have to fit the label area; the line height of a font may be taller than it is in Roboto.
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.requiredWidth(RoundLabelWidth).padding(horizontal = 3.dp),
        )
    }
}

@Composable
private fun BannerLabel(
    label: String,
    focused: Boolean,
) {
    Box(Modifier.height(BannerLabelAreaHeight).padding(top = LabelTopGap)) {
        if (focused) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** What the loader draws for [app] in [style]; the glow takes its colour from the same art. */
internal fun artKindFor(
    app: InstalledApp,
    style: AppStyle,
): ArtKind =
    when {
        style == AppStyle.RoundIcons -> ArtKind.RoundIcon
        app.hasBanner -> ArtKind.Banner
        else -> ArtKind.Icon
    }

/**
 * The art of [app] on a tile of [width] by [height], as [kind] says: a round icon fills the tile, a banner is stretched
 * over it and a flat icon sits in its middle.
 */
@Composable
internal fun AppArt(
    app: InstalledApp,
    bannerLoader: BannerLoader,
    width: Dp,
    height: Dp,
    kind: ArtKind,
) {
    when (kind) {
        ArtKind.RoundIcon -> {
            SquareArt(app, bannerLoader, min(width, height), kind)
        }

        ArtKind.Banner -> {
            BannerArt(app, bannerLoader, width, height)
        }

        ArtKind.Icon -> {
            Box(Modifier.size(width, height), contentAlignment = Alignment.Center) {
                SquareArt(app, bannerLoader, FlatIconSize, kind)
            }
        }
    }
}

/** The [kind] of art of [app], [size] wide and high. */
@Composable
internal fun SquareArt(
    app: InstalledApp,
    bannerLoader: BannerLoader,
    size: Dp,
    kind: ArtKind,
) {
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val art = rememberArt(app, bannerLoader, sizePx, sizePx, kind)
    art?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.size(size)) }
}

@Composable
private fun BannerArt(
    app: InstalledApp,
    bannerLoader: BannerLoader,
    width: Dp,
    height: Dp,
) {
    val density = LocalDensity.current
    val widthPx = with(density) { width.roundToPx() }
    val heightPx = with(density) { height.roundToPx() }
    val art = rememberArt(app, bannerLoader, widthPx, heightPx, ArtKind.Banner)
    art?.let {
        Image(
            bitmap = it,
            contentDescription = null,
            modifier = Modifier.size(width, height),
            contentScale = ContentScale.FillBounds,
        )
    }
}

@Composable
private fun rememberArt(
    app: InstalledApp,
    bannerLoader: BannerLoader,
    widthPx: Int,
    heightPx: Int,
    kind: ArtKind,
): ImageBitmap? {
    val bitmap by produceState(
        initialValue = bannerLoader.peek(app, widthPx, heightPx, kind),
        app.packageName,
        app.lastUpdateTime,
        widthPx,
        heightPx,
        kind,
    ) {
        value = bannerLoader.load(app, widthPx, heightPx, kind)
    }
    return remember(bitmap) { bitmap?.asImageBitmap() }
}
