# Field prototype — summary and demo script

Use this as a **12–15 minute live demo**: a 3-minute exec open, the walkthrough, then a 4-minute technical close. One story: *same jobsite, same objects, different job — one tap to capture, no dead ends, never lose your place.*

Product spec (source of truth): `Mobile Structure Validated v1.md`. Navigation contract: `NAVIGATION_PATTERNS.md`.

---

## 1. What this is (10 seconds)

This is a **field-first Android prototype** for Linarc Onsite. It proves that one chassis can serve seven jobsite roles without extra tabs, extra products, or a module maze. Default view: **Foreman on Riverside Medical — Area B.**

It is a strategy prototype, not a shipped backend. Capture, voice, records, plan pins, and outbox are real on-device flows. Sync, auth, and PDF plans are intentionally out of scope.

---

## 2. Customer value (say this first to the exec)

| Who | What they get | Why it matters |
|---|---|---|
| **Foreman** | Crew, blockers, Start My Day, capture in one tap | The day starts and evidence lands without hunting menus |
| **Crew** | My shift + my assignment | Clock and task, not a GC dashboard |
| **Superintendent** | Blockers and open issues first; Plan is one tap to pins | Oversight, not roster management |
| **Project manager** | Aging RFIs first (oldest = loudest) | Decisions before they become delays |
| **Project engineer** | RFI desk: draft + chase, then coordination & quality | Works the same RFIs the PM watches — one tap to Draft RFI |
| **Owner** | Confidence dashboard: photos + four decision topics | Schedule, budget, quality, “what needs me” — no labor ops |
| **Subcontractor** | My work + Request inspection | Trade scope and a GC message, not a second portal |

**Shared value:** one capture (photo, video, voice, issue) **fans out** to Today, Plan, and the right tool — never a private list. **Issued ≠ blocked:** only an explicit “blocks work” status stops the day. **Offline-first:** publish queues in the Outbox until signal returns.

**The bet:** reduce taps, keep orientation, and show the *same work objects* through the lens of each persona — instead of seven apps or a 5-tab kitchen sink.

---

## 3. Overall structure

**Startup:** Splash → Project list → Today.

**Bar (always):** **Today | Capture | Plans | Tools**

- **Today** — what needs me now (persona-specific order).
- **Capture** — *action, not a tab.* Camera. Never selected, no stack.
- **Plans** — one Area B sheet + pins (issues, photos, logs). Not a PDF engine.
- **Tools** — platform modules. Create lives *here* (FAB / +), not as a fourth tab.

**Three chrome patterns:**

- **A — full-screen task** (camera, record create, viewer): bar hides; Close / Save.
- **B — browse** (lists, details, settings): bar stays; Back one level; tab stacks survive.
- **C — sheet** (profile, Start My Day, new time entry): dismiss and you’re back.

---

## 4. Emphasis — four product claims

**Ease of use.** Nothing on Today dead-ends. Rows open the real record, photo, video, or log. Long forms keep **Save** sticky. Empty/error/offline states are named, not blank.

**Navigation.** Tabs are siblings (crossfade). Each tab keeps its own stack: leave Tools mid-task, check Today, tap Tools once — you’re back. Tap Tools again — catalog. Capture is always one tap from the bar, next to Today.

**Quick actions.** Camera chips: **Voice note** (EN/ES, works without camera permission) and **Issue**. Photo: Save, or Save & create a record with the shot attached. Markup on the shot. Contextual FAB only where create belongs (issues, time cards, images, collab).

**Personalization.** Same three tabs, same objects, **reordered** by persona (Settings → Demo: view as). Profile identity stays the signed-in user. Owner is the one exception: no time cards, no voice log, no crew ops — a stakeholder view, not a hidden tile.

---

## 5. How to run the room

**With the exec (first ~8 min):** value, IA, Foreman happy path, two persona flips (Crew + Owner), one “this is not seven apps” line. Skip Settings internals and code.

**With the technical team (next ~5 min, or a second pass):** Pattern A/B/C, nested graphs + `saveState`/`restoreState`, Capture as action, fan-out, blocking vs issued, Outbox as queued publishes, what is *not* built (no Room/Hilt/sync/PDF).

**Ground rules:** one device, Foreman first, don’t apologize for demo data. If something fails, skip to the next numbered beat — the script is ordered so each beat still stands.

---

## 6. Live demo script (~12 minutes)

### Open (45s) — both audiences

> “This is the field chassis. Not a module menu. Three destinations plus Capture as an action. Everything you create shows up where the crew actually looks: Today and the Plan.”

Show the bar: **Today, Capture, Plans, Tools.**

---

### Beat 1 — Orient (1 min) — Foreman

1. Splash → Project list → **Riverside Medical**.
2. Today: crew, Start My Day → **Confirm**.
3. Point: “This is ‘what needs me now,’ not a dashboard of every module.”

---

### Beat 2 — Capture is one tap (2.5 min)

1. Tap **Capture** (next to Today).
2. **Photo** path (if time is tight, skip markup): shoot → Save. Then: Today row, Plan pin, Images — “one save, three surfaces.”
3. Back to Capture → **Voice note** chip: speak a short Spanish line *or* English defect. Toggle EN/ES → **Create → Issue** → Save. Point: “Works even if camera permission is denied. The note is disposable; the record is the artifact.”
4. Optional if you have 30s: **Issue** chip → sticky Save on the form.

**Line:** “We did not put Capture in a tab. A tab would steal a selected state and a back stack. This is a shutter.”

---

### Beat 3 — Navigation doesn’t wipe work (1 min)

1. **Tools → Field task** (open a detail).
2. **Today**.
3. Tap **Tools** once → still on that task.
4. Tap **Tools** again → catalog.

**Line:** “Field people bounce. We restore place on the first tap, and give an escape hatch on the second. Same as iOS tabs, Material 3 nav bar, Android multiple back stacks.”

---

### Beat 4 — Plan as shared truth (1 min)

1. **Plans** → open the sheet → pinch/zoom → tap a pin (Column 4 if you ran Voice daily log; otherwise any capture pin).
2. Comment → **Publish to team** → mention Outbox.

**Line:** “The drawing is the shared map. Pins are the work. Comments queue offline.”

---

### Beat 5 — Voice-to-Log differentiator (1.5 min)

Optional if Voice note already landed an issue.

**Settings (Tools ⋮) → Demo → Voice daily log (demo)** → Hector script → Submit.

Today: delay + issue. Plan: Column 4 pin.

**Line:** “Structured daily log from speech — labor, delay, issue — not a private transcript.”

Manual mic setup: `VOICE_LOG_TESTING.md`.

---

### Beat 6 — Persona personalization (3 min)

Always: **Tools ⋮ → Settings → Demo: view as.** Profile avatar stays Alex Rivera.

| Order | Switch to | Show | One sentence |
|---|---|---|---|
| 1 | **Crew** | Today: My shift, My assignment. Start/end shift. Tools lead: Field task, Time card | “The crew sees *their* day, not the GC’s.” |
| 2 | **Superintendent** | Today opens on Blockers + open issues. Plans: **Pinned work** shortcut | “Oversight first. Plan is the power view.” |
| 3 | **Project manager** | Aging RFIs oldest-first (RFI-121, 6 days, red) | “Silence gets more expensive with age.” |
| 4 | **Project engineer** | RFI desk (count + oldest age) → **Draft RFI**. Open RFIs, then Coordination & quality (med-gas conflict first) | “The PE *works* the queue the PM *watches*.” |
| 5 | **Owner — Decision dashboard** | Photos + four topics (schedule, budget, quality, decisions) + delays. Tools: no Time card, no Voice logs | “Confidence, not operations.” |
| 6 | **Subcontractor** | Inspections → Request inspection → pick a task. My work (blocked med-gas) | “Trade scope + one message to the GC. Not a portal.” |

If time is short: **Crew + Owner + Subcontractor** only. If you already showed Aging RFIs, add **Project engineer** — Draft RFI is the 10-second proof they share objects.

---

### Beat 7 — Close (45s)

> “Same objects. Same bar. Role changes *priority*, not the product. Capture is always next to Today. Create never dies in a private list. That’s the field product we should scale — not another tab, and not seven apps.”

**Ask:** “Is this the IA we take to customers, or do we still want a module-first home?”

---

## 7. Technical appendix (2–4 min, after the demo)

- **Native:** Kotlin, Compose, Material 3, CameraX, SpeechRecognizer, on-device ML Kit translate.
- **Nav:** nested graphs per tab; Pattern A at graph root so camera/create don’t clobber a tab stack. Implementation: `AppNavHost.kt`, `AppChrome.kt`.
- **Data:** in-memory demo repositories; Outbox is a queue UI, not a sync engine.
- **Records:** one create form (issue / incident / punch); blocking is explicit and auditable.
- **Non-goals:** no backend, auth, Room, Hilt, PDF/vector markup, live Scan, LLM parser.
- **Nav contract tests:** `AppNavHostTest` — tab switch restores stack; reselect pops to Tools home.

---

## 8. Slack one-pager (paste before the meeting)

**Field prototype — exec + eng walkthrough**  
IA: Today · Capture (action) · Plans · Tools. Default Foreman. Seven live personas via Demo: view as (same tabs, reordered Today/Tools; Owner omits labor/voice). Project engineer is the RFI desk (draft + chase) next to the PM’s aging overview.  
Proofs: one-tap capture that fans out; tab stacks that restore; sticky create; bilingual voice → record; issued ≠ blocked; Outbox for offline publish.  
Not in this build: sync, auth, PDF plans, dashboards as a seventh tab.  
Ask: confirm this chassis as the field product, not a 5-tab or multi-app strategy.
