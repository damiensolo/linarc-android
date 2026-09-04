# Lead mobile developer handoff

This Android prototype is the **reference implementation** for Linarc Onsite’s field product. Use it to learn the information architecture, UX contracts, and interaction patterns — then rebuild them in the production app. Do not treat demo persistence, missing modules, or strategy-only screens as production requirements.

**Source of truth:** `Mobile Structure Validated v1.md`. If this handoff and the spec disagree, follow the spec.  
**What works vs placeholder + how to try each flow:** `FEATURE_GUIDE.md`.  
**Walkthrough for stakeholders:** `DEMO_SCRIPT.md`.  
**Tab-stack contract:** `NAVIGATION_PATTERNS.md`.

---

## 1. What you are looking at

A native **Kotlin + Jetpack Compose + Material 3** field app. Default login lens is **Foreman** on **Riverside Medical — Area B**.

It exists to prove:

1. A **three-destination chassis** (Today, Plans, Tools) plus **Capture as a bar action**, not a tab.
2. **One tap to capture**, with evidence that **fans out** (Today + Plan + the owning tool) instead of a private list.
3. **Same objects, seven personas** — reorder and (for Owner only) omit. Not seven apps. Not extra tabs.
4. **Orientation in the field:** tab stacks survive; Capture never owns a stack.

This is a **strategy prototype**. In-memory stores, no backend, no auth, no Room/Hilt, no sync engine. The Outbox is a **queue UI** that demonstrates offline-first publish, not a real sync pipeline.

---

## 2. How to walk the project (90 minutes)

Run the app as Foreman. Follow this order; it matches how the product is meant to be understood.

| Step | Do this | What to notice |
|---|---|---|
| 1 | Splash → Project list → **Riverside Medical** | Project picker is a **pre-shell gate**, not a fourth tab. Black Linarc header. |
| 2 | **Today** → Confirm **Start My Day** | Home is “what needs me now,” not a module menu. Nothing dead-ends. |
| 3 | Bar: **Today \| Capture \| Plans \| Tools** | Capture never shows selected. Primary pill is for the selected *tab* only. |
| 4 | **Capture** → photo → Save | Fan-out: Today row, Plan pin, Images. |
| 5 | Capture → **Voice note** (works without camera permission) → Pause/Resume if needed → type on review → Create → Issue | Note is ephemeral; the **record** is durable. Form Save is sticky. Speak is on Description, not Title. |
| 6 | **Tools → Field task** → switch to Today → tap **Tools** once → tap Tools again | First tap restores place; second tap is catalog. Required nav contract. |
| 7 | **Plans** *or* **Tools → Plans** → open sheet → pinch/zoom → pin → comment (or Speak) → Publish | Same sheet list either way. From Tools, Back is the catalog. Comments queue in Outbox. |
| 8 | Tools **⋮ → Settings → Demo: view as** | Same bar. Profile avatar stays Alex Rivera. Only Today/Tools (and Super’s Plan shortcut) change. |
| 9 | Flip **Crew → Superintendent → PM → Project engineer → Owner (dashboard) → Subcontractor** | Reorder, don’t fork. Owner is the one persona that **removes** labor/voice surfaces. |

Then read `AppChrome.kt` + `AppNavHost.kt` with `NAVIGATION_PATTERNS.md` open.

---

## 3. Product concepts (copy these, not older IA)

### Chassis

```
Bar:   Today | Capture | Plans | Tools
Jobs:  now   | shutter | map   | modules
```

- **Today** — persona-specific focus. Crew, blockers, captures (Foreman). Not a launcher. Every list section header is **collapsible** (title + live count + chevron, default expanded; the demoted roster defaults collapsed).
- **Capture** — action. `selected = false`. No back stack. Opens the in-app CameraX camera.
- **Plans** — Area B sheets + pins. Same list from the Plans tab or **Tools → Plans**. Not a PDF/CAD engine.
- **Tools** — platform modules. **Create lives here** (contextual FAB / card `+`), never as a global Capture FAB or a fourth tab.

**Do not resurrect:** 5-tab bar, Capture tab, Reports tab, Stream-as-home, nested Project Space, a global `+` tab.

### Fan-out

A saved photo, video, voice log, or record must appear where field users look:

- **Today** (stream / blockers as appropriate)
- **Plan pin** (at a location when one is known)
- **Owning tool** (Images grid, Issues/Incidents/Punch list, Voice logs history)

Never “saved” only inside the capture flow.

### Issued ≠ blocked

Creating an issue **logs work**; it does **not** stop the job. **Blocks work?** is an explicit, auditable toggle (some types default on: safety hazard, failed inspection, injury). Only blockers land in Today’s Blockers section.

### Offline-first (product, not engine)

Every publish-style action queues in **Outbox** (“waiting for signal”). **Signal restored — send all** is a demo drain. Production should keep this mental model (queue, then send) even when the transport is real. Outbox rows are **receipts, not dead-ends**: each links back to what it published (photo → viewer, record → detail, etc.) and photo-backed rows carry thumbnails.

### Persona rule

Same three tabs, same objects, **reordered**. Workers do not switch roles in production; **Demo: view as** is a strategy lens.

| Persona | Today lead | Tools lead | Special |
|---|---|---|---|
| Foreman (default) | Start My Day, crew, blockers, captures | Catalog order | Hero capture: camera + voice/issue chips |
| Crew | My shift, my assignment (Hector Ortiz) | Field task, Time card, Images… | Shift start/end logs a real queued time entry |
| Superintendent | Blockers, open issues (`attentionOrder`) | Issues, Punch, Incidents… | Plans: **Pinned work** shortcut to the pin sheet |
| Project manager | Aging RFIs **oldest first**, delays, collab | RFIs, Collaboration… | Age is urgency (inversion of newest-first) |
| Project engineer | **RFI desk** (count + oldest age, Draft RFI), Open RFIs, Coordination & quality, collab | RFIs, Issues, **Plans** (live sheets)… | Works the same RFI objects the PM overviews; Draft RFI stages the Issue form on the RFI type |
| Owner | Progress photos + **four decision topics** + delays | Images, **Plans** (live sheets)… | **Omits** time cards, voice logs, crew ops |
| Subcontractor | Request inspection + my work (Sam Reyes / plumbing trade) | Field task, Checklist, Punch… | Inspection request is a **message**, not a record |

Profile identity (**Alex Rivera**) never changes with view-as.

---

## 4. UX / UI system

### Three chrome patterns

Resolved per route in `ui/navigation/AppChrome.kt` via `resolveChrome()`.

| Pattern | Use | Chrome | Stack |
|---|---|---|---|
| **A — task** | Camera, record create, viewers, voice | Bar hidden. Close left, Save/Done right | Graph **root** (must not live inside a tab) |
| **B — browse** | Tool lists/details (including **Tools → Plans**), Settings, Outbox | Bar **stays**. Back one level | Inside the tab graph; saved/restored |
| **C — sheet** | Profile, Start My Day, new time entry, new topic | Bar unchanged under the sheet | Not a destination |

**Tab roots** (Today/Plan/Tools home): bar visible, large **in-content** titles (`FieldPageHeader`), no Material top app bar. FAB only on screens that own a create action.

### Motion

- Tab ↔ tab: **crossfade** (siblings, not a stack).
- Pattern B push/pop: Android **parallax slide**.
- Pattern A: **vertical slide** (task takeover).

### Tab stacks (required)

Field users bounce to look something up. Leaving Tools must not wipe the module.

- Tap another tab → **restore** that tab’s stack.
- Tap the **already-selected** tab → pop to that tab’s **root**.
- Capture is **not** in this system.

Contract tests: `AppNavHostTest` (`switchingTabs_preservesEachTabsOwnBackStack`, `reselectingActiveTab_returnsThatTabToItsRoot`, `tools_plansCardOpensTheLivePlanList`). Details: `NAVIGATION_PATTERNS.md`.

### Capture UX

- In-app **CameraX** (flip, torch, tap-focus, pinch-zoom). Photo default; video capped (~90s).
- Quick chips on the camera (usable if camera permission is denied): **Voice note**, **Issue**.
- Voice note: bilingual EN/ES, live transcript, **Pause / Resume**, editable review, on-device translate, Create → record form. **No audio file** (transcription-only). Create is a **full handoff** (form Save returns to where capture began, not the recorder). Sequential mic with the camera.
- **Dictation reliability (copy this contract):** transient recognizer errors (`ERROR_CLIENT`/`ERROR_RECOGNIZER_BUSY` — fired by any stop/re-arm race, including the language toggle and Pause) self-heal by recycling the `SpeechRecognizer`, capped before a plain-language message. A manual language pick is **authoritative** — it turns auto-detect off for the take. Re-record cancels the in-flight utterance (late results never resurrect); review edits re-translate automatically.
- **Dictated text never fills a title** (2026-09-03): voice note and video-issue seeds carry description/location/photos only; the reporter types the title on the form's required, empty field. No transcript-derived titles until parsing is genuinely smarter.
- Photo: review → Save, or **Save & create…** (issue / incident / punch) with the shot attached. Tags: suggested chips **plus search-or-add** (`TagEditor` over the project tag vocabulary). Markup optional; baked into the JPEG.
- Video: describe (skippable) → review → optional file-as-issue (location/note prefilled, title empty — see above).
- **Speak** on long text only (record Description / Blocking reason, collab message, pin comment). Not a mic on every field — keyboard/IME voice typing stays the fallback. One in-app take at a time; the camera stops Speak. **Opt-in, off by default** (2026-09-04): Settings → Voice input → "Voice input on forms" shows the control; off, those fields are plain. Voice note on Capture is not gated.

### Forms

Long create (records): scrolling fields + **sticky Save footer** (clear of gesture nav and keyboard). Progressive validation — don’t disable Save; announce missing required fields and jump to the first one. `*` on required labels. Description and Blocking reason have an explicit **Speak** control (EN/ES) when "Voice input on forms" is on in Settings; do not put a mic on Title, chips, dropdowns, dates, or hours.

Lists: Material 3 `ListItem`, outlined text fields, contextual **FAB** (or extended FAB with a label) for create on that list. Do not put the list’s primary create CTA inside a scrolling column. Collaboration composer and plan pin comment use the same compact Speak control.

### Visual

- Material 3; semantic tokens (`DesignTokens`, theme), not one-off colors.
- Selected nav: **filled primary pill** behind the icon (Linarc Onsite). Capture uses **unselected** colors.
- Dark theme default; Appearance toggle in Settings.
- Prefer Android conventions when they conflict with a literal Figma copy.
- Loading / empty / error / offline / disabled / validation where relevant. Explain unbuilt actions — **don’t fake a broken UI**.

---

## 5. Architecture (what to copy vs replace)

**Copy the shape.** Replace the stores.

| Layer | This prototype | Production |
|---|---|---|
| UI | Composables + unidirectional state | Same |
| Chrome | `resolveChrome(route)` — UI never sets bar/FAB ad hoc | Keep a single resolver |
| Data | In-memory `*Repository` objects, snapshot state | Real API + local cache |
| Outbox | In-process queue + “send all” demo | Real transport + retry |
| Auth | None; profile sheet explains | Real session |

Rules already in `CLAUDE.md`: no network/persistence/business rules in Composables; ask before new dependencies; reuse design-system components.

**Nav layout rule:** Pattern B lives **inside** `today_graph` / `plan_graph` / `tools_graph`. Pattern A / immersive routes live at the **nav-graph root** so one tab’s saved stack cannot clobber another’s. Shared destinations (e.g. daily-log playback, the plan sheet viewer) also sit at root for the same reason. The Plans **list** is dual-entry on purpose: `plan_home` is the tab root; `plans` (`PLAN_LIST`) is Pattern B inside the Tools graph so **Tools → Plans** does not jump tabs or overwrite the Plans tab stack.

---

## 6. Code map

Package root: `app/src/main/java/com/solomondesign/app/`

| Concern | Start here |
|---|---|
| Shell, tabs, Capture action | `ui/navigation/AppNavHost.kt`, `AppChrome.kt`, `AppRoutes.kt` |
| Today (all personas) | `ui/today/` (`TodayScreen`, `OwnerDashboard`, `OwnerTodaySections`) |
| Plan + pins | `ui/plan/` (`PlansScreen` is the tab *and* the Tools card; `PLAN_LIST` vs `PLAN_HOME`) |
| Tools catalog + Settings | `ui/tools/` (`PlatformTools.catalogFor` for persona order) |
| Camera / photo / video | `ui/capture/camera/` |
| Voice note | `ui/voicenote/` (`SpeakableTextField`, `VoiceNoteScreen`) |
| Field dictation (one take at a time) | `ui/voicelog/audio/FieldDictationBroker.kt`, `DictationController.kt` |
| Voice-to-Log (scripted demo) | `ui/voicelog/` — entry: Settings → Demo |
| Records (issue/incident/punch) | `ui/records/` |
| Images, markup, zoom | `ui/images/`, `ui/markup/`, `ui/designsystem/ZoomableContainer.kt` |
| Design system | `ui/designsystem/` — `TaskFlowScaffold`, `BrowseScaffold`, `FieldPageHeader`, `FieldForm`, `AppButton`, `TagEditor`, `FieldCollapsibleSectionHeader` |
| Demo seed + fan-out | `ui/demo/DemoProjectRepository.kt`, `DemoSession.kt` |
| Personas | `ui/persona/FieldPersona.kt` |

**What’s fully built vs placeholder**

- **Built:** Today, Plan viewer, Capture (photo/video/voice note/issue), Field task, Time card, Crew, Collaboration, Images, **Plans (tab and Tools card)**, Issues / Incidents / Punch list, Outbox, Voice log history, Settings / view-as, seven personas.
- **Catalog only / placeholder:** RFIs (as a tool — PM “RFIs” are Issues of type “RFI / design clarification”), T&M, Checklist, Drive, Toolbox Talks, Scan (no live scanner), Accounts on the project picker.

---

## 7. Non-goals (do not build from this repo)

Backend, auth, Hilt, Room, PDF/vector markup, live QR scan, Gantt/OAC dashboards, on-device Whisper/LLM (regex parser stays unless a dependency is approved), a separate subcontractor portal, a persona chip in the header.

**Still open in the prototype:** de-emphasize Plan content for Crew/Owner (spec iteration 2).

---

## 8. Verification

Before calling work done in this repo:

```text
./gradlew lint
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Voice mic path (Voice-to-Log, Voice note, Speak): `VOICE_LOG_TESTING.md`.

---

## 9. Building production from this reference

1. Keep the **chassis, patterns A/B/C, Capture-as-action, fan-out, issued≠blocked, persona reorder rule**.
2. Replace repositories with real data; keep the same screen jobs.
3. Fill placeholder tools without adding tabs.
4. Treat Settings → Demo as **internal**; production role comes from identity, not a picker.
5. When unsure, open the matching screen in this app, then the spec section — not an older 5-tab mock.
