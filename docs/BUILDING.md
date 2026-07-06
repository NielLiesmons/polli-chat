# Building Polli

Polli is an Android + desktop (KMP) app on top of the **chatmail** Rust engine (`jni/deltachat-core-rust/`).

## Requirements

- JDK 17+
- Android SDK (API 35) + NDK (see `build.gradle` for the pinned version)
- Rust toolchain (`rustup`) when rebuilding native libs

## Android (primary)

```bash
./gradlew assembleFossDebug    # APK → build/outputs/apk/foss/debug/
./gradlew installFossDebug    # install on connected device
```

Smoke path after UI changes: home → chat → attach → send → back.

## Desktop (KMP)

```bash
./gradlew :polli-desktop:run
```

Accounts default to `~/.polli/accounts`. Engine uses JSON-RPC (`deltachat-rpc-server` + `polli-engine-rpc`).

## Architecture pointers

| Layer | Location |
|-------|----------|
| Compose UI (shared) | `polli-ui/` |
| Domain + repository interfaces | `polli-domain/` |
| Kotlin adapters (thin) | `polli-core/`, `polli-engine-rpc/`, `com.polli.android.data.engine` |
| Engine + Polli rules | `jni/deltachat-core-rust/` (`polli-home` crate) |
| Legacy Android shell (pruning) | `src/main/java/org/thoughtcrime/securesms/` |

See [ENGINE_RUST.md](ENGINE_RUST.md) and [DEAD_CODE_PRUNE.md](DEAD_CODE_PRUNE.md).

## Native / Rust

The Android app loads prebuilt JNI from the Gradle build. To work on core:

```bash
cd jni/deltachat-core-rust
cargo test -p polli-home
cargo build -p deltachat_ffi --release   # when touching FFI
```

Desktop uses `deltachat-rpc-server` (stdio JSON-RPC), not JNI.
