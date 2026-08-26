package com.solomondesign.app.ui.today

/**
 * The two demoable Owner Today layouts, switchable at Settings → Demo → "Owner Today layout"
 * (mirroring the Splash animation picker). Both existed in the product's history and are kept
 * side-by-side deliberately for A/B demo conversations — this is a demo control, not a user
 * preference:
 *
 * - [DASHBOARD] — the 2026-08-25 confidence dashboard (OwnerTodaySections.kt): progress
 *   photos, the four decision topics, enriched delays. The default.
 * - [CLASSIC] — the original v1 Owner view it replaced: progress photos, the flat
 *   Decisions & discussions topic list, the shared Delays rows, and the collapsed
 *   "On site today" roster.
 *
 * Selection lives on [com.solomondesign.app.ui.demo.DemoProjectRepository.ownerTodayVariant]
 * and resets to [DASHBOARD] with the rest of the demo state.
 */
enum class OwnerTodayVariant(val title: String, val subtitle: String) {
    DASHBOARD(
        title = "Decision dashboard (v2)",
        subtitle = "Photos · four decision topics · delays with impact",
    ),
    CLASSIC(
        title = "Photos & discussions (v1)",
        subtitle = "Photos · topic list · delays · roster — the original",
    ),
}
