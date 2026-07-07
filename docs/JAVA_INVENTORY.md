# Java package inventory (`org.thoughtcrime.securesms`)

Cross-reference: references from `com.polli.android` vs legacy-only Java.

**2026-07 status:** 248 legacy `securesms` Java files remain (down from ~376). Polli chat/home/onboarding run on `polli-engine` JSON-RPC; legacy Java is JNI bootstrap + platform bridges only. Rust core is reached via `com.b44t.messenger` (JNI) and `chat.delta.rpc` (JSON-RPC) — those are the Rust interface and stay.

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
