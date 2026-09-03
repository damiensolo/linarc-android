package com.solomondesign.app.ui.designsystem

/**
 * Pure matching logic behind [TagEditor]'s search-or-add field. Kept free of
 * `androidx.compose` imports so it runs under `./gradlew testDebugUnitTest`, like the other
 * model-layer files.
 */

/**
 * Existing tags offered for [query]: a case-insensitive contains-match over [allTags], minus
 * anything already in [selected] (case-insensitive — "framing" and "Framing" are one tag).
 * A blank query offers nothing; the field is a search, not a browse-all list.
 */
fun tagMatches(query: String, allTags: List<String>, selected: Collection<String>): List<String> {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return emptyList()
    return allTags
        .filter { it.contains(trimmed, ignoreCase = true) }
        .filterNot { tag -> selected.any { it.equals(tag, ignoreCase = true) } }
        .distinct()
}

/**
 * The brand-new tag [query] would create, or null when it shouldn't: blank input, an existing
 * tag with the same name (the match chip already offers it — offering "Add" too would mint a
 * case-variant duplicate), or one already selected.
 */
fun newTagCandidate(query: String, allTags: List<String>, selected: Collection<String>): String? {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return null
    val known = allTags.any { it.equals(trimmed, ignoreCase = true) } ||
        selected.any { it.equals(trimmed, ignoreCase = true) }
    return trimmed.takeUnless { known }
}
