package rs.bonge.stauncher

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.home.HomeCallbacks
import rs.bonge.stauncher.home.HomeScreen
import rs.bonge.stauncher.home.HomeViewModel
import rs.bonge.stauncher.settings.SettingsActivity

class HomeActivity : ComponentActivity() {
    private val container get() = (application as StauncherApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = viewModel<HomeViewModel>(factory = HomeViewModel.Factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            // An open panel or a picked-up app should not still be there when the user comes back.
            LifecycleEventEffect(Lifecycle.Event.ON_STOP, onEvent = viewModel::dismiss)
            val callbacks =
                remember(viewModel) {
                    HomeCallbacks(
                        onLaunch = ::launch,
                        onOpenActions = viewModel::openActions,
                        onDismiss = viewModel::dismiss,
                        onStartMove = viewModel::startMove,
                        onMoveBy = viewModel::moveBy,
                        onDrop = viewModel::drop,
                        onHide = viewModel::hide,
                        onAppInfo = { app ->
                            viewModel.dismiss()
                            if (!container.appActions.showAppInfo(
                                    app,
                                )
                            ) {
                                reportFailure(getString(R.string.action_app_info))
                            }
                        },
                        onUninstall = { app ->
                            viewModel.dismiss()
                            if (!container.appActions.uninstall(
                                    app,
                                )
                            ) {
                                reportFailure(getString(R.string.action_uninstall))
                            }
                        },
                        onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) },
                        onOpenSystemSettings = {
                            if (!container.appActions.openSystemSettings()) reportFailure(getString(R.string.settings))
                        },
                    )
                }
            HomeScreen(state = state, bannerLoader = container.bannerLoader, callbacks = callbacks)
        }
    }

    private fun launch(app: InstalledApp) {
        if (!container.appActions.launch(app)) {
            container.appRepository.refresh()
            reportFailure(app.label)
        }
    }

    private fun reportFailure(what: String) {
        Toast.makeText(this, getString(R.string.launch_failed, what), Toast.LENGTH_SHORT).show()
    }
}
