package com.cookmate.orchestrator.Auth.Service;

import com.cookmate.orchestrator.Auth.Converter.GoogleTokenResponseConverter;
import com.cookmate.orchestrator.Auth.Converter.GoogleUserResponseConverter;
import com.cookmate.orchestrator.Auth.DTO.GoogleTokenResponse;
import com.cookmate.orchestrator.Auth.DTO.GoogleUserInfo;
import com.cookmate.orchestrator.Common.ApiPayload.Status.ErrorStatus;
import com.cookmate.orchestrator.Common.Exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final GoogleTokenResponseConverter googleTokenResponseConverter;
    private final GoogleUserResponseConverter googleUserResponseConverter;

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
        String url = GOOGLE_TOKEN_URI;
        String decodedCode = URLDecoder.decode(code, StandardCharsets.UTF_8);

        // 헤더 설정: form-urlencoded 필수
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // 바디에 파라미터 넣기 (Form URL Encoded)
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("code", decodedCode);
        params.add("client_id", GOOGLE_CLIENT_ID);
        params.add("client_secret", GOOGLE_CLIENT_SECRET);
        params.add("redirect_uri", GOOGLE_REDIRECT_URI); // 인가코드 받을 때 쓴 URI와 완전히 동일해야 함
        params.add("grant_type", "authorization_code");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("구글 토큰 발급 실패 status={}, body={}",
                        response.getStatusCode(), response.getBody());
                throw new GeneralException(
                        ErrorStatus.INVALID_AUTHORIZATION_CODE,
                        "구글 토큰 발급 실패"
                );
            }

            String responseBody = response.getBody();
            GoogleTokenResponse tokenResponse =
                    googleTokenResponseConverter.toDto(responseBody);

            log.info("구글 토큰 발급 성공: {}", tokenResponse);
            return tokenResponse;

        } catch (GeneralException e) {
            // 이미 우리 쪽 도메인 예외면 그대로 던짐
            throw e;
        } catch (Exception e) {
            // RestTemplate 에러, 파싱 에러 등
            log.error("구글 토큰 발급 중 예외 발생", e);
            throw new GeneralException(
                    ErrorStatus._INTERNAL_SERVER_ERROR,
                    "구글 토큰 발급 중 내부 예외"
            );
        }
    }

    @Transactional
    public GoogleUserInfo requestUserInfo(GoogleTokenResponse tokenResponse) {
        String url = "https://openidconnect.googleapis.com/v1/userinfo";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(tokenResponse.accessToken());

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                log.warn("구글 유저 정보 조회 실패 status={}, body={}",
                        response.getStatusCode(), response.getBody());
                throw new GeneralException(
                        ErrorStatus.INVALID_TOKEN,
                        "구글 유저 정보 조회 실패"
                );
            }

            String body = response.getBody();
            log.info("Google userinfo response: {}", body);

            return googleUserResponseConverter.toUserInfoDto(body);

        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            log.error("구글 유저 정보 조회 중 예외 발생", e);
            throw new GeneralException(
                    ErrorStatus._INTERNAL_SERVER_ERROR,
                    "구글 유저 정보 조회 중 내부 예외"
            );
        }
    }

}