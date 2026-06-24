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

Place branding assets in `assets/branding/`:

| File | Purpose |
|------|---------|
| `icon-full.png` | 1024×1024 composite (purple background + white logo) |
| `icon-foreground.png` | Optional transparent PNG; auto-extracted from `icon-full.png` if missing |

Then run:

```bash
./scripts/generate-launcher-icons.sh
```

This generates all `mipmap-*` densities, sets the adaptive icon background color, and updates the Play Store `fastlane` icon. Reinstall the app after changing icons (launcher caches aggressively).
