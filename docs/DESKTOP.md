# Polli Desktop

Compose Desktop shell sharing **polli-ui** and **polli-domain** with the Android app.

## Run

```sh
cd polli-chat
./gradlew :polli-desktop:run
```

Android builds are unchanged:

```sh
./gradlew installFossDebug
```

## Module map

| Module | Role |
|--------|------|
| **polli-desktop** | JVM app entry (`Main.kt`), mock data, account setup UI |
| **polli-ui** | Shared Compose screens/components (`commonMain` + `jvmMain` actuals) |
| **polli-domain** | Repository interfaces, models, prefs |
| **polli-core** | Pure Kotlin helpers (categorizer, time format, chatlist flags) |
| **polli-engine-rpc** | Java JSON-RPC client + `StdioRpcTransport` for desktop |

## Mock vs engine

On launch the app tries to spawn `deltachat-rpc-server` from your `PATH`.

- **Mock data** — used when the binary is missing, or when you choose “Continue with mock data” on the setup screen.
- **Live engine** — add email + password on the setup screen; inbox/archive load via `RpcChatRepository`.

Accounts are stored under `~/.polli/accounts` (`DC_ACCOUNTS_PATH`).

### Install rpc-server (optional)

From source (requires Rust):

```sh
cargo install --path jni/deltachat-core-rust/deltachat-rpc-server
```

Or download a prebuilt binary from [chatmail/core releases](https://github.com/chatmail/core/releases), rename to `deltachat-rpc-server`, and add it to `PATH`.

Verify:

```sh
deltachat-rpc-server --version
```

## What works today

- Inbox list grouped by category (Channels / Spaces / Mail)
- Archive list via shared `ArchiveScreen`
- Media browser layout demo (stub cells)
- Chat open → placeholder screen
- Real sync for inbox/archive when rpc-server + account are configured

## Not yet on desktop

- Full chat feed + composer
- WebRTC / calls, notifications, share intents
- Image loading from blob paths (avatars use colored initials)

## Package (later)

```sh
./gradlew :polli-desktop:packageDistributionForCurrentOS
```
