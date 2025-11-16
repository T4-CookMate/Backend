package com.cookmate.orchestrator.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                /** WebSocket / API 테스트 편하게 하려고 CSRF 끔 (나중에 필요하면 다시 켜기) */
                .csrf(csrf -> csrf.disable())
                /** 일단 모든 요청 허용 */
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/ws/**").permitAll()  // websocket
                        .anyRequest().permitAll()
                )
                /** 기본 로그인 폼 / httpBasic 비활성화 */
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }
}