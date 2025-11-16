package com.cookmate.orchestrator.Config;

import com.microsoft.cognitiveservices.speech.SpeechConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class AzureSpeechConfig {

    @Bean
    public SpeechConfig speechConfig(
            @Value("${azure.speech.key}") String key,
            @Value("${azure.speech.region}") String region
    ) {
        SpeechConfig config = SpeechConfig.fromSubscription(key, region);
        config.setSpeechRecognitionLanguage("ko-KR");
        return config;
    }
}
