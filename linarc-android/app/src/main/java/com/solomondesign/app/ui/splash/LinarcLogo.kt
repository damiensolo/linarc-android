package com.solomondesign.app.ui.splash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.testTag

/** Linarc brand orange from the design-system primary logo (`icon/logo`). */
val LinarcOrange = Color(0xFFF97316)

/** Figma Primary Logo frame. */
internal const val LockupViewBox = 400f

/** Group 37 — the stacked mark + wordmark + tagline. */
internal const val LockupContentMinX = 100.390625f
internal const val LockupContentMinY = 71.09375f
internal const val LockupContentWidth = 199.21875f
internal const val LockupContentHeight = 257.81247f

/** On-screen lockup width. 108dp is ~55% of the previous 240dp treatment. */
internal const val LockupDisplayWidthDp = 108

/** Figma bounds for each LINARC letter — used as SplitText overflow masks. */
private data class LetterSlot(val minX: Float, val minY: Float, val width: Float, val height: Float)

private val LetterSlots = listOf(
    LetterSlot(100.390625f, 258.763f, 24.190f, 39.930f),
    LetterSlot(130.273f, 258.763f, 9.961f, 39.930f),
    LetterSlot(145.926f, 258.763f, 34.864f, 39.930f),
    LetterSlot(185.059f, 258.763f, 39.843f, 39.930f),
    LetterSlot(229.171f, 258.763f, 32.729f, 39.930f),
    LetterSlot(266.169f, 258.763f, 33.440f, 39.930f),
)

private val TaglineSlot = LetterSlot(100.390625f, 314.66f, 199.21875f, 14.24f)

private const val PivotX = 201f
private const val PivotY = 157f

internal val RestingBlade = BladeMotion()

internal data class BladeMotion(
    val alpha: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val rotationDeg: Float = 0f,
    val scale: Float = 1f,
)

internal data class LinarcMarkPaths(
    val top: Path,
    val right: Path,
    val left: Path,
    val letters: List<Path>,
    val tagline: Path,
) {
    companion object {
        fun parse(): LinarcMarkPaths = LinarcMarkPaths(
            top = parseSvgPath(TopBlade),
            right = parseSvgPath(RightBlade),
            left = parseSvgPath(LeftBlade),
            letters = listOf(
                parseSvgPath(LinarcLogoPaths.LetterL),
                parseSvgPath(LinarcLogoPaths.LetterI),
                parseSvgPath(LinarcLogoPaths.LetterN),
                parseSvgPath(LinarcLogoPaths.LetterA),
                parseSvgPath(LinarcLogoPaths.LetterR),
                parseSvgPath(LinarcLogoPaths.LetterC),
            ),
            tagline = parseSvgPath(LinarcLogoPaths.Tagline),
        )
    }
}

internal fun parseSvgPath(data: String): Path =
    PathParser().parsePathString(data).toPath()

/**
 * Figma Primary Logo lockup: three blades, LINARC outlines, and
 * "Connect - Build - Thrive" in the original 400 viewBox so kerning is exact.
 *
 * [letterAlphas] is six values (L I N A R C). Letters may fade, clip, or shift
 * in Y, but never move in X — that would re-kern the wordmark.
 *
 * [letterClips] is a bottom-up mask (0 hidden, 1 full). [letterScramble] cycles
 * other LINARC glyphs inside each letter's Figma slot, then locks.
 */
@Composable
internal fun LinarcLockup(
    top: BladeMotion,
    right: BladeMotion,
    left: BladeMotion,
    markColor: Color,
    ink: Color,
    modifier: Modifier = Modifier,
    glowAlpha: Float = 0f,
    markScale: Float = 1f,
    markBlur: Float = 0f,
    letterAlphas: List<Float> = List(6) { 1f },
    letterOffsetY: Float = 0f,
    letterOffsetYs: List<Float> = emptyList(),
    letterClips: List<Float> = List(6) { 1f },
    letterScramble: Float = 0f,
    scrambleClock: Float = 0f,
    taglineAlpha: Float = 1f,
    taglineClip: Float = 1f,
) {
    val paths = remember { LinarcMarkPaths.parse() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(LockupContentWidth / LockupContentHeight)
            .testTag("splashLockup"),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (markBlur > 0.4f) {
                        renderEffect = BlurEffect(markBlur, markBlur, TileMode.Decal)
                    }
                },
        ) {
            lockupTransform {
                val glowCenter = Offset(PivotX, PivotY)
                if (glowAlpha > 0.01f) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                markColor.copy(alpha = glowAlpha * 0.55f),
                                Color.Transparent,
                            ),
                            center = glowCenter,
                            radius = 120f,
                        ),
                        radius = 120f,
                        center = glowCenter,
                    )
                }
                scale(markScale, markScale, pivot = glowCenter) {
                    drawBlade(paths.top, top, markColor)
                    drawBlade(paths.right, right, markColor)
                    drawBlade(paths.left, left, markColor)
                }
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            lockupTransform {
                paths.letters.forEachIndexed { index, path ->
                    val alpha = letterAlphas.getOrElse(index) { 1f }
                    if (alpha <= 0.01f) return@forEachIndexed
                    val offsetY = letterOffsetYs.getOrElse(index) { letterOffsetY }
                    val clip = letterClips.getOrElse(index) { 1f }
                    drawLetterInSlot(
                        path = path,
                        paths = paths.letters,
                        index = index,
                        alpha = alpha,
                        offsetY = offsetY,
                        clip = clip,
                        scramble = letterScramble,
                        clock = scrambleClock,
                        ink = ink,
                    )
                }
                if (taglineAlpha > 0.01f && taglineClip > 0.01f) {
                    val wipe = taglineClip.coerceIn(0f, 1f)
                    if (wipe >= 0.995f) {
                        drawPath(paths.tagline, ink.copy(alpha = taglineAlpha.coerceIn(0f, 1f)))
                    } else {
                        clipRect(
                            left = TaglineSlot.minX,
                            top = TaglineSlot.minY,
                            right = TaglineSlot.minX + TaglineSlot.width * wipe,
                            bottom = TaglineSlot.minY + TaglineSlot.height,
                        ) {
                            drawPath(paths.tagline, ink.copy(alpha = taglineAlpha.coerceIn(0f, 1f)))
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.lockupTransform(block: DrawScope.() -> Unit) {
    val fitted = size.width / LockupContentWidth
    scale(fitted, fitted, pivot = Offset.Zero) {
        translate(-LockupContentMinX, -LockupContentMinY) {
            block()
        }
    }
}

private fun DrawScope.drawLetterInSlot(
    path: Path,
    paths: List<Path>,
    index: Int,
    alpha: Float,
    offsetY: Float,
    clip: Float,
    scramble: Float,
    clock: Float,
    ink: Color,
) {
    val slot = LetterSlots[index]
    val sourceIndex = if (scramble > 0.04f) {
        val ticks = (clock * (10f + scramble * 28f)).toInt()
        (index + ticks * 3 + ticks / 2) % paths.size
    } else {
        index
    }
    val sourcePath = if (sourceIndex == index) path else paths[sourceIndex]
    val from = LetterSlots[sourceIndex]
    val clipAmount = clip.coerceIn(0f, 1f)
    val color = ink.copy(alpha = alpha.coerceIn(0f, 1f))
    val rest = scramble < 0.02f && clipAmount >= 0.995f && kotlin.math.abs(offsetY) < 0.25f && sourceIndex == index
    if (rest) {
        drawPath(sourcePath, color)
        return
    }
    val clipTop = slot.minY + slot.height * (1f - clipAmount)
    clipRect(
        left = slot.minX - 0.6f,
        top = clipTop,
        right = slot.minX + slot.width + 0.6f,
        bottom = slot.minY + slot.height + 0.6f,
    ) {
        translate(slot.minX - from.minX, offsetY) {
            drawPath(sourcePath, color)
        }
    }
}

private fun DrawScope.drawBlade(
    path: Path,
    motion: BladeMotion,
    color: Color,
) {
    if (motion.alpha <= 0.01f) return
    translate(motion.offsetX, motion.offsetY) {
        rotate(motion.rotationDeg, pivot = Offset(PivotX, PivotY)) {
            scale(motion.scale, motion.scale, pivot = Offset(PivotX, PivotY)) {
                drawPath(path, color.copy(alpha = motion.alpha.coerceIn(0f, 1f)))
            }
        }
    }
}

// Three closed subpaths of the evenodd Linarc mark (Figma Primary Logo).
private const val TopBlade =
    "M196.959 103.638V103.658C196.767 104.921 196.585 106.174 196.403 107.436L193.662 125.912L192.985 130.801C192.873 131.67 192.813 132.579 192.762 133.478C192.701 134.367 192.651 135.236 192.55 136.034C192.317 139.69 192.378 143.347 192.742 146.964C193.086 150.903 193.976 154.762 195.381 158.429C196.898 162.611 199.335 166.379 202.521 169.48C205.848 172.621 209.953 174.874 214.494 176.036C218.357 176.975 222.371 177.319 226.396 177.056C229.187 176.884 231.806 176.652 234.334 176.42C238.228 176.076 241.919 175.743 245.772 175.662C251.253 175.581 256.703 175.914 262.083 176.672C262.285 176.712 262.498 176.672 262.68 176.581C262.862 176.49 263.013 176.329 263.104 176.147C263.195 175.965 263.216 175.753 263.175 175.561C263.125 175.369 263.013 175.197 262.852 175.076C258.331 171.52 253.204 168.752 247.683 166.864C244.427 165.722 241.09 164.803 237.682 164.116L228.065 162.298C225.537 161.833 223.1 161.045 220.824 159.944C219.216 159.126 217.841 157.934 216.84 156.469C214.656 152.772 213.493 148.549 213.442 144.196C213.29 141.569 213.27 138.791 213.29 135.963L213.836 126.902C214.201 120.275 214.079 113.659 213.452 107.103C212.896 100.446 211.703 93.8903 209.882 87.5062C208.315 81.819 205.949 76.4046 202.854 71.4145C202.743 71.2832 202.591 71.1821 202.419 71.1316C202.248 71.0811 202.066 71.0811 201.884 71.1316C201.712 71.1821 201.55 71.2832 201.418 71.4145C201.297 71.5458 201.206 71.7175 201.176 71.8892C199.992 82.9706 198.466 93.3044 196.949 103.669L196.959 103.638Z"

private const val RightBlade =
    "M299.267 240.806C290.297 234.271 282.146 227.816 273.965 221.341C272.964 220.553 271.963 219.755 270.962 218.967L256.39 207.411L252.517 204.391C251.819 203.865 251.061 203.36 250.322 202.865C249.584 202.38 248.866 201.895 248.229 201.411C245.195 199.39 242.01 197.633 238.703 196.138C235.123 194.471 231.361 193.319 227.498 192.713C223.14 191.946 218.67 192.168 214.403 193.38C210.034 194.683 206.04 197.117 202.773 200.461C200.033 203.33 197.737 206.623 195.958 210.239C194.714 212.734 193.612 215.108 192.54 217.411C190.901 220.947 189.334 224.301 187.483 227.664C184.824 232.432 181.81 236.978 178.473 241.251C178.342 241.402 178.261 241.604 178.251 241.817C178.241 242.029 178.301 242.231 178.412 242.402C178.524 242.574 178.696 242.695 178.888 242.756C179.08 242.817 179.282 242.796 179.464 242.716C184.783 240.574 189.738 237.534 194.127 233.705C196.736 231.463 199.194 229.038 201.489 226.442L207.86 219.048C209.518 217.098 211.42 215.391 213.503 213.967C215.01 212.977 216.719 212.391 218.488 212.25C222.766 212.209 226.993 213.3 230.775 215.421C233.121 216.593 235.518 217.957 237.955 219.391L245.499 224.371C251.03 227.988 256.804 231.17 262.771 233.897C268.788 236.736 275.047 238.968 281.459 240.564C287.152 242.039 292.997 242.685 298.852 242.504C299.024 242.473 299.186 242.392 299.317 242.271C299.449 242.15 299.54 241.988 299.58 241.807C299.621 241.625 299.621 241.443 299.57 241.261C299.52 241.089 299.418 240.928 299.277 240.817L299.267 240.806Z"

private const val LeftBlade =
    "M131.359 229.493C121.611 233.352 111.893 237.2 101.699 241.726C101.527 241.786 101.345 241.796 101.163 241.756C100.981 241.716 100.819 241.625 100.688 241.493C100.556 241.362 100.465 241.2 100.415 241.029C100.374 240.857 100.385 240.675 100.445 240.514C103.226 235.321 106.725 230.553 110.871 226.341C115.482 221.563 120.569 217.24 126.06 213.431C131.43 209.603 137.103 206.179 143.029 203.188L151.159 199.128C153.627 197.724 156.023 196.319 158.218 194.875C161.97 192.653 165.044 189.521 167.157 185.774C167.926 184.167 168.27 182.379 168.169 180.571C167.986 178.036 167.451 175.531 166.581 173.096L163.335 163.833C162.222 160.53 161.343 157.166 160.706 153.772C159.573 148.024 159.411 142.186 160.22 136.478C160.24 136.276 160.331 136.094 160.473 135.953C160.614 135.812 160.807 135.731 161.019 135.721C161.221 135.71 161.434 135.761 161.606 135.872C161.777 135.983 161.919 136.155 161.99 136.347C164.033 141.398 166.48 146.307 169.291 151.024C171.293 154.338 173.427 157.378 175.682 160.59C177.148 162.671 178.655 164.833 180.212 167.167C182.468 170.53 184.187 174.197 185.299 178.026C186.573 182.541 186.684 187.248 185.623 191.703C184.531 196.027 182.488 200.027 179.626 203.441C177.159 206.492 174.256 209.199 171.01 211.472C168.057 213.593 164.922 215.482 161.636 217.108C160.888 217.421 160.109 217.805 159.31 218.199C158.501 218.603 157.682 219.007 156.873 219.341L152.302 221.199L134.919 228.068C133.725 228.543 132.532 229.008 131.349 229.483L131.359 229.493Z"
