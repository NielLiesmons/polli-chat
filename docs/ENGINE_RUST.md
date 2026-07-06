# Engine logic (Rust)

Non-UI business logic for Polli lives in **Rust** under `jni/deltachat-core-rust/`.

## Layout

| Crate | Role |
|-------|------|
| `deltachat` | Chatmail core (fork) |
| `deltachat-jsonrpc` | JSON-RPC API surface |
| `polli-home` | Polli-specific home/inbox routing (tab categorization, archive link rules, etc.) |
| `polli-spaces` *(planned)* | Cryptree-backed spaces + MNS naming — see [spaces/ARCHITECTURE.md](spaces/ARCHITECTURE.md) |

Kotlin modules (`polli-core`, `polli-engine`, `polli-domain`) are **thin adapters** at platform boundaries: JNI, JSON-RPC transport, Compose state. Do not add new categorization or engine rules there — add them in Rust and mirror only where the type system requires it.

Domain stubs for the spaces path: `com.polli.domain.model.space` (`Space`, `MnsName`, `InboxSource`, `SpaceDrive`) and `SpaceRepository` in `polli-domain` (`EmptySpaceRepository` in `polli-engine` returns no rows until `polli-spaces` lands).

## Desktop RPC

Desktop uses `deltachat-rpc-server` + `polli-engine`. Inbox rows are parsed in `RpcChatListMapper` (JSON boundary only); tab routing must match `polli-home::categorize`, archive link visibility must match `polli-home::archive_link_state` (mirrored in `polli-domain` `ArchiveLinkRules`), and reserved chat ids must be filtered with `polli-home::is_listable_inbox_chat` (mirrored in `polli-core` `InboxFilterRules`).

Android uses the same `polli-engine` module with FFI JSON-RPC transport (`FFITransport` → `RpcMessageRepository`, `RpcChatRepository`, `RpcEventLoop`).

**RPC gotcha:** `get_chatlist_entries` takes `query_contact_id: Option<u32>`. Pass `null`, not `0` — `0` means “filter by contact 0” and returns an empty list. JNI used `0` as “no filter”; JSON-RPC does not.

When extending the engine API, prefer new methods or fields on the Rust JSON-RPC layer, then consume from Kotlin/Swift/etc.
