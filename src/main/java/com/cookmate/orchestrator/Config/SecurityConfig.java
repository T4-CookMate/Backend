package com.cookmate.orchestrator.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowCredentials(true);
        cfg.setAllowedOrigins(List.of(
                "http://localhost:5173"   // Vite
                // 나중에 배포 프론트 도메인 추가
        ));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   CorsConfigurationSource corsConfigurationSource) throws Exception {

        /**
         *  csrf : 사이트 위변조 방지 설정 (스프링 시큐리티에는 자동으로 설정 되어 있음)
         *  csrf 기능 켜져있으면 post 요청을 보낼때 csrf 토큰도 보내줘야 로그인 진행됨 !
         *  개발단계에서만 csrf 잠시 꺼두기
         */
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource));

        /** URL 별로 인가 설정: 위에서 아래로 순서대로 인가 동작 (순서 중요!) */
        http
                .authorizeHttpRequests((auth) -> auth
                        // 로그인하지 않아도 모든 사용자가 접근 가능
                        .requestMatchers("/auth/google", "/ws/**").permitAll()
                        // 위에서 처리하지 않은 나머지 경로에 대한 처리: 로그인만 한다면 모든 사용자가 접근 가능
                        .anyRequest().authenticated()
                );

        /** OAuth 2.0 로그인 방식 설정 */
        http
                .oauth2Login((auth) -> auth
                        .loginPage("/oauth-login/login")
                        .defaultSuccessUrl("/oauth-login")
                        .failureUrl("/oauth-login/login")
                        .permitAll());

        /** 로그아웃 URL 설정 */
        http
                .logout((auth) -> auth
                        .logoutUrl("/logout")             // 로그아웃이 발생하는 페이지 URL
                );

        /** form 로그인 완전 비활성화 */
        http
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}