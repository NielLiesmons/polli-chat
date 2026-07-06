# Java package inventory (`org.thoughtcrime.securesms`)

Cross-reference: references from `com.polli.android` vs legacy-only Java.

## Removed from Polli path (2026-07)

- `ConversationActivity`, `ConversationFragment`, `ConversationItem` + adapters
- `ConversationListActivity`, archive list, relay list
- Java `WelcomeActivity`, `NewConversationActivity`

Polli hosts: `HomeActivity`, `ChatActivity`, `ArchiveActivity`, Compose `WelcomeActivity`, `NewConversationActivity`, `GroupCreateActivity`, `AccountSetupActivity`, `AdvancedOnboardingActivity`, `ProfileDetailActivity`, `ProfilesActivity`, `NotificationSettingsActivity`, `com.polli.android.webxdc.WebxdcActivity` / `WebxdcStoreActivity`.

## Polli-referenced (keep — engine / bridge)

| Package | Role |
|---------|------|
| `connect` | Chatmail JNI: `DcHelper`, events, accounts |
| `util` | Share, media, send helpers |
| `components` | Legacy views bridged by Compose (avatars, audio) |
| `qr` | QR scan, backup transfer |
| `mms` | `AttachmentManager` bridges |
| `providers` | Blob / file providers |
| `notifications` | `NotificationCenter` (uses `AppNav`) |
| `preferences` | `NotificationsPreferenceFragment` (hosted by Kotlin `NotificationSettingsActivity`) |
| `scribbles` | Image editor (`ImageEditLauncher`) |

## Legacy UI still in tree

| Target | Polli replacement | Status |
|--------|-------------------|--------|
| `WebViewActivity`, `ConnectivityActivity`, `FullMsgActivity` | — | Java only (shared WebView base) |

## Do not delete yet

`connect/`, `notifications/`, `imageeditor/`, `scribbles/` until replacements or product decisions land.
