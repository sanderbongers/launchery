package rs.bonge.stauncher.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import rs.bonge.stauncher.R

private val BarHorizontalPadding = 48.dp
private val BarVerticalPadding = 27.dp
private val ButtonSize = 48.dp
private val ButtonIconSize = 24.dp
private val ButtonFocusBorderWidth = 3.dp
private val LabelAreaHeight = 32.dp
private val LabelTopGap = 12.dp
private const val FOCUSED_SCALE = 1.1f

/** The top bar, reached with up from the row. The clock will join the settings button here. */
@Composable
fun TopBar(
    onOpenSettings: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    settingsFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = BarHorizontalPadding, vertical = BarVerticalPadding),
        horizontalArrangement = Arrangement.End,
    ) {
        SettingsButton(onOpenSettings, onOpenSystemSettings, Modifier.focusRequester(settingsFocus))
    }
}

/**
 * Select opens the Stauncher settings and a long press goes straight to the system Settings. Like a banner card it
 * names itself below while focused.
 */
@Composable
private fun SettingsButton(
    onOpenSettings: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val surfaceColor = colorResource(R.color.card_surface)
    val label = stringResource(R.string.settings)

    Column(horizontalAlignment = Alignment.End) {
        Surface(
            onClick = onOpenSettings,
            onLongClick = onOpenSystemSettings,
            modifier =
                modifier
                    .size(ButtonSize)
                    .onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = label },
            shape = ClickableSurfaceDefaults.shape(CircleShape),
            colors =
                ClickableSurfaceDefaults.colors(
                    containerColor = surfaceColor,
                    focusedContainerColor = surfaceColor,
                    pressedContainerColor = surfaceColor,
                ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = FOCUSED_SCALE),
            border =
                ClickableSurfaceDefaults.border(
                    focusedBorder = Border(BorderStroke(ButtonFocusBorderWidth, Color.White), shape = CircleShape),
                ),
        ) {
            Box(Modifier.size(ButtonSize), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonIconSize),
                    tint = colorResource(if (focused) R.color.text_primary else R.color.text_secondary),
                )
            }
        }
        Box(
            modifier = Modifier.height(LabelAreaHeight).padding(top = LabelTopGap),
            contentAlignment = Alignment.TopEnd,
        ) {
            if (focused) {
                Text(text = label, color = colorResource(R.color.text_primary), fontSize = 14.sp, maxLines = 1)
            }
        }
    }
}
