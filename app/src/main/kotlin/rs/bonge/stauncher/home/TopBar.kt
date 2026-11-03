package rs.bonge.stauncher.home

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import rs.bonge.stauncher.R
import rs.bonge.stauncher.ui.FocusStyle
import rs.bonge.stauncher.ui.FocusSurface
import rs.bonge.stauncher.ui.ScreenMargin

// The measurements are those of the top menu of Google TV: a capsule 44 dp high, with round buttons of 36 dp in it.
private val BarTop = 32.dp
private val CapsuleHeight = 44.dp
private val CapsulePadding = 4.dp
private val ButtonSize = CapsuleHeight - CapsulePadding * 2
private val ButtonIconSize = 18.dp

/**
 * The top bar, reached with up from the row: the settings button at the far left and the [time] at the far right. While
 * [focusable] is false nothing in the bar can take focus, so the first focus does not land here before the row has its
 * apps.
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
                .padding(start = ScreenMargin, top = BarTop, end = ScreenMargin)
                .focusProperties { canFocus = focusable },
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        SettingsButton(onOpenSettings, onOpenSystemSettings, Modifier.focusRequester(settingsFocus))
        Box(Modifier.height(CapsuleHeight), contentAlignment = Alignment.Center) {
            Text(text = time, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 18.sp, maxLines = 1)
        }
    }
}

/**
 * Select opens the settings and a long press goes straight to the system Settings. The round button sits in a capsule,
 * turns light while it has focus, and names itself below.
 */
@Composable
private fun SettingsButton(
    onOpenSettings: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val label = stringResource(R.string.settings)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(CapsuleHeight)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(CapsulePadding),
        ) {
            FocusSurface(
                onClick = onOpenSettings,
                onLongClick = onOpenSystemSettings,
                shape = CircleShape,
                style = FocusStyle.Fill,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                modifier =
                    modifier
                        .size(ButtonSize)
                        .onFocusChanged { focused = it.isFocused }
                        .semantics { contentDescription = label },
            ) {
                Box(Modifier.size(ButtonSize), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(ButtonIconSize))
                }
            }
        }
        ButtonLabel(text = label, visible = focused, buttonWidth = CapsuleHeight)
    }
}
