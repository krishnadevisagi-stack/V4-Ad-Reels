package com.example.data.adengine

/**
 * MetadataPresentationConfiguration
 * Contains configuration options for the advertisement rendering system.
 * Keeps header styles, font sizes, margins, animations, and fallbacks configurable.
 */
data class MetadataPresentationConfiguration(
    val headerStyle: String = "CHIP_TAGGED", // "CHIP_TAGGED", "MINIMALIST", "BOXED"
    val titleFontSizeSp: Int = 14,
    val bodyFontSizeSp: Int = 12,
    val labelFontSizeSp: Int = 10,
    val cardSpacingDp: Int = 16,
    val innerPaddingDp: Int = 12,
    val animateTransitions: Boolean = true,
    val transitionDurationMs: Int = 300,
    val showPlaceholderOnLoading: Boolean = true,
    val defaultFallbackText: String = "Sponsored Advertisement",
    val defaultCtaFallbackText: String = "Learn More",
    val forceNonClickableHeader: Boolean = true,
    val categoryDisplayEnabled: Boolean = true
) {
    companion object {
        val Default = MetadataPresentationConfiguration()
    }
}
