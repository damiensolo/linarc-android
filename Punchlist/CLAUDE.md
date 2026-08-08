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

## Agent behavior
- Before implementation, provide a short plan: files to change, existing components to reuse, UI state, tests, and assumptions.
- Make small, reviewable changes.
- Do not modify unrelated files.
- Do not delete or overwrite working functionality without approval.

## Required verification
Before declaring a task complete, run:
1. ./gradlew lint
2. ./gradlew testDebugUnitTest
3. ./gradlew assembleDebug

Report:
- Files changed
- Commands run and results
- Any remaining manual emulator/device QA