package com.cookmate.orchestrator.Auth.Controller;

import com.cookmate.orchestrator.Auth.DTO.AuthResponse;
import com.cookmate.orchestrator.Auth.DTO.GoogleTokenRequest;
import com.cookmate.orchestrator.Auth.DTO.GoogleTokenResponse;
import com.cookmate.orchestrator.Auth.DTO.GoogleUserInfo;
import com.cookmate.orchestrator.Auth.Service.GoogleOAuthService;
import com.cookmate.orchestrator.Common.ApiPayload.ApiResponse;
import com.cookmate.orchestrator.Common.ApiPayload.BaseCode;
import com.cookmate.orchestrator.Common.ApiPayload.Status.SuccessStatus;
import com.cookmate.orchestrator.User.Service.UserService;
import com.sun.net.httpserver.Authenticator;
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
    private final UserService userService;

    @PostMapping("/google")
    public ResponseEntity<ApiResponse> googleLogin(@RequestBody GoogleTokenRequest request) {
        GoogleTokenResponse tokenResponse = googleOauthService.requestAccessToken(request.code());
        GoogleUserInfo userInfo = googleOauthService.requestUserInfo(tokenResponse);

        return ResponseEntity.ok(ApiResponse.onSuccess(userService.findOrCreateUser(userInfo)));
    }
}