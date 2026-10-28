package rs.bonge.stauncher

import android.app.Application

class StauncherApplication : Application() {
    val container by lazy { AppContainer(this) }
}
