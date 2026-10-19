package com.jarvis.assistant.ui.island

/** Android counterpart of the macOS island presentation state. */
enum class IslandMode { HIDDEN, COMPACT, EXPANDED }

/** Stable views used by the overlay and widget status renderer. */
enum class IslandView {
    OVERVIEW, EMPTY, LISTENING, THINKING, SPEAKING, QUESTION, ERROR, FINISHED,
    SEARCHING, APPROVAL, GREETING
}

/** Mood/emote shown beside the compact pill without relying on emoji glyphs. */
enum class IslandEmote(val assetName: String) {
    CALM("greet"), LISTENING("peek"), THINKING("think"), PROUD("proud"),
    QUESTION("question"), ERROR("error"), FINISHED("finish"), TIRED("yawn"), LOVE("love")
}

data class IslandPresentation(
    val mode: IslandMode,
    val view: IslandView,
    val emote: IslandEmote,
    val title: String,
    val message: String
)
