package com.solomondesign.app.ui.splash

/**
 * Launch-brand treatments for A/B testing. The selected variant plays on cold start
 * and can be replayed from Tools → Demo.
 *
 * [revealsHome] variants keep Today mounted underneath and unblur into it.
 */
enum class SplashVariant(
    val title: String,
    val subtitle: String,
    val revealsHome: Boolean = false,
) {
    DEPTH(
        title = "Depth",
        subtitle = "Parallax mark, then blur into Today",
        revealsHome = true,
    ),
    MORPH(
        title = "Morph",
        subtitle = "Blades resolve, letters rise through a mask",
        revealsHome = true,
    ),
    SPLIT(
        title = "Split",
        subtitle = "GSAP-style characters unmask in sequence",
    ),
    BLOOM(
        title = "Bloom",
        subtitle = "Emissive mark, scrambled wordmark, then home",
        revealsHome = true,
    ),
    ASSEMBLE(
        title = "Assemble",
        subtitle = "Three blades lock, letters split in",
    ),
    SIGNATURE(
        title = "Signature",
        subtitle = "Glyphs scramble, then lock",
    ),
    IRIS(
        title = "Iris",
        subtitle = "Orange field opens onto split type",
    ),
}
