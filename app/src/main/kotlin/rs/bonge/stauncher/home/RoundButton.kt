package rs.bonge.stauncher.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import rs.bonge.stauncher.ui.FocusStyle
import rs.bonge.stauncher.ui.FocusSurface

// The round buttons of Google TV's top menu and quick settings.
internal val RoundButtonSize = 36.dp
private val RoundButtonIconSize = 18.dp
private val LabelAreaHeight = 28.dp
private val LabelTopGap = 6.dp

/** A round button with the settings cog, named [label]. It turns light while it has focus. */
@Composable
internal fun RoundSettingsButton(
    label: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    FocusSurface(
        onClick = onClick,
        onLongClick = onLongClick,
        shape = CircleShape,
        style = FocusStyle.Fill,
        containerColor = containerColor,
        modifier = modifier.size(RoundButtonSize).semantics { contentDescription = label },
    ) {
        Box(Modifier.size(RoundButtonSize), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(RoundButtonIconSize))
        }
    }
}

/**
 * The name of a round button of [buttonWidth], shown below it while the button has focus. It is wider than the button
 * and is centred on it, or, with [endAligned], ends where the button ends. It always takes its space, so that nothing
 * moves when it shows.
 */
@Composable
internal fun ButtonLabel(
    text: String,
    visible: Boolean,
    buttonWidth: Dp,
    modifier: Modifier = Modifier,
    endAligned: Boolean = false,
) {
    Box(
        modifier = modifier.width(buttonWidth).height(LabelAreaHeight).padding(top = LabelTopGap),
        contentAlignment = if (endAligned) Alignment.TopEnd else Alignment.TopCenter,
    ) {
        if (visible) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                maxLines = 1,
                softWrap = false,
                modifier =
                    Modifier.wrapContentWidth(
                        align = if (endAligned) Alignment.End else Alignment.CenterHorizontally,
                        unbounded = true,
                    ),
            )
        }
    }
}
