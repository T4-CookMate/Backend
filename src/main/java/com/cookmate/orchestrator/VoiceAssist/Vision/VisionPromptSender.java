package com.cookmate.orchestrator.VoiceAssist.Vision;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
@RequiredArgsConstructor
public class VisionPromptSender {

    private final RestTemplate restTemplate;

    @Value("${vision.prompt-url}")
    private String baseUrl;

    @Value("${vision.prompt-path}")
    private String promptPath;

    @Async("visionExecutor")
    public void send(Long recipeId, String prompt) {
        try {
            String url = baseUrl + promptPath;
            log.info("POST -> {}", url);
            VisionPromptRequest req = new VisionPromptRequest(recipeId, prompt);

            // 인증/헤더 없이 그냥 JSON POST
            restTemplate.postForEntity(url, req, Void.class);

            log.info("Sent vision prompt. recipeId={}", recipeId);
        } catch (Exception e) {
            log.error("Failed to send vision prompt. recipeId={}", recipeId, e);
        }
    }
}
