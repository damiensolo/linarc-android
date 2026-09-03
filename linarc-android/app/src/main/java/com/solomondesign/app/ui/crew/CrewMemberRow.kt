package com.solomondesign.app.ui.crew

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.solomondesign.app.ui.demo.CrewMember
import com.solomondesign.app.ui.demo.DemoProjectRepository
import com.solomondesign.app.ui.demo.badgeColor
import com.solomondesign.app.ui.demo.statusLabel
import com.solomondesign.app.ui.designsystem.FieldWorkRow

/**
 * The standard crew row, shared by Today's roster, the Crew list, and Time cards.
 *
 * Resolves the avatar colour via [DemoProjectRepository.avatarColorFor] so the same person is the
 * same colour on every screen — [com.solomondesign.app.ui.theme.AvatarPalette] is index-driven,
 * so resolving by list position would drift between screens.
 */
@Composable
fun CrewMemberRow(
    member: CrewMember,
    modifier: Modifier = Modifier,
    enabled: Boolean = false,
    subtitleOverride: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit = {},
) {
    FieldWorkRow(
        title = member.name,
        subtitle = subtitleOverride
            ?: "${member.trade} · ${member.presence.statusLabel(DemoProjectRepository.AREA)}",
        statusColor = member.presence.badgeColor(),
        avatarName = member.name,
        avatarColor = DemoProjectRepository.avatarColorFor(member.id),
        avatarPhotoRes = member.photoRes,
        enabled = enabled,
        trailing = trailing,
        onClick = onClick,
        modifier = modifier,
    )
}
