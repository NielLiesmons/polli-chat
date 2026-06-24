## Polli (Android)

Polli is a fork of [Delta Chat Android](https://github.com/deltachat/deltachat-android) with a custom UI layer built on the Delta Chat / Chatmail core.

Upstream: [deltachat/deltachat-android](https://github.com/deltachat/deltachat-android)  
This fork: [NielLiesmons/polli-chat](https://github.com/NielLiesmons/polli-chat)

For building the app, refer to [BUILDING.md](./BUILDING.md).  
For Polli UI parity notes, see [docs/POLLI_PARITY.md](./docs/POLLI_PARITY.md).

### App identity

| Setting | Value |
|---------|-------|
| Display name | Polli |
| Package ID (FOSS) | `com.polli.chat` |
| APK base name | `polli` |

### Launcher icon

Replace the adaptive launcher assets under `src/main/res/`:

- **Foreground (required):** `mipmap-mdpi` through `mipmap-xxxhdpi` → `ic_launcher_foreground.png` (108×108 dp safe zone; typical PNG sizes: 108, 162, 216, 324, 432 px)
- **Legacy fallback:** same density folders → `ic_launcher.png` (48–192 px per density)
- **Adaptive icon XML:** `mipmap-anydpi-v26/ic_launcher.xml` (usually leave as-is)
- **Background color:** `values/ic_launcher_background.xml`
- **Monochrome (Android 13+ themed icon):** `drawable/ic_launcher_foreground_monochrome.xml` or replace with a vector/PNG

For Play Store / F-Droid listings, also update `fastlane/metadata/android/en-US/images/` (e.g. `icon.png`, `featureGraphic.png`) when you add store assets.
