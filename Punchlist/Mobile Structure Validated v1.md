# Field Prototype — Product Spec

This is the **single source of truth** for the field Android prototype. Do not resurrect older IA (5-tab bar, Capture tab, Reports tab, Stream-as-home, nested Project Space, OAC/dashboards). If a screen is not in this spec, do not build it.

## Purpose

Internal strategy prototype: prove a field-first information architecture on Android, with Voice-to-Log as the working differentiator. The app opens as a **Foreman**. Other personas are listed in a demo switcher for a later iteration.

## Startup flow

Launch → Splash (brand) → Project List → Today. This sits above the three-tab chassis, with its own two-item `NavigationBar` (Projects, Accounts) — it never coexists with the Today/Plan/Tools bar; only one or the other is on screen at a time.

**Project List (Projects tab):** black branded header (Linarc wordmark) above an M3 `OutlinedTextField` search (client-side name/address filter, no backend) and a `ListItem` roster (name, address, chevron, dividers). **Riverside Medical — Area B** is the only entry backed by real seeded data; the remaining rows are demo flavor so the picker doesn't read as a single-item list. Tapping **any** row loads the same seeded Today/Plan/Tools data — there are no per-project datasets in this build. This is a one-time pre-shell gate, distinct from the barred "Projects tab" below, which would be a fourth destination *inside* the Today/Plan/Tools navigation bar.

**Accounts tab:** placeholder ("Accounts isn't part of this prototype yet") — same explain-don't-fake treatment as every other unbuilt action.

**Getting back to the picker:** three shortcuts, one handler. On every Today/Plan/Tools header: a tappable project-name chip (above the title) and a `⋮` overflow menu ("Switch project") next to the profile avatar — plus Profile → Switch project (see Profile, below). All three reset the Today/Plan/Tools back stack and re-show the Project List; none clear demo data. The picker's own footer nav cannot otherwise be reached from inside the chassis.

**Selected-nav color:** every bottom `NavigationBar` in the app (this picker's Projects/Accounts, and the chassis's Today/Plan/Tools) uses the same blue filled pill behind the selected icon, matching the Linarc Onsite Figma file.

**Motion:** Pattern B push/pop uses the standard Android parallax slide (new screen enters fully from the right, previous screen partially exits left, reversed on back); Pattern A immersive routes slide vertically like a task takeover; switching Today/Plan/Tools tabs crossfades rather than sliding, since tabs are siblings, not a stack. The Splash → Project List → Today handoff and the picker's own Projects/Accounts tab swap crossfade too.

## Canonical chassis

Three bottom destinations. Capture is **not** a tab.

```
Content: Today | Plan | Tools (large in-content page titles)
FAB (end): opens Capture sheet → Voice / Photo / Issue
```

| Destination | Job |
|---|---|
| **Today** | What needs me now: crew, blockers, recent captures. Not a module menu. |
| **Plan** | One sheet (Area B) with pins for issues, photos, and logs. Not a PDF engine. |
| **Tools** | Platform modules (grid or list), then demo controls: view as, outbox, voice log history, Appearance (theme). |

**Capture:** Material 3 `FloatingActionButton` → `ModalBottomSheet` with Voice, Photo, Issue. A center “+” in the navigation bar is not allowed (it reads as a fourth tab). Quick-create `+` on a Tools card is a module action, not a fourth tab.

**Profile:** an avatar in the upper-right of the Today/Plan/Tools header → `ModalBottomSheet` with the signed-in user's photo/name/job title and account actions (Switch project, Edit profile, Help & Support, Driving & Operating licenses, Reset password, Logout). This identity is fixed and independent of **Demo: view as** — it is not a persona chip and must not change when the demo persona changes. There is no backend/auth in this prototype, so every action except Switch project and Logout surfaces an explanatory message instead of a real flow. Switch project returns to the startup Project List (see Startup flow, above) without clearing demo data. Logout resets the local demo session (equivalent to a fresh app launch) and returns to Today.

**Navigation patterns.** Every destination follows one of three patterns, resolved per route by `resolveChrome()` in `ui/navigation/AppChrome.kt`:

- **Pattern A — full-screen task flow** (voice recording/review, photo, quick issue, playback, tool create, image viewer): hides the navigation bar and FAB. Close/Cancel top-left, Save/Done top-right. Warn before discard *only* when unsaved edits exist. Back from capture/voice returns to Today.
- **Pattern B — nested browsing stack** (the five built Tools areas, the remaining tool placeholders, voice log history, outbox): **keeps the navigation bar visible**. Back moves one level at a time, and reselecting the active tab returns that tab to its root screen. Each tab is a nested graph, so tab stacks are preserved independently.
- **Pattern C — modal bottom sheet** for compact contextual actions or one-or-two parameter changes (Capture, Profile, new time entry, new topic, image source). Dismissible via scrim or swipe.

**FAB.** One shared component whose icon, description, and action are configured by the current screen: Capture on the three tab roots, New time entry in Time cards, New topic on the Collaboration list, Add image on the Images grid, and absent everywhere else.

## Non-goals (do not build)

- Capture tab, Reports tab, Projects tab, nested Project Space (Overview / Field / Issues / Docs) — the pre-shell Project List in Startup flow above is not this: it is a one-time gate before the chassis, not a bottom-nav destination
- OAC reports, dashboards, Gantt charts, live QR/barcode scanning, cost codes
- Legal-pad OCR, on-device Whisper/LLM, MediaPipe
- Vector-tile / PDF markup editor, offline lasso download
- Sync engine, conflict visual diff, backend, auth, Hilt, Room
- Subcontractor portal as a separate product
- Distinct live UIs for non-Foreman personas (iteration 2)
- Persona chip in the top app bar (demo control belongs in Tools)

## Personas

Same three tabs for every persona. Only Today focus, capture CTAs, and Plan density change later. Workers do not switch roles in production; **Demo: view as** is a strategy-demo control in Tools.

| Persona | Status | Focus when live |
|---|---|---|
| **Foreman** | **Live (default)** | Crew today, blockers, Start My Day, Voice Daily as hero capture |
| Superintendent | Next | Open issues / inspections first; Plan is the power view |
| Crew | Next | My assignment, start/end shift, take a photo |
| Project manager | Next | Aging RFIs, delays, decisions |
| Owner | Next | Progress photos and decisions; no time cards or voice log |
| Subcontractor | Next | Assigned work + Request Inspection |

## Foreman UX (this build)

**Seeded project:** Riverside Medical — Area B.

**Today**

- Start My Day (if not confirmed): `ModalBottomSheet` with crew, area, and weather already filled; one Confirm tap.
- Crew roster (Hector Ortiz, Dave Miller, Maria Chen, Sam Reyes).
- Blockers (delivery, weather, issues) — including items created from Voice-to-Log.
- Recent captures (voice logs, photos, issues). Tapping a voice log opens playback.

**Plan**

- One static Area B sheet (Compose drawing, not a PDF).
- Seed pins plus pins created from voice/photo/issue.
- Issue “near column 4” from the Hector script lands on the Column 4 pin.
- Tap pin → `ModalBottomSheet` with title and snippet.

**Capture (FAB)**

1. **Voice** — existing Voice-to-Log (record → parse → review → submit). After submit, labor/delays/issues appear on Today and issues are pinned on Plan.
2. **Photo** — camera, gallery, or demo photo → suggested tags → save and optional “Create issue?”
3. **Issue** — title, location, note → Today + Plan pin.

**Tools**

- Grid/list catalog of platform modules, with a Material 3 segmented control to switch layout. Neutral icons and labels; no category color tiles.
- Modules: Field task, Time card, Crew, Collaboration, Images, Plans, RFIs, Punch list, Incidents, Issues, T & M, Checklist, Drive, Toolbox Talks, Scan.
- Quick create `+` on Collaboration, Images, RFIs, Punch list, Incidents, Issues, Toolbox Talks. Images `+` opens the existing photo capture flow; other `+` actions open a create placeholder.
- **Five modules are built as real Pattern B stacks** backed by demo data: Field task (list → detail with status control, checklist, filters), Time card (crew list → member detail, contextual FAB → new-entry sheet), Crew (list → detail), Collaboration (topic list → conversation, contextual FAB → new-topic sheet), and Images (grid → full-screen Pattern A viewer with a Share / Markup / Delete / Create footer toolbar). Markup is an explicit placeholder — no drawing engine ships in this build. The remaining ten modules keep the generic list/detail placeholders.
- Scan is catalog-only in this build (no live scanner).
- Below the catalog: Demo: view as — Foreman selected and live; other personas visible. Tapping a non-live persona explains that the view is next; it must not fake a broken UI.
- Outbox — seeded queued items (badge/count only; no sync).
- Voice logs — history and playback of submitted recordings.
- Appearance — Dark theme toggle (light and dark chrome).

## Voice-to-Log contract

Keep the real mic / `SpeechRecognizer` / regex parser pipeline. Do not replace the parser with an LLM unless explicitly approved (new dependency).

Hector demo line must continue to extract: Hector + Dave labor, 40 studs, delivery delay, weather delay, spalling issue at Column 4.

On **Submit**, write to `DailyLogRepository` **and** publish structured items into the shared demo store so Today and Plan update in the same session.

## Android patterns (required)

- `NavigationBar` with exactly three items (Today, Plan, Tools).
- `FloatingActionButton` on `Scaffold` for capture; `ModalBottomSheet` for the action list and for Start My Day / pin detail / view-as / profile.
- Tools catalog: `Card` grid and `ListItem` rows, plus `SingleChoiceSegmentedButtonRow` for grid/list. Demo/appearance stay as `ListItem` / `Switch` below the catalog.
- Full-screen voice flow; hide bottom chrome.
- Prefer Material 3 over custom chrome. Custom is allowed only for the static sheet + pin overlay (no map/PDF SDK in this prototype).

## Demo script (v1)

1. Launch as Foreman → Splash → Project List → select Riverside Medical → Today shows crew + Start My Day. Confirm.
2. FAB → Voice → Hector script → review → Submit.
3. Today shows delay + issue. Plan shows a pin near Column 4.
4. Tools → Demo: view as → other personas visible, not live.

## Iteration 2 (not this build)

Enable Demo: view as so the same objects reorder by persona. Do not add tabs. Do not hide Plan; de-emphasize its content for Crew/Owner.
