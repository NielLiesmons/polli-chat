# Dead code & Java prune — migration tracker

Branch: `feature/java-prune-kmp`

Goal: shrink the Delta Chat Android fork surface while keeping Chatmail/Delta core (Rust via JNI) as the engine. Prefer **Kotlin/Compose** for UI and **Rust** (deltachat/chatmail crates) for shared logic — not more Java.

## Principles

1. **Delete only when unreachable** — verify via manifest, `AppNav`, and ripgrep before removing.
2. **Keep JNI + `connect/` + notifications** until a non-Android core binding exists.
3. **Move shareable UI into `polli-ui` commonMain** as screens are pruned from Java.
4. **No product features in `org.thoughtcrime.securesms`** — only plumbing we still need.

## Phase A — Inventory (first PRs)

- [ ] Manifest activity audit: mark each Activity as `polli` / `legacy-bypass` / `infra` / `delete-candidate`
- [ ] Confirm `POLLI_UI` paths never launch legacy chat/home (`ConversationActivity`, `ConversationListActivity`)
- [ ] List Java packages with zero references from `com.polli.android`

## Phase B — Safe deletes (high confidence)

| Target | ~LOC | Notes |
|--------|------|-------|
| `ConversationActivity` + `ConversationFragment` + `ConversationItem` | ~4.3k | Replaced by `ChatActivity` |
| `ConversationListActivity` + archive variant | ~1k | Replaced by `HomeActivity` / `ArchiveActivity` |
| Java welcome/onboarding duplicates | ~1.5k | Polli Compose paths exist |
| `calls/` + `webrtc/` | ~5.5k | Delete if Polli won't ship DC calls |
| `imageeditor/` + `scribbles/` | ~6.7k | Delete if attach markup not needed |

## Phase C — Kotlin replacements

| Java screen | Compose target | Module |
|-------------|----------------|--------|
| `ProfileActivity` | Profiles detail sheet | `polli-ui` |
| `WebxdcActivity` | Webxdc host | app → `polli-ui` |
| `ApplicationPreferencesActivity` | Settings depth | app → `polli-ui` |

## Phase D — KMP extraction

- [ ] Implement `polli-domain` repositories on Android (`DcHelper` behind interfaces)
- [ ] Move home list models + categorizer consumers to shared code
- [ ] First screen fully in `polli-ui` commonMain: TBD (profiles or settings)

## Build check

After each prune batch: `./gradlew assembleFossDebug` + smoke: home → chat → send → back.
