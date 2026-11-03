package rs.bonge.launchery.home

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import rs.bonge.launchery.LauncheryApplication
import rs.bonge.launchery.R
import rs.bonge.launchery.apps.InstalledApp
import rs.bonge.launchery.ui.LauncheryTheme

class HomeActivity : ComponentActivity() {
    private val container get() = (application as LauncheryApplication).container
    private val viewModel by viewModels<HomeViewModel> { HomeViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val time by container.clock.time.collectAsStateWithLifecycle(
                initialValue = remember { container.clock.now() },
            )
            // An open screen or sheet, or a picked-up app, should not still be there when the user comes back.
            LifecycleEventEffect(Lifecycle.Event.ON_STOP, onEvent = viewModel::dismiss)
            val callbacks =
                remember(viewModel) {
                    val actions = container.appActions
                    HomeCallbacks(
                        onLaunch = ::launch,
                        onOpenActions = viewModel::openActions,
                        onDismiss = viewModel::dismiss,
                        onStartMove = viewModel::startMove,
                        onMoveBy = viewModel::moveBy,
                        onDrop = viewModel::drop,
                        onHide = viewModel::hide,
                        onAppInfo = { app ->
                            if (!actions.showAppInfo(app)) reportFailure(getString(R.string.action_app_info))
                        },
                        onUninstall = { app ->
                            if (!actions.uninstall(app)) reportFailure(getString(R.string.action_uninstall))
                        },
                        onOpenSettings = viewModel::openSettings,
                        onToggleAppStyle = viewModel::toggleAppStyle,
                        onUnhide = viewModel::unhide,
                        onOpenSystemSettings = {
                            if (!actions.openSystemSettings()) reportFailure(getString(R.string.settings))
                        },
                    )
                }
            LauncheryTheme {
                HomeScreen(state = state, time = time, bannerLoader = container.bannerLoader, callbacks = callbacks)
            }
        }
    }

    // The Home button reaches an already running Launchery here, and so does a launch from the stock launcher.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        viewModel.goHome()
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
