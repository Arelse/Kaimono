package eu.kanade.domain.ui.model

/** Library grid card style (applies to the comfortable grid display mode). */
enum class LibraryCardStyle {
    /** Container card with cover and title. */
    DEFAULT,

    /** Clean vertical poster with the badge in the bottom-right corner. */
    SAIKOU,

    /** Bordered card with a glowing shadow and a colored title banner. */
    EXOTIC,

    /** Bordered card with the title over a bottom gradient. */
    MINIMAL_EXOTIC,
}

/** History list card style. */
enum class HistoryCardStyle {
    /** Classic row with cover, title, chapter and date. */
    REGULAR,

    /** Row over a blurred, tinted copy of the cover. */
    FROSTED_GLASS,

    /** Large poster banner on top, details below. */
    BOOTIFUL,
}
