# Tab stacks: stay oriented in the field

**UX principle:** Field users bounce between Today, Plan, and Tools to *look something up*, not to start over. Leaving a tool must not wipe their place. Getting back to the Tools catalog must stay one obvious tap away.

This is a required navigation pattern for the Punchlist chassis, not an implementation quirk. Product spec: `Mobile Structure Validated v1.md` (Pattern B). Implementation: `AppNavHost.kt` bottom-bar `onClick`.

## Why it exists

A foreman is rarely in one module for a whole session. Typical loop:

1. Open **Tools → Time cards** (or Field task, Crew, Images, …) for a fact.
2. Switch to **Today** or **Plan** to place that fact in context.
3. Return to **Tools** expecting the same screen, still scrolled to the same crew member or task.

If Tools always reset to the catalog, they get lost: extra taps, lost place, and the catalog starts to feel like a trap instead of a launcher. If Tools *only* restored and never offered a reset, they get stuck inside a module with only the Back arrow.

Both halves are the product:

| Tap | Situation | Result | Why |
|---|---|---|---|
| **First** | User is on Today or Plan; last Tools screen was a nested module | Restore that module | Resume work; don’t dump them on the grid |
| **Second** | Tools is already the selected tab, and they are inside a module | Pop to **Tools home** (catalog) | Escape hatch; one tap back to the launcher |
| Already at Tools home | Tools selected, catalog visible | No-op (`popBackStack` returns false) | Don’t bounce or clear state |

This is most visible on Tools because that is the nested-browsing tab. The same contract applies to Today and Plan: each tab owns its own stack, independently.

## Material 3 bottom navigation

[Material 3 bottom navigation](https://m3.material.io/components/navigation-bar/guidelines) treats the bar as **persistent top-level destinations**, not a reset button.

Recommended behavior this prototype follows:

1. **Destinations are siblings.** Today, Plan, and Tools are peers. Switching tabs must not look like a hierarchical push (no horizontal slide). The chassis crossfades tab-to-tab.
2. **The bar stays visible** while the user drills into a destination that still belongs to that tab (Pattern B: tool lists, details, outbox, voice-log history). Hiding the bar would imply they left the chassis.
3. **Tapping a different destination restores that destination’s hierarchy** (multiple back stacks), so returning to Tools restores Field task / Time cards / etc.
4. **Tapping the already-selected destination returns to the top of that destination’s hierarchy.** That is the second press: Tools → catalog.

Capture lives in the bar (between Today and Plans, revised 2026-08-25) but is an **action, not a destination**: `selected` is always false, it owns no back stack, and tapping it pushes the full-screen camera (an immersive route, so the whole bar disappears). A capture *tab* — one that stays lit or restores a stack — remains disallowed.

iOS `UITabBarController` matches this: switching tabs preserves each tab’s stack; tapping the selected tab pops to root. Field users coming from either platform should feel the same orientation model.

## Android Navigation: multiple back stacks

This is the official Navigation Component pattern (`saveState` / `restoreState` / `launchSingleTop`), not a custom stack manager.

```kotlin
if (selected) {
    // Reselect → pop this tab to its root destination.
    navController.popBackStack(tab.route, inclusive = false)
} else {
    navController.navigate(tab.graphRoute) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
```

Each tab is a nested graph (`today_graph`, `plan_graph`, `tools_graph`). Graphs are never shown themselves; they exist so:

- `destination.hierarchy` can keep the correct tab lit on nested screens
- each tab owns a real back stack that survives a visit to another tab

Root destinations (`today_home`, `plan_home`, `tools_home`) are the reselect pop targets.

## How this sits with Pattern A / B / C

Resolved per route in `AppChrome.kt` via `resolveChrome()`.

| Pattern | What it is | Bottom bar | Stack behavior |
|---|---|---|---|
| **B — nested browsing** | Tool lists/details, outbox, voice-log history | **Visible** | Lives *inside* the tab graph. Saved/restored on tab switch. Reselect pops to tab root. System Back pops one level. |
| **A — full-screen task** | Camera + photo review, quick issue, tool create, image viewer, plan sheet viewer | Hidden | Lives at the **nav-graph root**, not inside a tab, so a tab’s saved stack cannot clobber another’s. Close/Cancel; warn before discard only if unsaved edits exist (a captured-but-unsaved photo counts). |
| **C — modal sheet** | Profile, new time entry, new topic, image source | Unchanged (under the sheet) | Not a nav destination. Scrim or swipe dismiss. |
| Tab roots | Today / Plan / Tools home | Visible, Capture action in the bar | Start destinations of each nested graph. No FAB — the FAB is contextual to tool screens. |

Layout rule in `AppNavHost.kt`: Pattern B destinations live inside their tab’s graph; Pattern A / immersive destinations live at the root. `daily_log_detail` is at root on purpose — it is reachable from both Today and Tools; nesting it would make one tab’s saved stack overwrite the other.

## What this is not

- **Not** “Tools always opens the last tool.” Only returning from another tab restores. Reselecting Tools while it is current goes home.
- **Not** a global “last screen” across the whole app. Stacks are per tab.
- **Not** an invitation to add tabs. The bar stays Today / Capture / Plans / Tools, and Capture is an action (never selected, no stack) — not a fourth tab.
- **Not** a substitute for the Back arrow. Back still walks the current tab one level at a time. Reselect is the fast path to the tab root.

## Tests (do not drop)

`AppNavHostTest`:

- `switchingTabs_preservesEachTabsOwnBackStack` — Tools → Field task → Today → Tools still shows the task.
- `reselectingActiveTab_returnsThatTabToItsRoot` — Tools → Field task → tap Tools again → catalog.

If you change tab `onClick`, those two tests are the contract.

## Manual check

1. Tools → Field task (or Time cards).
2. Today. Confirm the task is gone from view (you’re on Today).
3. Tap **Tools** once → you should be back in that tool, not the grid.
4. Tap **Tools** again → catalog.
5. Tap **Tools** a third time on the catalog → nothing happens.
