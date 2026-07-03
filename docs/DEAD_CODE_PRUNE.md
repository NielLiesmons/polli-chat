# Dead code & Java prune — migration tracker

Branch: `feature/java-prune-kmp`

Goal: shrink the Delta Chat Android fork surface while keeping Chatmail/Delta core (Rust via JNI) as the engine. Prefer **Kotlin/Compose** for UI and **Rust** (deltachat/chatmail crates) for shared logic — not more Java.

## Principles

1. **Replace before delete** — never remove a user-facing feature until the Polli path has parity (e.g. image editor, attach draft, calls).
2. **Delete only when unreachable** — verify via manifest, `AppNav`, and ripgrep before removing.
3. **Keep JNI + `connect/` + notifications** until a non-Android core binding exists.
4. **Bridge legacy Java temporarily** — thin Kotlin wrappers (`ImageEditLauncher`, `MediaSend`) are fine while migrating.
5. **Move shareable UI into `polli-ui` commonMain** as screens are pruned from Java.

## Feature parity checklist (Polli vs legacy DC)

| Feature | Legacy | Polli status | Action |
|---------|--------|--------------|--------|
| Chat feed + composer | `ConversationActivity` | `ChatActivity` | Replace-only prune when manifest gated |
| Image editor on attach | `ScribbleActivity` | **Wired** via `ImageEditLauncher` | Compose editor later; keep Java editor until then |
| Attach draft (preview before send) | `AttachmentManager` | **Missing** | Add composer pending attachment UI |
| Gallery / file / contact / location | `AttachmentManager` | Partial in `ChatActivity` | Keep bridging |
| Voice record + send | DC input panel | `ChatComposer` | OK |
| Calls / WebRTC | `CallActivity` | Not in Polli UI | Replace or explicitly drop — do not silent-delete |
| Profile avatar crop | `ScribbleActivity` + `AvatarHelper` | **Missing** on `ProfileEditActivity` | Wire `ImageEditLauncher(cropAvatar=true)` |
| Webxdc | `WebxdcActivity` | Java only | Compose host later |
| Settings depth | `ApplicationPreferencesActivity` | Partial | Compose settings |

## Phase A — Inventory

- [x] Confirm Polli chat bypassed image editor (fixed: `ImageEditLauncher`)
- [ ] Manifest activity audit: `polli` / `legacy-bypass` / `infra` / `delete-candidate`
- [ ] Confirm `POLLI_UI` paths never launch legacy chat/home
- [ ] List Java packages with zero references from `com.polli.android`

## Phase B — Safe deletes (only after parity)

| Target | ~LOC | Prerequisite |
|--------|------|----------------|
| `ConversationActivity` + `ConversationFragment` + `ConversationItem` | ~4.3k | Polli chat + attach draft parity |
| `ConversationListActivity` + archive | ~1k | `HomeActivity` / `ArchiveActivity` only |
| Java welcome/onboarding duplicates | ~1.5k | Polli onboarding paths verified |
| `imageeditor/` + `scribbles/` | ~6.7k | **Compose image editor** — do not delete until then |
| `calls/` + `webrtc/` | ~5.5k | Product decision + replacement |

## Phase C — Kotlin replacements

| Java screen | Compose target | Module |
|-------------|----------------|--------|
| `ProfileActivity` | Profiles detail sheet | `polli-ui` |
| `WebxdcActivity` | Webxdc host | app → `polli-ui` |
| `ApplicationPreferencesActivity` | Settings depth | app → `polli-ui` |
| `ScribbleActivity` | Image editor | app → `polli-ui` (long term) |

## Phase D — KMP extraction

- [ ] Implement `polli-domain` repositories on Android (`DcHelper` behind interfaces)
- [ ] Move home list models + categorizer consumers to shared code
- [ ] First screen fully in `polli-ui` commonMain: TBD

## Build check

After each batch: `./gradlew assembleFossDebug` + smoke: home → chat → attach image → edit → send → back.
