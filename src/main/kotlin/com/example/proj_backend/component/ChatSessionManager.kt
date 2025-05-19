package com.example.proj_backend.component

import com.example.proj_backend.data.ChatMessage
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class ChatSessionManager {

    private val sessionContexts: MutableMap<String, MutableList<ChatMessage>> = ConcurrentHashMap()

    fun initSession(userId: String) {
        sessionContexts.putIfAbsent(
            userId, mutableListOf(
                ChatMessage(
                    "system",
                    "Jesteś asystentem edukującym na temat etyki w przetwarzaniu danych osobowych. " +
                            "Użytkownicy to przedsiębiorstwa, które wykorzystują różne dane do swoich modeli. " +
                            "Twoim zadaniem jest edukowanie ich na temat anonimizacji, zagrożeń związanych z brakiem anonimizacji oraz związanych z nieodpowiednią anonimizacją. " +
                            "Potrafisz przedstawić odpowiednie metody anonimizacji na podstawie nazwy kolumny danych." +
                            "W odpowiedziach nie używaj funkcji formatujących tekst, jak np. **"
                ),
            )
        )
    }

    fun getChatHistory(userId: String): List<ChatMessage> {
        return sessionContexts[userId] ?: emptyList()
    }

    fun addMessage(userId: String, userMessage: String, assistantReply: String) {
        val context = sessionContexts.computeIfAbsent(userId) { mutableListOf() }
        context.add(ChatMessage("user", userMessage))
        context.add(ChatMessage("assistant", assistantReply))
    }

    fun resetSession(userId: String) {
        sessionContexts[userId] = mutableListOf(
            ChatMessage(
                role = "system",
                content = "Jesteś asystentem edukującym na temat etyki w przetwarzaniu danych osobowych."
            )
        )
    }
}