package rs.bonge.stauncher.settings

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import rs.bonge.stauncher.R
import rs.bonge.stauncher.StauncherApplication
import rs.bonge.stauncher.ui.StauncherTheme

class SettingsActivity : ComponentActivity() {
    private val container get() = (application as StauncherApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = viewModel<SettingsViewModel>(factory = SettingsViewModel.Factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            StauncherTheme {
                SettingsScreen(
                    state = state,
                    bannerLoader = container.bannerLoader,
                    onUnhide = viewModel::unhide,
                    onToggleAppStyle = viewModel::toggleAppStyle,
                    onOpenSystemSettings = ::openSystemSettings,
                )
            }
        }
    }

    private fun openSystemSettings() {
        if (!container.appActions.openSystemSettings()) {
            Toast
                .makeText(
                    this,
                    getString(R.string.launch_failed, getString(R.string.all_settings)),
                    Toast.LENGTH_SHORT,
                ).show()
        }
    }
}
