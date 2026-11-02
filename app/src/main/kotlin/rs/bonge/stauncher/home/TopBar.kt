package rs.bonge.stauncher.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import rs.bonge.stauncher.R
import rs.bonge.stauncher.ui.FocusStyle
import rs.bonge.stauncher.ui.FocusSurface

private val BarHorizontalPadding = 48.dp
private val BarVerticalPadding = 27.dp
private val ButtonSize = 48.dp
private val ButtonIconSize = 24.dp
private val ClockGap = 24.dp
private val LabelAreaHeight = 32.dp
private val LabelTopGap = 12.dp
private const val FOCUSED_SCALE = 1.1f

/**
 * The top bar, reached with up from the row: the [time], and the settings button at the far right. While [focusable] is
 * false nothing in the bar can take focus, so the first focus does not land here before the row has its apps.
 */
@Composable
fun TopBar(
    time: String,
    onOpenSettings: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    settingsFocus: FocusRequester,
    focusable: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = BarHorizontalPadding, vertical = BarVerticalPadding)
                .focusProperties { canFocus = focusable },
        horizontalArrangement = Arrangement.End,
    ) {
        Box(Modifier.height(ButtonSize), contentAlignment = Alignment.Center) {
            Text(text = time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 20.sp, maxLines = 1)
        }
        Spacer(Modifier.width(ClockGap))
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
    val label = stringResource(R.string.settings)

    Column(horizontalAlignment = Alignment.End) {
        FocusSurface(
            onClick = onOpenSettings,
            onLongClick = onOpenSystemSettings,
            shape = CircleShape,
            style = FocusStyle.Fill,
            modifier =
                modifier
                    .size(ButtonSize)
                    .onFocusChanged { focused = it.isFocused }
                    .semantics { contentDescription = label },
            focusedScale = FOCUSED_SCALE,
        ) {
            Box(Modifier.size(ButtonSize), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(ButtonIconSize),
                    tint = if (focused) LocalContentColor.current else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box(
            modifier = Modifier.height(LabelAreaHeight).padding(top = LabelTopGap),
            contentAlignment = Alignment.TopEnd,
        ) {
            if (focused) {
                Text(text = label, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, maxLines = 1)
            }
        }
    }
}
