package com.cookmate.orchestrator.Auth.Controller;

import com.cookmate.orchestrator.Auth.DTO.GoogleTokenRequest;
import com.cookmate.orchestrator.Auth.DTO.GoogleTokenResponse;
import com.cookmate.orchestrator.Auth.Service.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/auth")
public class OauthController {
    private final GoogleOAuthService googleOauthService;

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleTokenRequest request) {
        GoogleTokenResponse tokenResponse = googleOauthService.requestAccessToken(request.code());

        // 여기서 tokenResponseJson 파싱해서
        // id_token에서 구글 사용자 정보 추출 → 우리 User 매핑 → JWT 발급 이런 식으로 진행

        return ResponseEntity.ok().build();
    }

    /**
     * 테스트용: 백엔드에서 code 직접 받기
     */
    @GetMapping("/google/callback")
    public void googleCallback(@RequestParam String code){
        GoogleTokenResponse tokenResponse = googleOauthService.requestAccessToken(code);

    }
}