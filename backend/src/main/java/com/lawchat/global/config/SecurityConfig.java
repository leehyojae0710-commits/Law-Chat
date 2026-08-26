package com.lawchat.global.config;

import com.lawchat.global.security.JwtAuthenticationEntryPoint;
import com.lawchat.global.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정 (Spring Security 6.x / Spring Boot 3.x 기준).
 *
 * 예전 방식인 WebSecurityConfigurerAdapter 상속은 제거되었고,
 * 이제는 SecurityFilterChain 을 빈으로 등록하는 방식만 사용한다.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
    }

    /**
     * 비밀번호 암호화기.
     *
     * BCrypt 는 같은 비밀번호라도 매번 다른 해시를 만든다(내부에 랜덤 salt 포함).
     * 그래서 "암호화해서 비교"가 아니라 반드시 encoder.matches(평문, 저장된해시) 로 검증해야 한다.
     * 또한 의도적으로 느리게 설계되어 무차별 대입 공격에 강하다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF: 세션 쿠키 기반이 아닌 JWT 방식이므로 불필요 → 비활성화
                .csrf(csrf -> csrf.disable())

                // CORS 설정 적용 (프론트가 다른 포트/도메인일 때 필요)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // 세션을 아예 만들지 않는다. 인증 상태는 오직 토큰으로만 판단(무상태).
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 폼 로그인 / 기본 인증 팝업 비활성화 (REST API 이므로)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // 인증 실패 시 JSON 401 응답
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))

                // URL 별 접근 권한
                .authorizeHttpRequests(auth -> auth
                        // CORS 사전 요청(Preflight)은 무조건 허용해야 한다
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 로그인 전에 호출해야 하는 API 들은 열어둔다
                        .requestMatchers(
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/auth/kakao",
                                "/api/auth/naver",
                                "/api/auth/naver/state",
                                "/api/users/check-email",
                                "/api/users/check-nickname"
                        ).permitAll()

                        // 판례 북마크(저장)는 로그인 필요 — 아래 permitAll 규칙보다 먼저 와야 우선 적용된다.
                        .requestMatchers(HttpMethod.GET, "/api/precedents/bookmarks").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/precedents/*/bookmark").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/precedents/*/bookmark").authenticated()

                        // 공지사항/판례 조회 등 비로그인 열람 허용 영역
                        .requestMatchers(HttpMethod.GET, "/api/notices/**", "/api/precedents/**").permitAll()

                        // 개발 편의용
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/actuator/health").permitAll()

                        // 관리자 전용
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 나머지는 전부 로그인 필요
                        .anyRequest().authenticated()
                )

                // ★ JWT 필터를 아이디/비밀번호 인증 필터 "앞"에 끼워 넣는다.
                //   그래야 컨트롤러에 도달하기 전에 SecurityContext 에 사용자 정보가 채워진다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 설정.
     * 프론트 개발 서버 주소를 allowedOrigins 에 넣어야 브라우저가 응답을 차단하지 않는다.
     * 실제 배포 시에는 운영 도메인으로 교체할 것. ("*" 는 인증 헤더와 함께 쓸 수 없다)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L); // Preflight 결과 캐시 시간(초)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
