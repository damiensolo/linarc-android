# Field prototype Android project instructions

## Technology
- Native Android application
- Kotlin and Jetpack Compose
- Material 3
- Kotlin DSL Gradle files
- Coroutines and Flow for asynchronous work
- ViewModels expose immutable UI state
- Repository pattern for data access

## Architecture
- Keep UI, data, and domain responsibilities separate.
- Use unidirectional data flow.
- Do not put network, persistence, or business rules in Composables.
- Prefer existing project patterns over inventing new architecture.
- Ask before adding a new dependency, Gradle plugin, or module.

## UI and design system
- Reuse design-system components before creating screen-specific duplicates.
- Use semantic design tokens rather than hard-coded colors, spacing, or typography.
- Support loading, empty, error, offline, disabled, and validation states where relevant.
- Use accessible content descriptions and proper semantics.
- Keep Android conventions when they conflict with a literal Figma translation.

## Field prototype (source of truth)
- Product spec: `Mobile Structure Validated v1.md`. Follow it; do not invent parallel IA.
- Lead mobile developer handoff (how to walk the prototype and use it as a production reference): `HANDOFF.md`.
- Functional vs placeholder tools and step-by-step try-it workflows: `FEATURE_GUIDE.md`.
- Bottom navigation is Today, Capture, Plans, Tools — and **Capture is an action, not a destination** (decided 2026-08-24, superseding the earlier FAB + sheet rule): it opens the full-screen in-app CameraX camera, never shows a selected state, and never owns a back stack. Do not add further tabs.
- Voice note (bilingual EN/ES voice capture → creates Issue/Incident/Punch item) and Quick issue stay one tap away as quick chips on the camera screen; they must remain usable even when camera permission is denied. The Voice note chip replaced the Voice daily log chip on 2026-08-25 — do not re-add voice daily log to the Capture UI.
- The FAB is contextual-only (New time entry, New topic, Add image, New issue/incident/punch item on the record tool lists). Never reintroduce a global Capture FAB or a capture bottom sheet.
- Default persona is Foreman; all seven personas are live via Settings → Demo: view as (six on 2026-08-25, Project engineer on 2026-09-03): same three tabs, same objects reordered — Crew's Today leads with My shift (start/end shift logs a real queued time entry) and My assignment (Hector Ortiz is the crew-view member); Superintendent's leads with Blockers + Open issues & inspections (`attentionOrder()`) and Plans gains a pin-sheet shortcut row; Project manager's leads with Aging RFIs (`agingRfis()`, oldest first) then Delays & blockers and Decisions & discussions (Collaboration topics); Project engineer's leads with the RFI desk card (live open-RFI count + oldest age; Draft RFI stages the Issue form on the RFI type via `RecordDraft.begin(seedType = RFI_ISSUE_TYPE)`) then Open RFIs (`agingRfis()`) and Coordination & quality (`technicalQueue()`: non-RFI issues + punch, attention-ordered, no incidents) and shares the PM's Decisions & discussions; Owner's is the confidence dashboard (`OwnerTodaySections.kt`, pure logic + thresholds in `OwnerDashboard.kt`, demo-seeded schedule/budget figures isolated in `OwnerDemoMetrics`): Progress photos (photos/videos only) then exactly four decision topics (Schedule health, Budget & changes, Quality & approvals — live from records, Decisions needed — ranked, no punch/incident rows) then Delays as `DelayBlockerCard`s, and no roster/captures/Start-My-Day at all — with the original v1 Owner layout kept demoable as a second Owner row in the Demo: view as picker (`OwnerTodayVariant`, dashboard is default; no separate setting); Subcontractor's leads with the Inspections card (Request inspection → Today row + queued Outbox entry) and My work (Sam Reyes's whole trade via `forTrade`). Tools reorders via `PlatformTools.catalogFor`. Never remove tabs per persona; tools only reorder — with one spec-sanctioned exception: Owner ("no time cards or voice log", extended 2026-08-25 to crew operations) drops the Time card tool, the Voice logs activity row, and every operational Today surface (roster, Recent captures, voice-log rows).
- Prefer Material 3 (`NavigationBar`, `FAB`, `ModalBottomSheet`, `ListItem`).
- Keep Voice-to-Log as the scripted demo at Settings → Demo → "Voice daily log (demo)" (moved 2026-08-25). Submitted logs must land on Today and as Plan pins, not only in a private history list; the Voice logs history stays on Tools.
- Captured photos must land on Today, as Plan pins, and in the Images grid — never only in a private list.

## Agent behavior
- Before implementation, provide a short plan: files to change, existing components to reuse, UI state, tests, and assumptions.
- Make small, reviewable changes.
- Do not modify unrelated files.
- Do not delete or overwrite working functionality without approval.
- Do not resurrect screens or tabs from older 5-tab IA drafts.

## Required verification
Before declaring a task complete, run:
1. ./gradlew lint
2. ./gradlew testDebugUnitTest
3. ./gradlew assembleDebug

Report:
- Files changed
- Commands run and results
- Any remaining manual emulator/device QA