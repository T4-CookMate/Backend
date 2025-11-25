package com.cookmate.orchestrator.Auth.Service;

import com.cookmate.orchestrator.Auth.Converter.GoogleTokenResponseConverter;
import com.cookmate.orchestrator.Auth.DTO.GoogleTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final GoogleTokenResponseConverter googleTokenResponseConverter;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String GOOGLE_CLIENT_ID;
    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String GOOGLE_CLIENT_SECRET;
    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String GOOGLE_REDIRECT_URI;
    @Value("${spring.security.oauth2.client.provider.google.token-uri}")
    private String GOOGLE_TOKEN_URI;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 프론트에서 받은 authorization code 로
     * 구글 토큰 엔드포인트에 access_token / id_token 요청
     */
    @Transactional
    public GoogleTokenResponse requestAccessToken(String code) {
        String url = GOOGLE_TOKEN_URI +
                "?code=" + code +
                "&client_id=" + GOOGLE_CLIENT_ID +
                "&client_secret=" + GOOGLE_CLIENT_SECRET +
                "&redirect_uri=" + GOOGLE_REDIRECT_URI +
                "&grant_type=authorization_code";

        HttpEntity<Void> request = new HttpEntity<>(null);

        ResponseEntity<String> response =
                restTemplate.postForEntity(url, request, String.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            String responseBody = response.getBody();
            log.info("요청 완료: {}", responseBody);

            // 여기서 JSON → DTO 변환
            GoogleTokenResponse tokenResponse =
                    googleTokenResponseConverter.toDto(responseBody);

            return tokenResponse;
        }

        throw new IllegalStateException("구글 토큰 발급 실패: " + response.getStatusCode());
    }
}