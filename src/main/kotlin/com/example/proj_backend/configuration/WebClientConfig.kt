package com.example.proj_backend.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {

    private companion object {
        const val API_KEY =
            "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"
    }

    @Bean
    fun openAiWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader("Authorization", "Bearer $API_KEY")
            .build()
    }
}