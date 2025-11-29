package com.cookmate.orchestrator.User.Service;

import com.cookmate.orchestrator.Auth.DTO.AuthResponse;
import com.cookmate.orchestrator.Auth.DTO.GoogleUserInfo;
import com.cookmate.orchestrator.Common.Security.Jwt.JwtUtil;
import com.cookmate.orchestrator.User.Entity.User;
import com.cookmate.orchestrator.User.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /**
     * 구글 사용자 정보 -> 기존 사용자인지 새로운 사용자인지 확인하여 알맞는 서비스 호출
     */
    @Transactional
    public AuthResponse findOrCreateUser(GoogleUserInfo googleUserInfo) {
        User user =  userRepository.findByEmail(googleUserInfo.email())
                .orElseGet(() -> createUser(googleUserInfo));

        String accessToken = jwtUtil.generateAccessToken(user.getId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId());

        // 3) 로그인 응답 DTO 구성
        return new AuthResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getProfileImageUrl(),
                accessToken,
                refreshToken
        );
    }

    /**
     * 새로운 사용자: 사용자 등록
     */
    private User createUser(GoogleUserInfo googleUserInfo){
        User user = User.builder()
                .email(googleUserInfo.email())
                .name(googleUserInfo.name())
                .profileImageUrl(googleUserInfo.picture())
                .provider("google")
                .providerId(googleUserInfo.sub())
                .build();

        return userRepository.save(user);
    }
}