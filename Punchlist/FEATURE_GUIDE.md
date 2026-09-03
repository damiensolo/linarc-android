# Prototype feature guide

What is **real**, what is **partial**, and what is only a **placeholder** — plus step-by-step paths to try each working flow.

This is a **strategy prototype**. Data lives in memory (it resets on Logout or process kill). There is no backend, login, or real sync.

**Related docs:** product spec (`Mobile Structure Validated v1.md`) · developer handoff (`HANDOFF.md`) · live demo script (`DEMO_SCRIPT.md`)

---

## How to read this

| Label | Meaning |
|---|---|
| **Functional** | You can complete a real in-app workflow. Results show up on Today, Plan, and/or a tool. |
| **Semi-functional** | The UI and local behavior work; a production piece is faked or omitted (no cloud sync, demo metrics, one shared project dataset, etc.). |
| **Placeholder** | A tile or screen that shows the *idea* of a module. Tapping it opens a generic “not built yet” list or message. **Do not treat this as a working feature.** |

Placeholders are intentional. They show the full Tools catalog without faking broken product.

**Default start:** Foreman · project **Riverside Medical — Area B** · bar **Today | Capture | Plans | Tools**

---

## Chassis and home

| Area | Status | Notes |
|---|---|---|
| Splash | Functional | Brand animation, then project list |
| Project list | Semi-functional | Search works. **Any** row loads the same seeded job. Only Riverside Medical is “the” project. **Accounts** tab is a placeholder |
| Today | Functional | Persona-specific; rows open the real photo, video, record, task, or log. Every section header collapses (tap it — same as the crew roster) |
| Capture (bar button) | Functional | Action, not a tab — never stays selected |
| Plans (tab) | Functional | One Area B sheet + pins. Compose drawing, not a PDF engine |
| Tools catalog | Functional | Grid/list switch. Built tools open real screens; others open placeholders |
| Profile sheet | Semi-functional | **Switch project** and **Logout** work. Other rows explain they have no backend |
| Dark / light theme | Functional | Settings → Appearance |
| Demo: view as | Functional | All seven personas. Profile photo/name stay Alex Rivera |

---

## Tools catalog

Open **Tools**. A **+** on a card is quick-create (real or placeholder — see below).

### Functional

| Tool | What works | How to open |
|---|---|---|
| **Field task** | List, filters, detail, status, checklist | Tools → Field task |
| **Time card** | Crew list, member detail, new entry (FAB) | Tools → Time card |
| **Crew** | Roster → member detail | Tools → Crew |
| **Collaboration** | Topics → thread, new topic (FAB or +) | Tools → Collaboration |
| **Images** | Grid / Timeline / Albums / Map, viewer, markup, album, create-from-photo | Tools → Images |
| **Issues** | List, detail, create (FAB or +) | Tools → Issues |
| **Incidents** | Same record system as Issues | Tools → Incidents |
| **Punch list** | Same record system as Issues | Tools → Punch list |
| **Outbox** | Queue of publishes; **Signal restored — send all** (demo drain, not a sync engine) | Tools → Activity Center → Outbox |
| **Voice logs** | History + playback of submitted Voice-to-Log demos | Tools → Activity Center → Voice logs |
| **Settings** | Theme, Demo: view as, splash picker, Voice daily log (demo) | Tools header **⋮** → Settings |

**Plans (the tab)** is functional. **Plans (the Tools card)** is a placeholder that points at the same idea — use the **Plans** tab instead.

### Semi-functional (on purpose)

| Item | What is real | What is not |
|---|---|---|
| Outbox | Queue + “send all” flips items to sent | No network, no retry, no conflict handling |
| RFIs as a *product idea* | PM **Aging RFIs** and PE **RFI desk / Open RFIs / Draft RFI** are real Issues of type “RFI / design clarification” | The **RFIs** Tools card is still a placeholder |
| Owner dashboard charts | Quality/decisions use live records | Schedule and budget figures are labeled demo data |
| Image markup | Works on **photos you capture** | Seeded demo photos explain they can’t be marked up |
| Scan | Tile is in the catalog | No live camera scanner |

### Placeholder tools

Tapping these opens a generic sample list (“… · sample 1”). **+** on RFIs or Toolbox Talks opens “Quick create is a placeholder in this build.”

| Tool | Why it’s here |
|---|---|
| Plans (Tools card) | Catalog completeness — use the **Plans tab** |
| RFIs | Catalog; use Issues, PM Aging RFIs, or the PE **RFI desk** / **Draft RFI** for the real flow |
| T & M | Catalog only |
| Checklist | Catalog only (Field task still has a real checklist) |
| Drive | Catalog only |
| Toolbox Talks | Catalog only |
| Scan | Catalog only |

---

## Capture (camera)

| Path | Status | Lands on |
|---|---|---|
| Photo → Save | Functional | Today, Plan pin, Images |
| Photo → Save & create… | Functional | Photo + a new issue / incident / punch with the shot attached |
| Photo markup | Functional | Baked into the JPEG (captured photos only) |
| Video → describe → save | Functional | Today, Plan pin |
| Video → file as issue | Functional | Video + Issue form (location/note prefilled; **title left empty** — you name the issue) |
| Voice note chip | Functional | Create → record form (chips, optional photo, Add to existing). Dictation fills the **description only — never the title**. Works **without** camera permission. Note is not saved as audio |
| Issue chip | Functional | Record create form (Issue) |
| Voice daily log | Functional | **Settings → Demo → Voice daily log (demo)** — not on the camera anymore |

Voice note language detect is **best-effort**; the EN | Español toggle is the source of truth. Translation uses on-device ML Kit (models download once).

---

## Step-by-step workflows

Start each flow from **Today** unless a step says otherwise. Use a **device or emulator with a mic** for voice steps (`VOICE_LOG_TESTING.md`).

### 1. Get into the app

1. Launch the app.
2. Wait through splash.
3. On the project list, tap **Riverside Medical** (or any row — same data).
4. You should see **Today** (Foreman): crew, Start My Day, blockers, recent captures.

### 2. Start My Day

1. On Today, tap **Start My Day**.
2. Confirm the sheet (crew / area / weather are prefilled).
3. The prompt goes away for this session.

### 3. Take a photo and see it everywhere

1. Tap **Capture** (camera icon between Today and Plans). It should **not** stay highlighted.
2. Stay on **Photo**. Tap the shutter.
3. Add a title if you want. Tags: tap the suggested chips, or use **Search or add a tag** — type to find any existing project tag, or add a brand-new one → **Save**.
4. Check **Today** → Recent captures (tap the row → full-screen viewer).
5. Check **Plans** → open the sheet → tap the new pin.
6. Check **Tools → Images** — the photo is in the grid.

**Optional markup:** on the viewfinder, turn on **Markup** before the shutter, or markup from the photo review / image viewer. Save a **copy** (fans out again) or replace the original.

### 4. Save a photo as an issue

1. Capture → photo → review.
2. Tap **Save & create…**.
3. Pick **Issue** (or Incident / Punch item).
4. Fill **Title** (required). Sticky **Save record** at the bottom.
5. After save: Today, Plan pin, **Tools → Issues**, and Outbox all update.

### 5. Voice note → issue (bilingual)

1. Capture → tap the **Voice note** chip (works even if you deny camera).
2. Speak (try Spanish: *falta concreto en la pared del nivel dos*).
3. Confirm the **English | Español** toggle.
4. **Done** → **Translate** if you want the other language on screen.
5. Review shows chips (Issue / Incident / Punch). Change if needed.
6. Optional: **Add photo** (camera, then back — sequential, not at the same time).
7. **Create**. The full form opens: the description is prefilled, the **title is empty** (you type it — dictated text never auto-fills the title, so there's nothing to delete first).
8. The description is **exactly the text showing when you tapped Create** (one language, not both).
9. You should land back on Today, not the recorder.

**Blocking:** say “this is blocking plumbing at column 4” — review should pre-check Blocks work.

**Add to existing:** say “RFI-118 still waiting” or name the med-gas task → **Add to…** → Today row opens that record or task. (This is the Project engineer / Superintendent path: comment on the open RFI instead of minting a duplicate issue.)

**Photo then voice:** Capture → photo → **Add voice note** → dictate → Create (photo already attached).

### 6. Camera Issue chip

1. Capture → **Issue** chip.
2. Complete the record form → Save.

### 7. Record a video (optional)

1. Capture → **Video** on the mode rail.
2. Record (caps around 90 seconds) → describe or skip → review.
3. Save, or turn on **File an issue** then save.

### 8. Voice-to-Log (scripted daily log)

This is the older structured log (labor, delays, Column 4 issue). It is **not** on the camera.

1. **Tools** → header **⋮** → **Settings**.
2. **Voice daily log (demo)**.
3. Allow the microphone. Speak the Hector script (see `VOICE_LOG_TESTING.md`) or any site narration.
4. Review → **Submit**.
5. **Today** shows delay / issue rows. **Plans** gets a pin near Column 4 when the script mentions it.
6. **Tools → Voice logs** plays the submitted log.

### 9. Create an issue that blocks work

1. **Tools → Issues** → FAB **+** (or Capture → Issue).
2. Title required. Leave **Blocks work?** off first → Save. It **logs**; it should **not** appear under Today Blockers.
3. Create another (or the same flow) and turn **Blocks work?** **on**.
4. Fill **Blocking reason** (required). Save.
5. **Today → Blockers** shows it in red. The Issues list row flags “Blocks work.”

Try type **Safety hazard** or **Failed inspection** — blocking may default on.

### 10. Field task, time, crew, collab

**Field task**

1. Tools → Field task → tap a task.
2. Change status / checklist. Back stays in Tools.

**Time card**

1. Tools → Time card → a crew member.
2. FAB → new time entry → save. Check **Outbox**.

**Crew**

1. Tools → Crew → a name.

**Collaboration**

1. Tools → Collaboration → a topic → send a message, or FAB **New topic**.

### 11. Images views

1. Tools → Images.
2. Switch **Grid / Timeline / Albums / Map**.
3. Tap a photo. Toolbar: Share / Markup / Album / Delete / Create.
4. **Map** pins captures on the Level 2 sheet (site drawing — no GPS in this build).

### 12. Plan pins and comments

1. Tap **Plans**.
2. Open the Area B / Level 2 sheet.
3. Pinch to zoom, pan while zoomed, double-tap to reset.
4. Tap a pin → sheet with title / photo / comments.
5. Add a comment → **Publish to team**.
6. **Tools → Outbox** — the publish is queued.

### 13. Tab stacks (don’t get lost)

1. Tools → Field task → open a detail.
2. Tap **Today**.
3. Tap **Tools** **once** — you should still be on that task.
4. Tap **Tools** **again** — catalog.
5. Third tap on the catalog does nothing.

### 14. Switch project / logout

1. Header: tap the **project name**, or **⋮ → Switch project**, or avatar → **Switch project**. Demo data is kept.
2. Avatar → **Logout** resets the demo session (fresh launch).

### 15. Personas (same tabs, different Today)

**Tools → ⋮ → Settings → Demo: view as**

| Pick | Then look at | You should see |
|---|---|---|
| **Crew** | Today, then Tools | My shift (start/end), my assignment. Tools lead with Field task / Time card |
| **Superintendent** | Today, then Plans | Blockers + open issues first. Plans: **Pinned work** shortcut |
| **Project manager** | Today | **Aging RFIs** oldest first (RFI-121 ~6 days, red), then delays, then discussions |
| **Project engineer** | Today, then Tools | **RFI desk** card (count + oldest age, **Draft RFI** opens the Issue form already on the RFI type), Open RFIs, Coordination & quality. Tools lead with RFIs / Issues / Plans |
| **Owner — Decision dashboard** | Today, then Tools | Photos + four decision cards + delays. **No** Time card tile, **no** Voice logs row |
| **Owner — Photos & discussions (v1)** | Today | Older owner layout (kept for comparison) |
| **Subcontractor** | Today | **Request inspection** → pick a task → Today row + Outbox. My work includes the blocked med-gas task |

Flip back to **Foreman** when you’re done.

**Crew shift:** Crew view → **Start shift** → **End shift** → Time card + Outbox get an entry.

**Sub inspection:** Request inspection is a **message to the GC**, not a new punch item.

**PE Draft RFI:** Project engineer view → **Draft RFI** → the Issue form opens already on *RFI / design clarification* → Save → it lands in the Issues tool, at the bottom of Open RFIs ("Opened today"), and in the Outbox.

### 16. Outbox (offline demo)

1. Do any save/publish (photo, record, time entry, pin comments).
2. Tools → **Outbox** — items “waiting for signal.”
3. Tap **Signal restored — send all**. They flip to sent. This is **not** a real server.

### 17. What a placeholder looks like

1. Tools → **Drive** or **T & M** or **Scan**.
2. You get a back arrow and sample rows, or an explanation.
3. That is catalog IA, not a missing crash.

---

## What this prototype does not do

No real login, API, or database. No PDF plan engine. No live barcode/QR scan. No Gantt, cost codes, or OAC reports. No cloud sync (Outbox is the stand-in). Profile actions other than Switch project / Logout only explain.

---

## Quick “is it broken?” check

| You see | It means |
|---|---|
| “placeholder in this build” / sample 1–3 | Unbuilt tool — expected |
| “Demo data” on Owner charts | Seeded metrics — expected |
| “Demo tiles can’t be marked up” | Markup needs a **captured** photo |
| Capture not highlighted in the bar | Correct — it’s an action |
| Accounts tab empty explanation | Unbuilt — expected |
| Logout wiped your captures | Expected — in-memory demo session |
