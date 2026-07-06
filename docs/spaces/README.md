# Spaces — protocol architecture

Design notes for **Spaces**: shared encrypted environments (individual, private groups, teams, public communities) built from:

- **[MNS](https://github.com/mlkut/mns)** names and **sigils** for discovery and identity
- **Chatmail + classic email** as the transport layer between users and spaces
- **[Cryptree](https://github.com/mlkut/cryptree)** for files and materialized space state
- **Authorities** that order submissions, admit content, and publish signed snapshots

This is protocol-level documentation. Any client (Polli, mlkut, others) can implement it.

## Documents

| Doc | Contents |
|-----|----------|
| [ARCHITECTURE.md](./ARCHITECTURE.md) | Four-layer model, admission pipeline, authorities, cryptree role, phasing |
| [MNS_AND_SIGILS.md](./MNS_AND_SIGILS.md) | MNS as root authority, DNS records, sigil identity |
| [TRANSPORT_AND_PAYLOADS.md](./TRANSPORT_AND_PAYLOADS.md) | Mail headers for content types; payload format options |

## Reference implementations

| Resource | Role |
|----------|------|
| [mlkut](https://github.com/mlkut) | [mns](https://github.com/mlkut/mns), [cryptree](https://github.com/mlkut/cryptree), [mlkut](https://github.com/mlkut/mlkut) client/server |
| Chatmail / [chatmail/core](https://github.com/chatmail/core) | Encrypted messaging and group transport |
| Polli (`polli-chat`) | Chat-first client; sigil UI; Chatmail fork with `polli-home` routing |

## Status

**Draft / exploratory.** Transport uses mail headers; canonical payload encoding (Postcard, raw MIME, JSON, etc.) is not finalized.
