# Transport and payloads

Submissions to a Space travel over **chatmail** (group chat, 1:1, internal routing) and **classic email** (SMTP to the space inbox). This doc covers how to **differentiate content types in transit** and what to store **after** admission.

**Decision (current):** use **mail headers** as the primary transport discriminator. Payload encoding inside the body/MIME is deliberately open.

---

## Principle: headers for routing, body for payload

```
┌─────────────────────────────────────────────┐
│  Mail / MIME envelope                       │
│  ┌───────────────────────────────────────┐  │
│  │  Headers  → type, space, op, version │  │  ← transport & admission
│  └───────────────────────────────────────┘  │
│  ┌───────────────────────────────────────┐  │
│  │  Body / parts → actual content or op   │  │  ← format-agnostic
│  └───────────────────────────────────────┘  │
└─────────────────────────────────────────────┘
         │
         ▼ admission + apply
┌─────────────────────────────────────────────┐
│  Cryptree / canonical log                   │  ← may re-encode payload
└─────────────────────────────────────────────┘
```

Chatmail already parses MIME. Classic email gives federation. Headers give routers, filters, and automation a single inspectable layer **without** committing to one serialization yet.

---

## Proposed header set

Prefix convention: `X-Space-*` (final names TBD; may align with [mlkut](https://github.com/mlkut) or Chatmail conventions).

| Header | Required | Purpose |
|--------|----------|---------|
| `X-Space-Type` | yes | Content / operation type (see table below) |
| `X-Space-Id` | yes* | MNS name or sigil-encoded identity of target space |
| `X-Space-Op-Id` | for mutations | Stable id for dedup across transport retries |
| `X-Space-Seq` | no | Client hint; authority assigns canonical seq |
| `X-Space-Payload-Format` | no | Hint for body parser: `mime`, `postcard`, `json`, `raw` |
| `X-Space-Schema` | no | Version string for op semantics (e.g. `task/1`) |
| `X-Space-Sig` | yes for commit | Signature over canonical header set + body hash |

\*Omitted when `To:` already uniquely identifies the space inbox.

### `X-Space-Type` values (initial)

| Value | Meaning | Typical admission |
|-------|---------|-------------------|
| `chat.message` | Human chat line (may still mirror to cryptree optionally) | Member auto |
| `chat.reaction` | Reaction to a message id | Member auto |
| `task.create` / `task.update` | Task board ops | Member / rule |
| `article.publish` | Long-form article or post | Mod / rule |
| `video.publish` | Video metadata + blob refs | Mod / rule |
| `file.put` / `file.delete` | Drive mutations | Member / rule |
| `space.op` | Generic signed operation for applier | Policy-defined |
| `space.snapshot` | Authority-published snapshot announcement | Authority only |

Subtypes use dotted names; routers only need the first segment for coarse filtering (`chat.*`, `file.*`).

### Example (illustrative)

```http
From: alice@example.com
To: dozmarbin-wansamlit@spaces.example
Subject: Re: roadmap
X-Space-Type: task.create
X-Space-Id: dozmarbin-wansamlit
X-Space-Op-Id: 7f3a9c2e-…
X-Space-Payload-Format: postcard
X-Space-Schema: task/1
X-Space-Sig: base64(…)
Content-Type: application/octet-stream

<binary postcard bytes>
```

Same structure works for group-internal Chatmail messages if the engine preserves custom headers.

---

## Payload formats (open)

The canonical log and cryptree **may store a different representation** than what was sent. Transport is “whatever works”; admission normalizes.

| Format | Pros | Cons | Good for |
|--------|------|------|----------|
| **Raw mail** | Human-readable in any client; attachments native | Verbose; harder to diff/apply | Articles, external SMTP ingress |
| **MIME multipart** | Standard tooling; mix text + files | Parsing overhead | Chat with media, email interop |
| **Postcard (binary)** | Compact; schema-evolvable | Opaque on the wire; needs schema registry | `task.*`, `file.*` ops between cooperating clients |
| **JSON** | Debuggable; web-friendly | Larger; float/unicode footguns | Prototypes, public HTTP APIs |
| **Content-addressed blob** | Header points at `bafy…`; body empty | Needs blob fetch | Large video/file |

### `X-Space-Payload-Format` negotiation

1. Sender sets format hint + `Content-Type`
2. Admission applier dispatches parser registry: `postcard` → typed decode, `raw` → store verbatim under `/mail/inbox/…`, etc.
3. Materialized cryptree uses **whatever is most workable per subtree** — not required to match wire format

**No decision yet** on a single mandatory format. Early deployments might use:

- `chat.message` → plain text / Chatmail body (zero migration)
- `file.put` → MIME attachment + hash refs in a structured op part
- Authorities store normalized Postcard or CBOR in the log; cryptree leaves hold file bytes

---

## Content types vs cryptree subtrees

| `X-Space-Type` | Suggested cryptree destination | Notes |
|----------------|-------------------------------|--------|
| `chat.message` | optional `/chat/…` or mail-only | Prefer transport for live chat |
| `task.*` | `/tasks/…` | Structured state |
| `article.publish` | `/articles/…` or `/files/…` | May keep raw MIME |
| `video.publish` | `/files/…` + sidecar metadata | Blob by hash |
| `file.*` | `/files/…` | Primary drive path |
| `space.op` | applier-defined | Extension point |

---

## Admission inspects headers first

Pipeline:

1. **Ingress** — SMTP or Chatmail delivers message
2. **Header parse** — unknown `X-Space-Type` → `pending` or reject per policy
3. **Signature verify** — `X-Space-Sig` + sender identity
4. **Policy** — member? authority? external?
5. **Body parse** — according to `X-Space-Payload-Format`
6. **Apply** — append log, update cryptree, publish snapshot
7. **Notify** — Chatmail group ping; optional `space.snapshot` header on authority mail

Automation rules can filter on headers only (`X-Space-Type: article.publish` → always pending).

---

## Interop with classic email

External senders may omit Space headers. Options:

| Case | Handling |
|------|----------|
| No `X-Space-*` | Treat as `article.publish` or generic `mail.external`; always pending |
| Signed with S/MIME/OpenPGP only | Verify, then wrap as `space.op` with `payload-format=raw` |
| Attachments only | Infer `file.put` candidates; still pending unless from member |

Outbound to non-participating mail users: plain `Subject` + body; Space headers optional for humans, required for appliers.

---

## Relation to Chatmail spec

[Chatmail](https://github.com/chatmail/core/blob/main/spec.md) defines message semantics, encryption, and groups. Space headers are an **extension layer**:

- Do not break Autocrypt / verification
- Prefer headers that survive relay and quoting rules
- For group messages, the space applier is a logical subscriber — may run in-process before content appears as committed drive state

Engine work: preserve custom headers in Chatmail forks; add a `space-applier` (or equivalent) crate for parse, queue, apply.

---

## Next steps

1. Confirm which custom headers survive Chatmail send/receive
2. Prototype `X-Space-Type: chat.message` (no body change) end-to-end
3. Pick first binary format for `task.create` (Postcard is a leading candidate)
4. Document authority `space.snapshot` announcement format alongside MNS DNS records

See [ARCHITECTURE.md](./ARCHITECTURE.md) and [MNS_AND_SIGILS.md](./MNS_AND_SIGILS.md).
