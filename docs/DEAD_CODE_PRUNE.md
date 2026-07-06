# Dead code & Java prune — migration tracker

Branch: `feature/java-prune-kmp`

Goal: shrink the legacy Android fork surface while keeping the Chatmail engine (Rust via JNI) as the runtime. Prefer **Kotlin/Compose** for UI and **Rust** (chatmail crates) for shared logic — not more Java.

## Principles

1. **Replace before delete** — never remove a user-facing feature until the Polli path has parity (e.g. image editor, attach draft, calls).
2. **Delete only when unreachable** — verify via manifest, `AppNav`, and ripgrep before removing.
3. **Keep JNI + `connect/` + notifications** until a non-Android core binding exists.
4. **Bridge legacy Java temporarily** — thin Kotlin wrappers (`ImageEditLauncher`, `MediaSend`) are fine while migrating.
5. **Move shareable UI into `polli-ui` commonMain** as screens are pruned from Java.

## Feature parity checklist (Polli vs legacy DC)

| Feature | Legacy | Polli status | Action |
|---------|--------|--------------|--------|
| Chat feed + composer | `ConversationActivity` | `ChatActivity` | **Removed** — Compose chat only |
| Image editor on attach | `ScribbleActivity` | **Wired** via `ImageEditLauncher` | Compose editor later; keep Java editor until then |
| Attach draft (preview before send) | `AttachmentManager` | **Wired** — composer preview + caption on send | OK |
| Gallery / file / contact / location | `AttachmentManager` | Partial in `ChatActivity` | Keep bridging |
| Voice record + send | DC input panel | `ChatComposer` | OK |
| Calls / WebRTC | `CallActivity` | **Dropped** — not in Polli product | **Removed** (~5.5k LOC) |
| Profile avatar crop | `ScribbleActivity` + `AvatarHelper` | **Wired** via `ImageEditLauncher(cropAvatar=true)` | OK |
| Webxdc | `com.polli.android.webxdc.WebxdcActivity` | Java WebView host (engine in Rust core) | Compose host later |
| Settings depth | `ApplicationPreferencesActivity` + preference fragments | **Done** — `ProfilesActivity` + `NotificationSettingsActivity` |

## Phase A — Inventory

- [x] Confirm Polli chat bypassed image editor (fixed: `ImageEditLauncher`)
- [x] Manifest: launcher via `RoutingActivity` → `LauncherActivity` → `HomeActivity`
- [x] Legacy chat redirect removed — `ChatActivity` is the only chat host
- [x] Share intents route to `HomeActivity` / `ChatActivity` with relay support
- [x] List Java packages with zero references from `com.polli.android` — see [JAVA_INVENTORY.md](JAVA_INVENTORY.md)

## Phase B — Safe deletes (only after parity)

| Target | ~LOC | Prerequisite |
|--------|------|----------------|
| `ConversationActivity` + `ConversationFragment` + `ConversationItem` | ~4.3k | **Removed** |
| `ConversationListActivity` + archive | ~1k | **Removed** — `HomeActivity` / `ArchiveActivity` only |
| Java welcome/onboarding duplicates | ~1.5k | **Done** — Compose welcome + `AdvancedOnboardingActivity` |
| `imageeditor/` + `scribbles/` | ~6.7k | **Compose image editor** — do not delete until then |
| `calls/` + `webrtc/` | ~5.5k | **Removed** — Polli does not ship video calls |

## Phase C — Kotlin replacements

| Java screen | Compose target | Module |
|-------------|----------------|--------|
| `ProfileActivity` + `ProfileFragment` | `ProfileDetailActivity` | **Done** |
| `WebxdcActivity` | Webxdc host | app → `polli-ui` |
| `ApplicationPreferencesActivity` + settings fragments | Settings depth | **Removed** |
| `ScribbleActivity` | Image editor | app → `polli-ui` (long term) |

## Phase D — KMP extraction

- [x] `polli-core`: `ChatCategorizer`, `ChatSummaryFormat`, `ChatKind`, `ChatMediaFilter`, `MsgTypes`
- [x] `polli-domain`: `InboxItem`, `ArchiveLinkState`, `ChatIntentExtras`, `ChatRepository`, `MediaRepository`
- [x] Android adapters: `EngineChatRepository`, `EngineMediaRepository` via `PolliRepositories`
- [x] Home + archive wired through `ChatRepository` (`HomeViewModel`, `InboxLoad`, `ArchiveLoad`)
- [x] `polli-ui`: `ChatComingSoonTab`, `ChatInboxCard`; chat Files tab inline via `ChatMediaTabPanel` + `MediaRepository`
- [x] Composer attach draft preview before send (images, video, files)
- [x] Move `ChatMediaBrowser` into `polli-ui` commonMain (platform thumb/list slots)
- [x] First screen fully in `polli-ui` commonMain: **ArchiveScreen**
- [x] Home tab: single **Home** feed (spaces + mail), mail badge on inbox avatars

## Phase E — Repo hygiene (Delta Chat fork cruft)

- [x] Remove bundled `src/main/assets/help/` (~30k lines HTML, 16 locales)
- [x] Remove `LocalHelpActivity` + `calls/` assets; `openHelp()` opens browser only
- [x] Remove root DC docs: `CHANGELOG.md`, `BUILDING.md`, `CONTRIBUTING.md`, `RELEASE.md`, `standards.md`, `Dockerfile`, `flake.nix`
- [x] Polli build docs: [BUILDING.md](BUILDING.md)

Still upstream / prune later: `org/thoughtcrime/securesms/` (~64k LOC Java), translated `values-*/strings.xml`, `fastlane/`, F-Droid metadata in `docs/f-droid.md`.

## Build check

After each batch: `./gradlew assembleFossDebug` + smoke: home → chat → attach image → edit → send → back.
