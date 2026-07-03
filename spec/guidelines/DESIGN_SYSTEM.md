---
description: Design system — accent, dividers, modals, spacing, horizontal scroll rows
alwaysApply: true
---

# Polli Chat — Design System

Agent guidelines for the Lab UI layer in **polli-chat**. Intentionally minimal — only patterns this app actually uses. For Zapstore/Webapp rules, see those projects' `spec/guidelines/` trees; do not copy unrelated tokens here.

## Code locations

| What | File |
|------|------|
| Colors | `polli-ui/src/commonMain/kotlin/com/polli/ui/theme/LabColors.kt` |
| Spacing & stroke | `polli-ui/src/commonMain/kotlin/com/polli/ui/theme/LabDimens.kt` |
| Theme & accent | `polli-ui/.../LabTheme.kt`, `polli-chat/src/.../theme/Accent.kt` |
| Dividers | `polli-chat/src/.../ui/ShellDivider.kt` |
| Frosted surfaces & scrim | `polli-chat/src/.../ui/LabBackdrop.kt` |
| Modals | `polli-chat/src/.../ui/AppModal.kt` |

## Accent color

Polli uses **one accent color** per user (blurple presets, configurable in Profiles → Appearance). Access in Compose via `accent()` from `LocalAccentPalette`. Use for story rings, selected tabs, primary highlights. Do not scatter fixed accent hex values.

## Dividers

Standard divider: **`ShellDivider`**.

| Property | Token |
|----------|-------|
| Height | `LabDimens.ShellDividerWidth` (1dp) |
| Color | `LabColors.ShellDivider` (= `White8`, ~8% white) |

```kotlin
ShellDivider(screenPad = 0.dp)   // edge-to-edge (Profiles rows, stories row)
ShellDivider()                   // inset 14dp (`HomeBarPadding`)
```

Reference: Profiles screen list rows, home stories row (top + bottom full-bleed dividers).

## Modals

Pattern: dimmed barrier + frosted sheet sampling content behind.

1. Register background with `.hazeSource(hazeState)` on the screen body.
2. Show `PolliScreenScrim` for the flat dim (`PolliModalBarrier`) — **no blur on the scrim**.
3. Modal sheet uses `FrostedChromeSurface` + `polliModalSheetHazeStyle()`.
4. Sheet enters by sliding up from below the viewport with a short opacity fade (`AppModal`).

## Horizontal scroll rows

Examples: channel stories, home tab pills.

- Use `horizontalScroll` on the row.
- Pad the row (`StoriesRowPaddingStart`, `HomeBarPadding`, etc.), not a clipping parent.
- **Do not** put `clipToBounds()` on the direct parent of a horizontal scroll row — rings/pills get clipped at the edges.
- Pair with full-bleed `ShellDivider(screenPad = 0.dp)` when separating sections (stories row).

## Spacing

| Token | Value | Use |
|-------|-------|-----|
| `HomeBarPadding` | 14dp | Default screen horizontal inset |
| `TabSectionGap` | 14dp | Gap between home chrome blocks (stories ↔ tabs ↔ feed) |
| `StoryRowVerticalPadTop/Bottom` | 10dp | Stories content ↔ divider (home) |
| `StoryRowDividerGap` | 10dp | Header/tabs ↔ story-row dividers (home) |

## Surfaces (basics)

| Token | Use |
|-------|-----|
| `Black` | App / feed background |
| `Gray66` | Modal & composer frost base |
| `Gray` | Search panel frost (darker) |
| `White8` / `ShellDivider` | Divider line color |

## What we are *not* documenting yet

Button factories, icon registry, typography scale, and screen transition specs are shared with or inherited from the broader Lab stack. Add sections here only when polli-chat has a concrete, verified pattern — do not paste Zapstore/Webapp design-system chapters wholesale.

## General rules

- Use `LabColors` / `LabDimens` — no raw hex or magic dp in feature code.
- Reuse `ShellDivider`, `AppModal`, `FrostedChromeSurface` instead of one-offs.
- When adding a new screen section separated by lines, default to `ShellDivider(screenPad = 0.dp)` unless inset dividers match surrounding 14dp content.
