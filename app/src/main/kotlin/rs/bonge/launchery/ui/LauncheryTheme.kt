package rs.bonge.launchery.ui

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.darkColorScheme
import rs.bonge.launchery.R

// Google TV devices register Google's typeface under this name. On any other device the name is unknown, and this is
// the default font.
private val GoogleSans = FontFamily(Typeface.create("google-sans", Typeface.NORMAL))

/** The colours and the typeface of every screen. The colour values live in `colors.xml`. */
@Composable
fun LauncheryTheme(content: @Composable () -> Unit) {
    val colorScheme =
        darkColorScheme(
            background = colorResource(R.color.background),
            onBackground = colorResource(R.color.text_primary),
            surface = colorResource(R.color.sheet_surface),
            onSurface = colorResource(R.color.text_primary),
            surfaceVariant = colorResource(R.color.card_surface),
            onSurfaceVariant = colorResource(R.color.text_secondary),
            secondaryContainer = colorResource(R.color.sheet_tile),
            onSecondaryContainer = colorResource(R.color.text_primary),
            // The capsule at the top of the home screen, and the round buttons in it.
            tertiaryContainer = colorResource(R.color.capsule_surface),
            onTertiaryContainer = colorResource(R.color.text_primary),
            primaryContainer = colorResource(R.color.capsule_button),
            onPrimaryContainer = colorResource(R.color.text_primary),
            inverseSurface = colorResource(R.color.focus_fill),
            inverseOnSurface = colorResource(R.color.on_focus_fill),
            border = colorResource(R.color.focus_ring),
            scrim = colorResource(R.color.scrim),
        )
    MaterialTheme(colorScheme = colorScheme) {
        ProvideTextStyle(TextStyle(fontFamily = GoogleSans), content)
    }
}
