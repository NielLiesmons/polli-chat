# Java package inventory (`org.thoughtcrime.securesms`)

Cross-reference: references from `com.polli.android` vs legacy-only Java.

**2026-07 status:** 166 legacy `securesms` Java files remain (down from ~376). Polli chat/home/onboarding run on `polli-engine` JSON-RPC; legacy Java is JNI bootstrap + platform bridges only. Rust core is reached via `com.b44t.messenger` (JNI) and `chat.delta.rpc` (JSON-RPC) — those are the Rust interface and stay.

**Dead-code sweep 2026-07-07 (−19 files):** Transitive reachability analysis (Kotlin `com.polli.*` + Java under `com/polli`, all foss/gplay/main source sets, manifests, and a Java↔resource fixpoint counting only *live* layouts) found 19 dead `securesms` Java classes orphaned by the scribbles/imageeditor removal. Deleted: legacy `video/` player package (`VideoPlayer`, `exo/AttachmentDataSource*`), recent-photos rail (`RecentPhotoViewRail`, `database/loaders/RecentPhotosLoader`, `database/CursorRecyclerViewAdapter`), legacy conversation-input orphans (`messagerequests/MessageRequestsBottomView`, `components/HidingLinearLayout`, `ScaleStableImageView`, `SquareFrameLayout`, `registration/PulsingFloatingActionButton`), `util/ParcelUtil`, and two dead-island subsystems: the unused `jobmanager/` package (no `Job` subclasses, `getJobManager()` had no callers — also dropped the field/init from `ApplicationContext`) and `database/model/ThreadRecord` (removed dead `DcHelper.getThreadRecord()`). Kept (look dead, aren't): `geolocation/*` (referenced from flavor `LocationSourceFactory`), `WebViewActivity` (used by `WebxdcActivity.java`), Glide `@GlideModule` classes, and all manifest-declared services/receivers. ~56 now-unreachable layouts still reference deleted classes but are harmless (never inflated) — a separate resource cleanup.

**Media editor foundation → Kotlin/Compose 2026-07-07:** New non-destructive media editor built on a shared, JSON-serialized `SceneDocument` (`com.polli.domain.editor`, kotlinx.serialization) with a swap-ready `DocumentCodec` (`JsonSceneCodec` now; a Postcard/Rust binary codec can drop in later without touching model or UI). Shared `EditorController` + Compose-Multiplatform `MediaEditor` live in `com.polli.ui.editor` (crop presets/free + rotate/flip, video trim scrubber, undo/redo). Android `AndroidSceneExporter` bakes results: images via `android.graphics` (EXIF-correct → rotate/crop → WebP), video via Media3 `Transformer` (`ClippingConfiguration` trim + `Crop`/`ScaleAndRotateTransformation`). `MediaEditorActivity` hosts the editor; `MediaEditLauncher` (renamed from `ImageEditLauncher`) repoints `ChatActivity` (image + **new** video-trim path), `ProfileEditActivity`, `GroupCreateActivity`, `AdvancedOnboardingActivity` (avatar square crop). Added `media3-transformer/effect/common` deps + serialization plugin. Dropped the dead `LegacyScribbleActivity` typealias. **Deleted the legacy `scribbles` + `imageeditor` Java cluster (44 files)** — the chat-first plan intentionally drops the old draw/text/sticker/blur tooling; the new editor covers crop + trim, and drawing can return later on the shared `SceneDocument` foundation. Severed the two external ties: `AttachmentManager.EditButtonListener` (legacy conversation edit button, now no-op) and dead `AvatarHelper.cropAvatar`; removed the `ScribbleActivity`/`StickerSelectActivity` manifest entries. Manual device test needed: pick/crop an image and a video in a chat and send; set an avatar.

**Log viewer + mute dialog → Compose/Kotlin 2026-07-07:** Rewrote `LogViewActivity` + `LogViewFragment` as `com.polli.android.debug.LogViewActivity` (Compose) + `LogDump` (diagnostic report + logcat grab + file write); share/save-to-Downloads (WRITE_EXTERNAL_STORAGE only on <30), zoom, scroll-to-top/bottom, selectable monospace text. Rewrote `MuteDialog` as `com.polli.android.ui.MuteDurationDialog`. Added `PlatformLegacyUtil.{reliableService,isPushEnabled,pushToken,fileProviderUri}`; repointed `AdvancedOnboardingActivity`, `BackupTransferActivity`, `EditRelayActivity`, `ProfileDetailActivity`; dropped `LegacyLogViewActivity`/`LegacyMuteDialog` typealiases; deleted 3 Java files + 2 layouts + 1 menu; manifest updated.

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
