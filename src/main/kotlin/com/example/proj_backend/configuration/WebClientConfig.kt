package com.example.proj_backend.configuration

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig(
    @Value("\${spring.ai.openai.api-key}")
    private val apiKey: String
) {

    @Bean
    fun openAiWebClient(): WebClient {
        println("Using OpenAI API Key of length: ${apiKey.length}") // debug log
        return WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader("Authorization", "Bearer $apiKey")
            .build()
    }
}