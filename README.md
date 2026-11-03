# Launchery

Launchery is an alternative app launcher for Android TV and Google TV that mainly does less. It leaves out the
advertising, sponsored recommendations, and promotional clutter that all compete for your attention. Instead, it sticks
to the essentials: a simple home screen for opening apps.

## Status

Pre-release 0.1.0. Launchery works as a home screen on a Chromecast with Google TV 4K running Android 14, the only
device it has been tested on. The HD model and other Android TV or Google TV devices are untested. Launchery needs
Android 14 (API 34) or newer.

## Using it

- Select starts an app. Holding select opens its actions: App info, Move, Hide and Uninstall. Force stop is in App info.
- Left and right move along the row. At the first or the last app, hold the key to break through to the other end. A
  short press there does nothing, so a stray press never loops the row.
- Up from the row reaches the cog. Select opens the settings: "Show apps as" switches between round icons and banners,
  "Hidden apps" lists what you hid and "All settings" opens the system settings. Holding select on the cog goes
  straight to the system settings.

## Installing

You need a computer with `adb` and the TV's Developer options with Wireless debugging switched on. The commands are the
same in any shell.

> **Install and start Launchery before you disable the stock launcher.** With no other home app the TV has no home
> screen, and Settings becomes hard to reach.

1. Download `launchery-0.1.0.apk` and `launchery-0.1.0.apk.sha256` from the release page, and check them as described
   in [Checking a download](#checking-a-download).
2. Connect to the TV. The pairing code, the pairing port and the connection port are on the Wireless debugging screen,
   and the connection port changes after a restart of the TV.

   ```bash
   adb pair <tv-ip>:<pairing-port>
   adb connect <tv-ip>:<connection-port>
   ```

3. Install Launchery, then start it once from the TV's app list.

   ```bash
   adb install -r launchery-0.1.0.apk
   ```

4. Optional: after the first start, compile the app. A fresh `adb install` leaves it uncompiled, which on the tested
   Chromecast meant a cold start of about 1 second until Android's background job compiled it, and about 0.4
   seconds after. This does it at once.

   ```bash
   adb shell cmd package compile -f -m speed-profile rs.bonge.launchery
   ```

5. Make Launchery the home screen. Give it the Home role, disable the stock launcher, press Home and check that
   Launchery opens, and only then disable the second package. On the tested Chromecast, with only the stock launcher
   disabled, Home still opened Google's recovery screen from `setupwraith` instead of Launchery.

   ```bash
   adb shell cmd role add-role-holder --user 0 android.app.role.HOME rs.bonge.launchery
   adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
   adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
   ```

These package names come from community guides. They were run on the tested Chromecast, and `launcherx` also on an
emulator, which has no `setupwraith`. Other devices may use other names.

### Checking a download

```bash
shasum -a 256 -c launchery-0.1.0.apk.sha256
apksigner verify --print-certs launchery-0.1.0.apk
```

The first command should print `OK`. The second needs `apksigner` from the Android SDK build tools and prints the
signing certificate, which must match this fingerprint:

```text
Signer #1 certificate DN: CN=Launchery
Signer #1 certificate SHA-256 digest: 20e974b26c7c12639b36f9f8373053e13df03c5c704a8be36a6925a251442c64
```

## Undoing it

```bash
adb shell pm enable com.google.android.apps.tv.launcherx
adb shell pm enable com.google.android.tungsten.setupwraith
```

Home then opens the stock launcher again, because it has the higher priority. If another launcher held the Home role
before, give it back with `adb shell cmd role add-role-holder --user 0 android.app.role.HOME <package>`. On the tested
Chromecast, Projectivy was that launcher.

## If something goes wrong

If Launchery fails to start while the stock launcher is disabled, the undo above needs a way in:

- From Launchery, the cog opens the system settings. On the tested Chromecast, Developer options and Wireless debugging
  could be reached by hand there after a restart, with a new connection port.
- Wireless debugging was still on after one restart of the tested Chromecast, but a switch-off after a restart or sleep
  has been reported, so check it before relying on `adb`.

Holding the Assistant button and saying "open settings" is not a route: on the tested Chromecast Google showed a
"Verify network change" screen and did not open Settings.

## Known losses

What was seen on the tested Chromecast with the stock launcher disabled:

- **The YouTube button does nothing.** Launchery receives no key from it, and the system starts nothing.
- **Apps that remap keys through an accessibility service take over.** With TV Quick Actions switched on, Home opened
  Projectivy and the YouTube button opened SmartTube, and neither press reached Launchery.
- **The profile switcher and the quick-settings panel of the stock launcher are gone.** They belong to the stock
  launcher and were not checked one by one.

Seen to work: Home and Back, a restart, sleep and wake (Launchery kept running and was back in about a tenth of a
second after the screen came on), the volume keys when the remote's volume control is set to the Chromecast, and
casting from a phone. Casting was tried with NLZIET and NPO Start (video) and Spotify (audio, which plays in the
background without a screen on the TV). Some people report that disabling the stock launcher breaks casting; that
did not happen here.

## Development

You need a JDK 17 or newer and the Android SDK with platform 37.2. Point `JAVA_HOME` at the JDK and `ANDROID_HOME` at
the SDK, or put `sdk.dir` in `local.properties`.

```bash
./gradlew check          # lint, format check and unit tests
./gradlew assembleDebug  # debug APK in app/build/outputs/apk/debug/
```

Optional commands:

```bash
./gradlew spotlessApply  # fix formatting
```

### Signed release build

Without a keystore, `./gradlew assembleRelease` builds an unsigned release APK. To sign it, set
`LAUNCHERY_KEYSTORE_FILE` (an absolute path), `LAUNCHERY_KEY_ALIAS` and `LAUNCHERY_KEYSTORE_PASSWORD` in the terminal,
and build without the configuration cache, so that Gradle does not store the password in it:

```bash
./gradlew assembleRelease --no-configuration-cache
./gradlew --stop
```

The APK is `app/build/outputs/apk/release/app-release.apk`. Stopping the daemon ends the process that held the password.

## License

[MIT](LICENSE)
