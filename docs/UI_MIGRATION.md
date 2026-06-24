# UI migration tracker

Branch: `feature/compose-kmp-migration`

## Module layout

| Module | Status |
|--------|--------|
| `polli-core` | KMP `commonMain` — shared logic |
| `polli-domain` | KMP `commonMain` — prefs, routes, repository interfaces |
| `polli-ui` | KMP `commonMain` + `androidMain` — Compose theme + future screens |
| `polli-chat` | Android app shell + DC JNI |

## Screen matrix

| Screen | Compose | Module | Java legacy |
|--------|---------|--------|-------------|
| Home | Yes | app (→ polli-ui) | ConversationListActivity |
| Chat | Yes | app (→ polli-ui) | ConversationActivity |
| Archive | Yes | app | ConversationListArchiveActivity |
| Stories | Yes | app | — |
| Welcome | Yes | app | WelcomeActivity.java |
| Profiles | Yes | app | — |
| ProfileEdit | Yes | app | — |
| NewConversation | Yes | app | NewConversationActivity |
| ContactPicker | Yes | app | — |
| GroupCreate | Yes | app | GroupCreateActivity |
| AppSettings | Partial | app | ApplicationPreferencesActivity |
| Onboarding depth | No | — | InstantOnboardingActivity |
| Media preview | No | — | MediaPreviewActivity |
| All media | No | — | AllMediaActivity |
| QR / backup | No | — | QrActivity, BackupTransferActivity |
| Settings depth | No | — | preference fragments |
| Profile detail | No | — | ProfileActivity |
| Webxdc | No | — | WebxdcActivity |
| Relay | No | — | RelayListActivity |
| Calls | No | — | CallActivity |

## Phase status

- [x] Phase 0: KMP modules scaffolded
- [ ] Phase 1: Move existing screens into `polli-ui`
- [ ] Phase 2A–F: Remaining Java screens
- [ ] Phase 3: Single-Activity NavHost
