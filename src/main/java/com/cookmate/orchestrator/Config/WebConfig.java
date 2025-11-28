package com.cookmate.orchestrator.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:5173"  // 로컬 프론트
                        //"http://43.200.xxx.xxx:5173" // 또는 필요 시
                        //"https://your-frontend-domain.com" // 나중에 배포용
                )
                .allowedMethods("*")
                .allowCredentials(true);
    }
}