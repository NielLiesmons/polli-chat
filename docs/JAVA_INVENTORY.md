# Java package inventory (`org.thoughtcrime.securesms`)

Branch: `feature/java-prune-kmp`

Cross-reference: references from `com.polli.android` vs total Kotlin/Java references in the app module.

## Polli-referenced (keep — engine / bridge)

| Package | Polli refs | Role |
|---------|------------|------|
| `connect` | 34 | Chatmail JNI: `DcHelper`, events, accounts |
| `util` | 10 | Share, media, send helpers |
| `components` | 9 | Legacy views bridged by Compose (avatars, audio) |
| `qr` | 5 | QR scan (partial Polli replacement) |
| `mms` | 3 | `AttachmentManager` bridges |
| `providers` | 2 | Blob / file providers |
| `audio` | 1 | Audio playback bridge |
| `notifications` | 1 | `NotificationCenter` (uses `AppNav`) |
| `permissions` | 1 | Permission prompts |
| `profiles` | 1 | Avatar disk helpers |
| `recipients` | 1 | Recipient model for shortcuts |
| `scribbles` | 1 | Image editor (`ImageEditLauncher`) |
| `service` | 1 | Background / playback services |

## Zero Polli references (legacy UI / infra only)

| Package | Notes | Prune when |
|---------|-------|------------|
| `accounts` | Java account UI | Compose account switcher |
| `animation` | Legacy transitions | Unused after Java UI gone |
| `attachments` | Legacy attachment views | Composer parity complete |
| `calls` | WebRTC UI | Product decision |
| `contacts` | Java contact pickers | Polli pickers only |
| `crypto` | Legacy crypto UI | N/A (mostly unused) |
| `database` | Old DB layer | Engine-only path |
| `geolocation` | Location attach | Polli location bridge |
| `glide` | Glide wrappers | Keep until media in KMP |
| `imageeditor` | Java editor | Compose editor |
| `jobmanager` | Async jobs | Infra — keep |
| `messagerequests` | Request UI | Polli inbox handles |
| `preferences` | Settings fragments | Compose settings depth |
| `proxy` | Proxy UI | Polli settings |
| `reactions` | Legacy reaction UI | Polli bubbles OK |
| `relay` | SMTP relay | Infra if unused |
| `search` | Java search | Polli home search |
| `video` | Video recode | Attach pipeline |
| `webrtc` | Calls stack | Product decision |
| `webxdc` | Mini-app host | Compose Webxdc host |

## Top-level Java activities (legacy UI)

Still in tree but **gated** when `POLLI_UI`: `ConversationActivity`, `ConversationListActivity`, `ConversationListArchiveActivity`, `WelcomeActivity`, `NewConversationActivity`, etc.

Polli replacements: `HomeActivity`, `ChatActivity`, `ArchiveActivity`, `WelcomeActivity`, `NewConversationActivity` (Kotlin).

## Next safe targets (after verification)

1. `ConversationListActivity` + relay — unreachable when redirects + non-exported
2. `ConversationListArchiveActivity` — replaced by `ArchiveActivity` + `ArchiveScreen`
3. Java welcome/onboarding duplicates — Polli onboarding verified

Do **not** delete: `connect/`, `notifications/`, `calls/`, `webrtc/`, `imageeditor/`, `scribbles/` until replacements or product decisions land.
