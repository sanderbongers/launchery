package rs.bonge.launchery.home

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key

/** The keys that click what has focus: the centre of the D-pad on a remote, Enter on a keyboard. */
internal fun KeyEvent.isSelect() = key == Key.DirectionCenter || key == Key.Enter || key == Key.NumPadEnter
