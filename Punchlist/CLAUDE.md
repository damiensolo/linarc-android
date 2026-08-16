# Punchlist Android project instructions

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
- Bottom navigation is **only** Today, Plan, More.
- Capture is a Material 3 FAB + bottom sheet (Voice / Photo / Issue), never a tab.
- Default persona is Foreman. Other personas appear in More → Demo: view as as placeholders until iteration 2.
- Do not add Capture/Reports/Projects tabs, OAC/dashboards/Gantt, QR scan, OCR, PDF/vector drawing engines, LLM/Whisper dependencies, nested Project Space tabs, or a persona chip in the top app bar.
- Prefer Material 3 (`NavigationBar`, `FAB`, `ModalBottomSheet`, `ListItem`). Do not put a center “+” in the nav bar.
- Keep Voice-to-Log. Submitted logs must land on Today and as Plan pins, not only in a private history list.

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