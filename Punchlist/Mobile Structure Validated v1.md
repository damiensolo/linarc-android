# Field Prototype — Product Spec

This is the **single source of truth** for the field Android prototype. Do not resurrect older IA (5-tab bar, Capture tab, Reports tab, Stream-as-home, nested Project Space, OAC/dashboards). If a screen is not in this spec, do not build it.

## Purpose

Internal strategy prototype: prove a field-first information architecture on Android, with Voice-to-Log as the working differentiator. The app opens as a **Foreman**. Other personas are listed in a demo switcher for a later iteration.

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

**Profile:** an avatar in the upper-right of the Today/Plan/Tools header → `ModalBottomSheet` with the signed-in user's photo/name/job title and account actions (Edit profile, Help & Support, Driving & Operating licenses, Reset password, Logout). This identity is fixed and independent of **Demo: view as** — it is not a persona chip and must not change when the demo persona changes. There is no backend/auth in this prototype, so every action except Logout surfaces an explanatory message instead of a real flow; Logout resets the local demo session (equivalent to a fresh app launch) and returns to Today.

**Immersive flows** (voice recording/review, photo, quick issue, playback, tool list/create placeholders): hide the navigation bar and FAB. Back from capture/voice returns to Today; back from a tool returns to Tools.

## Non-goals (do not build)

- Capture tab, Reports tab, Projects tab, nested Project Space (Overview / Field / Issues / Docs)
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
- Quick create `+` on Collaboration, Images, RFIs, Punch list, Incidents, Issues, Toolbox Talks. Images `+` opens the existing photo capture flow; other `+` actions open a create placeholder. Tool list/detail screens are placeholders.
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

1. Launch as Foreman → Today shows crew + Start My Day. Confirm.
2. FAB → Voice → Hector script → review → Submit.
3. Today shows delay + issue. Plan shows a pin near Column 4.
4. Tools → Demo: view as → other personas visible, not live.

## Iteration 2 (not this build)

Enable Demo: view as so the same objects reorder by persona. Do not add tabs. Do not hide Plan; de-emphasize its content for Crew/Owner.
