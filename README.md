# Stauncher

Stauncher is an alternative app launcher for Android TV and Google TV that mainly does less. It leaves out the advertising, sponsored recommendations, and promotional clutter that all compete for your attention. Instead, it sticks to the essentials: a simple home screen for opening apps.

## Status

Early development: the project builds and shows an empty home screen. The plan is in [docs/plan.md](docs/plan.md) and
the reasoning behind the choices in [docs/decisions.md](docs/decisions.md).

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

