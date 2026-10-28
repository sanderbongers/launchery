package rs.bonge.stauncher

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import rs.bonge.stauncher.apps.InstalledApp
import rs.bonge.stauncher.home.HomeScreen
import rs.bonge.stauncher.home.HomeViewModel

class HomeActivity : ComponentActivity() {
    private val container get() = (application as StauncherApplication).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = viewModel<HomeViewModel>(factory = HomeViewModel.Factory)
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            HomeScreen(state = state, bannerLoader = container.bannerLoader, onLaunch = ::launch)
        }
    }

    private fun launch(app: InstalledApp) {
        if (!container.appActions.launch(app)) {
            container.appRepository.refresh()
            Toast.makeText(this, getString(R.string.launch_failed, app.label), Toast.LENGTH_SHORT).show()
        }
    }
}
