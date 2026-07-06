# MNS names and sigils

The [Mlkut Name System (MNS)](https://github.com/mlkut/mns) provides decentralized naming that avoids ICANN-style squatting. **Spaces** (and people, communities, services) anchor to verifiable on-chain records that point at DNS and service endpoints.

**Sigils** are the visual encoding of the same 40-bit identity — speakable MNS names and deterministic glyphs for avatars and discovery.

---

## MNS as root authority

For Spaces, the **MNS name is the root identity** — not an alias on top of a local chat id.

```
MNS name (on-chain registry, e.g. Rootstock)
    └── DNS / TXT / SRV records (or .mns.alt resolution)
            ├── space inbox (chatmail / SMTP)
            ├── authority endpoints (HTTPS, WebDAV)
            ├── current snapshot pointer (seq, root_hash) — optional fast path
            ├── admission policy reference
            └── member / governance hints (or links to signed docs in cryptree)
```

### Why MNS + sigils instead of ICANN

| ICANN DNS | MNS + sigils |
|-----------|----------------|
| Squatting, renewal politics, registrar lock-in | Registry rules aimed at reducing squatting ([mns](https://github.com/mlkut/mns) WIP) |
| Opaque branding (`company.io`) | Deterministic **sigil** glyph + speakable name (`dozmarbin-wan…`) |
| Centralized WHOIS | On-chain record + client-verified resolution |

Sigil bit layout matches the MNS visualizer (9×9 mirrored grid from 40 bits).

---

## Sigil identity

### 40-bit encoding

- **5×9 source grid** mirrored to **9×9** for rendering
- Encoded as a speakable name: `prefix+suffix-prefix+suffix` (e.g. `dozmarbin-wansamlit`)
- **Chatmail local-part** can carry an MNS name: `dozmarbin-wansamlit@chatmail.example`
- Non-MNS addresses may hash to a **stable derived sigil** so every identity gets a consistent glyph

### Client resolution flow

1. User sees sigil or MNS name
2. Client resolves on-chain / [mns-cli](https://github.com/mlkut/mns) registry → DNS records
3. DNS yields inbox address, authority URLs, optional snapshot hint
4. Client connects transport (chatmail) and state (cryptree) using those endpoints

---

## Chatmail address ↔ MNS name

Chatmail engines use local chat ids for UI rows. Global space identity should bind separately:

| Field | Purpose |
|-------|---------|
| `engine_chat_id` | Local transport row (implementation-specific) |
| `space_mns_name` | Canonical space identity |
| `space_sigil` | Cached visual / lookup key from MNS name |
| `display_name` | Human label (can differ from MNS name) |

A space inbox might use:

- `local-part` derived from the MNS name, or a dedicated subdomain scheme
- Full address published in MNS-linked DNS (`MX`, `SRV`, or protocol-specific `TXT`)

**MNS resolution is source of truth** for authorities and endpoints — not engine-local group metadata alone.

---

## DNS records (illustrative)

Record shapes follow the [mlkut/mns](https://github.com/mlkut/mns) registry spec. Conceptually:

```txt
; illustrative — not final syntax
space.inbox     TXT  "v=1; inbox=design-team@spaces.example; auth=https://auth.example/space/…"
space.authority TXT  "v=1; pubkey=…; role=primary"
space.snapshot  TXT  "v=1; seq=42; root=bafy…; sig=…"
```

Clients:

1. Resolve MNS name → record set
2. Verify signatures / chain state as defined by MNS
3. Cache `(name, sigil, seq, root_hash, endpoints)` locally
4. Fall back to last-good snapshot if authorities disagree (policy-defined)

---

## Governance on-chain vs in cryptree

| On-chain (MNS / registry) | In cryptree `/meta/` |
|---------------------------|----------------------|
| Space name ownership | Display settings, feature flags |
| Authority pubkey set + threshold | Fine-grained admission rules |
| DNS pointer updates | Member roles, bans, automation scripts |
| Optional: membership policy hash | Full audit history |

On-chain defines **who may sign snapshots** and **how to find the space**. Cryptree holds **what the space contains** and most mutable policy detail.

---

## Reference code

| Project | Role |
|---------|------|
| [mlkut/mns](https://github.com/mlkut/mns) | Registry, `*.mns.alt`, CLI for Rootstock testnet |
| [mlkut/cryptree](https://github.com/mlkut/cryptree) | Encrypted tree state |
| [mlkut/mlkut](https://github.com/mlkut/mlkut) | Client/server platform |

---

## Invariants

1. **One canonical MNS name per space** (forks/splits are explicit new names or signed delegation).
2. **Sigil derived from MNS name** when decodable; do not assign conflicting glyphs for the same space.
3. **Authorities listed in MNS records** must sign snapshots clients accept (unless offline cache policy says otherwise).
4. **Engine-local chat ids** are not global space identifiers.
