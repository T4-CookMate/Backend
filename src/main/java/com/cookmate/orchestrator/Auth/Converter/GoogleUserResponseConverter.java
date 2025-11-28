package com.cookmate.orchestrator.Auth.Converter;

import com.cookmate.orchestrator.Auth.DTO.GoogleTokenResponse;
import com.cookmate.orchestrator.Auth.DTO.GoogleUserInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GoogleUserResponseConverter {

    private final ObjectMapper objectMapper;

    /**
     * 구글 토큰 JSON 문자열을 GoogleTokenResponse DTO로 변환
     */
    public GoogleUserInfo toUserInfoDto(String json) {
        try {
            return objectMapper.readValue(json, GoogleUserInfo.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Google user 정보 JSON 파싱 실패: " + e.getMessage(), e);
        }
    }

}