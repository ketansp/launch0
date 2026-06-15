# Quick Actions — Feature Brief for Design

A reference for the design team to create a polished version. Please preserve the
original spirit captured under **Principles to preserve** at the end.

## What it is

Quick Actions is a **swipe-right panel** on the Launch0 home screen (it mirrors the
Notes panel reached by swiping left). It gives users one-gesture access to the device
controls and information they reach for most often — **without leaving the launcher's
calm, text-only world**. It has two stacked sections: a **quick-actions grid** on top
and a **notification centre** below.

**Entry point:** swipe right on home → panel slides in from the left. Swipe left (back)
returns home. Swipe-right can alternatively be bound to open a specific app; Quick
Actions is the default behaviour.

## Section 1 — Quick Actions grid

A 3-column grid of **text-only buttons** (no icons, true to the launcher's aesthetic).
Each is either an inline toggle or a shortcut into the relevant system screen:

| Action | Behaviour |
|---|---|
| **Flashlight** | True inline toggle — turns the torch on/off without leaving the panel. Label shows an "On" state when active. Only appears if the device has a flash unit. |
| **Wi-Fi** | Opens system Wi-Fi settings |
| **Bluetooth** | Opens system Bluetooth settings |
| **Mobile data** | Opens data / wireless settings |
| **Airplane mode** | Opens airplane-mode settings |
| **Display** | Opens display settings |
| **Settings** | Opens the main Android settings |

**Design notes:**

- The grid is built dynamically, so the **count of buttons varies** (e.g. devices
  without a flash drop the Flashlight tile). Layout must gracefully handle a
  partially-filled final row.
- Flashlight is the only true in-panel toggle today; the rest are **deep links** into
  the OS because a privacy-minimal launcher deliberately avoids holding system
  permissions it doesn't need.

## Section 2 — Notification centre

Below the grid, a live list of **active notifications**, presented as plain text rows
(app name, title, body). This is the launcher's own distraction-controlled view of
notifications.

- **Tap a notification** → opens it in its source app (and dismisses it if clearable).
- **Long-press** → dismisses that notification.
- **Clear all** → appears only when clearable notifications exist.
- **Empty state:** "No notifications".
- **Permission gate:** requires Notification Access. When not granted, the list is
  replaced by a single tappable prompt to grant access. Nothing is shown until the user
  opts in.
- The launcher's own notifications are filtered out, and the list updates live while the
  panel is open.

### Related: "Hold notifications" (Do Not Disturb)

The same underlying notification service powers an opt-in **"Hold notifications"**
feature (configured in Settings): the user picks specific apps and a duration
(30–180 min), and their notifications are *parked* (snoozed) during a focus window, then
released together when it ends. Users can release a given app's held notifications early.
This is the productivity counterpart to the notification centre — worth keeping
visually/conceptually connected if the design surfaces parked counts.

## Principles to preserve

1. **Text-only, no icons.** The entire launcher is deliberately iconless. Quick Actions
   should stay typographic — hierarchy through type, weight, and spacing, not glyphs or
   colourful toggle chips.
2. **Minimal & distraction-free.** It's a focused utility surface, not a busy control
   centre. Resist adding tiles "because we can."
3. **Productivity-first.** Everything here exists to make the user *faster* — one gesture
   to the torch, to a system toggle, to triage notifications, or into a focus hold.
4. **Privacy is paramount.** Zero data collection, no tracking, no unnecessary
   permissions. The deep-link-to-settings pattern (vs. direct toggles) is an intentional
   privacy choice, not a limitation to "fix" by grabbing more permissions.
5. **Theme-aware.** Must work across dark / light / system themes and both portrait and
   landscape, like the rest of the app.

A natural area for design to elevate: making the **flashlight on/off state** and the
**notification triage gestures** (tap-to-open, long-press-to-dismiss) feel obvious and
tactile while staying within the text-only language.
