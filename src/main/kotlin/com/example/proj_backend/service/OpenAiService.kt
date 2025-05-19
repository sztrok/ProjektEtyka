package com.example.proj_backend.service

import com.example.proj_backend.component.ChatSessionManager
import com.example.proj_backend.data.ChatMessage
import com.example.proj_backend.data.ChatRequest
import com.example.proj_backend.data.ChatResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient


@Service
class OpenAiService(
    private val chatSessionManager: ChatSessionManager,
    private val openAiWebClient: WebClient,
) {

    private val logger = KotlinLogging.logger {}

    fun testOpenAiConnection(): String {
        val response = openAiWebClient.get()
            .uri("/models")
            .retrieve()
            .bodyToMono(String::class.java)
            .block()
        return "Response from OpenAI: $response"
    }

    fun chat(userId: String, prompt: String): String {

        chatSessionManager.initSession(userId)
        val messages = chatSessionManager.getChatHistory(userId).toMutableList()

        val isValid = requestCheck(prompt)

        if (!isValid) {
            return "Prompt spoza zakresu aplikacji."
        }


        messages.add(ChatMessage("user", prompt))

        val request = ChatRequest(
            model = "gpt-3.5-turbo",
            messages = messages
        )

        val response = openAiWebClient.post()
            .uri("/chat/completions")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ChatResponse::class.java)
            .block()

        val responseMessage = response?.choices?.firstOrNull()?.message?.content ?: "Brak odpowiedzi"
        chatSessionManager.addMessage(userId, prompt, responseMessage)
        return responseMessage
    }

    private fun requestCheck(prompt: String): Boolean {
        val request = ChatRequest(
            model = "gpt-3.5-turbo",
            messages = mutableListOf(
                ChatMessage(
                    "system", """
                    Jesteś asystentem edukującym na temat etyki w przetwarzaniu danych osobowych. 
                    Użytkownicy to firmy, które chcą dowiedzieć się więcej o takich tematach jak:
                    - anonimizacja danych,
                    - zagrożenia związane z brakiem anonimizacji,
                    - ochrona danych osobowych,
                    - etyczne wykorzystanie danych.
                    
                    Twoim zadaniem jest sprawdzenie, w jakim stopniu podana wiadomość dotyczy co najmniej jednego z poniższych tematów:
                    1. anonimizacja danych,
                    2. ochrona danych osobowych,
                    3. etyka przetwarzania danych,
                    4. prośba o więcej informacji,
                    5. doprecyzowanie informacji,
                    6. pytanie o Twoje zadanie lub zastosowanie,
                    7. powitanie (np. „Cześć”, „Dzień dobry”).
                    
                    Weź pod uwagę kontekst poprzednie wiadomosci i odpowiedzi.

                    Zwróć **wyłącznie** procentową pewność jako liczbę całkowitą od 0 do 100, w formacie:
                    { "confidence": XX }

                    Nie dodawaj żadnych dodatkowych komentarzy ani tekstu. Tylko czysty JSON.
                """.trimIndent()
                ),
                ChatMessage("user", prompt),
            )
        )

        val response = openAiWebClient.post()
            .uri("/chat/completions")
            .bodyValue(request)
            .retrieve()
            .bodyToMono(ChatResponse::class.java)
            .block()

        val content = response?.choices?.firstOrNull()?.message?.content?.trim() ?: ""

        val match = Regex("""\{\s*"confidence"\s*:\s*(\d{1,3})\s*}""").find(content)
        val confidence = match?.groups?.get(1)?.value?.toIntOrNull() ?: return false

        logger.info { "CONFIDENCE: $confidence" }

        return confidence >= 60
    }


}