package com.example.proj_backend.controller

import com.example.proj_backend.service.OpenAiService
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/openai")
class OpenAiController(private val openAiService: OpenAiService) {

    private val logger = KotlinLogging.logger {}

    @GetMapping("/chat")
    fun chat(@RequestParam prompt: String): String {
        logger.info { prompt }
        return openAiService.chat(prompt)
    }
}