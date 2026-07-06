# Java package inventory (`org.thoughtcrime.securesms`)

Cross-reference: references from `com.polli.android` vs legacy-only Java.

**2026-07 status:** ~270 Java files remain (down from ~376). Polli chat/home/onboarding run on `polli-engine` JSON-RPC; legacy Java is JNI bootstrap + platform bridges only.

## Removed from Polli path (2026-07)

- `ConversationActivity`, `ConversationFragment`, `ConversationItem` + adapters
- `ConversationListActivity`, archive list, relay list
- Java `WelcomeActivity`, `NewConversationActivity`
- `com.polli.android.bridge.ChatListMapper` (superseded by `RpcChatListMapper` / `ChatRepository`)
- `EngineChatRepository`, `EngineMediaRepository` (superseded by `PolliEngine` / `RpcChatRepository`)
- Manifest zombies: `FullMsgActivity`, `BlockedContactsActivity`, `ConnectivityActivity`
- Dead UI: `ProfileAdapter` cluster, legacy `conversation_item_*` layouts, `reactions/*` Java, ~30 orphan widgets

Polli hosts: `HomeActivity`, `ChatActivity`, `ArchiveActivity`, Compose `WelcomeActivity`, `NewConversationActivity`, `GroupCreateActivity`, `AccountSetupActivity`, `AdvancedOnboardingActivity`, `ProfileDetailActivity`, `ProfilesActivity`, `NotificationSettingsActivity`, `com.polli.android.webxdc.WebxdcActivity` / `WebxdcStoreActivity`.

## Polli platform bridges (`com.polli.android.platform`)

Kotlin adapters — **only** package that should import legacy Java for new code:

| Bridge | Wraps |
|--------|--------|
| `EngineBridge` | `DcHelper` (context, RPC, events, config) |
| `EngineBlobStore` | blob dir copy |
| `AttachmentIntents` | share/export attachment |
| `PlatformMedia` | `MediaUtil`, `PersistentBlobProvider` |
| `PlatformShare` | `ShareUtil`, `SendRelayedMessageUtil` |
| `PlatformAudio` | `AudioPlaybackViewModel`, service |
| `PlatformAttachments` | `AttachmentManager` |
| `LegacyActivities` | Scribble, QR, contact pickers |

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
| `ShareActivity` | `HomeRelayingActivity` + `ShareInbound` | Java share target until manifest retarget |
| `WebViewActivity` | — | Java only (shared WebView base) |

## Do not delete yet

`connect/`, `notifications/`, `imageeditor/`, `scribbles/`, `video/` (composer preview) until replacements land.

## Cryptree / spaces

See [ENGINE_RUST.md](ENGINE_RUST.md) — `Space` / `SpaceDrive` stubs in `polli-domain`; no Kotlin implementation.
