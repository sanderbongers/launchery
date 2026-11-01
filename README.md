# Stauncher

Stauncher is an alternative app launcher for Android TV and Google TV that mainly does less. It leaves out the advertising, sponsored recommendations, and promotional clutter that all compete for your attention. Instead, it sticks to the essentials: a simple home screen for opening apps.

## Status

Early development: Stauncher works as a home screen on a Chromecast with Google TV and has no release yet. The plan is
in [docs/plan.md](docs/plan.md) and the reasoning behind the choices in [docs/decisions.md](docs/decisions.md).

## Using it

- Select starts an app. Holding select opens its actions: App info, Move, Hide and Uninstall. Force stop is in App info.
- Left and right move along the row. At the first or the last app, hold the key to break through to the other end. A
  short press there does nothing, so a stray press never loops the row.
- Up from the row reaches the cog. Select opens the Stauncher settings, where "Hidden apps" lists what you hid and
  "All settings" opens the system settings. Holding select on the cog goes straight to the system settings.

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

## License

[MIT](LICENSE)

