# Polli × Delta Chat — QA Matrix

All user-facing navigation goes through `com.polli.android.navigation.AppNav`.

Reference: Polli web at 390px width (`polli/` + `polli/assets/global.css`).

## Home & data

| Flow | Must pass | Status |
|------|-----------|--------|
| Spaces tab | Shows `DC_CHAT_TYPE_GROUP` chats only | Wired via `ChatCategorizer` |
| Mail tab | Shows 1:1 chats only | Wired |
| Story row | Shows mailing lists + in/out broadcast | Wired |
| Live refresh | List updates on incoming messages while home visible | `HomeViewModel` + `DcEventCenter` |
| Unread badges | Shows actual fresh count (not 0/1) | `getFreshMsgCount` |
| Empty states | Sync hint when offline; channel hint when only lists exist | Wired |

## Stories

| Flow | Must pass | Status |
|------|-----------|--------|
| Posts load | Text + attachment messages shown | `StoriesViewModel` |
| Live updates | New channel posts appear without restart | DC events |
| Progress | 2px bars, 5.5s segment | Wired |
| Chrome | Polli play/pause/cross icons | Wired |
| Private reply | Composer sends quoted reply | Wired |

## Chat composer

| Flow | Must pass | Status |
|------|-----------|--------|
| Default trailing | Voice icon 24dp, White33 | Wired |
| With draft | Send 16dp blurple circle | Wired |
| Plus | Polli plus icon → attach modal | Wired |
| Attach | Camera, browse files | Activity result launchers |
| Voice | Press-hold, swipe-left cancel, send | `VoiceRecorderBridge` |

## Navigation / Compose flows

| Flow | Must pass | Status |
|------|-----------|--------|
| Home + | Polli new conversation sheet (not Java DC) | `NewConversationActivity` |
| New 1:1 | Contact picker → chat | `ContactPickerActivity` |
| New group/broadcast | Compose create → chat | `GroupCreateActivity` |
| Profiles | Floating header, safe area insets | Rewritten |
| Settings | Polli UI, DC config toggles | `AppSettingsActivity` |
| Onboarding entry | Polli welcome when unconfigured | `WelcomeActivity` |
| Profile edit | Compose edit name | `ProfileEditActivity` |

## Theme & icons

| Flow | Must pass | Status |
|------|-----------|--------|
| Background | `#121212` gray mode on Polli activities | `BaseComposeActivity` |
| Icons | Polli SVG set on home/chat/stories/composer | 16 drawables + `PolliIcon` |

## Chat parity (incremental)

| Flow | Must pass | Status |
|------|-----------|--------|
| Bubble media | Glide thumbnails in bubbles | Label + tap preview |
| Audio playback | In-bubble player | Next increment |
| Bubble swiper | 220ms bezier, 32dp trigger | Partial |
| Mute / ephemeral | Compose UI | Next increment |
| In-chat search | Group header tab | Redirect only |
| Webxdc embed | Inline in bubbles | `WebxdcActivity` full screen |

## Screenshot comparison checklist

Compare at 390px width against Polli reference:

1. Home top bar (profile, search pill, archive, plus)
2. Story rings (58/48/2/3px)
3. Chat composer empty (voice default)
4. Chat composer with text (send morph)
5. Profiles floating header
6. New conversation sheet

## Build & run

```bash
cd polli-chat
./gradlew installFossDebug
adb shell monkey -p com.b44t.messenger.beta -c android.intent.category.LAUNCHER 1
```
