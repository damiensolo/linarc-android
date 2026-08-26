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

Three bottom destinations plus the Capture **action** in the bar (revised 2026-08-24; previously a FAB + sheet). Capture is still **not** a tab: it never shows a selected state, never owns a back stack, and simply opens the full-screen in-app camera.

```
Content: Today | Plan | Tools (large in-content page titles)
Bar: Today | Capture | Plans | Tools — Capture (camera icon, primary-tinted) opens the camera
```

| Destination | Job |
|---|---|
| **Today** | What needs me now: crew, blockers, recent captures. Not a module menu. |
| **Plan** | One sheet (Area B) with pins for issues, photos, and logs. Not a PDF engine. |
| **Tools** | Platform modules (grid or list), then the Activity Center: outbox and voice log history. Appearance (theme) and demo controls (view as, splash) live on Settings, reached from the header overflow menu. |

**Capture:** a `NavigationBarItem` action between Today and Plans that opens the full-screen CameraX camera directly. A Photo | Video mode rail sits above the shutter: photo is the default; video records a clip (90-second cap, torch usable while recording, silent-video fallback if the mic permission is denied), then flows through a "Describe what you saw" dictation step (skippable — sequential with recording so the mic is never contended) and a video review with inline playback. Saving publishes the video to Today and as a Plan pin; the parsed description can preselect **File an issue**, which opens Quick issue prefilled (title/location/note) after saving. Voice daily log and Quick issue remain one tap away as quick chips on the camera screen — both usable even when camera permission is denied. Saved photos land on Today, as Plan pins, and in the Images grid. A generic center “+” tab remains disallowed; Capture must stay an action (no selected state, no stack). Quick-create `+` on a Tools card is a module action, not a fourth tab.

**Profile:** an avatar in the upper-right of the Today/Plan/Tools header → `ModalBottomSheet` with the signed-in user's photo/name/job title and account actions (Switch project, Edit profile, Help & Support, Driving & Operating licenses, Reset password, Logout). This identity is fixed and independent of **Demo: view as** — it is not a persona chip and must not change when the demo persona changes. There is no backend/auth in this prototype, so every action except Switch project and Logout surfaces an explanatory message instead of a real flow. Switch project returns to the startup Project List (see Startup flow, above) without clearing demo data. Logout resets the local demo session (equivalent to a fresh app launch) and returns to Today.

**Navigation patterns.** Every destination follows one of three patterns, resolved per route by `resolveChrome()` in `ui/navigation/AppChrome.kt`:

- **Pattern A — full-screen task flow** (voice recording/review, camera + photo review, record create, playback, tool create, image viewer): hides the navigation bar and FAB. Close/Cancel top-left, Save/Done top-right (the camera viewfinder draws its own chrome). Warn before discard *only* when unsaved edits exist — a captured-but-unsaved photo counts. Back from capture/voice returns to the tab it was opened from.
- **Pattern B — nested browsing stack** (the five built Tools areas, the remaining tool placeholders, voice log history, outbox, settings): **keeps the navigation bar visible**. Back moves one level at a time, and reselecting the active tab returns that tab to its root screen. Each tab is a nested graph, so tab stacks are preserved independently.
- **Pattern C — modal bottom sheet** for compact contextual actions or one-or-two parameter changes (Profile, new time entry, new topic, image source). Dismissible via scrim or swipe.

**FAB.** Contextual-only since Capture moved into the bar: New time entry in Time cards, New topic on the Collaboration list, Add image on the Images grid, and New issue / New incident / New punch item on their record tool lists — absent everywhere else. Never reintroduce a global Capture FAB or a capture bottom sheet.

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
- Blockers — **only actual work stoppages** (issued ≠ blocked): records explicitly marked "Blocks work" (by the reporter or their type's configured default) and dictated delays from Voice-to-Log. Every blocker row opens the thing behind it: record-backed rows open that tool's record detail, voice-log rows open the daily log they came from.
- Recent captures (voice logs, photos, videos, record-backed tasks, and logged-not-blocking issues). Nothing on Today dead-ends: voice logs and dictated issues open playback, photos open the image viewer, videos open video playback, and record-backed rows (e.g. the seeded Frame inspection punch item) open their record detail.

**Plan**

- One static Area B sheet (Compose drawing, not a PDF).
- Seed pins plus pins created from voice/photo/video/issue.
- Issue “near column 4” from the Hector script lands on the Column 4 pin.
- Tap pin → `ModalBottomSheet` with title, snippet, and — when the pin came from a capture — the photo itself (tap to open the full-screen viewer). Every pin carries a comment thread: add comments as the signed-in user, then **Publish to team** queues the unpublished batch to the Outbox (offline-first; nothing leaves the device).

**Capture (bottom-bar action)**

1. **Photo** — in-app CameraX camera (rear/front flip, flashlight, tap-to-focus, pinch-zoom) → review with title, description, suggested tags → Save, or “Save & create…” which picks a record category (issue / incident / punch item) and continues into that record form with the photo already attached and the fields seeded. Photos land on Today, as Plan pins, and in Images. A **Markup** toggle chip on the viewfinder routes the shot through the annotation editor (select/move/resize, pen, line, arrow, double arrow, box, oval, cloud, text, six colors, undo/redo) before review; annotations are baked into the saved JPEG.
2. **Voice note** (quick chip on the camera; replaced the Voice daily log chip 2026-08-25) — bilingual voice capture as a create workflow. Recording shows the transcript live with an English | Español segmented toggle; the spoken language is auto-detected best-effort (the recognizer re-arms in the detected language) and one tap overrides it. Done → review with a floating toolbar in the image-viewer style: **Share / Translate / Re-record / Delete / Create**. Translate (and the same toggle) flips the note between English and Spanish via on-device ML Kit translation — models download once, then it works offline. Create picks issue / incident / punch item and continues into the record form seeded with a derived title, a **bilingual description (original + translation)**, and a matched location. The note itself is ephemeral: the records made from it are the durable artifacts. No audio file is recorded (transcription-only avoids the mic-contention failure). Works without camera permission.
3. **Issue** (quick chip on the camera) — opens the record create form (Issue category); the dictated-video flow prefills it via the parsed title/location/description.

**Tools**

- Grid/list catalog of platform modules, with a Material 3 segmented control to switch layout. Neutral icons and labels; no category color tiles.
- Modules: Field task, Time card, Crew, Collaboration, Images, Plans, RFIs, Punch list, Incidents, Issues, T & M, Checklist, Drive, Toolbox Talks, Scan.
- Quick create `+` on Collaboration, Images, RFIs, Punch list, Incidents, Issues, Toolbox Talks. Images `+` opens the in-app camera; Issues / Incidents / Punch list `+` open their record create form; the remaining `+` actions open a create placeholder.
- **Five modules are built as real Pattern B stacks** backed by demo data: Field task (list → detail with status control, checklist, filters), Time card (crew list → member detail, contextual FAB → new-entry sheet), Crew (list → detail), Collaboration (topic list → conversation, contextual FAB → new-topic sheet), and Images (a Grid / Timeline / Albums / Map segmented switcher over one photo set → full-screen Pattern A viewer with a Share / Markup / Album / Delete / Create footer toolbar). Grid keeps the tag filter chips; Timeline groups by day (Today/Yesterday/date); Albums groups by album with an Unfiled bucket at the end — the viewer's Album action files a photo into an existing or new album; Map shows captures pinned at their plan positions on the Level 2 sheet (tap a thumbnail to open it — deliberately the site drawing, since there's no GPS in this prototype). Markup opens the annotation editor for captured photos and saves either a copy (default — the copy fans out to Today/Plans/Images like any capture) or replaces the original in place (same id, pins and Today row survive); bundled demo photos explain they can't be marked up (markup edits real capture files only). All seeded and demo-added photos are real free-licensed construction photos matching their subject (credits in PHOTO_CREDITS.md), never placeholder boxes. The viewer's Create action opens a category chooser (issue / incident / punch item) and continues into the record form with that photo attached.
- **Records: Issues, Incidents, and Punch list are built as one system** — Pattern B lists (each with a contextual create FAB) → a read-only detail, plus one shared Pattern A create form with the full capture set: title, per-category type, severity (low/medium/high/critical), impact (informational/schedule/cost/quality/safety), description, attachments (Camera → the real in-app camera returns with the shot attached; Photos → pick from project images; Files → system document picker; thumbnails with per-item delete), location, event date (date picker), and crew assignees. Photo attachments link back to their record. This form replaced the old quick-issue screen. The remaining seven modules keep the generic list/detail placeholders. The create form follows the **long-form pattern**: a scrolling form with a sticky "Save record" footer that stays visible (clear of gesture nav and the keyboard), a "* Required fields" note with `*` in each required label (Title; Blocking reason while blocking), and progressive validation — no errors on load, a field validates after blur or a save attempt, and tapping Save while incomplete announces "Complete N required fields to save" in the footer, shows specific inline errors, and jumps focus to the first missing field instead of sitting disabled. The post-save snackbar names the destinations and the Outbox queue state.
- **Blocking is an explicit, auditable status — issued ≠ blocked.** Creating a record logs it (tool list + one queued Outbox entry + a Plan pin at its location) and never stops work by itself. A **"Blocks work?" toggle (default off)** makes it a blocker; a narrow per-type policy turns it on by default (Issue types "Safety hazard" and "Failed inspection", Incident type "Injury" — the prototype's stand-in for admin-configurable defaults; observations, punch items, and RFIs stay off, RFIs blocking only when the reporter ties one to a scheduled task and flips the toggle). Enabling blocking reveals the blocking details — reason (required to submit), affected trade / scheduled task / work package (the block scopes to these, never the whole crew), expected resolution date, escalation contact, resolution authority (Superintendent / Safety manager / QA/QC / Project manager), and a crew-acknowledgement requirement (on by default while blocking). Only blocking records land on Today's Blockers; their list rows flag "Blocks work" in red and their detail shows the blocking banner.
- Scan is catalog-only in this build (no live scanner).
- Below the catalog: the **Activity Center** — product activity, not configuration: Outbox — every publish-style action (records, time entries, messages, photos, videos, voice logs, pin-comment publishes) queues here as "waiting for signal", and a **"Signal restored — send all"** button drains the queue one entry at a time to demo connectivity returning (entries flip to "Sent to project"; still no real sync engine — that stays a non-goal); Voice logs — history and playback of submitted recordings.
- **Settings** (Pattern B, reached from the Tools header's overflow/3-dot menu): Appearance — Dark theme toggle (light and dark chrome); then the Demo section (strategy-demo scaffolding, not product surface): Demo: view as — Foreman selected and live; other personas visible (tapping a non-live persona explains that the view is next; it must not fake a broken UI); Splash animation picker; **Voice daily log (demo)** — the original Voice-to-Log flow (record → parse → review → submit), kept launchable for scripted demos after the Capture chip became Voice note; submitted demo logs still land on Today, Plans, and the Voice logs history on Tools.

## Voice-to-Log contract

Entry moved on 2026-08-25: Settings → Demo → Voice daily log (demo). The camera quick chip now opens Voice note instead; Today links and the Tools history list are unchanged.

Keep the real mic / `SpeechRecognizer` / regex parser pipeline. Do not replace the parser with an LLM unless explicitly approved (new dependency).

## Voice note contract

Real dictation (`SpeechRecognizer` with an `EXTRA_LANGUAGE` hint per utterance) and real on-device translation (ML Kit `translate`, approved dependency 2026-08-25). Spoken-language auto-detect is a text heuristic (`VoiceNoteLanguageDetector`) — best-effort by design; the one-tap toggle is authoritative. Create must prefill at minimum the description (bilingual when a translation exists) and whatever else derives cheaply (title, location).

Hector demo line must continue to extract: Hector + Dave labor, 40 studs, delivery delay, weather delay, spalling issue at Column 4.

On **Submit**, write to `DailyLogRepository` **and** publish structured items into the shared demo store so Today and Plan update in the same session.

## Android patterns (required)

- `NavigationBar` with three destinations (Today, Plans, Tools) plus the Capture action item between Today and Plans (`selected` always false).
- CameraX (`LifecycleCameraController` + `PreviewView`) for capture; contextual `FloatingActionButton`s on `Scaffold` for tool actions; `ModalBottomSheet` for Start My Day / pin detail / view-as / profile / image source.
- Tools catalog: `Card` grid and `ListItem` rows, plus `SingleChoiceSegmentedButtonRow` for grid/list. Demo/appearance stay as `ListItem` / `Switch` below the catalog.
- Full-screen voice flow; hide bottom chrome.
- Prefer Material 3 over custom chrome. Custom is allowed only for the static sheet + pin overlay (no map/PDF SDK in this prototype).

## Demo script (v1)

1. Launch as Foreman → Splash → Project List → select Riverside Medical → Today shows crew + Start My Day. Confirm.
2. Capture (bottom bar) → Voice note chip → speak Spanish ("falta concreto en la pared del nivel dos") → toggle auto-switches to Español with live text → Done → Translate shows the English version → Create → Issue → form arrives with a bilingual description and Level 2 location → Save.
3. Settings (Tools header overflow) → Demo → Voice daily log (demo) → Hector script → review → Submit.
4. Today shows delay + issue. Plan shows a pin near Column 4.
5. Settings → Demo: view as → other personas visible, not live.

## Iteration 2 (not this build)

Enable Demo: view as so the same objects reorder by persona. Do not add tabs. Do not hide Plan; de-emphasize its content for Crew/Owner.
