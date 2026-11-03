package rs.bonge.stauncher.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import rs.bonge.stauncher.R
import rs.bonge.stauncher.apps.ArtKind
import rs.bonge.stauncher.apps.BannerLoader
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.settings.AppStyle
import rs.bonge.stauncher.ui.FocusStyle
import rs.bonge.stauncher.ui.FocusSurface

// The measurements are those of the quick settings of Google TV.
private val SheetWidth = 272.dp
private val SheetMargin = 16.dp
private val SheetEnd = 24.dp
private val SheetCornerRadius = 24.dp
private val SheetPadding = 16.dp
private val TileHeight = 54.dp
private val TileCornerRadius = 12.dp
private val TileGap = 8.dp
private val TileIconSlot = 32.dp
private val TileIconSize = 24.dp
private val RoundButtonSize = 36.dp
private val RoundButtonIconSize = 18.dp

// A tile's second line keeps its contrast whether the tile is dark or, focused, light.
private const val SECONDARY_ALPHA = 0.7f

private enum class Page { Main, HiddenApps }

/**
 * The settings, as a sheet that floats over the dimmed home screen: a round button for the system settings, which has
 * focus first, a tile for how the apps are shown and one for the hidden apps. Select on the hidden apps tile opens a page of the sheet that lists them,
 * where select puts an app back on the row. Back on that page returns to the main page, with focus on the tile that led
 * there. Focus stays in the sheet while it is open.
 */
@Composable
fun SettingsSheet(
    hidden: List<InstalledApp>,
    hiddenLoaded: Boolean,
    appStyle: AppStyle,
    bannerLoader: BannerLoader,
    onToggleAppStyle: () -> Unit,
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

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
                .padding(top = SheetMargin, end = SheetEnd, bottom = SheetMargin),
    ) {
        Column(
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .width(SheetWidth)
                    .clip(RoundedCornerShape(SheetCornerRadius))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(SheetPadding)
                    .focusProperties { onExit = { cancelFocusChange() } }
                    .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(TileGap),
        ) {
            when (page) {
                Page.Main -> {
                    MainPage(
                        appStyle = appStyle,
                        focusHiddenApps = cameBack,
                        onToggleAppStyle = onToggleAppStyle,
                        onOpenHiddenApps = {
                            cameBack = false
                            page = Page.HiddenApps
                        },
                        onOpenSystemSettings = onOpenSystemSettings,
                    )
                }

                Page.HiddenApps -> {
                    HiddenAppsPage(hidden, hiddenLoaded, bannerLoader, onUnhide)
                }
            }
        }
    }
}

@Composable
private fun MainPage(
    appStyle: AppStyle,
    focusHiddenApps: Boolean,
    onToggleAppStyle: () -> Unit,
    onOpenHiddenApps: () -> Unit,
    onOpenSystemSettings: () -> Unit,
) {
    val allSettingsFocus = remember { FocusRequester() }
    val hiddenAppsFocus = remember { FocusRequester() }
    // The route to the system settings is the first thing in the sheet, as in the quick settings of Google TV.
    LaunchedEffect(Unit) { (if (focusHiddenApps) hiddenAppsFocus else allSettingsFocus).requestFocus() }

    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
        AllSettingsButton(onOpenSystemSettings, Modifier.focusRequester(allSettingsFocus))
    }
    SheetTile(
        label = stringResource(R.string.show_apps_as),
        value = stringResource(appStyle.labelRes()),
        onClick = onToggleAppStyle,
    ) {
        Icon(painterResource(R.drawable.ic_apps), contentDescription = null, modifier = Modifier.size(TileIconSize))
    }
    SheetTile(
        label = stringResource(R.string.hidden_apps),
        onClick = onOpenHiddenApps,
        modifier = Modifier.focusRequester(hiddenAppsFocus),
    ) {
        Icon(
            painterResource(R.drawable.ic_visibility_off),
            contentDescription = null,
            modifier = Modifier.size(TileIconSize),
        )
    }
}

/** The round button that opens the system settings; it names itself below while it has focus. */
@Composable
private fun AllSettingsButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val label = stringResource(R.string.all_settings)

    Column(horizontalAlignment = Alignment.End) {
        FocusSurface(
            onClick = onClick,
            shape = CircleShape,
            style = FocusStyle.Fill,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            modifier =
                modifier
                    .size(RoundButtonSize)
                    .onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = label },
        ) {
            Box(Modifier.size(RoundButtonSize), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(RoundButtonIconSize))
            }
        }
        ButtonLabel(text = label, visible = focused, buttonWidth = RoundButtonSize, endAligned = true)
    }
}

private fun AppStyle.labelRes() =
    when (this) {
        AppStyle.RoundIcons -> R.string.style_round_icons
        AppStyle.Banners -> R.string.style_banners
    }

/**
 * After an app is unhidden focus stays at the same place in the list. When the list is empty there is nothing left to
 * focus, and Back leaves the page.
 */
@Composable
private fun ColumnScope.HiddenAppsPage(
    hidden: List<InstalledApp>,
    loaded: Boolean,
    bannerLoader: BannerLoader,
    onUnhide: (InstalledApp) -> Unit,
) {
    val listState = rememberLazyListState()
    var unhiddenIndex by remember { mutableStateOf<Int?>(null) }
    var pendingFocus by remember { mutableStateOf<Int?>(if (hidden.isEmpty()) null else 0) }

    LaunchedEffect(hidden) {
        val index = unhiddenIndex ?: return@LaunchedEffect
        unhiddenIndex = null
        if (hidden.isNotEmpty()) pendingFocus = index.coerceAtMost(hidden.lastIndex)
    }
    // An off-screen target has to be composed before it can take focus.
    LaunchedEffect(pendingFocus) { pendingFocus?.let { listState.scrollToItem(it) } }

    Text(
        text = stringResource(R.string.hidden_apps),
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 18.sp,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
    )
    if (loaded && hidden.isEmpty()) {
        Text(
            text = stringResource(R.string.no_hidden_apps),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.weight(1f, fill = false),
        verticalArrangement = Arrangement.spacedBy(TileGap),
    ) {
        itemsIndexed(hidden, key = { _, app -> app.packageName }) { index, app ->
            val focusRequester = remember { FocusRequester() }
            if (pendingFocus == index) {
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                    pendingFocus = null
                }
            }
            val iconPx = with(LocalDensity.current) { TileIconSlot.roundToPx() }
            val icon = rememberArt(app, bannerLoader, iconPx, iconPx, ArtKind.Icon)
            SheetTile(
                label = app.label,
                value = stringResource(R.string.unhide),
                onClick = {
                    unhiddenIndex = index
                    onUnhide(app)
                },
                modifier = Modifier.focusRequester(focusRequester),
            ) {
                icon?.let { Image(bitmap = it, contentDescription = null, modifier = Modifier.size(TileIconSlot)) }
            }
        }
    }
}

/** A tile with an [icon] in a slot of the same width on every tile, the [label] and, below it, a muted [value]. */
@Composable
private fun SheetTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    icon: @Composable () -> Unit,
) {
    FocusSurface(
        onClick = onClick,
        shape = RoundedCornerShape(TileCornerRadius),
        style = FocusStyle.Fill,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        modifier = modifier.fillMaxWidth().height(TileHeight),
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(TileIconSlot), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.Center) {
                Text(text = label, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (value != null) {
                    Text(
                        text = value,
                        color = LocalContentColor.current.copy(alpha = SECONDARY_ALPHA),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
