package rs.bonge.launchery

import android.app.Application

class LauncheryApplication : Application() {
    val container by lazy { AppContainer(this) }
}
