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

Drop your **1024×1024** app icon here (your file is never modified):

```
assets/branding/icon-full.png
```

Then regenerate launcher assets:

```bash
./scripts/generate-launcher-icons.sh
```

The script writes legacy mipmaps and adaptive-icon layers from that file exactly as provided (density resize only). Reinstall the app after changing icons (launchers cache aggressively).
