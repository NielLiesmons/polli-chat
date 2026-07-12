# Polli

![Your Handle. Your Spaces. For Chat, Mail, Drive & More.](assets/branding/readme-hero.png)

**Super-app with encrypted Chat, Drive, Mail & more. On a domain that's simply yours.**

Polli is for everyday life online — chat with people you know, keep files in your spaces, get mail at your handle, and stay in control of what's private and what's shared.

---

## Why Polli

### Encrypted by default, share on your terms

Polli is **private-first**, not private-only. A **space** is the foundation — likely its own [MNS](https://github.com/mlkut/mns) domain — and the same model covers a personal profile drive *and* a group or community drive. Chat, files, and state live in that space; nothing is visible outside it unless you grant it.

**ACLs on each file and folder** specify which profiles can read or write — per path, not once for the whole space.

Each space type starts with **sensible defaults**:

- **Personal / profile space** — encrypted and private to you by default.
- **Private group space** — by default, members can read and write shared content (a practical default for cryptree performance); you can tighten to read-only or exclude specific members per path.
- **Public community space** — by default, much of the drive is readable publicly; you can still keep private branches on the same tree.

Publishing outward (a link, a channel, mail to a space inbox, content on the web) is always explicit. The same space can hold quiet coordination and public-facing material; you set ACLs where you need them.

### Chat-first, not chat-only

Polli organizes life around **Spaces**: places for a household, a project, a club, or just you. Chat is how you stay in sync; files, tasks, articles, and posts live in the same space without turning the app into a cluttered dashboard. Talk first; structure when you need it.

### Built on standards that already exist

Polli does not ask the world to adopt a new network overnight.

- **Chatmail** ([Delta Chat core](https://github.com/chatmail/core)) for encrypted messaging, groups, and delivery that works offline
- **Email** so anyone can reach a space inbox from a normal mail client
- **[MNS](https://github.com/mlkut/mns)** names and **sigils** for identity without ICANN squatting
- **[Cryptree](https://github.com/mlkut/cryptree)** for encrypted shared files and durable space state

Standards mean other clients can participate. Polli is one implementation, not the platform.

### Open-ended and interoperable

Headers, MIME, SMTP, WebDAV: boring on purpose. Content types (chat, tasks, articles, files) ride on mail-shaped transport so routers, filters, and future apps can interoperate without a proprietary SDK.

### Cheap to run, seriously

No megascale cloud bill required. A space can be served from a small VPS, a home box, or a provider you trust. Chatmail already runs lean; state is content-addressed blobs and signed snapshots, not an expensive always-on database cluster. Self-host when you want; pay a few euros when you do not.

---

## Current status

Android app with encrypted chat (groups, mail, channels), voice and attachments, and MNS sigils — actively building toward shared drive and full space inboxes. See [docs/spaces](./docs/spaces/) for the protocol roadmap.
