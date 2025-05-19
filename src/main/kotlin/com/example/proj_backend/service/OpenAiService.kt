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

        val lowerPrompt = prompt.lowercase().trim()

        if (isGreeting(lowerPrompt)) {
            val greeting = getGreetingResponse()
            chatSessionManager.addMessage(userId, prompt, greeting)
            return greeting
        }

        if (isAskingAboutPurpose(lowerPrompt)) {
            val purposeExplanation = getChatbotPurposeExplanation()
            chatSessionManager.addMessage(userId, prompt, purposeExplanation)
            return purposeExplanation
        }

        val isValid = requestCheck(userId, prompt, messages)

        if (!isValid) {
            val rejectionMessage = "Przepraszamy, ale Twoje pytanie wykracza poza zakres tej aplikacji. Ten asystent specjalizuje się w tematach związanych z ochroną danych osobowych, ich anonimizacją i etyką przetwarzania. Czy mogę pomóc Ci w tych obszarach?"
            logger.warn { "Request rejected for user $userId with prompt: \"${prompt.take(50)}...\"" }
            chatSessionManager.addMessage(userId, prompt, rejectionMessage)
            return rejectionMessage
        }

        messages.add(ChatMessage("user", prompt))

        if (messages.none { it.role == "system" }) {
            messages.add(0, ChatMessage(
                "system", """
            Jesteś asystentem specjalizującym się w obszarze ochrony danych osobowych i etyki przetwarzania danych.
            Dostarczasz profesjonalnych, rzeczowych i dokładnych informacji na tematy:
            - Anonimizacja i pseudonimizacja danych
            - RODO i inne przepisy o ochronie danych osobowych
            - Bezpieczeństwo danych i zapobieganie naruszeniom
            - Etyczne aspekty przetwarzania danych
            - Dobre praktyki w zakresie ochrony prywatności
            
            Twoje odpowiedzi powinny być:
            - Informacyjne i edukacyjne
            - Oparte na aktualnych regulacjach i standardach
            - Dostosowane do potrzeb firm i organizacji
            - Praktyczne i możliwe do zastosowania
            
            Unikaj odpowiedzi na pytania niezwiązane z tematyką ochrony danych osobowych.
            """
            ))
        }

        val request = ChatRequest(
            model = "gpt-3.5-turbo",
            messages = messages
        )

        try {
            val response = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse::class.java)
                .block()

            val responseMessage = response?.choices?.firstOrNull()?.message?.content ?: "Brak odpowiedzi"
            chatSessionManager.addMessage(userId, prompt, responseMessage)
            return responseMessage
        } catch (e: Exception) {
            logger.error(e) { "Error getting response from OpenAI for user $userId" }
            val errorMessage = "Przepraszamy, wystąpił problem z uzyskaniem odpowiedzi. Proszę spróbować ponownie za chwilę."
            chatSessionManager.addMessage(userId, prompt, errorMessage)
            return errorMessage
        }
    }

    private fun isGreeting(input: String): Boolean {
        val greetingPatterns = listOf(
            "cześć", "czesc", "hej", "siema", "dzień dobry", "dzien dobry",
            "dobry wieczór", "dobry wieczor", "witaj", "witam", "halo", "hi", "hello"
        )

        return greetingPatterns.any { greeting ->
            input == greeting ||
                    input.startsWith("$greeting ") ||
                    input.contains(" $greeting ") ||
                    input.endsWith(" $greeting")
        }
    }

    private fun isAskingAboutPurpose(input: String): Boolean {
        val purposePatterns = listOf(
            "co potrafisz", "co umiesz", "czym się zajmujesz", "czym sie zajmujesz",
            "jakie jest twoje zadanie", "jakie masz zadanie", "do czego służysz",
            "do czego sluzysz", "w czym możesz pomóc", "w czym mozesz pomoc",
            "jak możesz mi pomóc", "jak mozesz mi pomoc", "co robisz", "kim jesteś",
            "kim jestes", "o co mogę cię zapytać", "o co moge cie zapytac",
            "czego mogę się dowiedzieć", "czego moge sie dowiedziec", "w czym mi pomożesz",
            "jaka jest twoja rola", "jaka jest twoja funkcja"
        )

        return purposePatterns.any { pattern ->
            input.contains(pattern)
        }
    }

    private fun getGreetingResponse(): String {
        val greetings = listOf(
            "Dzień dobry! W czym mogę pomóc odnośnie ochrony danych osobowych?",
            "Witam! Jestem tu, aby pomóc w kwestiach związanych z etyką przetwarzania danych. Jak mogę Ci pomóc?",
            "Cześć! Chętnie odpowiem na Twoje pytania dotyczące anonimizacji i ochrony danych osobowych.",
            "Witaj! Służę pomocą w tematach związanych z RODO i bezpieczeństwem danych osobowych. W czym mogę pomóc?"
        )

        return greetings.random()
    }

    private fun getChatbotPurposeExplanation(): String {
        return """
        Jestem asystentem specjalizującym się w tematach związanych z ochroną danych osobowych i etycznym przetwarzaniem informacji.
        
        Mogę Ci pomóc w następujących obszarach:
        
        - Anonimizacja i pseudonimizacja danych osobowych
        - Przepisy RODO i inne regulacje dotyczące ochrony danych
        - Bezpieczeństwo danych i zapobieganie wyciekom
        - Etyczne aspekty przetwarzania danych osobowych
        - Dobre praktyki w zakresie "privacy by design"
        - Procedury i polityki ochrony danych
        
        Możesz zadawać mi pytania dotyczące powyższych tematów, a ja postaram się udzielić Ci rzetelnych informacji i wskazówek.
        W czym konkretnie mogę Ci dziś pomóc?
    """.trimIndent()
    }

    private fun requestCheck(userId: String, prompt: String, chatHistory: List<ChatMessage>): Boolean {
        try {
            val messages = chatHistory.toMutableList()

            messages.add(0, ChatMessage(
                "system", """
            Jesteś modułem filtrującym dla czatbota edukującego na temat etyki w przetwarzaniu danych osobowych.
            Twoim **jedynym zadaniem** jest ocena, czy ostatnia wiadomość użytkownika powinna zostać przetworzona.
            
            Główne tematy, których dotyczy bot:
            - Anonimizacja i pseudonimizacja danych
            - RODO i przepisy o ochronie danych osobowych
            - Bezpieczeństwo danych i zagrożenia związane z wyciekami
            - Etyka wykorzystania danych osobowych
            
            Oceń wiadomość użytkownika na skali od 0 do 100, gdzie:
            - 0-59: Wiadomość niezwiązana z tematami ochrony danych
            - 60-100: Wiadomość związana z tematami ochrony danych lub jest naturalną częścią konwersacji
            
            Odpowiedz używając WYŁĄCZNIE tego formatu JSON:
            {"confidence": LICZBA}
            
            Gdzie LICZBA to wartość od 0 do 100.
            Nie dodawaj żadnych wyjaśnień, komentarzy ani innych treści.
        """.trimIndent()
            ))

            messages.add(ChatMessage("user", prompt))

            logger.info { "Validating message for user $userId: \"${prompt.take(50)}...\"" }

            val request = ChatRequest(
                model = "gpt-3.5-turbo",
                messages = messages,
            )

            val response = openAiWebClient.post()
                .uri("/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatResponse::class.java)
                .block()

            if (response == null) {
                logger.error { "Null response from OpenAI API while validating message for user $userId" }
                return true
            }

            val content = response.choices.firstOrNull()?.message?.content?.trim() ?: ""
            logger.info { "Raw validation response: $content" }

            val confidenceValue = extractConfidenceValue(content)

            if (confidenceValue != null) {
                logger.info { "CONFIDENCE for user $userId: $confidenceValue" }
                return confidenceValue >= 60
            } else {
                logger.warn { "Failed to parse confidence value from response: $content" }

                val acceptanceKeywords = listOf("valid", "appropriate", "relevant", "related", "yes", "acceptable")
                val rejectionKeywords = listOf("invalid", "inappropriate", "irrelevant", "unrelated", "no", "reject")

                val contentLower = content.lowercase()
                val hasAcceptanceKeywords = acceptanceKeywords.any { contentLower.contains(it) }
                val hasRejectionKeywords = rejectionKeywords.any { contentLower.contains(it) }

                return when {
                    hasAcceptanceKeywords && !hasRejectionKeywords -> {
                        logger.info { "Message accepted based on keywords" }
                        true
                    }
                    hasRejectionKeywords && !hasAcceptanceKeywords -> {
                        logger.info { "Message rejected based on keywords" }
                        false
                    }
                    else -> {
                        logger.info { "Could not determine confidence, defaulting to accept" }
                        true
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error in requestCheck for user $userId" }
            return true
        }
    }

    /**
     * Attempts to extract the confidence value from the response using multiple patterns
     */
    private fun extractConfidenceValue(content: String): Int? {
        val jsonPattern = """\{\s*"confidence"\s*:\s*(\d{1,3})\s*\}""".toRegex()
        jsonPattern.find(content)?.groups?.get(1)?.value?.toIntOrNull()?.let { return it }

        val numberOnlyPattern = """^\s*(\d{1,3})\s*$""".toRegex()
        numberOnlyPattern.find(content)?.groups?.get(1)?.value?.toIntOrNull()?.let { return it }

        try {
            val jsonObject = org.json.JSONObject(content)
            if (jsonObject.has("confidence")) {
                return jsonObject.getInt("confidence")
            }
        } catch (e: Exception) {
            // Ignore JSON parsing errors and try other methods
        }

        val colonPattern = """confidence\s*[:=]\s*(\d{1,3})""".toRegex(RegexOption.IGNORE_CASE)
        colonPattern.find(content)?.groups?.get(1)?.value?.toIntOrNull()?.let { return it }

        val phrasePattern = """confidence\s+(?:is|equals|score|value|rating)\s+(\d{1,3})""".toRegex(RegexOption.IGNORE_CASE)
        phrasePattern.find(content)?.groups?.get(1)?.value?.toIntOrNull()?.let { return it }

        return null
    }
}