package com.example.proj_backend.service

import com.example.proj_backend.data.ChatMessage
import com.example.proj_backend.data.ChatRequest
import com.example.proj_backend.data.ChatResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient


@Service
class OpenAiService(private val openAiWebClient: WebClient) {

    fun chat(prompt: String): String {
        val isValid = requestCheck(prompt)

        if (!isValid) {
            return "Prompt spoza zakresu aplikacji."
        }

        val request = ChatRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(
                ChatMessage(
                    "system",
                    "Jesteś asystentem edukującym na temat etyki w przetwarzaniu danych osobowych. " +
                            "Użytkownicy to przedsiębiorstwa, które wykorzystują różne dane do swoich modeli. " +
                            "Twoim zadaniem jest edukowanie ich na temat anonimizacji, zagrożeń związanych z brakiem anonimizacji oraz związanych z nieodpowiednią anonimizacją. " +
                            "Potrafisz przedstawić odpowiednie metody anonimizacji na podstawie nazwy kolumny danych."
                ),
                ChatMessage("user", prompt)
            )
        )

        val response = openAiWebClient.post()
            .uri("/chat/completions")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ChatResponse::class.java)
            .block()

        return response?.choices?.firstOrNull()?.message?.content ?: "Brak odpowiedzi"
    }

    fun requestCheck(prompt: String): Boolean {
        val request = ChatRequest(
            model = "gpt-3.5-turbo",
            messages = listOf(
                ChatMessage(
                    "system",
                    "Jesteś asystentem edukującym na temat etyki w przetwarzaniu danych osobowych. " +
                            "Twoim zadaniem jest określenie, czy podany prompt dotyczy kwestii anonimizacji danych lub jest pytaniem dotyczącym twojego zastosowania i zadania. " +
                            "Zwracasz TYLKO jeden obiekt JSON w formacie: { \"valid\": true } lub { \"valid\": false }. " +
                            "Nie dodajesz żadnych wyjaśnień ani tekstu poza tym JSON-em."
                ),
                ChatMessage("user", prompt)
            )
        )

        val response = openAiWebClient.post()
            .uri("/chat/completions")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ChatResponse::class.java)
            .block()

        val content = response?.choices?.firstOrNull()?.message?.content?.trim() ?: ""

        val match = Regex("""\{\s*"valid"\s*:\s*(true|false)\s*}""").find(content)
        return match?.groups?.get(1)?.value?.toBooleanStrictOrNull() ?: false
    }


}