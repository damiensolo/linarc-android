package com.solomondesign.app.ui.profile

/**
 * The field user signed in on this device — distinct from
 * [com.solomondesign.app.ui.demo.DemoProjectRepository.persona], which only controls which
 * role's UI **Tools → Demo: view as** is previewing. The spec explicitly disallows anything
 * that functions as a persona chip in the header, so this identity stays fixed regardless of
 * which persona is being demoed.
 *
 * Fixed for this build since Foreman is the only live persona. If a future iteration makes
 * multiple personas live at once, revisit whether this needs to vary per signed-in user.
 */
object CurrentUser {
    const val NAME = "Alex Rivera"
    const val JOB_TITLE = "Foreman"
    val photoRes: Int? = null
}
