package com.cookmate.orchestrator.Common.Security.Jwt;

import com.cookmate.orchestrator.Common.Security.AuthenticationContext;
import com.cookmate.orchestrator.User.Entity.User;
import com.cookmate.orchestrator.User.Repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final AuthenticationContext authenticationContext;

    // 화이트리스트: 인증 없이 통과시킬 경로
    private static final AntPathMatcher matcher = new AntPathMatcher();
    private static final String[] WHITELIST = {
            "/auth/google"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        log.info("요청 url: "+path);
        for (String p : WHITELIST) {
            if (matcher.match(p, path)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // 토큰 없는 요청은 그냥 다음 필터로 넘김 (익명 요청)
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7); // "Bearer " 이후

        if (!jwtUtil.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = jwtUtil.getUserId(token);

        // DB에서 유저 조회 (캐시나 UserDetailsService로 바꿀 수도 있음)
        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            filterChain.doFilter(request, response);
            return;
        }

        authenticationContext.setPrincipal(user);
        // 스프링 시큐리티 컨텍스트에 인증 정보 세팅
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user, // principal
                        null,
                        Collections.emptyList() // 권한 필요하면 여기에 ROLE_... 추가
                );

        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}