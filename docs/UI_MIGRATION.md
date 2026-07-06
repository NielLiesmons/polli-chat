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
| AppSettings | Yes | app | ProfilesActivity + NotificationSettingsActivity |
| Onboarding depth | Yes | app | AdvancedOnboardingActivity |
| Media preview | Yes | app | MediaPreviewActivity |
| All media | Yes | app | AllMediaActivity |
| QR hub | Yes | app | QrActivity |
| Account setup | Yes | app | AccountSetupActivity + AdvancedOnboardingActivity |
| Settings depth | No | — | preference fragments |
| Profile detail | Yes | app | ProfileDetailActivity |
| Webxdc | No | — | WebxdcActivity |
| Relay | No | — | RelayListActivity |
| Calls | No | — | CallActivity |

## Phase status

- [x] Phase 0: KMP modules scaffolded
- [x] Phase 1 (partial): Media preview, all media, onboarding, QR hub via AppNav
- [ ] Phase 1: Move existing screens into `polli-ui`
- [ ] Phase 2A–F: Remaining Java screens
- [ ] Phase 3: Single-Activity NavHost
