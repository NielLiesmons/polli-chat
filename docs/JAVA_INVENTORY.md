# Java package inventory (`org.thoughtcrime.securesms`)

Cross-reference: references from `com.polli.android` vs legacy-only Java.

**2026-07 status:** 232 legacy `securesms` Java files remain (down from ~376). Polli chat/home/onboarding run on `polli-engine` JSON-RPC; legacy Java is JNI bootstrap + platform bridges only. Rust core is reached via `com.b44t.messenger` (JNI) and `chat.delta.rpc` (JSON-RPC) — those are the Rust interface and stay.

**QR / backup screens → Compose 2026-07-07:** Rewrote the whole `securesms.qr` UI cluster as Kotlin/Compose in `com.polli.android.qr`: `QrShowActivity` (show invite QR + share/withdraw), `QrActivity` (scanner-only capture), `RegistrationQrActivity` (registration / add-second-device scanner + confirm dialog), `BackupTransferActivity` (sender show-QR / receiver progress, foreground-service + IMEX progress merged from the two fragments). The proven zxing engine is kept (`DecoratedBarcodeView` + `CaptureManager` hosted via `AndroidView`; `InterceptingCaptureManager` in `ZxingScanner.kt`). New helpers: `QrSvg.fixSvg`, `WifiSsid`, and `PlatformLegacyUtil.{isNetworkConnected,openHelp,maybeShowMigrationError}`; `appendBackupTransferSsid` reimplemented on `WifiSsid`. Deleted 9 Java files (`QrShowActivity/QrShowFragment/QrActivity/QrScanFragment/CustomCaptureManager/RegistrationQrActivity/BackupTransferActivity/BackupProviderFragment/BackupReceiverFragment`) + `LegacyQrExtras.kt` + 15 layouts/menus; dropped the `LegacyQrShow*`/`LegacyRegistrationQrActivity`/`LegacyBackupTransferActivity` typealiases; repointed `ProfileDetailActivity`, `WelcomeActivity`, `AdvancedOnboardingActivity`, `QrHubActivity`, `DcHelper` (unencrypted dialog → `QrHubActivity`), `QrCodeHandler`; manifest updated. `QrCodeHandler.java` (QR→securejoin/relay/account dispatch) is kept as a Rust bridge, not a screen. ⚠ Manual device test needed: camera scanning + add-second-device backup transfer require a real camera / two devices.

**Transport screens → Compose 2026-07-07:** Rewrote `relay/RelayListActivity` + `RelayListAdapter`, `relay/EditRelayActivity` (manual IMAP/SMTP config form), and `proxy/ProxySettingsActivity` + `ProxyListAdapter` as Kotlin/Compose in `com.polli.android.transports`. Same RPC surface (`chat.delta.rpc` transports/proxy), same string/color resources. Deleted the 5 Java files + 8 layouts + 1 menu; dropped `LegacyRelay/EditRelay/ProxySettings` typealiases; repointed `QrCodeHandler` + `AdvancedOnboardingActivity`; manifest updated (proxy deep-link `ss`/`socks5` preserved). ⚠ Manual test needed: adding a relay/proxy requires a real server.

**Dead-code sweep 2026-07-07:** Removed 14 unreachable classes (video transcoding `video/recode/*` except Sample/Track, `TransportOptions*`, subsampling decoders, `FutureTaskListener`, span utils). Reachability computed via transitive closure from the kept surface (Kotlin/manifest/XML). Note: `@GlideModule SignalGlideModule` + `ContactPhotoLoader`/`ContactPhotoFetcher` look unreferenced but are discovered by Glide annotation processing and register the `ContactPhoto` loader used by avatars/quotes/notifications — keep them.

## Removed from Polli path (2026-07)

- `ConversationActivity`, `ConversationFragment`, `ConversationItem` + adapters
- `ConversationListActivity`, archive list, relay list
- Java `WelcomeActivity`, `NewConversationActivity`
- `com.polli.android.bridge.ChatListMapper` (superseded by `RpcChatListMapper` / `ChatRepository`)
- `EngineChatRepository`, `EngineMediaRepository` (superseded by `PolliEngine` / `RpcChatRepository`)
- Manifest zombies: `FullMsgActivity`, `BlockedContactsActivity`, `ConnectivityActivity`
- Dead UI: `ProfileAdapter` cluster, legacy `conversation_item_*` layouts, `reactions/*` Java, ~30 orphan widgets

Polli hosts: `HomeActivity`, `ChatActivity`, `ArchiveActivity`, Compose `WelcomeActivity`, `NewConversationActivity`, `GroupCreateActivity`, `AccountSetupActivity`, `AdvancedOnboardingActivity`, `ProfileDetailActivity`, `ProfilesActivity`, `NotificationSettingsActivity`, `com.polli.android.webxdc.WebxdcActivity` / `WebxdcStoreActivity`.

**2026-07-07:** Onboarding, profiles, QR, home, notes, and media screens no longer import `DcHelper` directly — all go through `com.polli.android.platform` bridges. `PolliShareActivity` is Kotlin-native; legacy `ShareActivity.java`, `ResolveMediaTask.java`, and `MailtoUtil.java` deleted. Remaining direct legacy imports: `org.thoughtcrime.securesms.R`, `webxdc/*.java` (feature-flagged off).

## Polli platform bridges (`com.polli.android.platform`)

Kotlin adapters — **only** package that should import legacy Java for new code:

| Bridge | Wraps |
|--------|--------|
| `EngineBridge` | `DcHelper` (context, RPC, events, config, blob copy) |
| `EngineBlobStore` | blob dir copy |
| `AttachmentIntents` | share/export attachment |
| `PlatformMedia` | `MediaUtil`, `PersistentBlobProvider` |
| `PlatformShare` | `ShareUtil`, `SendRelayedMessageUtil` |
| `PlatformAudio` | `AudioPlaybackViewModel`, service |
| `PlatformAttachments` | `AttachmentManager` |
| `PlatformAvatars` | `AvatarHelper` |
| `PlatformPrefs` | `Prefs` |
| `PlatformAccounts` | `AccountManager` |
| `PlatformDialogs` | `ProgressDialog` |
| `PlatformLegacyUtil` | `IntentUtils`, `Util` (background thread, browser) |
| `PlatformDates` | `DateUtils` |
| `LegacyActivities` | Scribble, QR, contact pickers, proxy, relay |
| `LegacyQrExtras` | backup transfer intent extras |

## Polli-referenced (keep — engine / bridge)

| Package | Role |
|---------|------|
| `connect` | Chatmail JNI: `DcHelper`, events, accounts |
| `util` | Share, media, send helpers (bridged via `platform/`) |
| `components` | Legacy views bridged by Compose (avatars, audio) |
| `qr` | QR scan, backup transfer |
| `mms` | `AttachmentManager` bridges |
| `providers` | Blob / file providers |
| `notifications` | `NotificationCenter` (uses `AppNav`) |
| `preferences` | `NotificationsPreferenceFragment` |
| `scribbles` | Image editor (`ImageEditLauncher`) |

## Legacy UI still in tree

| Target | Polli replacement | Status |
|--------|-------------------|--------|
| `ShareActivity` (Java impl) | `PolliShareActivity` (Kotlin) | Fully ported 2026-07-07; Java class unused by manifest |
| `WebViewActivity` | — | Java only (shared WebView base) |

## Do not delete yet

`connect/`, `notifications/`, `imageeditor/`, `scribbles/`, `video/` (composer preview) until replacements land.

## Cryptree / spaces

See [ENGINE_RUST.md](ENGINE_RUST.md) — `Space` / `SpaceDrive` stubs in `polli-domain`; no Kotlin implementation.
