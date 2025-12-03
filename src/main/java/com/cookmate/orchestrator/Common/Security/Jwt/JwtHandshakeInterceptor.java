package com.cookmate.orchestrator.Common.Security.Jwt;

import com.cookmate.orchestrator.User.Entity.User;
import com.cookmate.orchestrator.User.Repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Collections;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();

            // 1) 토큰 추출: 헤더 → 없으면 쿼리 파람 token 사용
            String token = resolveToken(httpRequest);
            if (token == null) {
                return false; // 인증 없으면 연결 거절
            }

            if (!jwtUtil.validateToken(token)) {
                return false; // 토큰 유효하지 않으면 거절
            }

            Long userId = jwtUtil.getUserId(token); // 너가 이미 쓰는 메서드 있을 거임
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 2) WebSocketSession attributes에 저장
            attributes.put("userId", userId);
            attributes.put("user", user);

            // 3) SecurityContext에도 Authentication 세팅
            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // 쿼리 파라미터에서 recipeId도 같이 가져와서 attributes에 넣어두기
            String recipeIdParam = httpRequest.getParameter("recipeId");
            if (recipeIdParam != null && !recipeIdParam.isBlank()) {
                try {
                    Long recipeId = Long.parseLong(recipeIdParam);
                    attributes.put("recipeId", recipeId);
                } catch (NumberFormatException e) {
                    return false; // 이상한 값이면 연결 거절
                }
            }

        }

        return true;
    }

    private String resolveToken(HttpServletRequest request) {
        // 1) Authorization 헤더에서 Bearer 토큰 추출
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        // 2) 브라우저 WebSocket은 헤더 못 보내니까 쿼리 파람 token=? 도 지원
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isBlank()) {
            return tokenParam;
        }

        return null;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // WebSocket으로 넘어가면 HTTP request/response 끝났으니까
        SecurityContextHolder.clearContext();
    }
}
