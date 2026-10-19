package com.jarvis.assistant.ui.island

import com.jarvis.assistant.data.model.ConversationState

/** Maps live assistant output to the presentation states used by the island. */
object IslandStateMachine {
    fun classify(state: ConversationState, userText: String, assistantText: String): IslandView {
        val text = "$userText $assistantText".lowercase()
        return when {
            text.contains("onay") || text.contains("izin gerekli") || text.contains("approval") -> IslandView.APPROVAL
            text.contains("hata") || text.contains("başarısız") || text.contains("error") -> IslandView.ERROR
            text.contains("emin misin") || text.contains("soru") || text.contains("seç") -> IslandView.QUESTION
            text.contains("aranıyor") || text.contains("arama yapılıyor") || text.contains("search") -> IslandView.SEARCHING
            state == ConversationState.LISTENING -> IslandView.LISTENING
            state == ConversationState.THINKING -> IslandView.THINKING
            state == ConversationState.SPEAKING -> IslandView.SPEAKING
            state == ConversationState.IDLE && assistantText.isNotBlank() -> IslandView.FINISHED
            else -> IslandView.OVERVIEW
        }
    }
}
